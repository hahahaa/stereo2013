package com.example.stereoplayer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


import com.example.stereoplayer.MyApplication.SocketConnect;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
	
	private final static int ONE_BYTE = 1;
	private final static int MAX_BYTES = 255;
	private boolean initialized = false;
	private ArrayList<String[]> mainPlaylist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState){
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
		app.new SocketConnect().execute((Void) null);
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 200);
		
		while(initialized == false);
		Toast.makeText(this, "Initialized", Toast.LENGTH_SHORT).show();
		
		Intent resultIntent = new Intent();
		resultIntent.putExtra("FromLoading", mainPlaylist);
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
		
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
									
									String[] playlist = buffer;
									int count = Integer.parseInt(buffer[0]);
									initializeList(count, playlist);
									initialized = true;
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
