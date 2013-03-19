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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	ListView listView;

	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	private SimpleAdapter adapter;
	//String database;
	
	String[] mSongs = new String[] {
			"AAA", "BBB", "CCC", "DDD", "EEE",
			"aaa","abc","bbb","bcd","123",
			"AAA", "BBB", "CCC", "DDD", "EEE",
			"FFF", "GGG","HHH", "III", "JJJ",
			"FFF", "GGG","HHH", "III", "JJJ",
			"aaa","abc","bbb","bcd","123"
	};

	String[] mArtists = new String[] {
			"Dan", "Sae",  "Dan",  "Dan",  "Dan",
			"Dan", "Sae",  "Dan",  "Dan",  "Dan",
			"Dan", "Sae",  "Dan",  "Dan",  "Dan",
			"Dan", "Sae",  "Dan",  "Dan",  "Dan",
			"Dan", "Sae",  "Dan",  "Dan",  "Dan",
			"FFF", "GGG","HHH", "III", "JJJ",
	};



	String[] mRatings = new String[] {
			"1", "2", "3", "4", "5",
			"7", "6","5", "4", "3",
			"1", "2", "3", "4", "5",
			"7", "6","3", "4", "5",
			"1",  "4", "5", "4", "5",
			"7", "6","5", "4", "3",
	};
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// This call will result in better error messages if you
		// try to do things in the wrong thread.
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
				.detectDiskReads().detectDiskWrites().detectNetwork()
				.penaltyLog().build());

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		listView = (ListView) findViewById(R.id.listView);       

		for(int i=0; i<mSongs.length; i++)
		{
			HashMap<String,String> item = new HashMap<String,String>();
			item.put( "Song", mSongs[i]);
			item.put( "Artist",mArtists[i] );
			item.put("Rating","  Rating:"+ mRatings[i]+" stars");
			list.add( item );
		}

		adapter = new SimpleAdapter( this, list, R.layout.mylistview1, new String[] { "Song","Artist","Rating" },
				new int[] { R.id.textView1, R.id.textView2, R.id.textView3 });

		listView.setAdapter(adapter);
		
		

		
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() 
		{

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
			{				
				String song_id = Integer.toString(position); 
				//Toast.makeText(MainActivity.this, song_id, 

				Object obj = parent.getItemAtPosition(position);

				@SuppressWarnings("unchecked")
				HashMap<String,String> item = (HashMap<String, String>) obj;

				//Toast.makeText(MainActivity.this, item.get("Song") + " is playing!", Toast.LENGTH_SHORT).show();
				
				//Toast.makeText(MainActivity.this, database, Toast.LENGTH_SHORT).show();

				new SocketSend().execute(item.get("Song") );

			



			}
		}
		);

		// Set up a timer task.  We will use the timer to check the
		// input queue every 500 ms
		
		TCPReadTimerTask tcp_task = new TCPReadTimerTask();
		Timer tcp_timer = new Timer();
		tcp_timer.schedule(tcp_task, 3000, 500);
		
		new SocketConnect().execute((Void) null);
		
		//new SocketSend().execute("playlist");
		
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	// Route called when the user presses "connect"
	

	
//	public void openSocket(View view) {
//		new SocketSend().execute((Void) null);
//	}

	//  Called when the user wants to send a message
	
	/*public void sendMessage(View view) {
		MyApplication app = (MyApplication) getApplication();
		
		// Get the message from the box
		
		//EditText et = (EditText) findViewById(R.id.MessageText);
		String msg = "gg";

		// Create an array of bytes.  First byte will be the
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
	}
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

	// Construct an IP address from the four boxes
	
	public String getConnectToIP() {
//		String addr = "192.168.1.67";
		String addr = "206.87.112.215";
		//EditText text_ip= (EditText) findViewById(R.id.ip1);
//		text_ip.setText(addr);
		
//		EditText text_ip;
//		text_ip = (EditText) findViewById(R.id.ip1);
//		addr += text_ip.getText().toString();
//		text_ip = (EditText) findViewById(R.id.ip2);
//		addr += "." + text_ip.getText().toString();
//		text_ip = (EditText) findViewById(R.id.ip3);
//		addr += "." + text_ip.getText().toString();
//		text_ip = (EditText) findViewById(R.id.ip4);
//		addr += "." + text_ip.getText().toString();
		return addr;
	}

	// Gets the Port from the appropriate field.
	
	public Integer getConnectToPort() {
		Integer port = (Integer) 50002;

		return port;
	}
	
	public void playSong(View view){
		
		new SocketSend().execute("P");
		
	}
	
	public void pauseSong(View view)
	{
		new SocketSend().execute("P");
	}


    // This is the Socket Connect asynchronous thread.  Opening a socket
	// has to be done in an Asynchronous thread in Android.  Be sure you
	// have done the Asynchronous Tread tutorial before trying to understand
	// this code.

	
	
	
	
	public class SocketConnect extends AsyncTask<Void, Void, Socket> {

		// The main parcel of work for this thread.  Opens a socket
		// to connect to the specified IP.
		
		protected Socket doInBackground(Void... voids) {
			Socket s = null;
			String ip = getConnectToIP();
			Integer port = getConnectToPort();

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
			MyApplication myApp = (MyApplication) MainActivity.this
					.getApplication();
			myApp.sock = s;
			new SocketSend().execute("playlist");
		}
	}

	
	
	
	public class SocketSend extends AsyncTask< String, String, Socket> {

		// The main parcel of work for this thread.  Opens a socket
		// to connect to the specified IP.
	
		
		protected Socket doInBackground(String...strings) {
			Socket s = null;
			MyApplication app = (MyApplication) getApplication();
			
			
			
			String msg = strings[0].toString();
			//Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();

			// Create an array of bytes.  First byte will be the
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
	
	
	
	
	
	
	
	
	// This is a timer Task.  Be sure to work through the tutorials
	// on Timer Tasks before trying to understand this code.
	
	public class TCPReadTimerTask extends TimerTask {
		public void run() {
			MyApplication app = (MyApplication) getApplication();
			if (app.sock != null && app.sock.isConnected()
					&& !app.sock.isClosed()) {
				
				try {
					InputStream in = app.sock.getInputStream();

					// See if any bytes are available from the Middleman
					
					int bytes_avail = in.available();
					if (bytes_avail > 0) {
						
						// If so, read them in and create a string
						
						byte buf[] = new byte[bytes_avail];
						in.read(buf);

						final String s = new String(buf, 0, bytes_avail, "US-ASCII");
						
		
						// As explained in the tutorials, the GUI can not be
						// updated in an asyncrhonous task.  So, update the GUI
						// using the UI thread.
						
						runOnUiThread(new Runnable() {
							public void run() {										
								
								TextView text = (TextView) findViewById(R.id.viewText2);
								//String msg = "gg";
								//Log.i("HHHHHHH", s);
								
								String[] ss = s.split(".");
							
								text.setText(ss[1]);
							
								//database = s;
						
								
				
								
								
							}
						});
						
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
