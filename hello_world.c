#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <system.h>
#include <string.h>
#include "io.h"
#include "inttypes.h"
#include "altera_up_avalon_character_lcd.h"
#include "altera_up_avalon_parallel_port.h"
#include "altera_up_sd_card_avalon_interface.h"
#include "altera_up_avalon_audio_and_video_config.h"
#include "altera_up_avalon_audio.h"
#include "altera_up_avalon_rs232.h"
#include "sys/alt_irq.h"

/* Define I/O ports */
#define switches (int) 0x00004420
#define leds (char *) 0x00004430
#define keys (int) 0x00004440

/* Define states */
#define STOP 0
#define PAUSED 1
#define PLAYING_NORMAL 2
#define NEXT_PLAY 3

/* Constants for high level song stuff */
const int MAX_NUMBER_SONGS = 2;
const int MAX_STRING_SIZE = 7;
const int MAX_NUM_SONGS = 100;
const int MAX_DIGIT_OF_MAX_NUM_SONG = 4; // has to include memory for null character
const int EXTENSION_LENGTH = 4;

/* Constants for SongDetail; the length includes the null character */
const int ID_LENGTH = 5;	// id length includes the carriage return character in addition
const int NAME_LENGTH = 26;
const int ARTIST_LENGTH = 21;
const int RATING_LENGTH = 3;

//const int SONG_SIZE = 2000;
//const int SONG_SIZE = 32766;	//around 0.5 second long
const int SONG_SIZE = 65534;	//around 1 second long
//const int SONG_SIZE = 131068;	//around 2 second long
//const int SONG_SIZE = 196602;	//around 3 seconds long
const int WAV_HEADER_SIZE = 44;
const int SAMPLE_SIZE = 96;
int song_index;	// 0 <= song_count < SONG_SIZE
unsigned int song_wav[2];
unsigned int song_sample[96];
//unsigned int* streamA;
//unsigned int* streamB;
unsigned int streamA[65534];
unsigned int streamB[65534];
int streamA_size;
int streamB_size;
int stream_flag;
int state;
int currSong;
int shuffle_flag;
int volume;
int aa;

typedef struct {
	char* id;
	char* name;
	char* artist;
	char* rating;
} SongDetail;

/* Device references */
alt_up_sd_card_dev *sd_card_reference;
alt_up_av_config_dev * av_config;
alt_up_audio_dev * audio;
alt_up_character_lcd_dev * char_lcd_dev;
alt_up_rs232_dev * uart;

/* Function prototypes */

/* Initialization */
void initialization();

/* SD Functions */
char openFileInSD( char* fileName, short int* file_handle );
char closeFileInSD( short int file_handle );
char readACharFromSD( short int file_handle );

/* Read song list functions */
SongDetail** getListOfSongDetails();
char initializeSongDetail( SongDetail* song );
SongDetail* readDetailForOneSong( short int file_handle );
char readWordFromSD( char* name, const int length, short int file_handle );

/* Send song list functions */
void sendSongListToMiddleMan( SongDetail** songList, int numSong );
void sendOneSongDetailToMiddleMan( SongDetail* song );

/* DE2 to MiddleMan function */
void sendStringToMiddleMan( char* str );

/* MiddleMan to DE2 function */
void getSongListFromMiddleManAndPrintForDebuggingPurpose();
char* getWordFromMiddleMan();
unsigned char getByteFromMiddleMan();
int isThereSomething();
void clearMiddleManBuffer();

/* Song Functions */
int playSong( short int file_handle );
void stopSong( short int file_handle );
void nextSong();
void audio_isr (void * context, unsigned int irq_id);
int streamSong( short int file_handle );
int findSong( SongDetail** list, int numSongs, char* id );

/* Update */
void updateStateFromKeys();
void updateStateFromUART();
void updateState();
void updateVolume();

