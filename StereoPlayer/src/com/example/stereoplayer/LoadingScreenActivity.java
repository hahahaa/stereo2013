package com.example.stereoplayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

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

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class LoadingScreenActivity  extends Activity {
	
	private String ipStr = "206.87.117.1";
	private int portNumber = 50002;
	
	public class SocketConnect extends AsyncTask<Void, Void, Socket> {

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
			return s;
		}

		@Override
		protected void onPostExecute(Socket s) {
			Log.i("flow", "SocketConnect: onPostExecute" );
			MyApplication myApp = (MyApplication) LoadingScreenActivity.this.getApplication();
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
			
			Log.i("flow", "SocketConnect: Sending command playlist" );
			myApp.new SocketSend().execute("playlist");
		}
	}
	
	private final static int ONE_BYTE = 1;
	private boolean initialized = false;
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
		
		Log.i("flow", "LoadingScreenActivity: Calling SocketConnect().execute()" );
		cc = (SocketConnect) new SocketConnect().execute((Void) null);
		Log.i("flow", "LoadingScreenActivity: SocketConnect().execute is done" );
		
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 200);
		Log.i("flow", "LoadingScreenActivity: Scheduled ReadTimerTask" );	
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

								if ( !initialized ) 
								{
									Log.i("Prog", "Initializing Song List");

									String[] buffer = command.split("\\.");
									
									for (int k = 0; k < buffer.length; k++)
										Log.i("buffer", buffer[k]);
									
									String[] playlist = Arrays.copyOfRange(buffer, 1, buffer.length);
									
									Intent resultIntent = new Intent();
									resultIntent.putExtra("FromLoading", playlist);
									setResult(Activity.RESULT_OK, resultIntent);
									finish();								
								}
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
}
