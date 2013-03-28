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
const int switches = 0x00004420;
//#define leds (char *) 0x00004430
const int keys = 0x00004440;

/* Define states */
#define STOP 0
#define PAUSED 1
#define PLAYING_NORMAL 2
#define NEXT_PLAY 3
#define PREV_PLAY 4

/* Constants for high level song stuff */
#define MAX_NUM_SONGS 100
#define MAX_DIGIT_OF_MAX_NUM_SONG 4 // has to include memory for null character
#define EXTENSION_LENGTH 4

/* Constants for SongDetail; the length includes the null character */
#define ID_LENGTH 5 // id length includes the carriage return character in addition
#define NAME_LENGTH 26
#define ARTIST_LENGTH 21
#define RATING_LENGTH 3
#define TIME_LENGTH 4

//#define SONG_SIZE 65534	//around 1 second long
#define SONG_SIZE 32266
#define WAV_HEADER_SIZE 44
#define SAMPLE_SIZE 96

volatile int song_index;	// 0 <= song_count < SONG_SIZE
volatile int stream_flag;
//volatile unsigned int song_sample[96];
unsigned int song_wav[2];
unsigned int streamA[SONG_SIZE];
unsigned int streamB[SONG_SIZE];
int streamA_size;
int streamB_size;

int numSongs;
int state;
int currSong;
int shuffle_flag;
int volume;
int aa;	// debug thing

typedef struct {
	char* id;
	char* name;
	char* artist;
	char* rating;
	char* time;
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
void sendHandShakedLongMessageToMiddleMan( char command, char* str );
void sendStringToMiddleMan( char* str );

/* MiddleMan to DE2 function */
//void getSongListFromMiddleManAndPrintForDebuggingPurpose();
char* getWordFromMiddleMan();
unsigned char getByteFromMiddleMan();
int isThereSomething();
void clearMiddleManBuffer();

/* Song Functions */
int playSong( short int file_handle );
void stopSong( short int file_handle );
void nextSong( int next );
void audio_isr( void * context, unsigned int irq_id );
int streamSong( short int file_handle );
int findSong( SongDetail** list, int numSong, char* id );
void readTone( unsigned int* tone, int tone_size, char* name);

/* Update */
void updateStateFromKeys();
void updateStateFromUART();
void updateState();
//void updateVolume();

int main()
{
	initialization();

	short int file_handle;
	currSong = 0;
	shuffle_flag = 0;
	volume = 0;
	state = STOP;
	numSongs = 0;


/*
		unsigned int t0[78362];
		readTone(t0, 78362, "T0.wav");
		int i = 0;
		for(i = 0; i < 78362; i++)
		{
			alt_up_audio_write_fifo(audio, t0[i], 1, ALT_UP_AUDIO_LEFT);
			alt_up_audio_write_fifo(audio, t0[i], 1, ALT_UP_AUDIO_RIGHT);

			//usleep(20);
		}

		alt_up_audio_reset_audio_core(audio);
*/
	SongDetail** songDetailList = getListOfSongDetails( &numSongs );

	char sw = IORD_8DIRECT(switches, 0);
	if(sw == 0)
	{
		char* message;
		while ( !isThereSomething() )
		{
			message = getWordFromMiddleMan();
			printf( "%s.\n", message );
			if( strcmp(message, "playlist") == 0)
				break;
		}
		free(message);
		sendSongListToMiddleMan( songDetailList, numSongs );
	}

	int cc;
	for(cc = 0; cc < numSongs; cc++)
		printf("%s\t", songDetailList[cc]->id );
	printf( "\n" );

	while(1)
	{
		if(state == STOP)
		{
			//sendStringToMiddleMan( "S" );
			alt_up_character_lcd_init(char_lcd_dev);
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "STOP   ");
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
			alt_up_character_lcd_string(char_lcd_dev, songDetailList[currSong]->name);

			updateState();
		}
		else if(state == PLAYING_NORMAL)
		{
			//sendStringToMiddleMan( "P" );
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
				printf("file %s opened\n", temp);
			}
			else
			{
				printf("failed to open %s wav\n", temp );
				exit(1);
			}

			playSong( file_handle );
			printf("stop stop\n");
			stopSong( file_handle );
			printf("stop stop stop\n");
			if(state == NEXT_PLAY || state == PLAYING_NORMAL)
			{
				nextSong(1);
				state = PLAYING_NORMAL;
			}
			else if(state == PREV_PLAY)
			{
				nextSong(0);
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
			//sendStringToMiddleMan( "P" );
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PAUSED ");
			alt_irq_disable(AUDIO_0_IRQ);
			state = PAUSED;
		}
		else if(state == PAUSED)
		{
			//sendStringToMiddleMan( "P" );
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
		}
	}
	else if(key == 0x4)	//stop
	{
		//sendStringToMiddleMan( "S" );
		state = STOP;
	}
	else if(key == 0x2)	//next
	{
		state = NEXT_PLAY;
	}
	else if(key == 0x1)
	{
		state = PREV_PLAY;
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
	printf( "Message got from Middleman: %s.\n", temp );

	if( strcmp(temp, "P") == 0 )	//play or paused
	{
		if(state == PLAYING_NORMAL)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PAUSED ");
			alt_irq_disable(AUDIO_0_IRQ);
			state = PAUSED;
			sendStringToMiddleMan( "p" );
		}
		else if(state == STOP)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
			char* buf = (char*)malloc( 3 );
			sprintf( buf, "%d", currSong );
			sendStringToMiddleMan( buf );
			free(buf);
		}
		else if(state == PAUSED)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
			sendStringToMiddleMan( "p" );
		}
	}
	else if( strcmp(temp, "S") == 0 )	//stop
	{
		state = STOP;
		sendStringToMiddleMan( "S" );
	}
	else if( strcmp(temp, "N") == 0 )	//next
	{
		state = NEXT_PLAY;
	}
	else if( strcmp(temp, "L") == 0)	//prev
	{
		state = PREV_PLAY;
	}
	else if( strcmp(temp, "D") == 0)	//raise volume
	{
		sendStringToMiddleMan( "D" );
		if(volume < 4)
			volume++;
	}
	else if( strcmp(temp, "U") == 0)	//lower volume
	{
		sendStringToMiddleMan( "U" );
		if(volume > 0)
			volume--;
	}
	else if( strcmp(temp, "M" ) == 0 )	// debugging purpose
	{
		sendHandShakedLongMessageToMiddleMan( 'M', "This is the long message" );
	}
	free(temp);
}