int main()
{
	initialization();

	short int file_handle;
	currSong = 0;
	shuffle_flag = 0;
	volume = 0;

	char* message;
	while ( !isThereSomething() )
	{
		message = getWordFromMiddleMan();
		printf( "%s.\n", message );
		break;
	}

	/*
	 * 0 stop
	 * 1 paused
	 * 2 playing normal
	 * 3 next and play
	*/
	state = STOP;

	int numSongs;
	SongDetail** songDetailList = getListOfSongDetails( &numSongs );
	sendSongListToMiddleMan( songDetailList, numSongs );

	int cc;
	for(cc = 0; cc < numSongs; cc++)
		printf("%s\n", songDetailList[cc]->id );

	printf( "The song found is at index: %d.\n", findSong( songDetailList, numSongs, "02" ) );

	while(1)
	{
		if(state == STOP)
		{
			alt_up_character_lcd_init(char_lcd_dev);
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "STOP   ");
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
			alt_up_character_lcd_string(char_lcd_dev, songDetailList[currSong]->name);

			updateState();
		}
		else if(state == PLAYING_NORMAL)
		{
			alt_up_character_lcd_init(char_lcd_dev);
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
			alt_up_character_lcd_string(char_lcd_dev, songDetailList[currSong]->name);

			char temp[ID_LENGTH + EXTENSION_LENGTH];
			strcpy( temp, songDetailList[currSong]->id );
			strcat( temp, ".wav" );
			if( openFileInSD( temp, &file_handle ) == 0)
			{
				// skip the header
				for(cc = 0; cc < WAV_HEADER_SIZE; cc++)
					alt_up_sd_card_read( file_handle );
				printf("file opened\n");
			}
			else
			{
				printf("failed to open wav\n");
				exit(1);
			}

			playSong( file_handle );
			stopSong( file_handle );

			if(state == NEXT_PLAY || state == PLAYING_NORMAL)
			{
				nextSong();
				state = PLAYING_NORMAL;
			}
		}
	}

	return 0;
}

/*
 * read keys to update the current state
 * used while playing
 */
void updateStateFromKeys()
{
	char key = IORD_8DIRECT(keys, 0);
	while(IORD_8DIRECT(keys, 0) != 0);

	if(key == 0x8)	//play or paused
	{
		if(state == PLAYING_NORMAL)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PAUSED ");
			alt_irq_disable(AUDIO_0_IRQ);
			state = PAUSED;
		}
		else if(state == PAUSED)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
		}
	}
	else if(key == 0x4)	//stop
	{
		state = STOP;
	}
	else if(key == 0x2)	//next
	{
		//state = NEXT_PLAY;
		sendStringToMiddleMan("T");
	}
}

/*
 * read uart to update the current state
 * used while playing
 * precondition: there is data in the middleman
 */
void updateStateFromUART()
{
	char* temp = getWordFromMiddleMan();

	if( strcmp(temp, "P") == 0 )	//play or paused
	{
		if(state == PLAYING_NORMAL)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PAUSED ");
			alt_irq_disable(AUDIO_0_IRQ);
			state = PAUSED;
		}
		else if(state == PAUSED)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
		}
	}
	else if( strcmp(temp, "S") == 0 )	//stop
	{
		state = STOP;
	}
	else if( strcmp(temp, "N") == 0 )	//next
	{
		state = NEXT_PLAY;
	}
}

/*
 * read keys and uart to update the current state
 */
void updateState()
{
	char* temp = "S";
	char key;

	if( state == STOP)
	{
		key = 0;
		while(key == 0)
		{
			// wait for command
			key = IORD_8DIRECT(keys, 0);
			updateVolume();

			if ( isThereSomething() )
			{
				printf ( "start there is a duck\n" );
				temp = getWordFromMiddleMan();
				break;
			}
		}
		while(IORD_8DIRECT(keys, 0) != 0);

		if(key == 0x8 || strcmp(temp, "P") == 0)	//play
			state = PLAYING_NORMAL;
		else if(key == 0x4)	//stop
		{
			// current state is STOP
			// do nothing~
		}
		else if(key == 0x2 || strcmp(temp, "N") == 0)
		{
			nextSong();
		}
	}
	else
	{
		updateVolume();
		updateStateFromKeys();
		if ( isThereSomething() )
		{
			printf ( "there is something\n" );
			updateStateFromUART();
		}
	}
}

/*
 * read switches and change volume
 */
void updateVolume()
{
	char sw = IORD_8DIRECT(switches, 0);
	volume = sw;
}

