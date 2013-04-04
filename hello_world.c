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
const int SWITCHES = 0x00002420;
const int leds = 0x00002430;
const int KEYS = 0x00002440;

/* Define states */
#define STOP 0
#define PAUSED 1
#define PLAYING_NORMAL 2
#define NEXT_PLAY 3
#define PREV_PLAY 4
#define JUMP_SONG 5

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

//#define SONG_SIZE 65472
#define SONG_SIZE 32266
//#define SONG_SIZE 16384
#define WAV_HEADER_SIZE 44
#define SAMPLE_SIZE 96
//#define MIX_SONG_SIZE 690000
#define PIANO_SONG_SIZE 28000

volatile int volume;
volatile int stop_flag;
volatile int play_index;
volatile int read_index;
volatile unsigned int stream[SONG_SIZE];
volatile int isOneSec;
volatile int piano_index;
volatile int lock_uart;

/* Piano tones */
volatile unsigned int A3[PIANO_SONG_SIZE];
volatile unsigned int B3[PIANO_SONG_SIZE];
volatile unsigned int C3[PIANO_SONG_SIZE];
volatile unsigned int C4[PIANO_SONG_SIZE];
volatile unsigned int D3[PIANO_SONG_SIZE];
volatile unsigned int E3[PIANO_SONG_SIZE];
volatile unsigned int F3[PIANO_SONG_SIZE];
volatile unsigned int G3[PIANO_SONG_SIZE];
volatile unsigned int* mixSong;
volatile int mix_flag;
volatile int isListChanged;
volatile int sendOneSec;

int numSongs;
int currSong;
int shuffle_flag;
int repeat_flag;
int* playList;	// stores the song IDs
int playListSize;
int* origList;
int origListSize;

typedef struct {
	char* id;
	char* name;
	char* artist;
	char* rating;
	char* time;
} SongDetail;

SongDetail** songDetailList;

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
SongDetail** getListOfSongDetails( int* numSong );
char initializeSongDetail( SongDetail* song );
SongDetail* readDetailForOneSong( short int file_handle );
char readWordFromSD( char* name, const int length, short int file_handle );

/* Send song list functions */
void sendSongListToMiddleMan( SongDetail** songList, int numSong );
void sendOneSongDetailToMiddleMan( SongDetail* song );

/* Receive list from Android functions */
void receivePlayListFromMiddleMan( int* playList, int* size );

/* DE2 to MiddleMan function */
void sendStringToMiddleMan( char* str );
void sendHandShakedLongMessageToMiddleMan( char command, char* str );

/* MiddleMan to DE2 function */
char* getWordFromMiddleMan();
unsigned char getByteFromMiddleMan();
int isThereSomething();
void clearMiddleManBuffer();

/* Song Functions */
int playSong( short int file_handle, int state, int length );
void stopSong( short int file_handle );
int nextSong( int next, int size );
void audio_isr( void * context, unsigned int irq_id );
void readTone( unsigned int* tone, int tone_size, char* name);
void runStopState( SongDetail** list );
int runPlayingState( SongDetail** list );
void shufflePlayList( int* list, int size );
int* makeCopy( int* list, int listSize );
int findSong( SongDetail** list, int numSong, int id );
int findSongOnPlayList( int* list, int size, int id );

/* Update */
int updateStateFromUART( int prevState );
int updateStateFromKeysWhilePlaying( int prevState );
int updateStateWhileStop();
int updateStateWhilePlaying( int prevState );
int updateState( int prevState );

/* Mixing */
void readSong( volatile unsigned int* song, int size, char* fileName );
void playPianoBySW();

