package com.example.ece381;

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



public class Activity1 extends Activity {
	
	
	/* Constants */
	final static int ONE_BYTE = 1;
	
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
	public void onCreate(Bundle savedInstanceState) {
		// This call will result in better error messages if you
		// try to do things in the wrong thread.
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
		
		// Set up a timer task. We will use the timer to check the
		// input queue every 500 ms
		// initializeList(count);
		// rowView = listView.getChildAt(0);
		// rowView.setSelected(true);
		// while(initialized == false);
		// initializeList(count);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	// Route called when the user presses "connect"
	// public void openSocket(View view) {
	// new SocketSend().execute((Void) null);
	// }
	// Called when the user wants to send a message
	/*
	 * public void sendMessage(View view) { MyApplication app = (MyApplication)
	 * getApplication(); // Get the message from the box //EditText et =
	 * (EditText) findViewById(R.id.MessageText); String msg = "gg"; // Create
	 * an array of bytes. First byte will be the // message length, and the next
	 * ones will be the message byte buf[] = new byte[msg.length() + 1]; buf[0]
	 * = (byte) msg.length(); System.arraycopy(msg.getBytes(), 0, buf, 1,
	 * msg.length()); // Now send through the output stream of the socket
	 * OutputStream out; try { out = app.sock.getOutputStream(); try {
	 * out.write(buf, 0, msg.length() + 1); } catch (IOException e) {
	 * e.printStackTrace(); } } catch (IOException e) { e.printStackTrace(); } }
	 */
	// Called when the user closes a socket
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
		if (volume < 4)
			volume++;
		new SocketSend().execute("U");
	}

	public void downVolume(View view) {
		if (volume > 0)
			volume--;
		new SocketSend().execute("D");
	}

	public void pauseSong(View view) {
		new SocketSend().execute("P");
	}
	
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
		/*
		 * for(int i=0; i<mSongs.length; i++) { HashMap<String,String> item =
		 * new HashMap<String,String>(); item.put( "Song", mSongs[i]); item.put(
		 * "Artist",mArtists[i] ); item.put("Rating"," Rating:"+
		 * mRatings[i]+" stars"); list.add( item ); } adapter = new
		 * SimpleAdapter( this, list, R.layout.mylistview1, new String[] {
		 * "Song","Artist","Rating" }, new int[] { R.id.textView1,
		 * R.id.textView2, R.id.textView3 }); listView.setAdapter(adapter);
		 * listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
		 * {
		 * 
		 * @Override public void onItemClick(AdapterView<?> parent, View view,
		 * int position, long id) { //String song_id =
		 * Integer.toString(position); Object obj =
		 * parent.getItemAtPosition(position); //SelectionRectangle block;
		 * Drawable block = getWallpaper(); listView.setSelector(block);
		 * //Drawable draw; //draw.
		 * 
		 * @SuppressWarnings("unchecked") HashMap<String,String> item =
		 * (HashMap<String, String>) obj; showPlaying.setText(item.get("Song"));
		 * showPlaying.show(); new SocketSend().execute(item.get("Song") ); } }
		 * );
		 */
	}

	// This is the Socket Connect asynchronous thread. Opening a socket
	// has to be done in an Asynchronous thread in Android. Be sure you
	// have done the Asynchronous Tread tutorial before trying to understand
	// this code.
	public class SocketConnect extends AsyncTask<Void, Void, Socket> {
		// The main parcel of work for this thread. Opens a socket
		// to connect to the specified IP.
		protected Socket doInBackground(Void... voids) {
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

		// After executing the doInBackground method, this is
		// automatically called, in the UI (main) thread to store
		// the socket in this app's persistent storage
		protected void onPostExecute(Socket s) {
			MyApplication myApp = (MyApplication) Activity1.this
					.getApplication();
			myApp.sock = s;
			// new
			// SocketSend().execute("02.01.Super MArio.kai.5.02.LOL.new artist.4.");

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

			new SocketSend().execute("playlist");
		}
	}

	public class SocketSend extends AsyncTask<String, String, Socket> {
		// The main parcel of work for this thread. Opens a socket
		// to connect to the specified IP.
		protected Socket doInBackground(String... strings) {
			Socket s = null;
			MyApplication app = (MyApplication) getApplication();
			// rowView.setSelected(true);
			String msg = strings[0].toString();
			// Toast.makeText(MainActivity.this, msg,
			// Toast.LENGTH_SHORT).show();
			// Create an array of bytes. First byte will be the
			// message length, and the next ones will be the message
			byte buf[] = new byte[msg.length() + 1];
			buf[0] = (byte) msg.length();
			System.arraycopy(msg.getBytes(), 0, buf, 1, msg.length());
			// Now send through the output stream of the socket
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

	// This is a timer Task. Be sure to work through the tutorials
	// on Timer Tasks before trying to understand this code.
	public class TCPReadTimerTask extends TimerTask 
	{
		public void run() 
		{
			//Log.i("Prog", "Read Timer Task started");

			MyApplication app = (MyApplication) getApplication();
			if (app.sock != null && app.sock.isConnected()
					&& !app.sock.isClosed()) 
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

						// As explained in the tutorials, the GUI can not be
						// updated in an asyncrhonous task. So, update the GUI
						// using the UI thread.
						runOnUiThread(new Runnable() 
						{
							public void run() 
							{
//							      Intent intent = getIntent();
//							      String ipStr_ = intent.getStringExtra("ipStr+");
								
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
									
									Log.i("Prog", "s I have in Else is: " + command);
									
									songIndex = Integer.parseInt( message );
									Log.i("indexNumber", Integer.toString(songIndex));
									text.setText("Playing: " + mSongs[songIndex]);
									
									setupTime( Integer.parseInt( mLengths[songIndex] ) );
									currentSongPositionInTime = 0;
								} 
								else // to be modified
								{
									/*
									Log.i("Prog", "s I have in Else is: " + command);
									
									songIndex = Integer.parseInt(s);
									Log.i("indexNumber", Integer.toString(songIndex));
									text.setText("Playing: " + mSongs[songIndex]);
									
									setupTime( Integer.parseInt( mLengths[songIndex] ) );
									currentSongPositionInTime = 0;
									*/
								}
								// if(rowView!=null)
								// rowView.setSelected(true);
								
//							      Intent intent = getIntent();
//							      String ipStr_ = intent.getStringExtra("ipStr+");
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