/*
 * play a song
 * stops when KEY3 is pressed
 * return 1 if the song stopped before ending (user wants to play the next song)
 * otherwise 0
 */
int playSong( short int file_handle )
{
	alt_irq_disable(AUDIO_0_IRQ);

	int isLastSecond;
	int prev_stream_flag;

	streamA_size = SONG_SIZE;
	streamB_size = SONG_SIZE;
	song_index = 0;

	stream_flag = 1;
	streamSong( file_handle );	//save to stream A
	stream_flag = 0;	//play stream A

	prev_stream_flag = stream_flag;
	isLastSecond = 0;

	alt_irq_enable(AUDIO_0_IRQ);

	while(1)
	{
		//printf("where are u\n");
		if(state == STOP)
		{
			//printf("why why why\n");
			break;
		}
		else if(state == PLAYING_NORMAL)
		{
			isLastSecond = streamSong( file_handle );
			//printf("isLastSecond : %d\n", isLastSecond);
			while(stream_flag == prev_stream_flag)
				if(state == STOP || state == NEXT_PLAY)
					stream_flag = !prev_stream_flag;
			prev_stream_flag = stream_flag;
		}
		else if(state == NEXT_PLAY)	//next is pressed
		{
			return 1;
		}

		if(isLastSecond)
		{
			printf("last sec\n");
			// play the last second
			while(stream_flag == prev_stream_flag);
			break;
		}
		//printf("where am i\n");
	}

	//printf("hstop\n");
	return 0;
}

/*
 * Audio interrupt handler
 * which is used to play a song
 * play stream A when stream_flag == 0
 * otherwise play stream B
 */
void audio_isr (void * context, unsigned int irq_id)
{
	// fill 96 samples
	int cc;
	unsigned int* song;
	if(stream_flag == 0)
		song = streamA;
	else
		song = streamB;

	for(cc = 0; cc < SAMPLE_SIZE; cc++)
	{
		song_wav[0] = song[song_index];
		song_wav[1] = song[song_index+1];
		song_index+=2;

		song_sample[cc] = ((song_wav[1]<<8)|song_wav[0])<<8;

		if( volume == 1)
		{
			if(song_sample[cc] >= 0x00080000)
				song_sample[cc] = (song_sample[cc]<<(1))|0x00008000;
			else
				song_sample[cc] = song_sample[cc]<<(1);
		}
		else if(volume == 2)
		{
			if(song_sample[cc] >= 0x00080000)
				song_sample[cc] = (song_sample[cc]<<(3))|0x00008000;
			else
				song_sample[cc] = song_sample[cc]<<(3);
		}

		if(stream_flag == 0 && song_index == streamA_size)
		{
			printf("s A p B %d  || %d\n", aa, song_index);
			song = streamB;
			song_index = 0;
			stream_flag = 1;
		}
		else if(stream_flag == 1 && song_index == streamB_size)
		{
			printf("s B p A %d  || %d\n", aa, song_index);
			song = streamA;
			song_index = 0;
			stream_flag = 0;
		}
	}

	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_LEFT);
	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_RIGHT);
}

/*
 * precondition: file_handle, streamA and streamB are not null
 * save the song temporary to stream A when stream_flag == 1
 * otherwise save to stream B
 * return 1 if the file_handler reaches eof
 * otherwise return 0
 */
int streamSong( short int file_handle )
{
	unsigned int* stream;
	int buf;
	int i;
	int flag = stream_flag;

	if(flag == 1)
	{
		stream = streamA;
	}
	else
	{
		stream = streamB;
	}

	for(i = 0; i < SONG_SIZE; i++)
	{
		buf = alt_up_sd_card_read( file_handle );
		if( buf < 0 )	//reach eof
		{
			if(flag == 1)
				streamA_size = i;
			else
				streamB_size = i;
			return 1;
		}

		stream[i] = buf;
		aa = i;	//debug

		updateState();

		while(state == PAUSED)
			updateState();

		if(state == STOP || state == NEXT_PLAY)
		{
			printf("stop\n");
			break;
		}

	}

	if(flag == 1)
		streamA_size = SONG_SIZE;
	else
		streamB_size = SONG_SIZE;

	return 0;
}

