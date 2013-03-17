#include "system.h"
#include "stdio.h"
#include <stdlib.h>
#include "altera_up_avalon_character_lcd.h"
#include "altera_up_avalon_parallel_port.h"
#include "altera_up_avalon_video_pixel_buffer_dma.h"
#include "altera_up_avalon_video_character_buffer_with_dma.h"
#include <altera_up_sd_card_avalon_interface.h>
//#include "sys/alt_stdio.h"
//#include "sys/alt_timestamp.h"

#define switches (volatile char *) 0x00004420
#define leds (char *) 0x00004430
#define keys (char *) 0x00004440
#define PIXEL_BUFFER_BASE (char *) 0x00080000

#define MAX_FILE_NAME_LENGTH 20 // it's not 20 but let's just use that for now
#define MAX_NAME_LENGTH 25
#define MAX_NUM_SCORE_LIST 5
#define MAX_OF_A_BYTE 256

typedef unsigned short int usi;

/* SD Card Functions */
short int updateScoreBoard( char* newName, usi newScore );
void sortScore( char (*name)[MAX_NAME_LENGTH], usi* score , char* newName, usi newScore );
char closeFileInSD( short int file_handle );
char openFileInSD( char* fileName, short int* file_handle_ptr );
char resetScoreFile( void );
void copyString( char* src, char* dest );

/* SD Read */
char readAllNamesAndScoresFromSD( char (*name)[MAX_NAME_LENGTH], usi* score );
char readNameAndScoreFromSD( char* name, usi* score, short int file_handle );
char readNameFromSD( char* name, short int file_handle );
int readDataFromSD( short int file_handle );

/* SD Write */
short int writeAllNamesAndScoresToSD( char (*name)[MAX_NAME_LENGTH], usi *score );
short int writeNameAndScoreToSD( char* name, usi score, short int file_handle );
char writeNameToSD( char* name, short int file_handlle );
short int writeDataToSD( usi value, short int file_handle );




/* Device references */
alt_up_pixel_buffer_dma_dev* pixel_buffer;
alt_up_parallel_port_dev *KEY_dev;
alt_up_char_buffer_dev *char_buffer;
alt_up_character_lcd_dev * char_lcd_dev;
alt_up_parallel_port_dev *switch_reference;
alt_up_sd_card_dev *sd_card_reference;

//draw a heli at (x,y)
void draw_heli(int x, int y, int color) {
	/*
	 alt_up_pixel_buffer_dma_draw(pixel_buffer, 0xFFFF, x-1, y);
	 alt_up_pixel_buffer_dma_draw(pixel_buffer, 0xFFAA, x+1, y);
	 alt_up_pixel_buffer_dma_draw(pixel_buffer, 0xFFAA, x, y);
	 alt_up_pixel_buffer_dma_draw(pixel_buffer, 0xFFAA, x, y-1);
	 alt_up_pixel_buffer_dma_draw(pixel_buffer, 0xFFAA, x, y+1);
	 */
	//alt_up_pixel_buffer_dma_draw_line(pixel_buffer, 0, 0, 319, 239, 0xAAFF, 0);
	alt_up_pixel_buffer_dma_draw_box(pixel_buffer, x, 50, x + 3, 100, color, 0);
}

