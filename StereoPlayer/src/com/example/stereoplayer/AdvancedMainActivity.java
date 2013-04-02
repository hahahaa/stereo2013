package com.example.stereoplayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

public class AdvancedMainActivity extends Activity
{
	/* Set this to true if debugging saving and loading play list and/or rating list*/
	boolean Debug = false;
	
	/* Constants */
	final static int ONE_BYTE = 1;
	final static int MAX_BYTES = 255;
	
	/* Variables */	
	private MyApplication app;
	private ArrayList<String[]> mainPlaylist;
	private String[] rawPlaylist;
	private int songIndex;
	private int songVolume;
	private TCPReadTimerTask tcp_task;
	
	public class TCPReadTimerTask extends TimerTask 
	{		
		public void run() 
		{
			MyApplication app = (MyApplication) getApplication();
			if (app.sock != null && app.sock.isConnected() && !app.sock.isClosed()) 
			{
				try 
				{
					InputStream in = app.sock.getInputStream();
					int bytes_avail = in.available();
					
					if (bytes_avail > 0) 
					{						
						byte buf[] = new byte[ONE_BYTE];
						in.read(buf);
						String msg = new String(buf, 0, ONE_BYTE, "US-ASCII");
						String data = new String();
						//Log.i("AdvancedMain", "Handshake - msg to compare is: " + msg);
					
						if (msg.compareTo("M") == 0 || msg.compareTo("I") == 0 || msg.compareTo("O") == 0 )
						{							
							while ( in.available() == 0 );
							bytes_avail = in.available();
							
							byte buffer[] = new byte[ONE_BYTE];
							in.read(buffer);
							int numBytesOfNumber = buffer[0];
							//Log.i("AdvancedMain", "Handshake - numBytesOfNumber is: " + numBytesOfNumber);
							
							buffer = new byte[numBytesOfNumber];
							
							while( in.available() < numBytesOfNumber );
							in.read(buffer);
							String temp = new String( buffer, 0, numBytesOfNumber, "US-ASCII" );
							
							int numBytesOfData = Integer.parseInt( temp );
							//Log.i("AdvancedMain", "Handshake - numBytesOfData is: " + numBytesOfData );
							
							buffer = new byte[ONE_BYTE];
							for ( int i = 0; i < numBytesOfData; )
							{
								if ( in.available() > 0 )
								{
									in.read(buffer);
									//Log.i("AdvancedMain", "Handshake - data to concat is: " + buffer[0]);
									data = data.concat( new String(buffer, 0, ONE_BYTE, "US-ASCII") );
									i++;
								}
							}
							//Log.i("AdvancedMain", "Handshake - Data is: " + data);
						}
						
						final String command = new String(msg);
						final String message = new String( data );

						Log.i("AdvancedMain", "Handshake - Commands is: " + command);
						Log.i("AdvancedMain", "Handshake - message is: " + message);
						
						if (msg.compareTo("M") == 0)
						{
							Log.i("Shuffle", "String data is: " + data);
							Log.i("Shuffle", "String message is: " + message);
						}
						
						
						runOnUiThread(new Runnable() 
						{
							public void run() 
							{								
								Log.i("Prog", "Started Run On UI Thread");

								TextView volumeT = (TextView) findViewById(R.id.viewText1);
								TextView text = (TextView) findViewById(R.id.viewText2);

								
								if ( command.compareTo( "P" ) == 0 )
								{
									text.setText("Playing: " + mainPlaylist.get(songIndex)[1]);
								}
								else if (command.compareTo("p") == 0) 
								{
									text.setText("Paused");
								} 
								else if (command.compareTo("S") == 0) 
								{
									text.setText("Stoppped");
								} 
								else if (command.compareTo("U") == 0) 
								{
									volumeT.setText("Volume = " + Integer.toString(songVolume));
								} 
								else if (command.compareTo("D") == 0) 
								{
									volumeT.setText("Volume = " + Integer.toString(songVolume));
								} 
								/* Deprecated */
								else if (command.compareTo("N") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
									pb.setProgress( 0 );
								}
								/* Deprecated */
								else if (command.compareTo("L") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
									pb.setProgress( 0 );
								} 
								else if (command.compareTo("O") == 0) 
								{
									int currentSongPositionInTime = Integer.parseInt( message );
									
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
									int songLength = Integer.parseInt( mainPlaylist.get(songIndex)[4] );
									int currentProgress = (int) ( ((double) currentSongPositionInTime) / (double) songLength * 100.0 );
									pb.setProgress( currentProgress );
									
									updateTime( currentSongPositionInTime, songLength );

									Log.i("Prog", "currentSongPosition is: " + currentSongPositionInTime );
								}
								else if (command.compareTo("M") == 0) 
								{
									Log.i( "AdvancedMain", "Successfully reached here" );
									
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
									pb.setProgress( 0 );
									
									songIndex = Integer.parseInt( message );
									Log.i("indexNumber", Integer.toString(songIndex));
									text.setText("Playing: " + mainPlaylist.get(songIndex)[1] );
									
									setupTime( Integer.parseInt( mainPlaylist.get(songIndex)[4] ) );
								}
								else if ( command.compareTo( "I" ) == 0 )
								{
									songIndex = Integer.parseInt( message );
									Log.i("indexNumber", Integer.toString(songIndex));
								}
								else
								{
									
								}
							}
						});
					}
					
					/* Debugging purpose */
					if ( Debug )
					{
						String[] list = new String[100];
						for ( int i = 0; i < 100; i++ )
						{
							list[i] = Integer.toString( i );
						}
						
						/* playlist */
						Log.i( "list", "Before save playList" );
						savePlayList( "playList.txt", list );
						Log.i( "list", "After save playList and Before load PlayList" );
						
						list = loadPlayList( "playList.txt" );
						Log.i( "list", "After Load playList" );
						
						for ( int i = 0; i < list.length; i++ )
							Log.i( "list", list[i] );
						
						sendCurrentPlayListToDE2( list );
						
						/* rating */
						for ( int i = 0; i < 100; i++ )
						{
							list[i] = Integer.toString( 99-i );
						}
						
						Log.i( "list", "Before save saveRating" );
						saveRating( list );
						Log.i( "list", "After save saveRating and Before load saveRating" );
						
						list = loadRating( );
						Log.i( "list", "After Load saveRating" );
						
						for ( int i = 0; i < list.length; i++ )
							Log.i( "list", list[i] );
						
						Debug = false;
					}
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	
	
	
	
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.advanced_main);

		app = (MyApplication)AdvancedMainActivity.this.getApplication();
		
		songVolume = 0;
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectDiskReads().detectDiskWrites().detectNetwork()
		.penaltyLog().build());
		
		tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 200);
		
