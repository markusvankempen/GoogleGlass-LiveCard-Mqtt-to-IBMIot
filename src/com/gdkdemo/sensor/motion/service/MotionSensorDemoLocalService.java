package com.gdkdemo.sensor.motion.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.widget.RemoteViews;

import com.gdkdemo.sensor.motion.MotionSensorDemoActivity;
import com.gdkdemo.sensor.motion.R;
import com.gdkdemo.sensor.motion.common.SensorValueStruct;
import com.google.android.glass.timeline.LiveCard;
//import com.google.android.glass.timeline.TimelineManager;


// ...
public class MotionSensorDemoLocalService extends Service implements SensorEventListener
{
    // For live card
    private LiveCard liveCard = null;

    // Sensor manager
    private SensorManager mSensorManager = null;

    // Motion sensors
    private Sensor mSensorAccelerometer = null;
    private Sensor mSensorGravity = null;
    private Sensor mSensorLinearAcceleration = null;
    private Sensor mSensorGyroscope = null;
    private Sensor mSensorRotationVector = null;


    // Last known motionsensor values.
    private SensorValueStruct lastSensorValuesAccelerometer = null;
    private SensorValueStruct lastSensorValuesGravity = null;
    private SensorValueStruct lastSensorValuesLinearAcceleration = null;
    private SensorValueStruct lastSensorValuesGyroscope = null;
    private SensorValueStruct lastSensorValuesRotationVector = null;

    // TBD:
    // Need a timer, etc. to refresh the UI (LiveCard) ever x seconds, etc..
    // For now, we just use the "last timestamp".
    private long lastRefreshedTime = 0L;


    // No need for IPC...
    public class LocalBinder extends Binder {
        public MotionSensorDemoLocalService getService() {
            return MotionSensorDemoLocalService.this;
        }
    }
    private final IBinder mBinder = new LocalBinder();


    @Override
    public void onCreate()
    {
        super.onCreate();
        initializeSensorManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.i("Received start id " + startId + ": " + intent);
        onServiceStart();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        // ????
        onServiceStart();
        return mBinder;
    }

    @Override
    public void onDestroy()
    {
        // ???
        onServiceStop();
        super.onDestroy();
    }


    @Override
    public void onSensorChanged(SensorEvent event)
    {
        Log.d("onSensorChanged() called.");

        processMotionSensorData(event);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        Log.d("onAccuracyChanged() called.");

    }



    // Service state handlers.
    // ....

    private boolean onServiceStart()
    {
        Log.d("onServiceStart() called.");

        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorGravity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorLinearAcceleration, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mSensorRotationVector, SensorManager.SENSOR_DELAY_NORMAL);

        // Publish live card...
        publishCard(this);

        return true;
    }

    private boolean onServicePause()
    {
        Log.d("onServicePause() called.");
        return true;
    }
    private boolean onServiceResume()
    {
        Log.d("onServiceResume() called.");
        return true;
    }

    private boolean onServiceStop()
    {
        Log.d("onServiceStop() called.");

        mSensorManager.unregisterListener(this);

        // TBD:
        // Unpublish livecard here
        // .....
        unpublishCard(this);
        // ...

        return true;
    }




    // For live cards...

    private void publishCard(Context context)
    {
        publishCard(context, false);
    }
    private void publishCard(Context context, boolean update)
    {
        if(Log.D) Log.d("publishCard() called: update = " + update);
        if (liveCard == null || update == true) {

//            // TBD:
//            // We get multiple liveCards if we just call setViews() and publish()...
//            // As a workaround, for now, we always unpublish the previous card first.
//            if (liveCard != null) {
//                liveCard.unpublish();
//            }
//            // ....

            final String cardId = "motionsensordemo_card";
/*
            // Note: getLiveCard() always publishes a new live card....
            if(liveCard == null) {
            // if(liveCard == null || ! liveCard.isPublished()) {
                TimelineManager tm = TimelineManager.from(context);
                liveCard = tm.createLiveCard(cardId);
//                 liveCard.setNonSilent(true);       // for testing.
            }
 */
            if(liveCard == null)
                   	liveCard = new LiveCard(getApplicationContext(), cardId);
            
            
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.livecard_motionsensordemo);
            String content = "";
            if(lastSensorValuesAccelerometer != null) {
                content += "Accelerometer:\n" + lastSensorValuesAccelerometer.toString() + "\n";
            }
            if(lastSensorValuesGravity != null) {
                content += "Gravity:\n" + lastSensorValuesGravity.toString() + "\n";
            }
            if(lastSensorValuesLinearAcceleration != null) {
                content += "Linear Acceleration:\n" + lastSensorValuesLinearAcceleration.toString() + "\n";
            }
            if(lastSensorValuesGyroscope != null) {
                content += "Gyroscope:\n" + lastSensorValuesGyroscope.toString() + "\n";
            }
            if(lastSensorValuesRotationVector != null) {
                content += "Rotation Vector:\n" + lastSensorValuesRotationVector.toString() + "\n";
            }

            remoteViews.setCharSequence(R.id.livecard_content, "setText", content);
            liveCard.setViews(remoteViews);
            Intent intent = new Intent(context, MotionSensorDemoActivity.class);
            liveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
            // ???
            // Without this if(),
            // I get an exception:
            // "java.lang.IllegalStateException: State CREATED expected, currently in PUBLISHED"
            // Why???
            if(! liveCard.isPublished()) {
                liveCard.publish(LiveCard.PublishMode.REVEAL);
            } else {
                // ????
                // According to the GDK doc,
                // it appears we should call publish() every time the content changes...
                // But, it seems to work without re-publishing...
                if(Log.D) {
                    long now = System.currentTimeMillis();
                    Log.d("liveCard not published at " + now);
                }
            }
        } else {
            // Card is already published.
            return;
        }
    }

    private void unpublishCard(Context context)
    {
        Log.d("unpublishCard() called.");
        if (liveCard != null) {
            liveCard.unpublish();
            liveCard = null;
        }
    }


    // MotionSensor methods
    private void initializeSensorManager()
    {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mSensorLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mSensorGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    private void processMotionSensorData(SensorEvent event)
    {
        long now = System.currentTimeMillis();

        Sensor sensor = event.sensor;
        int type = sensor.getType();
        long timestamp = event.timestamp;
        float[] values = event.values;
        int accuracy = event.accuracy;
        SensorValueStruct data = new SensorValueStruct(type, timestamp, values, accuracy);

        switch(type) {
            case Sensor.TYPE_ACCELEROMETER:
                lastSensorValuesAccelerometer = data;
                break;
            case Sensor.TYPE_GRAVITY:
                lastSensorValuesGravity = data;
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                lastSensorValuesLinearAcceleration = data;
                break;
            case Sensor.TYPE_GYROSCOPE:
                lastSensorValuesGyroscope = data;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                lastSensorValuesRotationVector = data;
                break;
            default:
                Log.w("Unknown type: " + type);
        }

        // TBD:
        // Update the DB, etc..
        // ...

        // Update the UI.
        // temporary
        final long delta = 5000L;   // every 5 seconds
        if(lastRefreshedTime <= now - delta) {
            // if(liveCard != null && liveCard.isPublished()) {
                // Update...
                publishCard(this, true);
            // }
            lastRefreshedTime = now;
        }
    }


}