void initialization()
{
	//Keys: parallel port
	KEY_dev = alt_up_parallel_port_open_dev("/dev/KEYs");
	if (KEY_dev == NULL)
		printf("Error: could not open KEY parallel port\n");
	//int KEY_value = alt_up_parallel_port_read_data (KEY_dev);

	//VGA characters
	char_buffer = alt_up_char_buffer_open_dev(
			"/dev/video_character_buffer_with_dma_0");
	alt_up_char_buffer_init(char_buffer);
	//alt_up_char_buffer_string(char_buffer, "testing", 40, 30);


	//LCD screen
	// open the Character LCD port
	char_lcd_dev = alt_up_character_lcd_open_dev(CHARACTER_LCD_0_NAME);
	if (char_lcd_dev == NULL)
		printf("Error: could not open character LCD device\n");

	// Initialize the character display
	alt_up_character_lcd_init(char_lcd_dev);
	//alt_up_character_lcd_string(char_lcd_dev, "Push_button TEST!");
	//alt_up_character_lcd_set_cursor_pos(char_lcd_dev, 0, 1);

	//VGA
	pixel_buffer = alt_up_pixel_buffer_dma_open_dev(
			"/dev/video_pixel_buffer_dma_0");
	if (pixel_buffer == NULL)
		printf("Error: VGA\n");

	unsigned int pixel_buffer_addr1 = PIXEL_BUFFER_BASE;
	unsigned int pixel_buffer_addr2 = PIXEL_BUFFER_BASE + (320 * 240 * 2);

	// 2 buffers
	// Set the 1st buffer address
	alt_up_pixel_buffer_dma_change_back_buffer_address(pixel_buffer,
			pixel_buffer_addr1);

	// Swap buffers 좻 we have to swap because there is only an API function
	// to set the address of the background buffer.
	alt_up_pixel_buffer_dma_swap_buffers(pixel_buffer);
	while (alt_up_pixel_buffer_dma_check_swap_buffers_status(pixel_buffer))
		;

	// Set the 2nd buffer address
	alt_up_pixel_buffer_dma_change_back_buffer_address(pixel_buffer,
			pixel_buffer_addr2);

	// Clear both buffers (this makes all pixels black)
	alt_up_pixel_buffer_dma_clear_screen(pixel_buffer, 0);
	alt_up_pixel_buffer_dma_clear_screen(pixel_buffer, 1);


	/* SD card reader initialization */
	sd_card_reference = alt_up_sd_card_open_dev( "/dev/Altera_UP_SD_Card_Avalon_Interface_0" );

	if ( sd_card_reference )
	{
		printf( "SD Card port opened.\n" ); // debugging purpose
	}
	else
	{
		printf( "Error: SD card port not opened.\n" );
	}
}

int main() {
	initialization();

	// Draw a white line to the foreground buffer
	//alt_up_pixel_buffer_dma_draw_line(pixel_buffer, 0, 30, 319, 30, 0xACFF, 0);

	//alt_up_pixel_buffer_dma_draw_line(pixel_buffer, 0, 130, 319, 130, 0xCCFF, 0);
	//alt_up_pixel_buffer_dma_draw_line(pixel_buffer, 120, 230, 120, 230, 0xBBBB, 0);
	//alt_up_pixel_buffer_dma_draw_box(pixel_buffer, 90, 200, 120, 230, 0xBBBB, 1);

	//alt_up_pixel_buffer_dma_swap_buffers(pixel_buffer);

	//alt_up_pixel_buffer_dma_draw_box(pixel_buffer, 20, 200, 40, 230, 0xBBAA, 0);

	int cc = 0;
	int obj_x = 50;
	int obj_y = 100;
	int box_1_x = 200;
	int box_1_y = 90;
	// score is test data for now
	usi score = 31200; // I assume for now the score can't get higher than 65279.
	char* name = malloc( sizeof(char) * MAX_NAME_LENGTH );
	name = "YOOOO";
	int read = 0; // debbugging purpose


	int KEY_value;

	alt_up_pixel_buffer_dma_draw_box(pixel_buffer, 0, 0, 319, 239, 0xFFAA, 0);

	while (1)
	{
		KEY_value = alt_up_parallel_port_read_data(KEY_dev);
		if (KEY_value == 0x8)
			obj_y--;
		if (KEY_value == 0x4)
			obj_y++;

		box_1_x--;

		// Increase score by 1 whenever the fish moves 1 pixel to the right.
		//score++;

		//draw on background
		alt_up_pixel_buffer_dma_draw_box(pixel_buffer, obj_x, obj_y,
				obj_x + 15, obj_y + 8, 0xAAAA, 1);

		if (box_1_x >= 0)
			alt_up_pixel_buffer_dma_draw_box(pixel_buffer, box_1_x, box_1_y,
					box_1_x + 12, box_1_y + 30, 0xFFDD, 1);

		alt_up_char_buffer_string(char_buffer, "Score:", 5, 5);

		//delay
		//usleep(10000); //0.01s
		//usleep(100000); //0.1s
		//usleep(1000000); //1s

		//switch buffer
		alt_up_pixel_buffer_dma_swap_buffers(pixel_buffer);

		//clear background
		alt_up_pixel_buffer_dma_clear_screen(pixel_buffer, 1);


		if ( !read )
		{

			// Call this function to reset the highscores
			if ( resetScoreFile() != 0 ) // nononono
			{
				printf( "Error: Score.txt file reset Failed.\n" );
				return -1;
			}


			if ( updateScoreBoard( name, score ) != 0 )
			{
				printf( "Error: updateScoreBoard Failed.\n" );
				return -1;
			}
			read = 1;
		}
	}

	free( name );

	return 0;
}