int main()
{
	initialization();

	volume = 4;
	currSong = 0;

	stop_flag = 0;
	repeat_flag = 0;
	shuffle_flag = 0;
	mix_flag = 0;
	sendOneSec = 1;
	lock_uart = 0;

	piano_index = 0;
	play_index = 0;
	read_index = 0;
	isOneSec = 0;
	numSongs = 0;
	isListChanged = 0;
	memset(&stream, 0, sizeof(stream));	// init to zeros

	int state = STOP;
	alt_up_character_lcd_init(char_lcd_dev);
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "READING SD CARD ");

	songDetailList = getListOfSongDetails( &numSongs );

	readSong( A3, PIANO_SONG_SIZE, "A3.wav");
	readSong( B3, PIANO_SONG_SIZE, "B3.wav");
	readSong( C3, PIANO_SONG_SIZE, "C3.wav");
	readSong( C4, PIANO_SONG_SIZE, "C4.wav");
	readSong( D3, PIANO_SONG_SIZE, "D3.wav");
	readSong( E3, PIANO_SONG_SIZE, "E3.wav");
	readSong( F3, PIANO_SONG_SIZE, "F3.wav");
	readSong( G3, PIANO_SONG_SIZE, "G3.wav");
	mixSong = A3;

	// initialize the playlist
	playList = malloc(numSongs * sizeof(int));
	int k;
	for(k = 0; k < numSongs; k++)
		playList[k] = atoi( songDetailList[k]->id );
	playListSize = numSongs;
	origList = makeCopy(playList, playListSize);

	printf("Playlist:\n");
	for(k = 0; k < playListSize; k++)
		printf("%d ", playList[k]);
	printf("\n");

	IOWR_8DIRECT(leds, 0, 0xFF);

	alt_up_character_lcd_init(char_lcd_dev);
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "MIDDLEMAN       ");
	printf("waiting for middleman\n");

	char sw = IORD_8DIRECT(SWITCHES, 0);
	if(sw == 0)
	{
		// wait for connection from android
		char* message = NULL;
		while ( !isThereSomething() );
		message = getWordFromMiddleMan();
		printf( "%s.\n", message );
		if( strcmp(message, "playlist") != 0)
			printf("receving sth else: | %s |\n", message);
		free(message);
		sendSongListToMiddleMan( songDetailList, numSongs );
	}

	IOWR_8DIRECT(leds, 0, 0);

	int cc;
	for(cc = 0; cc < numSongs; cc++)
		printf("%s\t", songDetailList[cc]->id );
	printf("\n");

	srand(cc);	//TODO: random number generator
	//shufflePlayList( playList, numSongs );
	while(1)
	{
		if( state == STOP || state == NEXT_PLAY || state == PREV_PLAY )
		{
			state = STOP;
			runStopState( songDetailList );	// update the LCD screen as well
			while( state == STOP) // wait until there is something to do
				state = updateState(state);
		}
		else if( state == PLAYING_NORMAL )
		{
			state = runPlayingState( songDetailList );
		}
		else
		{
			printf("Enter unknown state, %d\n", state);
			state = updateState(STOP);
		}
	}

	return 0;
}

/*
 * read keys and uart to update the current state
 * return the next state
 */
int updateState( int prevState )
{
	int returnState = prevState;
	if( prevState == STOP )
	{
		returnState = updateStateWhileStop();
	}
	else
	{
		returnState = updateStateWhilePlaying( prevState );
	}
	return returnState;
}

/*
 * return the next state
 */
int updateStateWhilePlaying( int prevState )
{
	int state = updateStateFromKeysWhilePlaying( prevState );
	if( isThereSomething() )
		return updateStateFromUART( prevState );
	return state;
}

/*
 * read uart to update the current state
 * precondition: there is data in the middleman
 */
