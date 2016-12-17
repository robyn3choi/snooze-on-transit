package com.robyn.snoozeontransit;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.os.Binder;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.os.Vibrator;

import java.io.IOException;




/**
 * Created by Robyn on 12/11/2015.
 */
public class BGService extends Service {

    private IBinder mBinder = new MyBinder();
    private static String LOG_TAG = "BGService";
    public Location myLocation;

    boolean isAlarmSet;
    boolean isAlarmPlaying;
    final Context context = this;
    private MediaPlayer player;

    public LatLng destination;
    public LatLng current;
    public Vibrator vibrator;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        Bundle bundle = intent.getParcelableExtra("bundle");
//        destination = bundle.getParcelable("destination");

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(LOG_TAG, "in onCreate");
        // start getting location and stuff
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.v(LOG_TAG, "in onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.v(LOG_TAG, "in onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v(LOG_TAG, "in onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(LOG_TAG, "in onDestroy");
        // stop getting locations and stuff
    }

    public class MyBinder extends Binder {
        BGService getService() {
            return BGService.this;
        }
    }


    public void setUpLocation(GoogleMap mMap) {
        Log.v(LOG_TAG, "in setuplocation");

        // Enable MyLocation Layer of Google Map
        mMap.setMyLocationEnabled(true);

        // Get LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Create a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Get Current Location
        myLocation = locationManager.getLastKnownLocation(provider);

//         request that the provider send this activity GPS updates every 20 seconds
        locationManager.requestLocationUpdates(provider, 2000, 0, new LocationListener() {

            @Override
            public void onLocationChanged(Location location) {

                //  current = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                myLocation = location;

                double myLat = myLocation.getLatitude();
                double myLong = myLocation.getLongitude();
//                resultText.setText("myLat = " + String.valueOf(myLat) + " and myLong = " + String.valueOf(myLong) );


//                if (destination != null && myLocation != null) {
//                    if (isCloseEnough(calculateDistance(destination)) && isAlarmSet) {
//                        playAlarm();
//                    }
//                }


            }


            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });

        // set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Get latitude of the current location
        double latitude = myLocation.getLatitude();

        // Get longitude of the current location
        double longitude = myLocation.getLongitude();

        // Create a LatLng object for the current location
        current = new LatLng(latitude, longitude);
//
//        // Show the current location in Google Map
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));
//
//        // Zoom in the Google Map
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
//
//        mMap.setOnMapClickListener(this);

        final Handler h = new Handler();
        final int delay = 1000; //milliseconds

        h.postDelayed(new Runnable(){
            public void run(){

                if (destination != null && myLocation != null) {
                    if (isCloseEnough(calculateDistance(destination)) && isAlarmSet) {
                        playAlarm();
                    }
                }

                h.postDelayed(this, delay);
            }
        }, delay);

    }

    public void mapClick(LatLng latLng, GoogleMap mMap) {
        if (!isAlarmSet) {                              // a new destination can't be created if there's already an alarm set
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Destination")
                    .snippet("You're going here"));
            destination = latLng;

            if (destination != null && current != null) {  // if there is a destination and a current
                calculateDistance(destination);
            }
           // setUpLocation(mMap);


            //resultText.setText("You are " + Float.toString(mBGService.calculateDistance(destination)) + "km away from your destination." );

        }
    }

    public Location getMyLocation() {
        return myLocation;
    }



    public float calculateDistance(LatLng destination) {


        double destLat = destination.latitude;
        double destLong = destination.longitude;

//        Location currLocation = new Location("currLocation");
//        currLocation.setLatitude(currLat);
//        currLocation.setLongitude(currLong);

        Location destLocation = new Location("destLocation");
        destLocation.setLatitude(destLat);
        destLocation.setLongitude(destLong);

        float distance = myLocation.distanceTo(destLocation) / 1000; // in km
        double myLat = myLocation.getLatitude();

              Log.d(LOG_TAG, "myLocation is" + myLat +"km.");
//        resultText.setText("You are " + Float.toString(distance) + "km away from your destination." );



        return distance;
    }

        public boolean isCloseEnough(float distance) {
            return (distance <= 1);
        }


        public void setAlarm() {
            isAlarmSet = true;

        }

        public void stopAlarm() {
            if (isAlarmPlaying) {
                player.stop();
                isAlarmSet = false;
                isAlarmPlaying = false;
                vibrator.cancel();

            }
        }

        public void cancelAlarm() {
            isAlarmSet = false;
        }

        public void playAlarm() {
            if (isAlarmPlaying) {
                return;
            }
            isAlarmPlaying = true;
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

            long[] pattern = {0, 1000, 100};
            vibrator.vibrate(pattern, 0);

            player = new MediaPlayer();
            try {
                player.setDataSource(context, getAlarmSound());
                final AudioManager audio = (AudioManager) this
                        .getSystemService(Context.AUDIO_SERVICE);
                if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
                    player.setAudioStreamType(AudioManager.STREAM_ALARM);
                    player.prepare();
                    player.start();
                }
            } catch (IOException e) {
                //   Log.e("Error....","Check code...");
            }
        }

        //make the alarm sound the default alarm sound or ringtone or notification sound
        private Uri getAlarmSound() {
            Uri alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alertSound == null) {
                alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                if (alertSound == null) {
                    alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
            }
            return alertSound;
        }

}
