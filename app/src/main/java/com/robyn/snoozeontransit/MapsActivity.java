package com.robyn.snoozeontransit;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

public class MapsActivity extends FragmentActivity implements LocationListener, GoogleMap.OnMapClickListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    public LatLng destination;
    public LatLng current;
    Location myLocation;
    public static final String TAG = MapsActivity.class.getSimpleName();
    public float distance;

    Button setAlarmButton;
    Button stopAlarmButton;
    Button cancelAlarmButton;
    TextView resultText;

    boolean isAlarmSet;
    boolean isAlarmPlaying;

    private MediaPlayer player;
    final Context context = this;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        isAlarmSet = false;

        setAlarmButton = (Button) findViewById(R.id.set_alarm_button);

        setAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAlarm();
            }
        });

        stopAlarmButton = (Button) findViewById(R.id.stop_alarm_button);

        stopAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopAlarm();
            }
        });


        cancelAlarmButton = (Button) findViewById(R.id.cancel_alarm_button);

        cancelAlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAlarm();
            }
        });


        resultText = (TextView) findViewById(R.id.result_text);

        setUpMapIfNeeded();

        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {

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
        locationManager.requestLocationUpdates(provider, 5000, 0, this);

//        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
//            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, this);
//        } else {
//            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0,
//                    this);
//        }

        // set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Get latitude of the current location
        double latitude = myLocation.getLatitude();

        // Get longitude of the current location
        double longitude = myLocation.getLongitude();

        // Create a LatLng object for the current location
        current = new LatLng(latitude, longitude);

        // Show the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLng(current));

        // Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

        mMap.setOnMapClickListener(this);

    }


    @Override
    public void onLocationChanged(Location location) {

        current = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());

        if (destination != null && myLocation != null) {
            calculateDistance();
        }

        if (isCloseEnough(distance) && isAlarmSet) {
            playAlarm(this, getAlarmSound());
        }
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

    @Override
    public void onMapClick(LatLng latLng) {
        if (!isAlarmSet) {                              // a new destination can't be created if there's already an alarm set
            mMap.clear();
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Destination")
                    .snippet("You're going here"));
            destination = latLng;

            if (destination != null && current != null) {  // if there is a destination and a current
                calculateDistance();
            }
        }
    }

    public float calculateDistance() {

        double destLat = destination.latitude;
        double destLong = destination.longitude;

//        Location currLocation = new Location("currLocation");
//        currLocation.setLatitude(currLat);
//        currLocation.setLongitude(currLong);

        Location destLocation = new Location("destLocation");
        destLocation.setLatitude(destLat);
        destLocation.setLongitude(destLong);

        distance = myLocation.distanceTo(destLocation) / 1000; // in km
        double myLat = myLocation.getLatitude();

  //      Log.d(TAG, "myLocation is" + myLat +"km.");
        resultText.setText("You are " + Float.toString(distance) + "away from your destination. And myLat = " + String.valueOf(myLat) );



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
        }
    }

    public void cancelAlarm() {
        isAlarmSet = false;
    }

    public void playAlarm(Context context, Uri alert) {
        //STUB
        if (isAlarmPlaying) {
            return;
        }
        isAlarmPlaying = true;
        player = new MediaPlayer();
        try {
            player.setDataSource(context, alert);
            final AudioManager audio = (AudioManager) context
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