		getIndex();
		
		Intent intent = getIntent();
		rawPlaylist = intent.getStringArrayExtra("rawPlaylist");
		songVolume = intent.getIntExtra("volume", 4);
		initializeList(rawPlaylist);
		overridePendingTransition(R.anim.slide_to_left, R.anim.slide_to_left);
	}
	
	public void initializeList(String[] playlist)
	{
		Log.i("AdvancedMain", "SimpleMain - initializing Song List");
		mainPlaylist = new ArrayList<String[]>();
		
		for (int i = 0; i + 5 <= playlist.length; i += 5) 
		{
			String[] newSong = new String[5];
			newSong[0] = playlist[i];
			newSong[1] = playlist[i + 1];
			newSong[2] = playlist[i + 2];
			newSong[3] = playlist[i + 3];
			newSong[4]	= playlist[i + 4];
			mainPlaylist.add(newSong);
		}
		
		Log.i("AdvancedMain", "SimpleMain - Finished initializing Song List");
	}
	
	public void playPauseSong(View view) {
		app.new SocketSend().execute("P");
	}

	public void stopSong(View view) {
		app.new SocketSend().execute("S");
	}

	public void nextSong(View view) {
		app.new SocketSend().execute("N");
	}

	public void prevSong(View view) {
		app.new SocketSend().execute("L");
	}