/* EXPLICITLY for debugging purpose only
 * Resets SCORE.TXT file
 * Returns 0 if successful, otherwise -1
 */
char resetScoreFile()
{
	short int file_handle;
	int i;

	if ( openFileInSD( "SCORE.TXT", &file_handle ) != 0 )
	{
		printf( "Error: Cannot open the file.\n" );
		return -1;
	}

	/* Placeholder list of names and scores */
	for ( i = 0; i < MAX_NUM_SCORE_LIST; i++ )
	{
		//printf( "Writing this data: %s\t%d.\n", "CCC", 50 );	// debugging purpose
		if ( writeNameAndScoreToSD( "CCC", 50, file_handle ) != 0 )
		{
			printf( "Error: writeDataToSD Failed.\n" );
			return -1;
		}
	}

	closeFileInSD( file_handle );
	return 0;
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
		//printf( "File successfully closed.\n" ); // debugging purpose
		return 0;
	}
	else
	{
		printf( "Error: File not closed.\n" );
		return -1;
	}
}

/* Copy string from src to dest */
void copyString( char* src, char* dest )
{
	char* temp = src;

	while ( *temp != '\0' )
	{
		*dest = *temp;
		dest++;
		temp++;
	}

	*dest = *temp;
}

/* Sorts the score board after comparing the new score to original scores */
void sortScore( char (*name)[MAX_NAME_LENGTH], usi* score , char* newName, usi newScore )
{
	int i;
	int j;

	for ( i = 0; i < MAX_NUM_SCORE_LIST; i++ )
	{
		if ( newScore >= score[i] )
		{
			/* Shift down scores that are lower than the new score*/
			for ( j = MAX_NUM_SCORE_LIST-1; j > i; j-- )
			{
				copyString( name[j-1], name[j] );
				score[j] = score[j-1];
			}

			/* Put the new score in the appropriate place */
			copyString( newName, name[i] );
			score[i] = newScore;

			break;
		}
	}
}

/* Updates the score board
 * Reads the score board from the SD Card
 * Sorts accordingly after comparing the score board to the new score
 * Stores the updated score board back into the SD Card
 * Returns 0 if successful, otherwise -1
 */
short int updateScoreBoard( char* newName, usi newScore )
{
	char name[MAX_NUM_SCORE_LIST][MAX_NAME_LENGTH];
	usi* score = malloc( MAX_NUM_SCORE_LIST * sizeof( usi ) );
	int k;

	/* Read original scoreboard from the SD Card */
	if ( readAllNamesAndScoresFromSD( name, score ) != 0 )
	{
		printf( "Error: readAllNamesAndScoresFromSD Failed.\n" );
		return -1;
	}

	/* print out scoreboard for dubugging purpose */
	for ( k = 0; k < MAX_NUM_SCORE_LIST; k++ )
	{
		printf( "Original Name: %s\tScore: %d.\n", name[k], score[k] );
	}

	/* Sort accordingly with the new score */
	sortScore( name, score, newName, newScore );

	/* Write new scoreboard to the SD Card */
	if ( writeAllNamesAndScoresToSD( name, score ) != 0 )
	{
		printf( "Error: writeAllNamesAndScoresToSD Failed.\n" );
		return -1;
	}

	/* print out scoreboard for dubugging purpose */
	for ( k = 0; k < MAX_NUM_SCORE_LIST; k++ )
	{
		printf( "New Name: %s\tScore: %d.\n", name[k], score[k] );
	}

	/* For unknown reason, this function has to be called to work */
	if ( readAllNamesAndScoresFromSD( name, score ) != 0 )
	{
		printf( "Error: readAllNamesAndScoresFromSD Failed.\n" );
		return -1;
	}

	free( score );
	return 0;
}

