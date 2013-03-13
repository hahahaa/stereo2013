#include "stdio.h"
#include "stdlib.h"
#include "unistd.h"
#include "io.h"
#include "system.h"
#include "string.h"
#include "inttypes.h"
#include "altera_up_avalon_character_lcd.h"
#include "altera_up_avalon_parallel_port.h"
#include "altera_up_sd_card_avalon_interface.h"
#include "altera_up_avalon_audio_and_video_config.h"
#include "altera_up_avalon_audio.h"
#include "sys/alt_irq.h"

#define switches (int) 0x00004420
#define leds (char *) 0x00004430
#define keys (int) 0x00004440

/* Define states */
#define PLAYING 0
#define STOP 1
#define PAUSED 2
#define NEXT 3

const int MAX_NUMBER_SONGS = 2;
const int MAX_STRING_SIZE = 7;

/* Constants for SongDetail; the length includes the null character */
const int ID_LENGTH = 4;
const int NAME_LENGTH = 26;
const int ARTIST_LENGTH = 21;
const int RATING_LENGTH = 2;

//const int SONG_SIZE = 2000;
//const int SONG_SIZE = 65534/2;	//around 0.5 second long
const int SONG_SIZE = 65534;	//around 1 second long
//const int SONG_SIZE = 196602;	//around 3 seconds long
const int WAV_HEADER_SIZE = 44;
const int SAMPLE_SIZE = 96;
int song_index;	// 0 <= song_count < SONG_SIZE
unsigned int song_wav[2];
unsigned int song_sample[96];
unsigned int* streamA;
unsigned int* streamB;
int streamA_size;
int streamB_size;
int stream_flag;
int state;


typedef struct {
	char* id;
	char* name;
	char* artist;
	char* rating;
} songDetail;



/* Device references */
alt_up_sd_card_dev *sd_card_reference;
alt_up_av_config_dev * av_config;
alt_up_audio_dev * audio;
alt_up_character_lcd_dev * char_lcd_dev;

/* Initialization */
void initialization();

/* SD Functions */
char openFileInSD( char* fileName, short int* file_handle );
char closeFileInSD( short int file_handle );
songDetail** getListOfSongDetails();
char initializeSongDetail( songDetail* song );
songDetail* readDetailForOneSong( short int file_handle );
char readWordFromSD( char* name, const int LENGTH, short int file_handle );
char readACharFromSD( short int file_handle );

/*
 * play a song
 * stops when KEY3 is pressed
 * return 1 if the song stopped before ending
 * otherwise 0
 */
int playSong( short int file_handle );

/*
 * stop playing the song
 * close the file
 */
void stopSong( short int file_handle );

/*
 * Audio interrupt handler
 * which is used to play a song
 * play stream A when stream_flag == 0
 * otherwise play stream B
 */
void audio_isr (void * context, unsigned int irq_id);

/*
 * precondition: file_handle, streamA and streamB are not null
 * save the song temporary to stream A when stream_flag == 1
 * otherwise save to stream B
 * return -1 if the file_handler reaches eof
 * otherwise return 0
 */
int streamSong( short int file_handle );

/*
 * read keys and update the current state
 * used while playing
 */
void updateState();

int main()
{
	initialization();
	char key;

	short int file_handle;
	int i;
	int currSong = 0;
	int keepPlaying = 0;
	/*
	 * 0 stop
	 * 1 playing
	 * 2 paused
	 * 3 next
	 */
	state = STOP;

	char songList [MAX_NUMBER_SONGS][MAX_STRING_SIZE];

	strcpy(songList[0], "01.wav");
	strcpy(songList[1], "02.wav");

	int cc;
	for(cc = 0; cc < MAX_NUMBER_SONGS; cc++)
		printf("%s\n", songList[cc]);

	while(1)
	{
		key = IORD_8DIRECT(keys, 0);
		while(IORD_8DIRECT(keys, 0) != 0);

		if(key == 0x8)	//play
			state = PLAYING;
		else if(key == 0x4)	//stop
			state = STOP;
		else if(key == 0x2)	//next
			state = NEXT;

		if(state == STOP)
		{
			keepPlaying = 0;
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "STOP   ");
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
			alt_up_character_lcd_string(char_lcd_dev, songList[currSong]);
		}
		else if(state == PLAYING)
		{
			keepPlaying = 1;
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
			alt_up_character_lcd_string(char_lcd_dev, songList[currSong]);

			if( openFileInSD( songList[currSong], &file_handle ) == 0)
			{
				// skip the header
				for(i = 0; i < WAV_HEADER_SIZE; i++)
					alt_up_sd_card_read( file_handle );
				printf("file opened\n");
			}
			else
			{
				printf("failed to open wav\n");
				exit(1);
			}

			if( playSong( file_handle ) == 0)
				state = NEXT;
		}
		else if(state == NEXT)
		{
			stopSong( file_handle );
			currSong = (currSong + 1) % MAX_NUMBER_SONGS;
			if(!keepPlaying)
				state = STOP;
			else
				state = PLAYING;
		}
	}

	return 0;
}

/*
 * play a song
 * stops when KEY3 is pressed
 * return 1 if the song stopped before ending
 * otherwise 0
 */