int updateStateFromUART( int prevState )
{
	int state = prevState;
	char* temp = getWordFromMiddleMan();
	printf( "Message got from Middleman: %s.\n", temp );

	if( strcmp(temp, "P") == 0 )	//play or paused
	{
		if(prevState == PLAYING_NORMAL)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PAUSED ");
			alt_irq_disable(AUDIO_0_IRQ);
			state = PAUSED;
			sendStringToMiddleMan( "p" );
		}
		else if(prevState == STOP)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
		}
		else if(prevState == PAUSED)
		{
			alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
			alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
			alt_irq_enable(AUDIO_0_IRQ);
			state = PLAYING_NORMAL;
			sendStringToMiddleMan( "P" );
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
		volume--;
		printf("volume = %d\n", volume);
	}
	else if( strcmp(temp, "U") == 0)	//lower volume
	{
		sendStringToMiddleMan( "U" );
		volume++;
		printf("volume = %d\n", volume);
	}
	else if( strcmp(temp, "R") == 0)	//set to repeat the current song
	{
		repeat_flag = 1;
	}
	else if( strcmp(temp, "r") == 0)
	{
		repeat_flag = 0;
	}
	else if( strcmp(temp, "H") == 0)	//shuffle the play list
	{
		shufflePlayList( playList, playListSize );
	}
	else if( strcmp(temp, "h") == 0)
	{
		int* tmp = playList;
		playList = makeCopy(origList, playListSize);
		free(tmp);
	}
	else if( strcmp(temp, "I") == 0)
	{
		// send the song ID
		char* songID = (char*)malloc( 3 );
		sprintf( songID, "%d", playList[currSong] );
		printf("debuggg : %s\n", songID);
		sendHandShakedLongMessageToMiddleMan( 'I', songID );
		free( songID );
		sendOneSec = 1;
	}
	else if( strcmp(temp, "V") == 0)
	{
		// send the volume
		char* buf = (char*)malloc( 1 );
		sprintf( buf, "%d", volume );
		sendHandShakedLongMessageToMiddleMan( 'V', buf );
		free( buf );
	}
	else if( strcmp(temp, "Y") == 0)
	{
		char* buf = (char*)malloc( 1 );
		buf = getWordFromMiddleMan();
		volume = atoi( buf );
		free( buf );
		printf("volume: %d\n", volume);
	}
	else if( strcmp(temp, "Z") == 0)
	{
		receivePlayListFromMiddleMan( playList , &playListSize );
		int i;
		printf("New list\n");
		for(i = 0; i < playListSize; i++)
			printf("%d ", playList[i]);
		printf("\n");					// debug
		currSong = 0;
		state = STOP;

		// make a original copy of the new playlist
		int* tmp = origList;
		origList = makeCopy(playList, playListSize);
		free(tmp);
	}
	else if( strcmp(temp, "W") == 0)
	{
		sendStringToMiddleMan( "W" );
	}
	else if( strcmp(temp, "playlist") == 0)
	{
		printf("hehhehehehe\n");
		alt_irq_disable(AUDIO_0_IRQ);
		sendSongListToMiddleMan( songDetailList, numSongs );
		alt_irq_enable(AUDIO_0_IRQ);
	}
	else if( strcmp(temp, "DoD") == 0)
	{
		mixSong = A3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "Re") == 0)
	{
		mixSong = B3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "Mi") == 0)
	{
		mixSong = C3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "Fa") == 0)
	{
		mixSong = C4;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "So") == 0)
	{
		mixSong = D3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "La") == 0)
	{
		mixSong = E3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "Ti") == 0)
	{
		mixSong = F3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "DoU") == 0)
	{
		mixSong = G3;
		piano_index = 0;
		mix_flag = 1;
	}
	else if( strcmp(temp, "X") == 0)
	{
		char* songID = (char*)malloc( ID_LENGTH );
		songID = getWordFromMiddleMan();
		//currSong = findSong(songDetailList, numSongs, atoi(songID));
		currSong = findSongOnPlayList( playList, playListSize, atoi(songID) );
		printf("songID: %s currSong: %d\n", songID, currSong);
		state = JUMP_SONG;
		free( songID );
	}
	else if( strcmp(temp, "o") == 0)
	{
		sendOneSec = 0;
	}
	else if( strcmp(temp, "T") == 0)
	{
		sendOneSec = 1;
	}
	else if( strcmp(temp, "K") == 0)
	{
		lock_uart = 1;
	}
	else if( strcmp(temp, "k") == 0)
	{
		lock_uart = 0;
	}
	else if( strcmp(temp, "Q") == 0)
	{
		char* songID = (char*)malloc( 3 );
		sprintf( songID, "%d", playList[currSong] );
		sendHandShakedLongMessageToMiddleMan( 'Q', songID );
		free( songID );
	}
	else if( strcmp(temp, "E") == 0)
	{
		currSong = nextSong( 2, playListSize);
		if(state == PLAYING_NORMAL)
			state = NEXT_PLAY;
		else
			state = PLAYING_NORMAL;
	}

	free(temp);
	return state;
}