	public void upVolume(View view) {
		app.new SocketSend().execute("U");
	}

	public void downVolume(View view) {
		app.new SocketSend().execute("D");
	}
	
	public void getIndex() {
		app.new SocketSend().execute("I");
	}
	
	public void onClickPlayOrderMode( View view )
	{
		boolean isShuffle = ((ToggleButton) view).isChecked();
		
		if ( isShuffle )
			app.new SocketSend().execute("H");
		else
			app.new SocketSend().execute("h");
	}
	
	public void onClickPlayRepeatMode( View view )
	{
		boolean isRepeatOneSong = ((ToggleButton) view).isChecked();
		
		if ( isRepeatOneSong )
			app.new SocketSend().execute("R");
		else
			app.new SocketSend().execute("r");
	}
	
	/* Sends a playList to DE2
	 * returns 0 if successful, otherwise -1
	 * Pre: playList != null
	 */
	public int sendCurrentPlayListToDE2( String[] playList )
	{
		MyApplication app = (MyApplication) getApplication();
		InputStream in = null;
		try 
		{
			in = app.sock.getInputStream();
		} 
		catch (IOException e1) 
		{
			Log.i( "Exception", "app.sock.getInputStream() failed in sendCurrentPlayListToDE2" );
			return -1;
		}
		
		int listLength = playList.length;
		Log.i( "list", "Start sending playList" );
		//new SocketSend().execute( Integer.toString( ( Integer.toString( listLength ) ).length() ) );
		//Log.i( "list", "Sending: " +  Integer.toString( ( Integer.toString( listLength ) ).length() ) );
		app.new SocketSend().execute( Integer.toString( listLength ) );
		Log.i( "list", "Sending: " +  Integer.toString( listLength ) );
		
		for ( int i = 0; i < listLength; i++ )
		{
			if ( i != 0 && i % 30 == 0 )	// for HandShake
			{
				try 
				{
					byte buf[] = new byte[ONE_BYTE];
										
					app.new SocketSend().execute( "H" );
					Log.i( "list", "Sending H" );
					
					
					/* Android loopback mode purpose */
					/*
					String msg = new String();
					while ( msg.compareTo( "H" ) != 0 )
					{
						if ( in.available() > 0 )
						{
							in.read( buf );
							msg = new String(buf, 0, ONE_BYTE, "US-ASCII");
						}
					}
					*/
					
					/* Real Purpose */
					while ( in.available() == 0 );
					Log.i( "list", "Got message from DE2" );
					
					in.read( buf );
					String msg = new String(buf, 0, ONE_BYTE, "US-ASCII");
					
					Log.i( "list", "Message got is: " + msg );
					
					if ( msg.compareTo( "H" ) != 0 )
					{
						Log.i( "list", "Invalid message came from DE2 in sendCurrentPlayListToDE2" );
						return -1;
					}
					Log.i( "list", "valid message came from DE2 in sendCurrentPlayListToDE2" );
				} 
				catch (IOException e) 
				{
					Log.i( "Exception", "IOException failed in sendCurrentPlayListToDE2" );
					return -1;
				}		
			}
				
			//new SocketSend().execute( Integer.toString( playList[i].length() ) );
			//Log.i( "list", "Sending: " +  Integer.toString( playList[i].length() ) );
			app.new SocketSend().execute( playList[i] );
			Log.i( "list", "Sending: " +  playList[i] );
		}
		Log.i( "list", "Done sending playList" );
		return 0;
	}
	
	/* Wrapper function for storing rating list */
	public void saveRating( String[] rating )
	{
		saveList( "Rating.txt", rating );
	}
	
	/* Wrapper function for loading rating list */
	public String[] loadRating()
	{
		return loadList( "Rating.txt" );
	}
	
