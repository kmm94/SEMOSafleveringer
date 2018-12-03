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
import android.os.Bundle;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nisrulz.sensey.ProximityDetector;
import com.github.nisrulz.sensey.Sensey;
import com.github.nisrulz.sensey.SoundLevelDetector;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, IsDoneWritingToCSV {

    public static TextView textView;
    public static final String ACTIVITY_TAG = "Activity recognition";
    public static final String WRITE_TO_CSV_TAG = "CSV writing";
    private Activity activity = this;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private ActivityRecognitionClient mActivityRecognitionClient;
    private final int WRITE_PERMISSION_CODE = 001;
    private ConstraintLayout constraintLayout;
    private SensorManager mSensorManager;
    private ArrayList<String> dataAcc;
    private ArrayList<String> dataGyro;
    private FusedLocationProviderClient mFusedLocationClient;
    private boolean isWritingToCSV = false;

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

        //Sensor manager for different task.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //View
        textView = findViewById(R.id.textView);
        textView.setText("");
        textView.setMovementMethod(new ScrollingMovementMethod());

        appendTextToTextView("Remember to set all permissions manually!! \n They can be found under settings -> apps -> find the app(SEOMS) -> permissions");

/**
 * Task 1:
 * Check out android sensor API
 *
 * Solution:
 * implement gps, light, sound and Proximity.
 */
        //Get last location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Get GPS Location
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                appendLocationToTextView(location, "\nGPS Pos:");
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(constraintLayout, "You are an idiot(Permission DENIED!)", Snackbar.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            WRITE_PERMISSION_CODE);
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            WRITE_PERMISSION_CODE);
                    return;
                }
                Toast.makeText(activity, "Getting GPS location", Toast.LENGTH_SHORT).show();
                /**
                 * Task 4
                 * Choose a QAS(Quality attribute scenario)
                 * i choose Energy efficiency, which is about optimising a sensor to use less power.
                 *
                 * Task 6
                 * Pick a tactic on energy efficiency.
                 * Implement an architectural prototype that illustrates the tactic.
                 * Hand-in a summary of how the prototype explores the tactic including code snippets from your prototype code.
                 *
                 * Solution to 4 & 6:
                 * Use android build in minDistance on gps compare 0 to 10 with the android profiler to see
                 * if power is estimated to be saved.
                 */
                int minDis = 0;

                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, minDis, locationListener);
            } else {
                locationManager.removeUpdates(locationListener);
            }
        });

        //Last Known Location
        Button buttonOne = findViewById(R.id.getLastLocation_Button);
        buttonOne.setOnClickListener(v -> {
            getLastKnownLocation();
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
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            WRITE_PERMISSION_CODE);
                } else {
                    Toast.makeText(activity, "listening to sound", Toast.LENGTH_SHORT).show();
                    Sensey.getInstance().startSoundLevelDetection(activity, soundLevelListener);
                }
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
            }

            @Override
            public void onFar() {
                appendTextToTextView("Phone is in safe distance from everything!");
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
            Sensor mLight;
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            if (isChecked) {
                if (mLight != null) {
                    mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_UI);
                } else {
                    Toast.makeText(getBaseContext(), "There is no light sensor", Toast.LENGTH_SHORT).show();
                }
            } else {
                mSensorManager.unregisterListener(this, mLight);
            }
        });

        /**
         * Task 5:
         * Implement an app that use one of the commercial activity recognition APIs to collect activity information.
         * Use the app to collect some inferences of your own activities over an hour.
         * Hand-in a plot of the inferences and comments for the accuracy of the inferences.
         *
         * Solution:
         * API: Google activity recognition API
         * Save the Activity in an csv file that is accessible from the phone file system, plot it using R.
         * CSV file contents: timestamp, confidence & activity.
         *
         * limits:
         * Not sure if it can track user with lock phone.
         */
        //Getting google activity info
        Intent intent = new Intent(this, BroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);


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
                            result -> Log.d(ACTIVITY_TAG, "successful attached")
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

        /**
         * Task 7
         * Make an Architectural Prototype addressing the availability pattern you observed in your collected data using one of the resource availability tactics.
         *
         * Solution:
         * Tactic: Increase Resources
         * The plan is to use the accelerometer and the gyroscope to avoid a degraded service.
         * goal: print acc and gyro data to screen. DONE
         * goal: save acc and gyro data to file for processing in R. DONE
         *
         * Limits:
         * Not running in background.
         */
        Switch recordGyroAndAcc = findViewById(R.id.task5_switch);
        recordGyroAndAcc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Sensor mAccSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (isChecked) {
                dataAcc = new ArrayList<>();
                dataGyro = new ArrayList<>();
                mSensorManager.registerListener(this, mAccSensor, SensorManager.SENSOR_DELAY_UI);
                mSensorManager.registerListener(this, mGyroSensor, SensorManager.SENSOR_DELAY_UI);
            } else {
                mSensorManager.unregisterListener(this, mGyroSensor);
                mSensorManager.unregisterListener(this, mAccSensor);
                if (!isWritingToCSV) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        // Permission is not granted
                        Snackbar.make(constraintLayout, "You are an idiot(Permission DENIED!)", Snackbar.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                WRITE_PERMISSION_CODE);
                    } else {
                        isWritingToCSV = true;
                        appendAccCSV(dataAcc, "AccData.csv");
                        appendAccCSV(dataGyro, "GyroData.csv");
                    }

                } else {
                    appendTextToTextView("Writing in progress, can't save data now");
                }
            }
        });


    }

    private void toArrays(String... strings) {
        if (strings[0].equals("Acc")) {
            toArray(strings, dataAcc);
        } else {
            toArray(strings, dataGyro);
        }
    }

    private void toArray(String[] strings, ArrayList<String> dataArray) {
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].startsWith("x") || strings[i].startsWith("y") || strings[i].startsWith("z")) {
                dataArray.add(strings[i]);
                dataArray.add(strings[++i]);
            }
        }
    }

    private void appendAccCSV(ArrayList<String> strings, String fileName) {
        strings.set(0, fileName);
        String[] data = new String[strings.size()];
        strings.toArray(data);
        new WriteToCSVFileTask(this).execute(data);
    }

    /**
     * Prints last known location to screen.
     */
    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Snackbar.make(constraintLayout, "You are an idiot(Permission DENIED!)", Snackbar.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    WRITE_PERMISSION_CODE);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    WRITE_PERMISSION_CODE);
        return;
        }
        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        appendLocationToTextView(location, "\nLast know location:");
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
                appendTextToTextView("Acc x: " + event.values[0] + " y: " + event.values[1] + " z: " + event.values[2]);
                toArrays("Acc", "x:", String.valueOf(event.values[0]), "y: ", String.valueOf(event.values[1]), "z: ", String.valueOf(event.values[2]));
                break;
            case (Sensor.TYPE_LIGHT):
                appendTextToTextView("Light: " + event.values[0] + " Lux");
                break;
            case (Sensor.TYPE_GYROSCOPE):
                appendTextToTextView("Gyro x: " + event.values[0] + " y: " + event.values[1] + " z: " + event.values[2]);
                toArrays("Gyro", "x:", String.valueOf(event.values[0]), "y: ", String.valueOf(event.values[1]), "z: ", String.valueOf(event.values[2]));
                break;
            default:
                appendTextToTextView(event.sensor.getStringType());
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void isDone(Boolean isCompleted, String fileName) {
        if (isCompleted) {
            isWritingToCSV = false;
            appendTextToTextView("Data saved in " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + ", File name: " + fileName);
        } else {
            Snackbar.make(constraintLayout, "An error occurred with file: " + fileName, Snackbar.LENGTH_LONG).show();
        }
    }
}
