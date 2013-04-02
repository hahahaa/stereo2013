package com.example.stereoplayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.TimerTask;


import android.app.Application;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MyApplication extends Application {
	
	final static int ONE_BYTE = 1;
	final static int MAX_BYTES = 255;
	
	Socket sock = null;
	String ipStr = null;
	int portNumber;

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
			Integer port = portNumber;
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
			//MyApplication myApp = (MyApplication) SimpleMainActivity.this.getApplication();
			//myApp.sock = s;
			sock = s;

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
			
			new SocketSend().execute("playlist");

			//if(mode == DE2)
			//	new SocketSend().execute("playlist");
			//else
			//	new SocketSend().execute("02.01.Super MArio.kai.5.64.02.LOL.new artist.4.22.");
		}
	}
	
	public class SocketSend extends AsyncTask<String, String, Socket> {
		// The main parcel of work for this thread. Opens a socket
		// to connect to the specified IP.
		protected Socket doInBackground(String... strings) {
			Socket s = null;
			String msg = strings[0].toString();
			// Create an array of bytes. First byte will be the
			// message length, and the next ones will be the message
			byte buf[] = new byte[msg.length() + 1];
			buf[0] = (byte) msg.length();
			System.arraycopy(msg.getBytes(), 0, buf, 1, msg.length());
			// Now send through the output stream of the socket
			OutputStream out;
			try {
				out = sock.getOutputStream();
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
}