/*
 * read keys and uart to update the current state
 */
void updateState()
{
	char key;

	if( state == STOP )
	{
		// stop state
		key = 0;
		while(key == 0)
		{
			// wait for command
			key = IORD_8DIRECT(keys, 0);

			if ( isThereSomething() )
			{
				//printf ( "start there is a duck\n" );
				//temp = getWordFromMiddleMan();
				updateStateFromUART();
				break;
			}
		}
		while(IORD_8DIRECT(keys, 0) != 0);

		if(key == 0x8)	//play
			state = PLAYING_NORMAL;
		else if(key == 0x4)	//stop
		{
			// current state is STOP
			// do nothing~
		}
		else if(key == 0x2 || state == NEXT_PLAY)
		{
			nextSong(1);
		}
		else if(key == 0x1 || state == PREV_PLAY)
		{
			nextSong(0);
		}
		// stop state
	}
	else
	{
		updateStateFromKeys();
		if ( isThereSomething() )
		{
			//printf ( "there is something\n" );
			updateStateFromUART();
		}
	}
}

/*
 * read switches and change volume
 */
/*void updateVolume()
{
	char sw = IORD_8DIRECT(switches, 0);
	volume = sw & 0x03;
}*/

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

	char* buf = (char*)malloc( 3 );
	sprintf( buf, "%d", currSong );
	sendStringToMiddleMan( buf );
	free(buf);

	alt_irq_enable(AUDIO_0_IRQ);

	while(1)
	{
		if(state == PLAYING_NORMAL)
		{
			isLastSecond = streamSong( file_handle );
			while(stream_flag == prev_stream_flag)	//wait for this second to pass
			{
				if( state != PLAYING_NORMAL)	// it is used to fix the pause next thing
					stream_flag = !prev_stream_flag;
			}
			prev_stream_flag = stream_flag;
			if(state == PLAYING_NORMAL)
				sendStringToMiddleMan( "O" );
		}
		else if(state == STOP)
		{
			return 0;
		}
		else// if(state == NEXT_PLAY || state == PREV_PLAY)	//next or prev is pressed
		{
			return 1;
		}

		if(isLastSecond)
		{
			sendStringToMiddleMan( "O" );
			printf("last sec\n");
			// play the last second
			while(stream_flag == prev_stream_flag);
			break;
		}
	}

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
	unsigned int song_sample[96];
	int cc;
	unsigned int* song;
	if(stream_flag == 0)
		song = streamA;
	else
		song = streamB;

	for(cc = 0; cc < SAMPLE_SIZE; cc++)
	{
		/*song_wav[0] = song[song_index];
		song_wav[1] = song[song_index+1];
		song_index+=2;

		song_sample[cc] = ((song_wav[1]<<8)|song_wav[0])<<8;*/
		song_sample[cc] = song[song_index];
		song_index++;

		// lower the volume
		if(volume == 4)
			song_sample[cc] = 0;
		else if(volume != 0)
		{
			if(song_sample[cc] >= 0x800000)
				song_sample[cc] = (song_sample[cc]>>volume)|0xE00000;
			else
				song_sample[cc] = song_sample[cc]>>volume;
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
	/*int freq;
	int cycles;
	float duration;
	freq = alt_timestamp_freq();
	alt_timestamp_start();
	*/
	unsigned int* stream;
	int buf[2];
	int i;
	int flag = stream_flag;

	if(flag == 1)
		stream = streamA;
	else
		stream = streamB;

	for(i = 0; i < SONG_SIZE; i++)
	{
		buf[0] = alt_up_sd_card_read( file_handle );
		if( buf[0] < 0 )	//reach eof
		{
			if(flag == 1)
				streamA_size = i;
			else
				streamB_size = i;
			return 1;
		}
		buf[1] = alt_up_sd_card_read( file_handle );

		stream[i] = ((buf[1]<<8)|buf[0])<<8;
		//stream[i] = buf;

		aa = i;	//debug

		updateState();

		while(state == PAUSED)
			updateState();

		//if(state == STOP || state == NEXT_PLAY || state == PREV_PLAY)
		if(state != PLAYING_NORMAL)
		{
			printf("stop streaming\n");
			break;
		}
	}

	if(flag == 1)
		streamA_size = SONG_SIZE;
	else
		streamB_size = SONG_SIZE;
	/*
	cycles = alt_timestamp();
	duration = (float) cycles / (float) freq;
	printf("It took %d cycles (%f seconds) to stream one second \n", cycles, duration);
	*/

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

/*
 * set the next song playing
 * if next == 1, play the next song
 * otherwise, play the previous song
 */
void nextSong( int next )
{
	char sw = IORD_8DIRECT(switches, 0);
	if( (sw & 0x80) != 0x80)	// repeat the same song when SW7 == 1
	{
		if(next == 1)
		{
			currSong = (currSong + 1) % numSongs;
			sendStringToMiddleMan( "N" );
		}
		else
		{
			if(currSong == 0)
				currSong = numSongs - 1;
			else
				currSong = (currSong - 1) % numSongs;
			sendStringToMiddleMan( "L" );
		}
	}
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
/*
void sendSongListToMiddleMan( SongDetail** songList, int numSongs )
{
	int i;
	char* temp = malloc( MAX_DIGIT_OF_MAX_NUM_SONG );
	sprintf( temp, "%d", numSongs );

	printf("Sending the message to the Middleman\n");
	sendStringToMiddleMan( temp );
	sendStringToMiddleMan( "." );

	for ( i = 0; i < numSongs; i++ )
	{
		sendOneSongDetailToMiddleMan( songList[i] );
	}

	free( temp );
}
*/


void sendSongListToMiddleMan( SongDetail** songList, int numSong )
{
	int i;
	char* temp = malloc( MAX_DIGIT_OF_MAX_NUM_SONG );
	sprintf( temp, "%d", numSong );
	char* temp1 = malloc( 5 );	// need to make a constant

	printf("numSong: %d\n", numSong);
	printf("Sending the message to the Middleman\n");
	sendStringToMiddleMan( temp );
	sendStringToMiddleMan( "." );

	while ( !isThereSomething() )
	{
		temp1 = getWordFromMiddleMan();
		printf( "Message got from Middleman: %s.\n", temp1 );
		if ( strcmp( temp1, "A" ) == 0 )
			break;
	}

	for ( i = 0; i < numSong; i++ )
	{
		sendOneSongDetailToMiddleMan( songList[i] );
		sendStringToMiddleMan( "+" );

		while ( !isThereSomething() )
		{
			temp1 = getWordFromMiddleMan();
			printf( "Message got from Middleman: %s.\n", temp1 );
			if ( strcmp( temp1, "A" ) == 0 )
				break;
		}
	}
	sendStringToMiddleMan( "," );
	printf( "Done sending message to Middleman.\n" );
	free( temp );
	free( temp1 );
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
	sendStringToMiddleMan( song->time );
	sendStringToMiddleMan ( "." );

}

/* Sends long message to the middle man no more than 128 bytes
 * str cannot be NULL
 * str cannot be longer than 99999 chars.
 */
void sendHandShakedLongMessageToMiddleMan( char command, char* str )
{
	int i;
	char buffer[5]; // Gonna change the magic number

	alt_up_rs232_write_data( uart, command );
	printf( "Sent %c\n", command );

	sprintf( buffer, "%d", (int)strlen( str ) );
	alt_up_rs232_write_data( uart, (int) strlen(buffer) );  // Number of digits of the number of chars of data
	printf( "Sent %d\n", (int) strlen( buffer ) );

	char* buffer2 = malloc( (int) strlen( buffer ) + 1 );
	sprintf( buffer2, "%d", (int) strlen( str ) );
	sendStringToMiddleMan( buffer2 );  // Number of chars of data
	printf( "Sent %s\n", buffer2 );

	for ( i = 0; i < strlen(str); i++ )
		alt_up_rs232_write_data( uart, str[i] );

	free( buffer2 );
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

/* Use this function to update currSong when user clicks a song from android device
 * Given the id of the song, finds the song from the song detail list
 * Returns the index of the song in the list if successful, otherwise -1.
 */
int findSong( SongDetail** list, int numSong, char* id )
{
	int i;

	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;

	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;

	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;


	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;	if ( !list || !id )
		return -1;

	for ( i = 0; i < numSong; i++ )
	{
		if ( strcmp( list[i]->id, id ) == 0 )
			return i;
	}

	return -1;











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
SongDetail** getListOfSongDetails( int *numSong )
{
	SongDetail** songList;
	short int file_handle;
	int i;
	char* numSongStr = (char*)malloc( MAX_DIGIT_OF_MAX_NUM_SONG );

	if ( !numSongStr )
	{
		printf( "Error: no memory to allocate memory for numSongs.\n" );
		return NULL;
	}

	openFileInSD( "SONGLIST.TXT", &file_handle );

	if ( readWordFromSD( numSongStr, MAX_NUM_SONGS, file_handle ) == -1 )
		return NULL;

	*numSong = atoi( numSongStr );

	songList = malloc( *numSong * sizeof(SongDetail) );

	if ( !songList )
	{
		printf( "Error: no memory to allocate memory for songList.\n" );
		closeFileInSD( file_handle );
		return NULL;
	}

	for ( i = 0; i < *numSong; i++ )
	{
		songList[i] = readDetailForOneSong( file_handle );
		if ( !songList[i] )
		{
			closeFileInSD( file_handle );
			return NULL;
		}

		// Debugging purpose
		printf( "Song: %s(sizeOfID: %d) %s %s %s %s\n", songList[i]->id, (int)strlen( songList[i]->id ), songList[i]->name, songList[i]->artist, songList[i]->rating, songList[i]->time );
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
	song->time = (char*)malloc( TIME_LENGTH );

	if ( !song->id || !song->name || !song->artist || !song->rating || !song->time)
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
	char t1, t2, t3, t4, t5;

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
	t5 = readWordFromSD( song->time, TIME_LENGTH, file_handle );

	if ( t1 == -1 || t2 == -1 || t3 == -1 || t4 == -1 || t5 == -1 )
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

/*
 * read a tone into memory
 */
void readTone( unsigned int* tone, int tone_size, char* name)
{
	short int file_handle;
	int buf[2];
	if( openFileInSD( name, &file_handle ) == 0)
	{
		int cc;
		for(cc = 0; cc < WAV_HEADER_SIZE; cc++)
			alt_up_sd_card_read( file_handle );

		while(1)
		{
			buf[0] = alt_up_sd_card_read( file_handle );
			if( buf[0] < 0 )	//reach eof
			{
				printf("tone size is of %s is %d\n", name, cc);
				break;
			}
			buf[1] = alt_up_sd_card_read( file_handle );

			tone[cc] = ((buf[1]<<8)|buf[0])<<8;
			cc++;
		}
	}
	closeFileInSD( file_handle );
}