/*
 * read keys to update the current state
 * used while playing
 */
int updateStateFromKeysWhilePlaying( int prevState )
{
	int state = prevState;
	char key = IORD_8DIRECT(KEYS, 0);
	while(IORD_8DIRECT(KEYS, 0) != 0);

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
		state = NEXT_PLAY;
		//volume++;
		//printf("volume: %d\n", volume);
	}
	else if(key == 0x1)
	{
		state = PREV_PLAY;
		//volume--;
		//printf("volume: %d\n", volume);
		//sendSongListToMiddleMan( songDetailList, numSongs );	//debug
	}
	return state;
}

/*
 * return the next state
 */
int updateStateWhileStop()
{
	int returnState = -1;
	char key = 0;
	while(key == 0)
	{
		// wait for command
		key = IORD_8DIRECT(KEYS, 0);
		if ( isThereSomething() )
		{
			returnState = updateStateFromUART( STOP );
			break;
		}
	}
	while(IORD_8DIRECT(KEYS, 0) != 0);

	if(key == 0x8 || returnState == PLAYING_NORMAL)
	{
		return PLAYING_NORMAL;
	}
	else if(key == 0x4 || returnState == STOP)	//stop
	{
		return STOP;
	}
	else if(key == 0x2 || returnState == NEXT_PLAY)
	{
		currSong = nextSong( 1, playListSize );
		return NEXT_PLAY;
	}
	else if(key == 0x1 || returnState == PREV_PLAY)
	{
		currSong = nextSong( 0, playListSize );
		return PREV_PLAY;
	}
	else if( returnState == JUMP_SONG)
	{
		// currSong is set in updateStateFromUART
		return PLAYING_NORMAL;
	}
	return returnState;
}

/*
 * start playing music
 * return the next state
 */
int runPlayingState( SongDetail** list )
{
	short int file_handle;

	alt_up_character_lcd_init(char_lcd_dev);
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "PLAYING");
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
	alt_up_character_lcd_string(char_lcd_dev, list[ findSong(list, numSongs, playList[currSong]) ]->name);

	char temp[ID_LENGTH + EXTENSION_LENGTH];
	printf("currSong: %d  playList[currSong]: %d\n", currSong, findSong(list, numSongs, playList[currSong]) );
	strcpy( temp, list[ findSong(list, numSongs, playList[currSong]) ]->id );
	strcat( temp, ".wav" );
	if( openFileInSD( temp, &file_handle ) == 0)
	{
		printf("file %s opened\n", temp);
	}
	else
	{
		printf("failed to open %s wav\n", temp );
		exit(1);
	}

	// send the song ID to andriod
	char* songID = (char*)malloc( 3 );
	sprintf( songID, "%d", playList[currSong] );
	sendHandShakedLongMessageToMiddleMan( 'M', songID );
	free( songID );

	int state = playSong( file_handle, PLAYING_NORMAL, atoi(list[playList[currSong]]->time) );
	stopSong( file_handle );

	if(state == NEXT_PLAY || state == PLAYING_NORMAL)
	{
		currSong = nextSong( 1, playListSize );
		return PLAYING_NORMAL;
	}
	else if(state == PREV_PLAY)
	{
		currSong = nextSong( 0, playListSize );
		return PLAYING_NORMAL;
	}
	else if(state == JUMP_SONG)
	{
		// currSong is set in updateStateFromUART
		return PLAYING_NORMAL;
	}
	else
	{
		printf("unknown state in runPlayingState: %d\n", state);
	}
	return state;
}

/*
 * update the LCD screen in STOP state
 */
