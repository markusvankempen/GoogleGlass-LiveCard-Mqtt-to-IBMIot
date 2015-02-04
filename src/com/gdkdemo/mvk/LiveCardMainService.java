package com.gdkdemo.mvk;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.hitlabnz.wailc.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.gdkdemo.sensor.motion.common.SensorValueStruct;
import com.google.android.glass.timeline.LiveCard;

public class LiveCardMainService extends Service {

	// live card and remoteviews on the card
	private LiveCard liveCard;
	private RemoteViews remoteViews;
	
	// location
	private LocationManager locationManager;
	private List<String> locationProviders;
	
	// sensor
	private SensorManager sensorManager;
	private Sensor gravitySensor;
	private Sensor mSensorGyroscope = null;
	SensorEvent myevent = null;
	
	float gravityValue[] = null;
	float gyroscopeValue[] = null;
	
	// screen update timer
	private Timer timer;
	
	

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		// stop timer. this is only used for the timer demo
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
				
		// stop listening to location
		if(locationProviders != null) {
			locationManager.removeUpdates(locationListener);
		}
		
		// stop listening to sensor
		if(gravitySensor != null)
			sensorManager.unregisterListener(gravitySensorListener);
		
		// unpublish the live card
		if (liveCard != null) {
			if (liveCard.isPublished())
				liveCard.unpublish();
			liveCard = null;
		}

		super.onDestroy();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("MVK", ".onStartCommand() -MotionSensorILiveCard");
		if (liveCard == null) {
			// create a live card object
			liveCard = new LiveCard(getApplicationContext(), "MotionSensorILiveCard");

			// set rendering view of the live card
			remoteViews = new RemoteViews(getPackageName(), R.layout.livecard);
			liveCard.setViews(remoteViews);

			// set to call menu activity when tapped on the live card
			Intent menuIntent = new Intent(this, MenuActivity.class);
			liveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));

			// publish the card into the timeline
			liveCard.publish(LiveCard.PublishMode.REVEAL); // LiveCard.PublishMode.SILENT

			// setup location manager
			locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			locationProviders = locationManager.getProviders(criteria, true /* enabledOnly */);
			for (String provider : locationProviders) {
				locationManager.requestLocationUpdates(provider, 
						1000, // update every 1 sec (1000 millisec)
						50, // update every 50 meters
						locationListener);
			}
			
			// setup sensor manager
			sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	    	gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); //TYPE_GRAVITY);
//	    	mSensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	    	
	    	if(gravitySensor == null)
	    		gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
			if(gravitySensor != null)
				sensorManager.registerListener(gravitySensorListener, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);

//	    	if(mSensorGyroscope == null)
//	    		mSensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	//		if(mSensorGyroscope != null)
		//		sensorManager.registerListener(gyroscopeSensorListener, mSensorGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
			
			
			// setup timer to update the text view for the sensor value
			if (timer == null) {
				timer = new Timer();
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						doTimerTask();
					}
				}, 1000, 1000); // start after 1 sec, and repeat every sec
			}
		} else {
			// Card is already published, go to the live card.
			liveCard.navigate();
		}

		return Service.START_STICKY; // keep running until explicitly stopped
	}
	
	// listener for the location
	private LocationListener locationListener = new LocationListener() {
		@Override
		public void onLocationChanged(Location location) {
			Log.d("MVK", ".onLocationChanged() " + 	String.format("Latitude = %.6f, Longitud = %.6f", location.getLatitude(), location.getLongitude()));
			remoteViews.setTextViewText(R.id.latLonCoordinate, 
					String.format("%.6f, %.6f", location.getLatitude(), location.getLongitude()));
			liveCard.setViews(remoteViews);
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	};
	
	// listener for the sensor
	SensorEventListener gyroscopeSensorListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			gyroscopeValue = event.values;
			myevent = event;
			
//			SensorValueStruct data = new SensorValueStruct(type, timestamp, values, accuracy);
			
		}
    };
