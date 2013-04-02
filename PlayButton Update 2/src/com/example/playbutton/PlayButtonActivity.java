package com.example.playbutton;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
 
public class PlayButtonActivity extends Activity implements SensorEventListener  {
 
	private float mLastX, mLastY, mLastZ;
	private boolean mInitialized;
	private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private final float NOISE = (float) 2.0;
	
	
	
	
	
	ImageButton Play_btn;
	ImageButton Next_btn;
	ImageButton Previous_btn;
	ImageButton Piano_btn;
	
	boolean play_active;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_play_button);
		play_active = false;
		
		 mInitialized = false;
	     mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	     mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	     mSensorManager.registerListener(this, mAccelerometer , SensorManager.SENSOR_DELAY_NORMAL);
		
		
		addListenerOnButton();
 
	}
	public void addListenerOnButton() {
 
		Play_btn = (ImageButton) findViewById(R.id.Play);
		Next_btn = (ImageButton) findViewById(R.id.Next);
		Previous_btn = (ImageButton) findViewById(R.id.Previous);
		Piano_btn = (ImageButton) findViewById(R.id.Piano);
		
		Play_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
				if( play_active == false){
					
					Play_btn.setImageResource(R.drawable.btn_play);
					play_active = true;
					Toast.makeText(PlayButtonActivity.this,"Pause! D:",Toast.LENGTH_SHORT).show();
			    } 
				else{
					Play_btn.setImageResource(R.drawable.btn_pause);
					play_active = false;
					Toast.makeText(PlayButtonActivity.this,"Play! :D",Toast.LENGTH_SHORT).show();
			    } 
			}
			//public void onPostExecute(){
				
			//}
		});
		
	
		Next_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
					Toast.makeText(PlayButtonActivity.this,"Next Song!",Toast.LENGTH_SHORT).show();
			}
			//public void onPostExecute(){
				
			//}
		});
		
		Previous_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
					Toast.makeText(PlayButtonActivity.this,"Previous Song!",Toast.LENGTH_SHORT).show();
			}
			//public void onPostExecute(){
				
			//}
		});
		Piano_btn.setOnClickListener(new OnClickListener(){
			public void onClick(View v) {
					Toast.makeText(PlayButtonActivity.this,"Piano Time!",Toast.LENGTH_SHORT).show();
					Intent intent = new Intent( PlayButtonActivity.this ,Piano.class);
					startActivity(intent);
			}
		});
	}
	public void onSensorChanged(SensorEvent event) {
		//TextView tvX= (TextView)findViewById(R.id.x_axis);
		//TextView tvY= (TextView)findViewById(R.id.y_axis);
		//TextView tvZ= (TextView)findViewById(R.id.z_axis);
		//ImageView iv = (ImageView)findViewById(R.id.image);
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		if (!mInitialized) {
			mLastX = x;
			mLastY = y;
			mLastZ = z;
		//	tvX.setText("0.0");
		//	tvY.setText("0.0");
		//	tvZ.setText("0.0");
			mInitialized = true;
		} else {
			float deltaX = Math.abs(mLastX - x);
			float deltaY = Math.abs(mLastY - y);
			float deltaZ = Math.abs(mLastZ - z);
			if (deltaX < NOISE) deltaX = (float)0.0;
			if (deltaY < NOISE) deltaY = (float)0.0;
			if (deltaZ < NOISE) deltaZ = (float)0.0;
			mLastX = x;
			mLastY = y;
			mLastZ = z;
		///	tvX.setText(Float.toString(deltaX));
		//	tvY.setText(Float.toString(deltaY));
		//	tvZ.setText(Float.toString(deltaZ));
		//	iv.setVisibility(View.VISIBLE);
			if (deltaX > deltaY) {
		//		iv.setImageResource(R.drawable.horizontal);
			} else if (deltaY > deltaX) {
		//		iv.setImageResource(R.drawable.vertical);
			} else {
		//		iv.setVisibility(View.INVISIBLE);
			}
		}
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
}