int playSong( short int file_handle )
{
	alt_irq_disable(AUDIO_0_IRQ);

	int i;
	int prev_stream_flag;

	stream_flag = 1;
	streamA_size = 0;
	streamB_size = 0;
	song_index = 0;

	streamSong( file_handle );	//save to stream A
	stream_flag = 0;	//play stream A

	alt_irq_enable(AUDIO_0_IRQ);
	prev_stream_flag = stream_flag;
	i = 0;
	while(i == 0)
	{
		if(state == PLAYING)
		{
			i = streamSong( file_handle );
			//printf("done streaming\n");
			while(stream_flag == prev_stream_flag);	//wait to switch stream
			prev_stream_flag = stream_flag;
		}
		else if(state == STOP)
		{
			stopSong( file_handle );
			return 1;
		}
		else if(state == NEXT)
			break;
	}

	stopSong( file_handle );
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

		if(stream_flag == 0 && song_index >= streamA_size)
		{
			song = streamB;
			song_index = 0;
			stream_flag = 1;
			//printf("playing B\n");
		}
		else if(stream_flag == 1 && song_index >= streamB_size)
		{
			song = streamA;
			song_index = 0;
			stream_flag = 0;
			//printf("playing A\n");
		}
	}

	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_LEFT);
	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_RIGHT);
}

/*
 * precondition: file_handle, streamA and streamB are not null
 * save the song temporary to stream A when stream_flag == 1
 * otherwise save to stream B
 * return -1 if the file_handler reaches eof
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
		//printf("streaming A\n");
	}
	else
	{
		stream = streamB;
		//printf("streaming B\n");
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
			return -1;
		}

		stream[i] = buf;

		updateState();
		while(state == PAUSED)
			updateState();
		if(state == STOP || state == NEXT)
			break;
	}

	if(flag == 1)
		streamA_size = SONG_SIZE;
	else
		streamB_size = SONG_SIZE;

	return 0;
}

void stopSong( short int file_handle )
{
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "STOP   ");
	alt_irq_disable(AUDIO_0_IRQ);
	alt_up_audio_reset_audio_core(audio);
	closeFileInSD(file_handle);
}

void initialization()
{
	//LCD screen
	char_lcd_dev = alt_up_character_lcd_open_dev(CHARACTER_LCD_0_NAME);
	if (char_lcd_dev == NULL)
		printf("Error: could not open character LCD device\n");
	alt_up_character_lcd_init(char_lcd_dev);

	//SD card reader
	sd_card_reference = alt_up_sd_card_open_dev("/dev/Altera_UP_SD_Card_Avalon_Interface_0");

	if ( sd_card_reference )
		printf( "SD Card port opened.\n" ); // debugging purpose
	else
		printf( "Error: SD card port not opened.\n" );

	//Audio
	av_config = alt_up_av_config_open_dev("/dev/audio_and_video_config_0");
	while (!alt_up_av_config_read_ready(av_config)) ;
	audio = alt_up_audio_open_dev("/dev/audio_0");
	alt_up_audio_reset_audio_core(audio);

	streamA = (unsigned int*)malloc(SONG_SIZE*sizeof(unsigned int));
	streamB = (unsigned int*)malloc(SONG_SIZE*sizeof(unsigned int));
	streamA_size = 0;
	streamB_size = 0;
	stream_flag = 0;
	song_index = 0;

	//Interrupt
	alt_up_audio_enable_write_interrupt(audio);
	alt_irq_register(AUDIO_0_IRQ, 0, (alt_isr_func)audio_isr);
	alt_irq_disable(AUDIO_0_IRQ);
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
				//printf( "SD Card successfully opened.\n" ); // debugging purpose
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

songDetail** getListOfSongDetails()
{
	songDetail** songList;
	short int file_handle;
	short int numSongs;
	int i;

	openFileInSD( "SONGLIST.TXT", &file_handle );

	numSongs = readACharFromSD( file_handle );
	if ( numSongs == -1 )
		return NULL;

	songList = malloc( numSongs * sizeof(songDetail) );

	for ( i = 0; i < numSongs; i++ )
	{
		songList[i] = readDetailForOneSong( file_handle );
		if ( !songList[i] )
		{
			closeFileInSD( file_handle );
			return NULL;
		}
	}

	closeFileInSD( file_handle );
	return songList;
}

char initializeSongDetail( songDetail* song )
{
	song->id = (char)malloc( ID_LENGTH );
	song->name = (char)malloc( NAME_LENGTH );
	song->artist = (char)malloc( ARTIST_LENGTH );
	song->rating = (char)malloc( RATING_LENGTH );

	if ( !song->id || !song->name || !song->artist || !song->rating )
	{
		printf( "Error: no more memory to malloc for song detail elements.\n" );
		return -1;
	}

	return 0;
}

songDetail* readDetailForOneSong( short int file_handle )
{
	songDetail* song = (songDetail*)malloc(sizeof(songDetail));
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

	return song;
}

/* Pre: name has to have at least LENGTH bytes of memory allocated
 * Post: the memory pointed by name has the string
 * Returns 0 if sucessful, otherwise -1
 */
char readWordFromSD( char* name, const int LENGTH, short int file_handle )
{
	int i = 0;
	char ch;

	ch = readACharFromSD( file_handle );

	while ( ch != -1 && ch != '.' )
	{
		name[i++] = ch;

		if ( i > LENGTH )
		{
			printf( "Error: Word is longer than the maximum length allowed.\n" );
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

void updateState()
{
	char key = IORD_8DIRECT(keys, 0);
	while(IORD_8DIRECT(keys, 0) != 0);

	if(key == 0x8)	//play or paused
	{
		if(state == PLAYING)
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
			state = PLAYING;
		}
	}
	else if(key == 0x4)	//stop
	{
		state = STOP;
	}
	else if(key == 0x2)	//next
	{
		state = NEXT;
	}


}