//
    
    
	// listener for the sensor
	SensorEventListener gravitySensorListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			gravityValue = event.values;		
			myevent = event;
		}
    };

   /* 
    private static MqttClient client = null;
    public  String clientId = "d:hhy6xb:GLASS:f88fca115144";
    public String org = "eywvcm";
    public String SETTINGS_MQTT_SERVER = ".messaging.internetofthings.ibmcloud.com";
    public String SETTINGS_MQTT_PORT = "1883";
    public String SETTINGS_USERNAME = "use-token-auth";
    public String SETTINGS_TOKEN = "8LCZFBjf+8Z57A+Kew";
   
*/
    
    private static MqttClient client = null;
    public  String clientId = "d:quickstart:GLASS:f88fca115133";    
    public String org = "quickstart";
    
    
    public String SETTINGS_MQTT_SERVER = ".messaging.internetofthings.ibmcloud.com";
    public String SETTINGS_MQTT_PORT = "1883";
    public String SETTINGS_USERNAME = "";
    public String SETTINGS_TOKEN = "";
    
    
    public void sendMQTTData(SensorEvent event) {        
               
        if ((client != null) && client.isConnected()) { 
			// Don't do anything
		} else {
			try {
				// File persist not working on Glass
				MemoryPersistence persistence = new MemoryPersistence();
				
	            String serverHost =  SETTINGS_MQTT_SERVER;
	            String serverPort =  SETTINGS_MQTT_PORT;
	            Log.d("MVK", ".initMqttConnection() - Host name: "+ org + serverHost + ", Port: " + serverPort
	                    + ", client id: " + clientId +"<<<");

	            String connectionUri = "tcp://" + org + serverHost + ":" + serverPort;

	            
				client = new MqttClient(connectionUri, clientId, persistence);
			} catch (MqttException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			MqttConnectOptions options = new MqttConnectOptions();
//            options.setUserName(SETTINGS_USERNAME);
//            options.setPassword(SETTINGS_TOKEN.toCharArray());
			
			// Connect to the broker
			try {
				Log.d("MVK", "TRY  to connect to IOT");
				//client.connect(options, context, listener);
				client.connect(options);
				Log.d("MVK", "MQTT connected "); 
			} catch (MqttSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }//esle connected
			
//Topic 
		if ((client != null) && client.isConnected()) {
			MqttTopic topic = client.getTopic("iot-2/evt/status/fmt/json");
						
        /*
         * if(xAcceleration>40 && direction != 4){
         
      	   directiontxt="right";
        
        if(xAcceleration<-40 && direction != 4){
      	   directiontxt="left";
*/
			
	        String messageData = "{ \"d\": {" +
	        		"\"myName\":\"Markus Google GLASS\"," +
	        		"\"direction\":" + "\""+ directiontxt + "\","+
	                "\"x\":" +  event.values[0] + "," +
	                "\"y\":" +  event.values[1] + "," +
	                "\"z\":" +  event.values[2] + " " +
	                "} }";
			
	        Log.d("MVK", "Send Message:"+messageData); 
	        
			MqttMessage message = new MqttMessage(messageData.getBytes());
			message.setQos(0);
			
	
			try {
				
				// Give the message to the client for publishing. For QoS 2, this
				// will involve multiple network calls, which will happen
				// asynchronously after this method has returned.
		/*		
			  	var myJsonData = {
		                   "d": {
		                     "myName": "Nest Data",
		                     "deviceName" : device.name,
		                     "deviceId"    : deviceId,
		                     "currentTemp" : device.current_temperature,
		                     "targetTemp"  :  device.target_temperature

		                    }
		                  };
*/
				
				
				//topic iot-2/evt/status/fmt/json
				
				topic.publish(message);
				Log.d("MVK", "Set MQTT message: "+message); 
			} catch (MqttException ex) {
				
				// Client has not accepted the message due to a failure
				// Depending on the exception's reason code, we could always retry
				System.err.println("Failed to send message");
				Log.d("MVK", "Failed to send message: "+ex.toString());
			}
		}

    }

	  int direction = 1;
	  String  directiontxt="forward";

    
    // update the textview for the sensor value
	void doTimerTask() {
		
		/*
		 * 
		 *         long now = System.currentTimeMillis();

        Sensor sensor = event.sensor;
        int type = sensor.getType();
        long timestamp = event.timestamp;
        float[] values = event.values;
        int accuracy = event.accuracy;
        SensorValueStruct data = new SensorValueStruct(type, timestamp, values, accuracy);

		 */
        Sensor sensor = myevent.sensor;
        int type = sensor.getType();

        switch(type) {
        case Sensor.TYPE_ACCELEROMETER:
        	Log.d("MVK", ".doTimerTask() TYPE_ACCELEROMETER ");
			Log.d("MVK", ".doTimerTask() Values = " + 	String.format("%.3f, %.3f, %.3f", gravityValue[0], gravityValue[1], gravityValue[2]));

            int xAcceleration=(int) (-myevent.values[0] * 10);
            int yAcceleration=(int) (-myevent.values[1] * 10);
            int zAcceleration=(int) (-myevent.values[2] * 10);
         		               
            
            if(zAcceleration>40 && direction != 1){
         	   direction = 1;
         	   directiontxt="forward";
            }else if(zAcceleration<-40 && direction != 2){
         	   direction = 2;
         	   //sendData("2");//backward
         	   directiontxt="backward";
            }else if(xAcceleration>40 && direction != 3){
         	   direction = 3;
         	   //sendData("3");//right
         	   directiontxt="right";
            }else if(xAcceleration<-40 && direction != 4){
         	   direction = 4;
         	   //sendData("4");//left
         	   directiontxt="left";
            }else if(zAcceleration>=-40 && zAcceleration<=40 && xAcceleration>=-40 && xAcceleration<=40 && direction != 5){
         	   direction = 5;
         	   //sendData("5");//stop
         	   directiontxt="stop";
            }

            sendMQTTData(myevent);
            
         Log.d("MVK", "Sensor: TYPE_ACCELEROMETER Direction: "+directiontxt+" xAcceleration =  : " +xAcceleration + " yAcceleration="+yAcceleration+ " zAcceleration="+zAcceleration);
			remoteViews.setTextViewText(R.id.sensorValues, 
					String.format("ACC: Z:%.3f, Y:%.3f, Z:%.3f %s" , myevent.values[0],  myevent.values[1], myevent.values[2],directiontxt));

        	
        	//          lastSensorValuesAccelerometer = data;
            break;
        case Sensor.TYPE_GRAVITY:
			Log.d("MVK", ".doTimerTask() gravityValues = " + 	String.format("Gravity: %.3f, %.3f, %.3f", gravityValue[0], gravityValue[1], gravityValue[2]));
			remoteViews.setTextViewText(R.id.sensorValues, 
					String.format("Gravity: %.3f, %.3f, %.3f", gravityValue[0], gravityValue[1], gravityValue[2]));
            break;
        case Sensor.TYPE_LINEAR_ACCELERATION:
        	Log.d("MVK", ".doTimerTask() TYPE_LINEAR_ACCELERATION ");
    //        lastSensorValuesLinearAcceleration = data;
            break;
        case Sensor.TYPE_GYROSCOPE:
			Log.d("MVK", ".doTimerTask() gyroscopeValue = " + 	String.format("GyroscopeValue: %.3f, %.3f, %.3f", gyroscopeValue[0], gyroscopeValue[1], gyroscopeValue[2]));
			remoteViews.setTextViewText(R.id.sensorValues, 
					String.format("Gyroscope: %.3f, %.3f, %.3f", gyroscopeValue[0], gyroscopeValue[1], gyroscopeValue[2]));
            break;
        case Sensor.TYPE_ROTATION_VECTOR:
        	Log.d("MVK", ".doTimerTask() TYPE_ROTATION_VECTOR ");
      //      lastSensorValuesRotationVector = data;
            break;
        default:
            Log.d("MVK","Unknown type: " + type);
    }

		liveCard.setViews(remoteViews);
		
		
	}
}
