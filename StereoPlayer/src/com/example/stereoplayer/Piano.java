package com.example.stereoplayer;

import com.example.stereoplayer.MyApplication.SocketSend;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;

public class Piano extends Activity {

	private Vibrator vibrator; 

	private Button DoD_btn;
	private Button Re_btn;
	private Button Mi_btn;
	private Button Fa_btn;
	private Button So_btn;
	private Button La_btn;
	private Button Ti_btn;
	private Button DoU_btn; 
	private boolean Dod_bool = false;
	private MyApplication app;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_piano);
		//vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		
		//addListenerOnButton();
		app = (MyApplication) getApplication();
	}
	
	@Override
	public void onPause()
	{
		super.onPause();
		app.new SocketSend().execute("K");
	}
	
	@Override
	public void onResume()
	{
		super.onResume();
		app.new SocketSend().execute("k");
	}

	public void playDoD(View view)
	{
		//Toast.make
		app.new SocketSend().execute("DoD");
		//vibrator.vibrate(300);
	}

	public void playRe(View view)
	{
		app.new SocketSend().execute("Re");
	}

	public void playMi(View view)
	{
		app.new SocketSend().execute("Mi");
	}

	public void playFa(View view)
	{
		app.new SocketSend().execute("Fa");
	}

	public void playSo(View view)
	{
		app.new SocketSend().execute("So");
	}

	public void playLa(View view)
	{
		app.new SocketSend().execute("La");
	}

	public void playTi(View view)
	{
		app.new SocketSend().execute("Ti");
	}

	public void playDoU(View view)
	{
		app.new SocketSend().execute("DoU");
	}



	/*public void addListenerOnButton() {


		DoD_btn = (Button) findViewById(R.id.DoD);
		Re_btn = (Button) findViewById(R.id.Re);
		Mi_btn = (Button) findViewById(R.id.Mi);
		Fa_btn = (Button) findViewById(R.id.Fa);
		So_btn = (Button) findViewById(R.id.So);
		La_btn = (Button) findViewById(R.id.La);
		Ti_btn = (Button) findViewById(R.id.Ti);
		DoU_btn = (Button) findViewById(R.id.DoU);



			DoD_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				   case MotionEvent.ACTION_DOWN:
				   {
					   vibrator.vibrate(300);
					   app.new SocketSend().execute("DoD");
				   }
				   break;
				   case MotionEvent.ACTION_UP:
					   //add code here
				   break;

				}
				return false;
			}

		});

		Re_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});

		Mi_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});

		Fa_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});
		So_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});

		La_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});

		Ti_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});

		DoU_btn.setOnTouchListener(new OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction()){
				case MotionEvent.ACTION_DOWN:
					vibrator.vibrate(300);
					break;
				case MotionEvent.ACTION_UP:
					//add code here
					break;

				}
				return false;
			}

		});


	}*/


}
