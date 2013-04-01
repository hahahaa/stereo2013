package com.example.ece381;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;



public class Activity1 extends Activity 
{
	/* Set this to true if debugging saving and loading play list and/or rating list*/
	boolean Debug = true;
	
	/* Constants */
	final static int ONE_BYTE = 1;
	final static int MAX_BYTES = 255;
	
	/* Variables */
	int songIndex;
	double currentSongPositionInTime;
	int count;
	int volume;
	boolean initialized;
	ListView listView;
	View rowView;
	Toast showPlaying;
	String[] playlist;
	ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
	private SimpleAdapter adapter;
	
	/* Song Detail Arrays */
	String[] mSongs;
	String[] mArtists;
	String[] mRatings;
	String[] mId;
	String[] mLengths;

	// for connection
	String ipStr;
	int portStr;
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		volume = 4;
		songIndex = 0;
		currentSongPositionInTime = 0;

		Intent intent = getIntent();
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 200);
		
	    String ipStr_ = intent.getStringExtra("ipStr+");
	    ipStr=ipStr_;
	    String portStr_ = intent.getStringExtra("portStr+");
	    portStr=Integer.parseInt(portStr_);
	    String line= ipStr +"\n     "+portStr;
		Toast.makeText(Activity1.this, line,Toast.LENGTH_SHORT).show();
		new SocketConnect().execute((Void) null);
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork()
				.penaltyLog().build());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		showPlaying = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		listView = (ListView) findViewById(R.id.listView);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void closeSocket(View view) {
		MyApplication app = (MyApplication) getApplication();
		Socket s = app.sock;
		try {
			s.getOutputStream().close();
			s.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void playSong(View view) {
		new SocketSend().execute("P");
	}

	public void stopSong(View view) {
		new SocketSend().execute("S");
	}

	public void nextSong(View view) {
		new SocketSend().execute("N");
	}

	public void prevSong(View view) {
		new SocketSend().execute("L");
	}

	public void upVolume(View view) {
		volume++;
		new SocketSend().execute("U");
	}

	public void downVolume(View view) {
		volume--;
		new SocketSend().execute("D");
	}

	public void pauseSong(View view) {
		new SocketSend().execute("P");
	}
	
	public void onClickPlayOrderMode( View view )
	{
		boolean isShuffle = ((ToggleButton) view).isChecked();
		
		if ( isShuffle )
			new SocketSend().execute("H");
		else
			new SocketSend().execute("h");
	}
	
	public void onClickPlayRepeatMode( View view )
	{
		boolean isRepeatOneSong = ((ToggleButton) view).isChecked();
		
		if ( isRepeatOneSong )
			new SocketSend().execute("R");
		else
			new SocketSend().execute("r");
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
		new SocketSend().execute( Integer.toString( listLength ) );
		Log.i( "list", "Sending: " +  Integer.toString( listLength ) );
		
		for ( int i = 0; i < listLength; i++ )
		{
			if ( i != 0 && i % 30 == 0 )	// for HandShake
			{
				try 
				{
					byte buf[] = new byte[ONE_BYTE];
										
					new SocketSend().execute( "H" );
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
			new SocketSend().execute( playList[i] );
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
		new SocketSend().execute("M");
	}

	public void initializeList(int count) {
		mSongs = new String[count];
		mArtists = new String[count];
		mRatings = new String[count];
		mId = new String[count];
		mLengths = new String[count];
		
		int songCount = 0;
		Log.i("indexNumber", "playlist length is: " + Integer.toString(playlist.length));
		
		for (int iterator = 1; iterator + 5 <= playlist.length; iterator += 5) 
		{
			mId[songCount] = playlist[iterator];
			mSongs[songCount] = playlist[iterator + 1];
			mArtists[songCount] = playlist[iterator + 2];
			mRatings[songCount] = playlist[iterator + 3];
			mLengths[songCount]	= playlist[iterator + 4];
			songCount++;
		}
	}

	public class SocketConnect extends AsyncTask<Void, Void, Socket> 
	{
		protected Socket doInBackground(Void... voids) 
		{
			Socket s = null;
			String ip = ipStr;
			Integer port = portStr;
			try {
				s = new Socket(ip, port);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return s;
		}

		protected void onPostExecute(Socket s)
		{
			MyApplication myApp = (MyApplication) Activity1.this
					.getApplication();
			myApp.sock = s;

			try {
				Log.i( "Prog", "Clearing UART" );
				
				InputStream in = s.getInputStream();
				int bytes_avail = in.available();
				if (bytes_avail > 0) {
					byte buf[] = new byte[bytes_avail];
					in.read(buf);
				}
			} catch (IOException e) {
				Log.i( "Prog", "Clearing UART Failed" );
				e.printStackTrace();
			}

			if ( !Debug )
				new SocketSend().execute("playlist");
		}
	}

	public class SocketSend extends AsyncTask<String, String, Socket> {
		protected Socket doInBackground(String... strings) {
			Socket s = null;
			MyApplication app = (MyApplication) getApplication();
			String msg = strings[0].toString();
			byte buf[] = new byte[msg.length() + 1];
			buf[0] = (byte) msg.length();
			System.arraycopy(msg.getBytes(), 0, buf, 1, msg.length());

			OutputStream out;
			try {
				out = app.sock.getOutputStream();
				try {
					out.write(buf, 0, msg.length() + 1);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return s;
		}
	}

	public class TCPReadTimerTask extends TimerTask 
	{		
		public void run() 
		{
			Log.i("Prog", "Read Timer Task started");

			MyApplication app = (MyApplication) getApplication();
			if (app.sock != null && app.sock.isConnected() && !app.sock.isClosed()) 
			{
				try 
				{
					InputStream in = app.sock.getInputStream();
					int bytes_avail = in.available();
					
					if (bytes_avail > 0) 
					{
						Log.i( "Prog", bytes_avail + "bytes are available to be read" );
						
						byte buf[] = new byte[ONE_BYTE];
						in.read(buf);
						String msg = new String(buf, 0, ONE_BYTE, "US-ASCII");
						String data = new String();

						if (!initialized) 
						{
							Log.i("Prog", "Receiving Song List");

							new SocketSend().execute("A");

							while (true) 
							{
								bytes_avail = in.available();
								if (bytes_avail > 0) 
								{
									buf = new byte[ONE_BYTE];
									in.read(buf);
									String tempStr = (new String(buf, 0, ONE_BYTE,	"US-ASCII"));

									if (tempStr.compareTo("+") == 0) 
									{
										new SocketSend().execute("A");
										continue;
									}

									if (tempStr.compareTo(",") == 0) 
									{
										break;
									}

									msg = msg.concat(tempStr);
								}
							}
							Log.i("Prog", "Done Receiving Song List");
						}
						else
						{
							if (msg.compareTo("M") == 0)
							{
								while ( in.available() == 0 );
								bytes_avail = in.available();
								
								byte buffer[] = new byte[ONE_BYTE];
								in.read(buffer);
								Log.i("HandShake", "buffer is: " + buffer[0]);
								int numBytesOfNumber = buffer[0];
								
								buffer = new byte[numBytesOfNumber];
								
								while( in.available() < numBytesOfNumber );
								in.read(buffer);
								Log.i("HandShake", "buffer[0] is: " + buffer[0]);
								String temp = new String( buffer, 0, numBytesOfNumber, "US-ASCII" );
								Log.i("HandShake", "temp is: " + temp );
								
								int numBytesOfData = Integer.parseInt( temp );
								Log.i("HandShake", "numBytesOfData is: " + numBytesOfData );
								
								buffer = new byte[ONE_BYTE];
								for ( int i = 0; i < numBytesOfData; )
								{
									if ( in.available() > 0 )
									{
										in.read(buffer);
										Log.i("HandShake", "buffer[0] is: " + buffer[0]);
										data = data.concat( new String(buffer, 0, ONE_BYTE, "US-ASCII") );
										i++;
									}
								}
								
								Log.i("HandShake", "Data is: " + data);
							}
						}
						
						final String command = new String(msg);
						final String message = new String( data );

						Log.i("HandShake", "String s is: " + command);
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

								if (initialized == false) 
								{
									Log.i("Prog", "Initializing Song List");

									String[] ss = command.split("\\.");
									
									for (int k = 0; k < ss.length; k++)
										Log.i("ss", ss[k]);
									
									playlist = ss;
									count = Integer.parseInt(ss[0]);
									initializeList(count);
									initialized = true;
									text.setText("Press play");
								} 
								else if ( command.compareTo( "P" ) == 0 )
								{
									text.setText("Playing: " + mSongs[songIndex]);
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
									volumeT.setText("Volume = " + Integer.toString(volume));
								} 
								else if (command.compareTo("D") == 0) 
								{
									volumeT.setText("Volume = " + Integer.toString(volume));
								} 
								/* Deprecated */
								else if (command.compareTo("N") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									pb.setProgress( 0 );
								}
								/* Deprecated */
								else if (command.compareTo("L") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									pb.setProgress( 0 );
								} 
								else if (command.compareTo("O") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									int songLength = Integer.parseInt( mLengths[songIndex] );
									double progressInterval = 100.0 / songLength;
									
									currentSongPositionInTime += progressInterval;
									pb.setProgress( (int) currentSongPositionInTime );
									
									updateTime( currentSongPositionInTime, songLength );
									
									Log.i("Prog", "Progress increased by " + progressInterval );
									Log.i("Prog", "currentSongPosition is: " + currentSongPositionInTime );
								}
								else if (command.compareTo("M") == 0) 
								{
									Log.i( "HandShake", "Successfully reached here" );
									
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									pb.setProgress( 0 );
									
									songIndex = Integer.parseInt( message );
									Log.i("indexNumber", Integer.toString(songIndex));
									text.setText("Playing: " + mSongs[songIndex]);
									
									setupTime( Integer.parseInt( mLengths[songIndex] ) );
									currentSongPositionInTime = 0;
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
	
	public void updateTime( double currentSongPositionInTime, int songLength )
	{
		TextView currTimeMin = (TextView) findViewById( R.id.textView1 );
		TextView currTimeSec = (TextView) findViewById( R.id.textView3 );
		
		int currTime = (int) (currentSongPositionInTime * songLength / 100.0 );
		
		if ( currTime > songLength )
			return;
		
		int currMin = currTime / 60;
		int currSec = currTime % 60;
		
		String currSecStr = Integer.toString( currSec );
		if ( currSec < 10 )
			currSecStr = "0" + currSecStr;
		
		currTimeMin.setText( Integer.toString( currMin ) );
		currTimeSec.setText( currSecStr );
	}

	public class ChangeUI extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... arg0) {
			return null;
			// TODO Auto-generated method stub
		}
	}

	public void test() {
		runOnUiThread(new Runnable() {
			public void run() {
				TextView text = (TextView) findViewById(R.id.viewText2);
				text.setText("test");
			}
		});
	}
}