/*
 * stop playing the song
 * close the file
 */
void stopSong( short int file_handle )
{
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "STOP   ");
	alt_irq_disable(AUDIO_0_IRQ);
	alt_up_audio_reset_audio_core(audio);
	closeFileInSD(file_handle);
}

/* Use this function to update currSong when user clicks a song from android device
 * Given the id of the song, finds the song from the song detail list
 * Returns the index of the song in the list if successful, otherwise -1.
 */
int findSong( SongDetail** list, int numSongs, char* id )
{
	int i;

	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSongs; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;
}

/*
 * set the next song
 */
void nextSong()
{
	currSong = (currSong + 1) % MAX_NUMBER_SONGS;
}

void initialization()
{
	/* LCD screen */
	char_lcd_dev = alt_up_character_lcd_open_dev(CHARACTER_LCD_0_NAME);
	if (char_lcd_dev == NULL)
		printf("Error: could not open character LCD device\n");
	alt_up_character_lcd_init(char_lcd_dev);

	/* SD card reader */
	sd_card_reference = alt_up_sd_card_open_dev("/dev/Altera_UP_SD_Card_Avalon_Interface_0");

	if ( sd_card_reference )
		printf( "SD Card port opened.\n" ); // debugging purpose
	else
		printf( "Error: SD card port not opened.\n" );

	/* Audio */
	av_config = alt_up_av_config_open_dev("/dev/audio_and_video_config_0");
	while (!alt_up_av_config_read_ready(av_config)) ;
	audio = alt_up_audio_open_dev("/dev/audio_0");
	alt_up_audio_reset_audio_core(audio);

	//streamA = (unsigned int*)malloc(SONG_SIZE*sizeof(unsigned int));
	//streamB = (unsigned int*)malloc(SONG_SIZE*sizeof(unsigned int));
	streamA_size = 0;
	streamB_size = 0;
	stream_flag = 0;
	song_index = 0;

	/* UART RS232 */
	uart = alt_up_rs232_open_dev("/dev/rs232_0");
	clearMiddleManBuffer();

	/* Interrupt */
	alt_up_audio_enable_write_interrupt(audio);
	alt_irq_register(AUDIO_0_IRQ, 0, (alt_isr_func)audio_isr);
	alt_irq_disable(AUDIO_0_IRQ);
}

/* Sends the song list to the middle man words by words
 * songList cannot be NULL
 */
void sendSongListToMiddleMan( SongDetail** songList, int numSong )
{
	int i;
	char* temp = malloc( MAX_DIGIT_OF_MAX_NUM_SONG );
	sprintf( temp, "%d", numSong );

	printf("Sending the message to the Middleman\n");
	sendStringToMiddleMan( temp );
	sendStringToMiddleMan( "." );

	for ( i = 0; i < numSong; i++ )
	{
		sendOneSongDetailToMiddleMan( songList[i] );
	}

	free( temp );
}

/* Sends the detail of one song to the middle man
 * song cannot be NULL
 */
void sendOneSongDetailToMiddleMan( SongDetail* song )
{
	sendStringToMiddleMan( song->id );
	sendStringToMiddleMan( "." );
	sendStringToMiddleMan( song->name );
	sendStringToMiddleMan( "." );
	sendStringToMiddleMan( song->artist );
	sendStringToMiddleMan( "." );
	sendStringToMiddleMan( song->rating );
	sendStringToMiddleMan( "." );
}

/* Sends one string to the middle man
 * str cannot be NULL
 */
void sendStringToMiddleMan( char* str )
{
	int i;

	//alt_up_rs232_write_data( uart, (unsigned char) strlen(str) );

	for ( i = 0; i < strlen(str); i++ )
		alt_up_rs232_write_data( uart, str[i] );
}

/* Reads a string from the middle man; the first byte needs to be the length of the string
 * This means the string cannot be longer than 255 letters
 * returns the pointer to the string
 */
char* getWordFromMiddleMan()
{
	int length = getByteFromMiddleMan();
	char* str = (char*)malloc( length );

	if ( !str )
	{
		printf( "Error: no memory to malloc in getWordFromMiddleMan().\n" );
		return NULL;
	}

	int i;
	for ( i = 0; i < length; i++ )
	{
		str[i] = getByteFromMiddleMan();
	}
	str[i] = '\0';

	return str;
}