void runStopState( SongDetail** list )
{
	alt_irq_disable(AUDIO_0_IRQ);
	alt_up_character_lcd_init(char_lcd_dev);
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 0);
	alt_up_character_lcd_string(char_lcd_dev, "STOP            ");
	alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);
	alt_up_character_lcd_string(char_lcd_dev, list[ findSong(list, numSongs, playList[currSong]) ]->name);
}

/*
 * Audio interrupt handler
 * which is used to play a song
 * play stream A when stream_flag == 0
 * otherwise play stream B
 */
void audio_isr (void * context, unsigned int irq_id)
{
	unsigned int song_sample[96];

	// fill 96 samples
	int cc;
	for(cc = 0; cc < SAMPLE_SIZE; cc++)
	{
		song_sample[cc] = stream[play_index];

		if(mix_flag == 1)	//piano
		{
			song_sample[cc] = song_sample[cc] + mixSong[piano_index];
			piano_index++;
			if(piano_index == PIANO_SONG_SIZE)
			{
				piano_index = 0;
				mix_flag = 0;
			}
			//if(((song_sample & 0x10)>>4) == 1)
			//	song_sample[cc] = song_sample[cc]>>1;
		}

		play_index++;
		if(play_index == SONG_SIZE)
		{
			play_index = 0;
			isOneSec = 1;
		}

		if(stop_flag && play_index == read_index)
		{
			stop_flag = 0;
			return;
		}

		// lower the volume
		if(volume == 0)
			song_sample[cc] = 0;
		else
		{

			song_sample[cc] = song_sample[cc] << volume;
		}
		//song_sample[cc] = song_sample[cc] & 0x00FFFFFF;	//try this
	}

	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_LEFT);
	alt_up_audio_write_fifo(audio, song_sample, SAMPLE_SIZE, ALT_UP_AUDIO_RIGHT);
}

/*
 * play a song
 * pass in the current state
 * return the next state
 */
int playSong( short int file_handle, int currState, int length)
{
	alt_irq_disable(AUDIO_0_IRQ);

	int buf[2];
	play_index = 0;
	read_index = 0;
	piano_index = 0;
	stop_flag = 0;
	isOneSec = 0;

	// read the 1st second
	int i = 0;
	for(i = 0; i < WAV_HEADER_SIZE; i++)
		alt_up_sd_card_read( file_handle );

	for(read_index = 0; read_index < 96; read_index++)
	{
		buf[0] = alt_up_sd_card_read( file_handle );
		if( buf[0] < 0 )	//reach eof
			break;
		buf[1] = alt_up_sd_card_read( file_handle );
		stream[read_index] = ((buf[1]<<8)|buf[0]);
	}

	int eof = 0;
	int wasPaused = 0;
	alt_irq_enable(AUDIO_0_IRQ);

	volatile int state = PLAYING_NORMAL;
	int time = 0;
	char buffer[5];
	while(1)
	{
		state = updateState(state);
		while(state == PAUSED)
		{
			alt_irq_disable(AUDIO_0_IRQ);
			state = updateState(state);
			wasPaused = 1;
		}

		if(wasPaused)
		{
			alt_irq_enable(AUDIO_0_IRQ);
			wasPaused = 0;
		}

		if(state != PLAYING_NORMAL)
		{
			printf("state != PLAYING_NORMAL %d\n", state);
			return state;
		}

		if(eof == 1)	// done
		{
			while(stop_flag == 1);
			return PLAYING_NORMAL;
		}

		while(read_index == play_index);

		if(!eof)
		{
			buf[0] = alt_up_sd_card_read( file_handle );
			if( buf[0] < 0 )	//reach eof
			{
				eof = 1;
				stop_flag = 1;
				continue;
			}
			buf[1] = alt_up_sd_card_read( file_handle );

			stream[read_index] = ((buf[1]<<8)|buf[0]);
			if( (stream[read_index] & 0x8000) > 0 )
				stream[read_index] = stream[read_index] | 0xFFFF0000;
			stream[read_index] = stream[read_index];
			read_index++;
			if(read_index == SONG_SIZE)
			{
				read_index = 0;
			}

			if(isOneSec)
			{
				time++;
				IOWR_8DIRECT(leds, 0, (char)(stream[play_index]>>4));
				sprintf( buffer, "%d", (int)time );
				if(sendOneSec == 1)
					sendHandShakedLongMessageToMiddleMan( 'O', buffer );
				isOneSec = 0;
			}
		}
	}
	printf("should not get to here\n");
	return PLAYING_NORMAL;
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
	memset(&stream, 0, sizeof(stream));
	closeFileInSD(file_handle);
}

