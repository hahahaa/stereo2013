package com.example.light;


import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class Light_Activity extends Activity {
 
 SensorManager mySensorManager;
 Sensor myLightSensor;
 TextView textLightSensorData;
 
   @Override
   public void onCreate(Bundle savedInstanceState) {
    
       super.onCreate(savedInstanceState);
       setContentView(R.layout.activity_light_);
      
       textLightSensorData = (TextView)findViewById(R.id.lightsensordata);
      
       mySensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
       myLightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
      
       
        
        mySensorManager.registerListener(lightSensorEventListener,myLightSensor, SensorManager.SENSOR_DELAY_FASTEST);
       }
   
  
   SensorEventListener lightSensorEventListener = new SensorEventListener(){

   @Override
   public void onAccuracyChanged(Sensor arg0, int arg1) {
    // auto generated 
   }

   @Override
   public void onSensorChanged(SensorEvent light_sense) {
    // TODO Auto-generated method stub
    if(light_sense.sensor.getType()==Sensor.TYPE_LIGHT){
     textLightSensorData.setText("Light Sensor Date:"
       + String.valueOf(light_sense.values[0]));
    }
    if (light_sense.values[0] < 50){
    	Toast.makeText(Light_Activity.this,"Darkness D:",Toast.LENGTH_SHORT).show();
    }
   }};
}