/* Reads one byte from the middle man
 * returns the byte
 */
unsigned char getByteFromMiddleMan()
{
	unsigned char data;
	unsigned char parity;

	while ( alt_up_rs232_get_used_space_in_read_FIFO(uart) == 0 );
	alt_up_rs232_read_data( uart, &data, &parity );

	return data;
}

int isThereSomething()
{
	return alt_up_rs232_get_used_space_in_read_FIFO(uart);
}

void clearMiddleManBuffer()
{
	unsigned char data;
	unsigned char parity;

	while (alt_up_rs232_get_used_space_in_read_FIFO(uart))
	{
		alt_up_rs232_read_data(uart, &data, &parity);
	}
}

/* This function is deprecated as of March 18th.
 * Reads the song list from the middle man and prints the song list in one line.
 * This function is used to check whether sending of song list work or not.
 * The algorithm of this function can also be used by Daniel to implement
 * 		reading from middle man in Android using Java.
 */
void getSongListFromMiddleManAndPrintForDebuggingPurpose()
{
	unsigned char data;
	unsigned char parity;
	int i, j, k, m = 0;
	char* numSong;

	printf("Waiting for data to come back from the Middleman\n");

	numSong = getWordFromMiddleMan();

	int num_to_receive = atoi( numSong );
	char* temp = (char*)malloc( num_to_receive * (ID_LENGTH + NAME_LENGTH + ARTIST_LENGTH + RATING_LENGTH) );

	printf("About to receive %d song details:\n", num_to_receive);

	for ( i = 0; i < num_to_receive; i++ )
	{
		for ( j = 0; j < 4; j++ )
		{
			while ( alt_up_rs232_get_used_space_in_read_FIFO(uart) == 0 );
			alt_up_rs232_read_data( uart, &data, &parity );
			int lengthOfData = (int)data;

			for ( k = 0; k < lengthOfData; k++ )
			{
				while (alt_up_rs232_get_used_space_in_read_FIFO(uart) == 0);

				alt_up_rs232_read_data( uart, &data, &parity );

				temp[m++] = data;
				//printf( "%c", data );
			}
			temp[m++] = ' ';
		}
	}
	temp[m] = '\0';

	printf( "Data Received: %s\n", temp );

	clearMiddleManBuffer();

	free( temp );
}

/* Opens a file and stores the file_handle in the memory pointed by file_handle_ptr
 * return 0 if successful, otherwise -1
 */
char openFileInSD( char* fileName, short int* file_handle_ptr )
{
	short int file_handle;

	if ( alt_up_sd_card_is_Present() )
	{
		//printf("SD Card connected.\n");	// debugging purpose

		if ( alt_up_sd_card_is_FAT16() )
		{
			//printf("FAT16 file system detected.\n"); // debugging purpose

			file_handle = alt_up_sd_card_fopen( fileName, false );
			if ( file_handle == -1 )
				printf( "Error: File could not be opened.\n" );
			if ( file_handle == -2 )
				printf( "Error: File is already open.\n" );

			if ( file_handle != -1 && file_handle != -2 )
			{
				printf( "SD Card successfully opened.\n" ); // debugging purpose
				*file_handle_ptr = file_handle;
				return 0;
			}
		}
		else
		{
			printf("Error: Unknown file system.\n");
		}
	}
	else
	{
		printf( "Error: SD Card not connected.\n" );
	}

	return -1;
}

/* Closes the file
 * Returns 0 if successful, otherwise -1
 */
char closeFileInSD( short int file_handle )
{
	if ( alt_up_sd_card_fclose( file_handle ) )
	{
		printf( "File successfully closed.\n" ); // debugging purpose
		return 0;
	}
	else
	{
		printf( "Error: File not closed.\n" );
		return -1;
	}
}

/* Reads all the song details in the song list
 * returns an array of songDetail struct if successful, NULL otherwise
 */
