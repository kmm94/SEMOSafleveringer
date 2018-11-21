package karim.seoms.seoms;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nisrulz.sensey.ProximityDetector;
import com.github.nisrulz.sensey.Sensey;
import com.github.nisrulz.sensey.SoundLevelDetector;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityInfo {

    private FusedLocationProviderClient mFusedLocationClient;
    public static TextView textView;
    private Activity activity = this;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private karim.seoms.seoms.BroadcastReceiver broadcastReceiver;
    public static final String ACTIVITY_TAG = "Activity recognition";
    private ActivityRecognitionClient mActivityRecognitionClient;
    private final int WRITE_PERMISSION_CODE = 001;
    private ConstraintLayout constraintLayout;

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        super.onPause();
        //This is for removing different listeners.
        //sound
        Sensey.getInstance().stopSoundLevelDetection();
        //GPS
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = findViewById(R.id.constrainLayout);
        //ActivtyReconition
//        broadcastReceiver = new karim.seoms.seoms.BroadcastReceiver(this);


        //View
        textView = findViewById(R.id.textView);
        textView.setText("");
        textView.setMovementMethod(new ScrollingMovementMethod());


        //Get last location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Get GPS Location

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(activity, "YOU SHALL NOT PASS", Toast.LENGTH_LONG).show();
            return;
        }
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                appendLocationToTextView(location, "\nGPS Pos:");
                sendLocationToFirebase(location, "GPS");
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
        };

        Switch gpsSwitch = findViewById(R.id.gps_switch);
        gpsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(activity, "Getting GPS location", Toast.LENGTH_SHORT).show();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 10, locationListener);
            } else {
                locationManager.removeUpdates(locationListener);
            }
        });

        //Last Known Location
        Button buttonOne = findViewById(R.id.getLastLocation_Button);
        buttonOne.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Log.d("location", "button_click");
                getLastKnownLocation();
            }
        });

        //Sound
        SoundLevelDetector.SoundLevelListener soundLevelListener = level -> {
            //Sound Level in dB is detected, do something
            //textView.append("Sound level: " + level); Too fast to append to text view!!
            TextView soundText = findViewById(R.id.soundTextView);
            soundText.setText("Sound level: " + level);
        };

        Switch soundSwitch = findViewById(R.id.sound_switch);
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(activity, "listning to sound", Toast.LENGTH_SHORT).show();
                Sensey.getInstance().startSoundLevelDetection(activity, soundLevelListener);
            } else {
                Sensey.getInstance().stopSoundLevelDetection();
            }
        });

        //Proximity
        ProximityDetector.ProximityListener proximityListener = new ProximityDetector.ProximityListener() {
            private final String proximitySensorKey = "ProximitySensor";

            @Override
            public void onNear() {
                appendTextToTextView("Phone is near something");
                sendTextToFireBase(proximitySensorKey, "something is near the phone");
            }

            @Override
            public void onFar() {
                appendTextToTextView("Phone is in safe distance from everything!");
                sendTextToFireBase(proximitySensorKey, "Nothing is close to the phone");
            }
        };

        Switch proximitySwitch = findViewById(R.id.proximity_switch);
        proximitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(activity, "Proximity sensor is reporting", Toast.LENGTH_SHORT).show();
                Sensey.getInstance().startProximityDetection(proximityListener);
            } else {
                Sensey.getInstance().stopProximityDetection(proximityListener);
            }
        });

        //Light
        Switch lightSwitch = findViewById(R.id.light_switch);
        lightSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SensorManager mSensorManager;
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            Sensor mLight;
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

            if (isChecked) {
                if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
                    mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
                } else {
                    Toast.makeText(getBaseContext(), "There is no light sensor", Toast.LENGTH_SHORT).show();
                }
            } else {
                mSensorManager.unregisterListener(this, mLight);
            }
        });

        /**
         * Opgave 5:
         * sammenling fx  accelerometer data med med et activity reconigtion api
         */
        //Getting google activity info
        List<ActivityTransition> transitions = new ArrayList<>();
        Intent intent = new Intent(this, BroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


        //Getting adding accelerometer listener
        Switch recordActivitySwitch = findViewById(R.id.activity_switch);
        recordActivitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                Snackbar.make(constraintLayout, "You are an idiot(Permission DENIED!)", Snackbar.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_PERMISSION_CODE);

            } else {
                mActivityRecognitionClient = new ActivityRecognitionClient(this);
                if (isChecked) {
                    //activity recognition:
                    Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(1000, pendingIntent);

                    task.addOnSuccessListener(
                            result -> {
                                Log.d(ACTIVITY_TAG, "successful attached");
                            }
                    );

                    task.addOnFailureListener(
                            e -> {
                                // Handle error
                                Log.e(ACTIVITY_TAG, "attaching Failed error: " + e.getMessage());
                            }
                    );
                } else {
                    //Activity recognition
                    Task<Void> task =
                            mActivityRecognitionClient.removeActivityTransitionUpdates(pendingIntent);

                    task.addOnSuccessListener(
                            result -> {
                                pendingIntent.cancel();
                                Log.d(ACTIVITY_TAG, "successful Detached");
                            });

                    task.addOnFailureListener(
                            e -> Log.e(ACTIVITY_TAG, e.getMessage()));
                }
            }
        });
    }

    private void sendTextToFireBase(String Key, String dark) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(Key);
        myRef.setValue(dark);
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("location", "no permission");
            return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        appendLocationToTextView(location, "\nLast know location:");
                        sendLocationToFirebase(location, "LastKnown");
                    } else {
                        Toast.makeText(activity, "location data is null", Toast.LENGTH_LONG).show();
                        Log.d("location", "null");
                    }
                });
    }

    private void appendLocationToTextView(Location location) {
        Log.d("location", "appending location data");
        textView.append("\nLatitude: " + location.getLatitude() + "\nLongitude: " + location.getLongitude() + "\naccuracy: " + location.getAccuracy());
    }

    private void appendLocationToTextView(Location location, String addString) {
        textView.append("\n" + addString);
        appendLocationToTextView(location);
    }

    public void appendTextToTextView(String text) {
        textView.append("\n\n" + text);
    }

    private void sendLocationToFirebase(Location location, String type) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(type);
        myRef.setValue("Latitude: " + location.getLatitude() + "Longitude: " + location.getLongitude() + "Accuracy: " + location.getAccuracy());
    }

    /**
     * Called when there is a new sensor event.  Note that "on changed"
     * is somewhat of a misnomer, as this will also be called if we have a
     * new reading from a sensor with the exact same sensor values (but a
     * newer timestamp).
     * <p>
     * <p>See {@link SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link SensorEvent SensorEvent}.
     * <p>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                appendTextToTextView("x: " + event.values[0] + " y: " + event.values[1] + " z: " + event.values[2]);
                break;
            case (Sensor.TYPE_LIGHT):
                appendTextToTextView("Light: " + event.values[0] + " Lux");
                break;
            default:
                appendTextToTextView(event.sensor.getStringType());
        }


    }

    /**
     * Called when the accuracy of the registered sensor has changed.  Unlike
     * onSensorChanged(), this is only called when this accuracy value changes.
     * <p>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void Activity(ActivityTransitionEvent e) {
        appendTextToTextView(e.toString());
    }
}
