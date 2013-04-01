package com.example.playbutton;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
 
public class PlayButtonActivity extends Activity  {
 
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
}