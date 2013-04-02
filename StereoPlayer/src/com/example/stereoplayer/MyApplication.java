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
	
	public class SocketSend extends AsyncTask<String, String, Socket> {
		// The main parcel of work for this thread. Opens a socket
		// to connect to the specified IP.
		protected Socket doInBackground(String... strings) {
			Log.i("flow", "SocketSend: doInBackground" );
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
