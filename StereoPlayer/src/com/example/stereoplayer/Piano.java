package com.example.stereoplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.Toast;

public class Piano extends Activity {
	
	private Vibrator vibrator; 
	
	Button DoD_btn;
	Button Re_btn;
	Button Mi_btn;
	Button Fa_btn;
	Button So_btn;
	Button La_btn;
	Button Ti_btn;
	Button DoU_btn; 
	boolean Dod_bool = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_piano);
		vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
		addListenerOnButton();
	}

	
	
	public void addListenerOnButton() {
		 
		
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
					   vibrator.vibrate(300);
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
	
		
	}
	

}