	/* Wrapper function for saving play list */
	public void savePlayList( String name, String[] str )
	{
		saveList( name, str );
	}
	
	/* Wrapper function for loading play list */
	public String[] loadPlayList( String name )
	{
		return loadList( name );
	}
	
	/* Stores a list of string into internal storage as a file
	 * @name is the name of the file
	 * @str is the list of strings to store
	 * Pre: Str != null
	 */
	public void saveList( String name, String[] str )
	{
		FileOutputStream fos = null;
		
		try 
		{
			fos = openFileOutput( name, Context.MODE_PRIVATE );
			
			for ( int i = 0; i < str.length; i++ )
			{
				fos.write( str[i].getBytes() );
				fos.write( ".".getBytes() );
			}
		} 
		catch (FileNotFoundException e) 
		{
			Log.i( "Exception", "File: " + name + " is not found." );
		} 
		catch (IOException e) 
		{
			Log.i( "Exception", "str.getBytes() threw an IOException for file: " + name + "." );
		}
		finally
		{
			try 
			{
				fos.close();
			} 
			catch (IOException e) 
			{
				Log.i( "Exception", "Failed close file: " + name + "." );
			}
		}
	}
	
	/* Loads a list of string from the internal storage
	 * @name is the name of the file
	 */
	public String[] loadList( String name )
	{
		FileInputStream fis = null;
		try 
		{
			fis = openFileInput( name );
			
			byte[] buf = new byte[MAX_BYTES];
			String temp = new String();
			int i;
			
			int bytesRead;
			while ( (bytesRead = fis.read( buf )) != -1 )
			{
				Log.i( "playList", "bytesRead is: " + bytesRead );
				temp = temp.concat( new String( buf, 0, bytesRead, "US-ASCII" ));			
			}
			Log.i( "playList", "bytesRead is: " + bytesRead );
			
			String[] str = temp.split("\\.");
			
			for ( i = 0; i < str.length; i++)
				Log.i( "ss", "str[" + i + " ]: " + str[i] );
			
			return str;
		} 
		catch (FileNotFoundException e) 
		{
			Log.i( "Exception", "File: " + name + " is not found." );
		} 
		catch (IOException e) 
		{
			Log.i( "Exception", "fis.read() threw an IOException for file: " + name + "." );
		}
		finally
		{
			try 
			{
				fis.close();
			} 
			catch (IOException e) 
			{
				Log.i( "Exception", "Failed close file: " + name + "." );
			}
		}
		
		return null;
	}
	
	/* Debugging purpose */
	public void debugHandShakedLongMessage(View view) {
		app.new SocketSend().execute("M");
	}
	
	public void setupTime( int songLength )
	{
		TextView MaxTimeMin = (TextView) findViewById( R.id.textView5 );
		TextView MaxTimeSec = (TextView) findViewById( R.id.textView7 );
		
		int maxMin = songLength / 60;
		int maxSec = songLength % 60;
		
		String maxSecStr = Integer.toString( maxSec );
		if ( maxSec < 10 )
			maxSecStr = "0" + maxSecStr;
		
		MaxTimeMin.setText( Integer.toString( maxMin ) );
		MaxTimeSec.setText( maxSecStr );
	}
	
	public void updateTime( int currentSongPositionInTime, int songLength )
	{
		TextView currTimeMin = (TextView) findViewById( R.id.textView1 );
		TextView currTimeSec = (TextView) findViewById( R.id.textView3 );
		
		//int currTime = (int) (currentSongPositionInTime * songLength / 100.0 );
		
		if ( currentSongPositionInTime > songLength )
			return;
		
		int currMin = currentSongPositionInTime / 60;
		int currSec = currentSongPositionInTime % 60;
		
		String currSecStr = Integer.toString( currSec );
		if ( currSec < 10 )
			currSecStr = "0" + currSecStr;
		
		currTimeMin.setText( Integer.toString( currMin ) );
		currTimeSec.setText( currSecStr );
	}
}