/*
 * set the next song playing
 * if next == 0, play the previous song
 * if next == 1, play the next song
 * otherwise, play a random song
 */
int nextSong( int next, int size )
{
	int tmp = 0;

	if(repeat_flag == 0)
	{
		if(next == 1)
		{
			tmp = (currSong + 1) % size;
		}
		else if(next == 0)
		{
			if(currSong == 0)
				tmp = size - 1;
			else
				tmp  = (currSong - 1) % size;
		}
		else
		{
			tmp = currSong;
			while(tmp == currSong)
				tmp = rand() % size;
		}
	}
	else
	{
		printf("repeat\n");	//debug
		return currSong;
	}

	return tmp;
}

/*
 * initialize connections to hardware
 */
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

	/* UART RS232 */
	uart = alt_up_rs232_open_dev("/dev/rs232_0");

	clearMiddleManBuffer();

	/* Interrupt */
	alt_up_audio_enable_write_interrupt(audio);
	alt_irq_register(AUDIO_0_IRQ, 0, (alt_isr_func)audio_isr);
	alt_irq_disable(AUDIO_0_IRQ);
}

/* Receives play list from Middle man
 * Stores the play list in playList
 * Stores the size of the playList in size
 * Pre: playList != null && size != null
 */
void receivePlayListFromMiddleMan( int* playList, int* size )
{
	char* buffer;
	int i;

	printf("Receiving play list from MiddleMan\n");

	buffer = getWordFromMiddleMan();
	int numToReceive = atoi( buffer );
	free( buffer );
	printf("Number of Songs to receive is: %d\n", numToReceive );

	for ( i = 0; i < numToReceive; i++ )
	{
		buffer = getWordFromMiddleMan();
		if ( strcmp( buffer, "H" ) == 0 )
		{
			sendStringToMiddleMan( "H" );
			i--;
			continue;
		}

		playList[i] = atoi( buffer );
		free( buffer );
		printf( "playList[%d] received is: %d\n", i, playList[i] );
	}
	printf( "Done Receiving play list from MiddleMan\n" );

	*size = i;
	currSong = 0;

	free( buffer );
}

void sendSongListToMiddleMan( SongDetail** songList, int numSong )
{
	int i;

	char* temp = malloc( 1 );
	sprintf( temp, "%d", volume );
	char* temp1 = malloc( 5 );	// need to make a constant
	sendStringToMiddleMan( temp );
	sendStringToMiddleMan( "." );

	char* songID = malloc(3);
	sprintf( songID, "%d", playList[currSong]);
	sendStringToMiddleMan( songID );
	sendStringToMiddleMan( "." );
	free(songID);

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
	if(lock_uart == 1)
		return;
	int i;
	char buffer[5]; // Gonna change the magic number

	alt_up_rs232_write_data( uart, command );
	//printf( "Sent command: %c\n", command );	// debug

	sprintf( buffer, "%d", (int)strlen( str ) );
	alt_up_rs232_write_data( uart, (int) strlen(buffer) );  // Number of digits of the number of chars of data
	//printf( "Sent %d\n", (int) strlen( buffer ) );

	char* buffer2 = malloc( (int) strlen( buffer ) + 1 );
	sprintf( buffer2, "%d", (int) strlen( str ) );
	sendStringToMiddleMan( buffer2 );  // Number of chars of data
	//printf( "Sent %s\n", buffer2 );

	for ( i = 0; i < strlen(str); i++ )
		alt_up_rs232_write_data( uart, str[i] );
	//printf("sendHandShakedLongMessageToMiddleMan done\n");	// debug
	free( buffer2 );
}