SongDetail** getListOfSongDetails( int *numSongs )
{
	SongDetail** songList;
	short int file_handle;
	int i;
	char* numSongsStr = (char*)malloc( MAX_DIGIT_OF_MAX_NUM_SONG );

	if ( !numSongsStr )
	{
		printf( "Error: no memory to allocate memory for numSongs.\n" );
		return NULL;
	}

	openFileInSD( "SONGLIST.TXT", &file_handle );

	if ( readWordFromSD( numSongsStr, MAX_NUM_SONGS, file_handle ) == -1 )
		return NULL;

	*numSongs = atoi( numSongsStr );

	songList = malloc( *numSongs * sizeof(SongDetail) );

	if ( !songList )
	{
		printf( "Error: no memory to allocate memory for songList.\n" );
		closeFileInSD( file_handle );
		return NULL;
	}

	for ( i = 0; i < *numSongs; i++ )
	{
		songList[i] = readDetailForOneSong( file_handle );
		if ( !songList[i] )
		{
			closeFileInSD( file_handle );
			return NULL;
		}

		// Debugging purpose
		printf( "Song: %s(sizeOfID: %d) %s %s %s\n", songList[i]->id, (int)strlen( songList[i]->id ), songList[i]->name, songList[i]->artist, songList[i]->rating );
	}

	closeFileInSD( file_handle );
	return songList;
}

/* Initializes SongDetail struct to have memory allocated
 * return 0 if successful, -1 otherwise
 */
char initializeSongDetail( SongDetail* song )
{
	song->id = (char*)malloc( ID_LENGTH );
	song->name = (char*)malloc( NAME_LENGTH );
	song->artist = (char*)malloc( ARTIST_LENGTH );
	song->rating = (char*)malloc( RATING_LENGTH );

	if ( !song->id || !song->name || !song->artist || !song->rating )
	{
		printf( "Error: no more memory to malloc for song detail elements.\n" );
		return -1;
	}

	return 0;
}

/* Reads id, name, artist, and rating of the song
 * Returns the pointer to the SongDetail struct if successful, NULL otherwise
 */
SongDetail* readDetailForOneSong( short int file_handle )
{
	SongDetail* song = (SongDetail*)malloc(sizeof(SongDetail));
	char t1, t2, t3, t4;

	if ( !song )
	{
		printf( "Error: No more memory to malloc for song detail.\n" );
		return NULL;
	}

	if ( initializeSongDetail( song ) == -1 )
		return NULL;

	t1 = readWordFromSD( song->id, ID_LENGTH, file_handle );
	t2 = readWordFromSD( song->name, NAME_LENGTH, file_handle );
	t3 = readWordFromSD( song->artist, ARTIST_LENGTH, file_handle );
	t4 = readWordFromSD( song->rating, RATING_LENGTH, file_handle );

	if ( t1 == -1 || t2 == -1 || t3 == -1 || t4 == -1 )
		return NULL;

	/* Removes the carraige return character */
	song->id = &(song->id[2]);

	return song;
}

/* Pre: name has to have at least LENGTH bytes of memory allocated
 * Post: the memory pointed by name has the string
 * Returns 0 if sucessful, otherwise -1
 */
char readWordFromSD( char* name, const int length, short int file_handle )
{
	int i = 0;
	char ch;

	ch = readACharFromSD( file_handle );

	while ( ch != -1 && ch != '.' )
	{
		name[i++] = ch;
		//printf( "%c", ch );	// debugging purpose

		if ( i > length )
		{
			printf( "Error: Word is longer than the maximum length(%d) allowed.\n", length );
			return -1;
		}

		ch = readACharFromSD( file_handle );
	}

	if ( ch == -1 )
	{
		printf( "Error: readDataFromSD Failed.\n" );
		return -1;
	}

	name[i] = '\0';
	//printf( "." ); // debugging purpose

	return 0;
}

/* Reads a char in the file
 * Returns a signed char if data successful, otherwise -1
 */
char readACharFromSD( short int file_handle )
{
	char byte;
	short int temp;

	temp = alt_up_sd_card_read( file_handle );
	if ( temp < 0 )
	{
		printf( "Error: alt_up_sd_card_read Failed.\n" );
		return -1;
	}
	byte = (char)temp;

	return byte;
}

