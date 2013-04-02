package com.example.stereoplayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.example.stereoplayer.MyApplication.SocketSend;




import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SimpleMainActivity extends Activity implements OnGestureListener {	
	
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
						Log.i( "HandShake", bytes_avail + " bytes are available to be read" );
						
						byte buf[] = new byte[ONE_BYTE];
						in.read(buf);
						Log.i( "HandShake", "buf we got for command is: " + buf[0] );
						String msg = new String(buf, 0, ONE_BYTE, "US-ASCII");
						String data = new String();
						Log.i( "HandShake", "Command got is: " + msg );
						
						if (msg.compareTo("M") == 0)
						{
							while ( in.available() == 0 );
							bytes_avail = in.available();
							
							byte buffer[] = new byte[ONE_BYTE];
							in.read(buffer);
							int numBytesOfNumber = buffer[0];
							Log.i("HandShake", "numBytesOfNumber is: " + numBytesOfNumber);
							
							buffer = new byte[numBytesOfNumber];
							
							while( in.available() < numBytesOfNumber );
							in.read(buffer);
							String temp = new String( buffer, 0, numBytesOfNumber, "US-ASCII" );
							
							int numBytesOfData = Integer.parseInt( temp );
							Log.i("HandShake", "numBytesOfData is: " + numBytesOfData );
							
							buffer = new byte[ONE_BYTE];
							for ( int i = 0; i < numBytesOfData; )
							{
								if ( in.available() > 0 )
								{
									in.read(buffer);
									Log.i("HandShake", "data to concat is: " + buffer[0]);
									data = data.concat( new String(buffer, 0, ONE_BYTE, "US-ASCII") );
									i++;
								}
							}
							Log.i("HandShake", "Data is: " + data);
						}
						
						final String command = new String(msg);
						final String message = new String( data );

						Log.i("HandShake", "Command is: " + command);
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

								//TextView volumeT = (TextView) findViewById(R.id.viewText1);
								//TextView text = (TextView) findViewById(R.id.viewText2);

								if ( command.compareTo( "P" ) == 0 )
								{
									Log.i( "Modee", "Got P: " );
									
									ImageView image = (ImageView) findViewById(R.id.mainButton);


									image.setImageResource(R.drawable.pause_button);
									showStatus.setText("Playing");
									showStatus.show();
									playing = true;
								}
								else if (command.compareTo("p") == 0) 
								{
									Log.i( "Modee", "Got p: " );
									
									ImageView image = (ImageView) findViewById(R.id.mainButton);


									image.setImageResource(R.drawable.play_button);
									showStatus.setText("Paused");
									showStatus.show();
									playing = false;
									
								} 
								else if (command.compareTo("S") == 0) 
								{
									//text.setText("Stoppped");
								} 
								else if (command.compareTo("U") == 0) 
								{
									//volumeT.setText("Volume = " + Integer.toString(volume));
								} 
								else if (command.compareTo("D") == 0) 
								{
									//volumeT.setText("Volume = " + Integer.toString(volume));
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
									Log.i( "Modee", "Got O: " );
									
									ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar2);
									int songLength = Integer.parseInt(mainPlaylist.get(songIndex)[4]);
									double progressInterval = 100.0 / songLength;
									
									currentSongPositionInTime += progressInterval;
									bar.setProgress( (int) currentSongPositionInTime );
									
									//updateTime( currentSongPositionInTime, songLength );
									
									Log.i("Prog", "Progress increased by " + progressInterval );
									Log.i("Prog", "currentSongPosition is: " + currentSongPositionInTime );
									
								}
								else if (command.compareTo("M") == 0) 
								{
									Log.i( "Modee", "Got M: " );
									
									ImageView image = (ImageView) findViewById(R.id.mainButton);
									TextView songName = (TextView) findViewById(R.id.songName);
									TextView artistName = (TextView) findViewById(R.id.artistName);
									ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar2);
									bar.setProgress( 0 );
									songIndex = Integer.parseInt( message );
									Log.i( "Mode", "In M: songIndex is: " + songIndex );
									songName.setText(mainPlaylist.get(songIndex)[1]);
									artistName.setText(mainPlaylist.get(songIndex)[2]);

									image.setImageResource(R.drawable.pause_button);
									showStatus.setText("Playing");
									showStatus.show();
									playing = true;
									currentSongPositionInTime = 0;

									//}
									//else {
									//	image.setImageResource(R.drawable.play_button);
									//	showStatus.setText("Paused");
									//	showStatus.show();
									//	playing = false;
									//}
									/*
									Log.i( "HandShake", "Successfully reached here" );
									
									ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar1);
									pb.setProgress( 0 );
									
									songIndex = Integer.parseInt( message );
									Log.i("indexNumber", Integer.toString(songIndex));
									text.setText("Playing: " + mSongs[songIndex]);
									
									setupTime( Integer.parseInt( mLengths[songIndex] ) );
									currentSongPositionInTime = 0;
									*/
								} 
								else
								{
									
								}
							}
						});
					}
					
					/* Debugging purpose */
