package com.example.stereoplayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import com.example.stereoplayer.MyApplication.SocketSend;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LoadingScreenActivity  extends Activity {
	
	private String ipStr = "206.87.117.1";
	private int portNumber = 50002;
	
	// This is the Socket Connect asynchronous thread. Opening a socket
	// has to be done in an Asynchronous thread in Android. Be sure you
	// have done the Asynchronous Tread tutorial before trying to understand
	// this code.
	public class SocketConnect extends AsyncTask<Void, Void, Socket> {

		// The main parcel of work for this thread. Opens a socket
		// to connect to the specified IP.
		@Override
		protected Socket doInBackground(Void... voids) {
			Log.i("flow", "SocketConnect: doInBackground" );
			Socket s = null;
			String ip = ipStr;
			Integer port = portNumber;
			try {
				s = new Socket(ip, port);
			} catch (UnknownHostException e) {
				Log.i("flow", "SocketConnect: UnknownHostException" );
			} catch (IOException e) {
				Log.i("flow", "SocketConnect: IOException" );
			}
			Log.i("flow", "SocketConnect: returning from doInBackground" );
			//cc.notifyAll();
			return s;
		}

		// After executing the doInBackground method, this is
		// automatically called, in the UI (main) thread to store
		// the socket in this app's persistent storage
		@Override
		protected void onPostExecute(Socket s) {
			Log.i("flow", "SocketConnect: onPostExecute" );
			MyApplication myApp = (MyApplication) LoadingScreenActivity.this.getApplication();
			myApp.sock = s;
			//sock = s;

			try {
				Log.i( "Prog", "Clearing UART" );

				InputStream in = s.getInputStream();
				int bytes_avail = in.available();
				if (bytes_avail > 0) {
					// If so, read them in and create a string
					byte buf[] = new byte[bytes_avail];
					in.read(buf);

				}
			} catch (IOException e) {
				Log.i( "Prog", "Clearing UART Failed" );
				e.printStackTrace();
			}
			
			Log.i("flow", "SocketConnect: Sending command playlist" );
			myApp.new SocketSend().execute("playlist");

			//if(mode == DE2)
			//	new SocketSend().execute("playlist");
			//else
			//	new SocketSend().execute("02.01.Super MArio.kai.5.64.02.LOL.new artist.4.22.");
		}
	}
	
	private final static int ONE_BYTE = 1;
	private final static int MAX_BYTES = 255;
	private boolean initialized = false;
	private ArrayList<String[]> mainPlaylist;
	SocketConnect cc;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		Log.i("flow", "LoadingScreenActivity: onCreate" );
		
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.loading_screen);
		View rootView = getWindow().getDecorView();
		rootView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
		.detectDiskReads().detectDiskWrites().detectNetwork()
		.penaltyLog().build());
		
		MyApplication app = (MyApplication)LoadingScreenActivity.this.getApplication();
		mainPlaylist = new ArrayList<String[]>();
		
		Log.i("flow", "LoadingScreenActivity: Calling SocketConnect().execute()" );
		cc = (SocketConnect) new SocketConnect().execute((Void) null);
		Log.i("flow", "LoadingScreenActivity: SocketConnect().execute is done" );
		
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 200);
		Log.i("flow", "LoadingScreenActivity: Scheduled ReadTimerTask" );	
	}
	
	/*
	@Override
	public void onResume()
	{
		try {
			while ( !initialized )
				cc.wait();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Log.i("flow", "LoadingScreenActivity: onResume" );
		//while(initialized == false);
		Log.i("flow", "LoadingScreenActivity: onResume after while loop" );	
		Toast.makeText(this, "Initialized", Toast.LENGTH_SHORT).show();
	}
	*/
	
	
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

							app.new SocketSend().execute("A");

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
										app.new SocketSend().execute("A");
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

								if (initialized == false) 
								{
									Log.i("Prog", "Initializing Song List");

									String[] buffer = command.split("\\.");
									
									for (int k = 0; k < buffer.length; k++)
										Log.i("buffer", buffer[k]);
									
									String[] playlist = Arrays.copyOfRange(buffer, 1, buffer.length);
									//int count = Integer.parseInt(buffer[0]);
									//initializeList(count, playlist);
									initialized = true;
									
									
									Intent resultIntent = new Intent();
									
									resultIntent.putExtra("FromLoading", playlist);
									setResult(Activity.RESULT_OK, resultIntent);
									finish();
									
									
									//text.setText("Press play");
								} /*
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
								
								else if (command.compareTo("N") == 0) 
								{
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									pb.setProgress( 0 );
								}
								
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
									
								}*/
							}
						});
					}
					
			
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public void initializeList(int count, String[] playlist ) {
		Log.i("indexNumber", "playlist length is: " + Integer.toString(playlist.length));
		
		for (int iterator = 1; iterator + 5 <= playlist.length; iterator += 5) 
		{
			String[] newSong = new String[5];
			newSong[0] = playlist[iterator];
			newSong[1] = playlist[iterator + 1];
			newSong[2] = playlist[iterator + 2];
			newSong[3] = playlist[iterator + 3];
			newSong[4]	= playlist[iterator + 4];
			mainPlaylist.add(newSong);
		}
		
		initialized = true;
	}

}
