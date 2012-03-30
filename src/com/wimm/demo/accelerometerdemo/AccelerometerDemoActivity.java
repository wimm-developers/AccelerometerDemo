/* 
 * Copyright (C) 2012 WIMM Labs Incorporated
 */

package com.wimm.demo.accelerometerdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;

import com.wimm.framework.app.LauncherActivity;

/*
 * This is a demo of accelerometer usage that will cycle the background color 
 * of a view each time a shake is detected.
 *
 */
public class AccelerometerDemoActivity extends LauncherActivity implements SensorEventListener {
    private static final String TAG = AccelerometerDemoActivity.class.getSimpleName();

    // The array of background colors to cycle through.
    private static final int[] mBackgroundColorArray = {
        Color.BLUE, Color.GREEN, Color.RED, Color.YELLOW, Color.CYAN, Color.MAGENTA };

    // An integer tracking the index of the current background color.
    private int mBackgroundColorIndex = 0;

    // The view whose background we will be changing.
    private View mTargetView = null;

    private SensorManager mSensorManager;

    // We will track the last time we changed the background color and only
    // update it again if a certain number of milliseconds has passed. This
    // will make the color changes more deterministic: one shake = one change,
    // rather than colors quickly cycling throughout shakes and stopping on
    // something random.
    private static final long MAX_COLOR_CHANGE_FREQUENCEY_MS = 500;
    
    /*
     * This threshold determine whether or not we would consider a force(measured in Gs)
     * experienced by the device to be considered a shake.
     *  
     */
    private static final long SHAKE_FORCE_THRESHOLD_IN_G = 2;
    
    private long mLastUpdateMs = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Retrieve the view whose background color we want to change and give
        // it an initial value.
        mTargetView = findViewById(R.id.textView);
        mTargetView.setBackgroundColor(mBackgroundColorArray[mBackgroundColorIndex]);

        // Retrieve a SensorManager instance.
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        // The system does not disable the accelerometer automatically when the screen
        // turns off. To help save power we will listen for screen change events and
        // register/unregister for accelerometer updates accordingly.
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.SCREEN_DIM");
        filter.addAction("android.intent.action.SCREEN_ON");
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setAccelerometerUpdatesEnabled(true);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        setAccelerometerUpdatesEnabled(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unregister for updates about screen on/dim changes.
        unregisterReceiver(mReceiver);
    }
    
    private void setAccelerometerUpdatesEnabled(boolean enabled) {
        if (enabled) {
            // Register for accelerometer update events.
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // Unregister for accelerometer update events. If nobody else in
            // the system is registered for events the accelerometer will be
            // allowed to power down.
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (isShakeDetected(event)) {
                // If we detected a shake, update to the next background color.
                mBackgroundColorIndex = (mBackgroundColorIndex + 1) % mBackgroundColorArray.length;
                mTargetView.setBackgroundColor(mBackgroundColorArray[mBackgroundColorIndex]);
            }
        }
    }

    /*
     * This function determines whether the accelerometer detected a force of
     * greater than SHAKE_FORCE_THRESHOLD_IN_G, which we will consider to be a shake.
     * For details of the information returned by the accelerometer and how to use it, 
     * please see the android documentation at:
     * http://developer.android.com/guide/topics/sensors/sensors_motion.html
     *
     */
    private boolean isShakeDetected(SensorEvent event){
        float[] values = event.values;
        float x = values[0];
        float y = values[1];
        float z = values[2];

        float numberOfGsDetected = (x * x + y * y + z * z)
                / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);
        if (numberOfGsDetected >= SHAKE_FORCE_THRESHOLD_IN_G)
        {
            long currentMs = System.currentTimeMillis();
            if ((currentMs - mLastUpdateMs) > MAX_COLOR_CHANGE_FREQUENCEY_MS) {
                mLastUpdateMs = currentMs;
                return true;
            }
        }

        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We aren't concerned with sensor accuracy in this demo.
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals("android.intent.action.SCREEN_DIM")) {
                // If the screen dims the device is about to go to sleep.
                // Unregister for accelerometer events.
                setAccelerometerUpdatesEnabled(false);
            } else if (action.equals("android.intent.action.SCREEN_ON")) {
                // When the screen turns on we will be active again. Register for
                // accelerometer events so we can resume watching for shakes.
                setAccelerometerUpdatesEnabled(true);
            }
        }
    };
}