/* Pre: name is a 2D array of char and each row has to have MAX_NAME_LENGTH of bytes of memory
 * 		score is a 1D array of size MAX_NUM_SCORE_LIST
 * Post: names are stored in name
 * 		 scores are stored in score
 * 		 related names and scores are in the same index
 * Returns 0 if sucessful, otherwise -1
 */
char readAllNamesAndScoresFromSD( char (*name)[MAX_NAME_LENGTH], usi* score )
{
	short int file_handle;
	int i;

	if ( openFileInSD( "SCORE.TXT", &file_handle ) != 0 )
	{
		printf( "Error: Cannot open the file.\n" );
		return -1;
	}

	printf( "Reading All Names and Scores.\n" );

	/* num has to be less than or equal to MAX_NUM_SCORE_LIST */
	for ( i = 0; i < MAX_NUM_SCORE_LIST; i++ )
	{
		if ( readNameAndScoreFromSD( name[i], &score[i], file_handle ) != 0 )
		{
			printf( "Error: readNameAndScoreFromSD Failed.\n" );
			closeFileInSD( file_handle );
			return -1;
		}
	}

	closeFileInSD( file_handle );
	return 0;
}

/* Pre: name has to have at least MAX_NAME_LENGTH bytes of memory allocated
 * 		score's memory is adequately allocated
 * Post: the memory pointed by name has the name
 * 		 the memory pointed by score has the score
 * Returns 0 if sucessful, otherwise -1
 */
char readNameAndScoreFromSD( char* name, usi* score, short int file_handle )
{
	int temp;

	if ( readNameFromSD( name, file_handle ) != 0 )
	{
		printf( "Error: readNameFromSD Failed.\n" );
		return -1;
	}

	temp = readDataFromSD( file_handle );
	if ( temp < 0 )
	{
		printf( "Error: readDataFromSD Failed.\n" );	//
		return -1;
	}
	*score = temp;

	return 0;
}

/* Writes all names and score to the SD Card in order
 * Returns 0 if successful, otherwise -1
 */
short int writeAllNamesAndScoresToSD( char (*name)[MAX_NAME_LENGTH], usi *score )
{
	short int file_handle;
	int i;

	if ( openFileInSD( "SCORE.TXT", &file_handle ) != 0 )
	{
		printf( "Error: Cannot open the file.\n" );
		return -1;
	}

	printf( "Writing All Names and Scores.\n" );

	for ( i = 0; i < MAX_NUM_SCORE_LIST; i++ )
	{
		//printf( "Writing this data: %s\t%d.\n", name[i], score[i] );
		if ( writeNameAndScoreToSD( name[i], score[i], file_handle ) != 0 )
		{
			printf( "Error: writeNameAndScoreToSD Failed.\n" );
			closeFileInSD( file_handle );
			return -1;
		}
	}

	closeFileInSD( file_handle );
	return 0;
}

/* Writes a name and score to the SD Card
 * Returns 0 if successful, otherwise -1
 */
short int writeNameAndScoreToSD( char* name, usi score, short int file_handle )
{
	if ( writeNameToSD( name, file_handle ) != 0 )
	{
		printf( "Error: WriteNameToSD Failed.\n" );
		return -1;
	}
	if ( writeDataToSD( score, file_handle ) != 0 )
	{
		printf( "Error: WriteDataToSD Failed.\n" );	//
		return -1;
	}

	return 0;
}

/* Pre: name has to have at least MAX_NAME_LENGTH bytes of memory allocated
 * Post: the memory pointed by name has the string
 * Returns 0 if sucessful, otherwise -1
 */
char readNameFromSD( char* name, short int file_handle )
{
	char nameStr[MAX_NAME_LENGTH];
	int i = 0;
	char ch;
	int temp;

	temp = readDataFromSD( file_handle );
	//printf( "Data Read is: %d.\n", temp );
	ch = temp;

	while ( ch != -1 && ch != '\0' )
	{
		nameStr[i] = ch;
		i++;

		if ( i > MAX_NAME_LENGTH )
		{
			printf( "Error: Name is longer than the maximum length allowed.\n" );
			return -1;
		}

		ch = readDataFromSD( file_handle );
	}

	if ( ch == -1 )
	{
		printf( "Error: readDataFromSD Failed.\n" );
		return -1;
	}

	/* put the null character */
	nameStr[i] = ch;

	/* Copy contents in nameStr to the memory pointed by name */
	copyString( nameStr, name );

	return 0;
}

