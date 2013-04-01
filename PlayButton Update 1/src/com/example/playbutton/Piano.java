package com.example.playbutton;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.view.View.OnClickListener;
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
		
		
		DoD_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Do!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		Re_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Re!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		Mi_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Mi!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		Fa_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Fa!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		So_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"So!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		La_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"La!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		Ti_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Ti!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
		
		DoU_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				Toast.makeText(Piano.this,"Do!",Toast.LENGTH_SHORT).show();
				vibrator.vibrate(300);
			}
			
		});
	
		
	}
	

}
