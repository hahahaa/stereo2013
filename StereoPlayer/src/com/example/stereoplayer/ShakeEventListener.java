package com.example.stereoplayer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

// found this code on stackoverflow from user peceps

// This is the listener that detects gestures 
public class ShakeEventListener implements SensorEventListener {
// minimum force to detect
  private static final int MIN_FORCE = 10;

 
 //Minimum times in a shake gesture that the direction of movement needs to change.
   
  private static final int MIN_DIRECTION_CHANGE = 3;

// maximum time between movements 
  private static final int MAX_PAUSE_BETHWEEN_DIRECTION_CHANGE = 200;

 // time allowed for shake gesture 
  private static final int MAX_TOTAL_DURATION_OF_SHAKE = 400;

 // time when the first gesture is detected 
  private long mFirstDirectionChangeTime = 0;

 // time of the last gesture
  private long mLastDirectionChangeTime;

// how many movements have occured 
  private int mDirectionChangeCount = 0;

// last x coordinate
  private float lastX = 0;

// last y coordinate
  private float lastY = 0;

// last z coordinate
  private float lastZ = 0;

// called when shake is detected 
  private OnShakeListener mShakeListener;

 // shake gesture interface
  public interface OnShakeListener {

    /**
     * Called when shake gesture is detected.
     */
    void onShake();
  }

  public void setOnShakeListener(OnShakeListener listener) {
    mShakeListener = listener;
  }

  @Override
  public void onSensorChanged(SensorEvent se) {
    // get sensor data
    float x = se.values[SensorManager.DATA_X];
    float y = se.values[SensorManager.DATA_Y];
    float z = se.values[SensorManager.DATA_Z];

    // calculate movement
    float totalMovement = Math.abs(x + y + z - lastX - lastY - lastZ);

    if (totalMovement > MIN_FORCE) {

      // get time
      long now = System.currentTimeMillis();

      // store first movement time
      if (mFirstDirectionChangeTime == 0) {
        mFirstDirectionChangeTime = now;
        mLastDirectionChangeTime = now;
      }

      // check if the last movement was not long ago
      long lastChangeWasAgo = now - mLastDirectionChangeTime;
      if (lastChangeWasAgo < MAX_PAUSE_BETHWEEN_DIRECTION_CHANGE) {

        // store movement data
        mLastDirectionChangeTime = now;
        mDirectionChangeCount++;

        // store last sensor data 
        lastX = x;
        lastY = y;
        lastZ = z;

        // check how many movements are so far
        if (mDirectionChangeCount >= MIN_DIRECTION_CHANGE) {

          // check total duration
          long totalDuration = now - mFirstDirectionChangeTime;
          if (totalDuration < MAX_TOTAL_DURATION_OF_SHAKE) {
            mShakeListener.onShake();
            resetShakeParameters();
          }
        }

      } else {
        resetShakeParameters();
      }
    }
  }

// reset the parameters 
  private void resetShakeParameters() {
    mFirstDirectionChangeTime = 0;
    mDirectionChangeCount = 0;
    mLastDirectionChangeTime = 0;
    lastX = 0;
    lastY = 0;
    lastZ = 0;
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {
  }

}