/* Writes a name to the SD Card
 * Returns 0 if successful, otherwise -1
 */
char writeNameToSD( char* name, short int file_handle )
{
	char* namePtr = name;

	while ( *namePtr != '\0' )
	{
		if ( writeDataToSD( *namePtr, file_handle ) != 0 )
		{
			printf( "Error: WriteDataToSD Failed.\n" );
			return -1;
		}
		namePtr++;
	}
	if ( writeDataToSD( '\0', file_handle ) != 0 )
	{
		printf( "Error: WriteDataToSD Failed.\n" );
		return -1;
	}

	return 0;
}

/* Can only write a positive value
 * Returns 0 if successful, otherwise -1
 */
short int writeDataToSD( usi value, short int file_handle )
{
	unsigned char valueHighByte;
	unsigned char valueLowByte;

	//printf( "Value going to be written is: %d.\n", value );

	//value & 0xFF00 >> 8
	//value & 0x00FF

	valueHighByte = value / MAX_OF_A_BYTE;
	valueLowByte = value % MAX_OF_A_BYTE;
	if ( value == 1 )
	{
		//printf( "valueHighByte going to be written is: %d.\n", valueHighByte );
		//printf( "valueLowByte going to be written is: %d.\n", valueLowByte );
	}

	if ( !alt_up_sd_card_write( file_handle, valueHighByte ) )
		printf( "Error: alt_up_sd_card_write failed.\n" );

	if ( !alt_up_sd_card_write( file_handle, valueLowByte ) )
		printf( "Error: alt_up_sd_card_write failed.\n" );

	return 0;
}

/* Reads the first data in the file
 * Returns a positive number if data successful, otherwise -1
 */
int readDataFromSD( short int file_handle )
{
	unsigned char valueHighByte;
	unsigned char valueLowByte;
	int value;

	/* Read the first byte of the 2 bytes memory */
	value = alt_up_sd_card_read( file_handle );
	if ( value < 0 )
	{
		printf( "Error: alt_up_sd_card_read Failed.\n" );
		return -1;
	}
	valueHighByte = value;
	//printf( "valueHighByte read is: %d.\n", valueHighByte );

	/* Read the second byte of the 2 bytes memory */
	value = alt_up_sd_card_read( file_handle );
	if ( value < 0 )
	{
		printf( "Error: alt_up_sd_card_read Failed.\n" );
		return -1;
	}
	valueLowByte = value;
	//printf( "valueLowByte read is: %d.\n", valueLowByte );

	value = valueHighByte * MAX_OF_A_BYTE + valueLowByte;
	//printf( "Value read is: %d.\n", value );

	return value;
}

/* leave this for now
//When using sd card. For now, since we don't have menu, i will just use a switch.
//char *fileName = (char*)malloc( MAX_FILE_NAME_LENGTH * sizeof(char) );
//short int isFileFound = 0;
//short int find_first = 0;
//short int find_next = 0;
//short int write_error = 0;
//unsigned char readData;

// Find the name of the first file
find_first = alt_up_sd_card_find_first( "", fileName );

switch ( find_first )
{
case -1:
	printf( "Error: No file in this directory.\n" );
	isFileFound = 0;
	break;
case 0:
	printf( "Found a file.\n" ); // debugging purpose
	isFileFound = 1;
	break;
case 1:
	printf( "Error: Invalid directory.\n" );
	isFileFound = 0;
	break;
case 2:
	printf( "Error: Something wrong with the card\n" );
	isFileFound = 0;
	break;
default:
	printf( "Error: should never come here.\n" );
	isFileFound = 0;
}

// List all the files in the directory
if ( isFileFound );
{
	printf( "File list: %s", fileName );

	// Find next files
	find_next = alt_up_sd_card_find_next( fileName );
	while ( find_next == 0 )
	{
		printf( " %s", fileName );
		find_next = alt_up_sd_card_find_next( fileName );
	}
	printf( "\n" );
}
*/

