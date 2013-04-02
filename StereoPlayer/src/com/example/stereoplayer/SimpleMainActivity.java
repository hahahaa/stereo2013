package com.example.stereoplayer;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;




import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
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

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SimpleMainActivity extends Activity implements OnGestureListener {
	
	
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
	private String ipStr = "206.87.117.1";
	private int portNumber = 50002;
	private int mode;

	@SuppressLint("ShowToast")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
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
		
		MyApplication app = (MyApplication)SimpleMainActivity.this.getApplication();
		app.ipStr = ipStr;
		app.portNumber = portNumber;
		
		
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
 			if (songVolume > 0) songVolume--;
 			updateVolume();
 			showStatus.setText("Volume = " + Integer.toString(songVolume));
			showStatus.show();
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
			if (songVolume < 8) songVolume++;
			updateVolume();
			showStatus.setText("Volume = " + Integer.toString(songVolume));
			showStatus.show();

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
		// TODO Auto-generated method stub
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
			return true;
		}
		if (start.getRawX() > finish.getRawX()){
			showStatus.setText("Left fling detected, Prev!");
			showStatus.show();
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2,
			float arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onShowPress(MotionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onSingleTapUp(MotionEvent arg0) {
		// TODO Auto-generated method stub
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
				String newText = data.getStringExtra("FromLoading");
				Toast.makeText(this, newText, Toast.LENGTH_SHORT).show();
			} 
			break; 
		} 
		} 
	}
	
	// This is the Socket Connect asynchronous thread. Opening a socket
	// has to be done in an Asynchronous thread in Android. Be sure you
	// have done the Asynchronous Tread tutorial before trying to understand
	// this code.
	/*public class SocketConnect extends AsyncTask<Void, Void, Socket> {
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
			MyApplication myApp = (MyApplication) SimpleMainActivity.this.getApplication();
			myApp.sock = s;

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

			//if(mode == DE2)
			//	new SocketSend().execute("playlist");
			//else
			//	new SocketSend().execute("02.01.Super MArio.kai.5.64.02.LOL.new artist.4.22.");
		}
	}*/

}