//					if ( Debug )
//					{
//						String[] list = new String[100];
//						for ( int i = 0; i < 100; i++ )
//						{
//							list[i] = Integer.toString( i );
//						}
//						
//						/* playlist */
//						Log.i( "list", "Before save playList" );
//						savePlayList( "playList.txt", list );
//						Log.i( "list", "After save playList and Before load PlayList" );
//						
//						list = loadPlayList( "playList.txt" );
//						Log.i( "list", "After Load playList" );
//						
//						for ( int i = 0; i < list.length; i++ )
//							Log.i( "list", list[i] );
//						
//						sendCurrentPlayListToDE2( list );
//						
//						/* rating */
//						for ( int i = 0; i < 100; i++ )
//						{
//							list[i] = Integer.toString( 99-i );
//						}
//						
//						Log.i( "list", "Before save saveRating" );
//						saveRating( list );
//						Log.i( "list", "After save saveRating and Before load saveRating" );
//						
//						list = loadRating( );
//						Log.i( "list", "After Load saveRating" );
//						
//						for ( int i = 0; i < list.length; i++ )
//							Log.i( "list", list[i] );
//						
//						Debug = false;
//					}
				} 
				catch (IOException e) 
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	/* Set this to true if debugging saving and loading play list and/or rating list*/
	boolean Debug = true;
	
	/* Constants */
	final static int DE2 = 1;
	final static int MIDDLEMAN = 0;
	final static int ONE_BYTE = 1;
	final static int MAX_BYTES = 255;
	
	private boolean playing;
	private final int loading = 1;
	private int songVolume;
	private Toast showStatus;
	private GestureDetector gestureDetector;
	private ArrayList<String[]> mainPlaylist;
	private int mode;
	private int songIndex;
	private MyApplication app;
	private double currentSongPositionInTime;

	@SuppressLint("ShowToast")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.simple_main);
		showStatus = Toast.makeText(this, "", Toast.LENGTH_SHORT);
		gestureDetector = new GestureDetector(this, this);
		
		playing = false;
		songVolume = 4;
		PositioningThread scale = new PositioningThread();
		scale.run();
		
		MyApplication myApp = (MyApplication)SimpleMainActivity.this.getApplication();
		app = myApp;
		//app.ipStr = ipStr;
		//app.portNumber = portNumber;
		
		
		//new SocketConnect().execute((Void) null);
		
		if (mainPlaylist == null )
		{
			Intent intent = new Intent(this, LoadingScreenActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
			startActivityForResult(intent, loading);
		}
		
		
		//Intent intent = new Intent(this, LoadingScreenActivity.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		//startActivity(intent);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		AnimationListener animationListener = new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				showStatus.setText("yatta");
				showStatus.show();
				ImageView volume = (ImageView) findViewById(R.id.volume);
				ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
				volume.setAlpha((float)0);
				bar.setAlpha(0);
			}
		};
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
			ImageView volume = (ImageView) findViewById(R.id.volume);
			volume.setAlpha((float)1);
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			fadeOutAnimation.setStartOffset(4000);
			fadeOutAnimation.setAnimationListener(animationListener);
			volume.startAnimation(fadeOutAnimation);
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setAlpha(1);
			bar.startAnimation(fadeOutAnimation);
 			if (songVolume > 0) {
 				songVolume--;
 				updateVolume();
 	 			showStatus.setText("Volume = " + Integer.toString(songVolume));
 				showStatus.show();
 				downVolume();
 			}
 			
			return true;
		} 
		else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
			ImageView volume = (ImageView) findViewById(R.id.volume);
			volume.setAlpha((float)1);
			Animation fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);
			fadeOutAnimation.setStartOffset(4000);
			fadeOutAnimation.setAnimationListener(animationListener);
			volume.startAnimation(fadeOutAnimation);
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setAlpha(1);
			bar.startAnimation(fadeOutAnimation);
			if (songVolume < 8){
				songVolume++;
				updateVolume();
				showStatus.setText("Volume = " + Integer.toString(songVolume));
				showStatus.show();
				upVolume();
			}

			return true;
		}
		else return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.simple_main, menu);
		return true;
	}
	
	public void mainButtonPressed (View view)
	{
		ImageView image = (ImageView) findViewById(R.id.mainButton);
	
		if (playing == false){
			image.setImageResource(R.drawable.pause_button);
			showStatus.setText("Playing");
			showStatus.show();
			playing = true;
		}
		else {
			image.setImageResource(R.drawable.play_button);
			showStatus.setText("Paused");
			showStatus.show();
			playing = false;
		}
		
	}
	
	private class PositioningThread implements Runnable{

		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
		@Override
		public void run() {
			//Resources res = getResources();
			Display display = getWindowManager().getDefaultDisplay();
			Point size = new Point();
			display.getSize(size);
			int width = size.x;
			int height = size.y;
			ImageView image = (ImageView) findViewById(R.id.mainButton);
			TextView songName = (TextView) findViewById(R.id.songName);
			TextView artistName = (TextView) findViewById(R.id.artistName);
			ImageView volume = (ImageView) findViewById(R.id.volume);
			ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
			bar.setProgress((int) (songVolume*12.5));
			float newWidth = (float) (width/1.6);
			float newHeight = (float) (height/8);
			
			/*
			float currentWidth = 256;
			float scale = newWidth/currentWidth;
			image.setScaleY( scale );
			image.setY( - newHeight);
			float changes = (newWidth-256)/2;
			songName.setY(-changes - newHeight);
			artistName.setY(-changes - newHeight);
			//layout.setScaleY(scale);
			//layout.set
			image.setScaleX(scale);
			
			
			scale = newHeight/256;
			volume.setScaleX(scale);
			volume.setScaleY(scale);
			//volume.setX(-100);
			*/
			
			volume.setImageResource(R.drawable.volumev3);
			volume.setAlpha((float)0);
			bar.setAlpha(0);
			//bar.setX(x)
			//image.setScaleY(2);
			
			//layout.setLayoutParams(new LinearLayout.LayoutParams((int) (LayoutParams.WRAP_CONTENT*scale), (int) (LayoutParams.WRAP_CONTENT*scale)));
			//image.setX((width-newWidth)/2);
			//image.setX(0);
			//image.setY(0);
			//LinearLayout layout = (LinearLayout) findViewById(R.id.mainButtonLayout);
			//layout.setScaleY(2);
			//image.setScaleX(2);
			//image.setScaleY(2);
			/*if (orientation == Configuration.ORIENTATION_PORTRAIT){
				float newSize = (float) (width/1.6);
				float currentWidth = 256;
				float scale = newSize/currentWidth;
				layout.setScaleY( scale );
				image.setScaleX( scale);
			}	*/		
			
		}
		
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent me){
		return gestureDetector.onTouchEvent(me);
	}

	@Override
	public boolean onDown(MotionEvent arg0) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent start, MotionEvent finish, float xVelocity,
			float yVelocity) {
		showStatus.setText("Fling detected");
		showStatus.show();
		if (start.getRawX() < finish.getRawX()){
			showStatus.setText("Right fling detected, Next!");
			showStatus.show();
			nextSong();
			return true;
		}
		if (start.getRawX() > finish.getRawX()){
			showStatus.setText("Left fling detected, Prev!");
			showStatus.show();
			prevSong();
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		return false;
	}
	
	private void updateVolume(){
		ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar1);
		bar.setProgress((int) (songVolume*12.5));
	}
	
	@Override 
	public void onActivityResult(int requestCode, int resultCode, Intent data) {     
		super.onActivityResult(requestCode, resultCode, data); 
		switch(requestCode) { 
		case (loading) : { 
			if (resultCode == Activity.RESULT_OK) { 
				
				String[] playlist = data.getStringArrayExtra("FromLoading");
				initializeList(playlist);
				
				TCPReadTimerTask tcp_task = new TCPReadTimerTask();
				Timer tcp_timer = new Timer();
				tcp_timer.schedule(tcp_task, 0, 200);
				ProgressBar bar = (ProgressBar) findViewById(R.id.progressBar2);
				bar.setProgress(0);
				TextView songName = (TextView) findViewById(R.id.songName);
				TextView artistName = (TextView) findViewById(R.id.artistName);
				songName.setText(mainPlaylist.get(0)[1]);
				artistName.setText(mainPlaylist.get(0)[2]);
			} 
			break; 
		} 
		} 
	}
	
	public void initializeList(String[] playlist)
	{
		Log.i("Modee", "SimpleMain - initializing Song List");
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
		
		Log.i("Modee", "SimpleMain - Finished initializing Song List");
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
	public void debugHandShakedLongMessage(View view) 
	{
		app.new SocketSend().execute("M");
	}
	
	public void openAdvancedPlaylist(View view)
	{
		showStatus.setText("Opening advanced playlist");
		showStatus.show();
		Intent intent = new Intent(this, LoadingScreenActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		//startActivityForResult(intent, loading);
	}

}