/* Sends one string to the middle man
 * str cannot be NULL
 */
void sendStringToMiddleMan( char* str )
{
	if(lock_uart == 1)
		return;
	int i;

	//alt_up_rs232_write_data( uart, (unsigned char) strlen(str) );

	for ( i = 0; i < strlen(str); i++ )
		alt_up_rs232_write_data( uart, str[i] );

	//printf("sendStringToMiddle: %s\n", str);	//debug
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

/* Opens a file and stores the file_handle in the memory pointed by file_handle_ptr
 * return 0 if successful, otherwise -1
 */
char openFileInSD( char* fileName, short int* file_handle_ptr )
{
	short int file_handle;

	if ( alt_up_sd_card_is_Present() )
	{
		if ( alt_up_sd_card_is_FAT16() )
		{
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
		printf( "Song: %s %s %s %s %s\n", songList[i]->id, songList[i]->name, songList[i]->artist, songList[i]->rating, songList[i]->time );
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

/* Use this function to update currSong when user clicks a song from android device
 * Given the id of the song, finds the song from the song detail list
 * Returns the index of the song in the list if successful, otherwise -1.
 */
int findSong( SongDetail** list, int numSong, int id )
{
	int i;
	if ( !list || !id )
		return -1;
	for ( i = 0; i < numSong; i++ )
	{
		if ( atoi(list[i]->id) == id )
			return i;
	}
	return -1;
}

/*
 * shuffle the numbers in the list
 */
void shufflePlayList( int* list, int size )
{
	int i, j, tmp;
	for(i = size - 1; i >= 1; i--)
	{
		j = rand() % i;
		tmp = list[i];
		list[i] = list[j];
		list[j] = tmp;
	}

	printf("new list:\n");
	for(i = 0; i < size; i++)
		printf("%d\t", list[i]);
	printf("\n");
}

/*
 * read a music clip to memory
 */
void readSong( volatile unsigned int* song, int size, char* fileName)
{
	short int file_handle;
	int cc;
	int buf[2];
	if( openFileInSD( fileName, &file_handle ) == 0)
	{
		for(cc = 0; cc < WAV_HEADER_SIZE; cc++)
			alt_up_sd_card_read( file_handle );
		printf("file %s opened\n", fileName );
	}
	else
	{
		printf("failed to open %s\n", fileName );
		return;
	}

	for(cc = 0; cc < size; cc++)
	{
		buf[0] = alt_up_sd_card_read( file_handle );
		if( buf[0] < 0 )	//reach eof
			break;
		buf[1] = alt_up_sd_card_read( file_handle );

		song[cc] = ((buf[1]<<8)|buf[0]);
		if( (song[cc] & 0x8000) > 0 )
			song[cc] = song[cc] | 0xFFFF0000;
		song[cc] = song[cc]<<1;
	}
	closeFileInSD( file_handle );
}

/**
 * play piano from switches
 */
void playPianoBySW()
{
	unsigned char sw = IORD_8DIRECT(SWITCHES, 0);
	if(sw == 1)
		mixSong = A3;
	else if(sw == 2)
		mixSong = B3;
	else if(sw == 4)
		mixSong = C3;
	else if(sw == 8)
		mixSong = C4;
	else if(sw == 16)
		mixSong = D3;
	else if(sw == 32)
		mixSong = E3;
	else if(sw == 64)
		mixSong = F3;
	else if(sw == 0x80)
		mixSong = G3;

	if(sw == 1)
		mix_flag = 1;
}

/**
 * return a copy of list
 */
int* makeCopy( int* list, int listSize )
{
	int* newList = malloc(listSize * sizeof(int));
	int i;
	for(i = 0; i < listSize; i++)
		newList[i] = list[i];
	return newList;
}


/**
 * return index of ID in list
 */
int findSongOnPlayList( int* list, int size, int id)
{
	int i;
	for(i = 0; i < size; i++)
		if(list[i] == id)
			return i;
	return -1;
}
