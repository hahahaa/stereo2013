package com.example.stereoplayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ToggleButton;

public class AdvancedMainActivity extends Activity
{
	final static int ONE_BYTE = 1;
	final static int MAX_BYTES = 255;
	
	private MyApplication app;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.advanced_main);

		app = (MyApplication)AdvancedMainActivity.this.getApplication();
	}
	
	
	
	
	public void playPauseSong(View view) {
		app.new SocketSend().execute("P");
	}

	public void stopSong(View view) {
		app.new SocketSend().execute("S");
	}

	public void nextSong() {
		app.new SocketSend().execute("N");
	}

	public void prevSong() {
		app.new SocketSend().execute("L");
	}

	public void upVolume() {
		app.new SocketSend().execute("U");
	}

	public void downVolume() {
		app.new SocketSend().execute("D");
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
}
