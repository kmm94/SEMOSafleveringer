package karim.seoms.seoms;

import android.app.IntentService;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import de.siegmar.fastcsv.writer.CsvAppender;
import de.siegmar.fastcsv.writer.CsvWriter;

public class BroadcastReceiver extends IntentService {

    public static final String IN_VEHICLE = "In Vehicle";
    public static final String ON_BICYCLE = "On Bicycle";
    public static final String RUNNING = "Running";
    public static final String STILL = "Still";
    public static final String WALKING = "Walking";
    public static final String ON_FOOT = "On FOOT";
    public static final String TILTING = "Tilting";
    public static final String UNKNOWN = "Unknown";
    public static final String EXTRA = ": ";
    private final String WRITE_TAG = "CSVWriter";
    public static TreeMap<String, ActivityHolder> data = new TreeMap<String, ActivityHolder>();

    public BroadcastReceiver() {
        super("ACTIVITY");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public BroadcastReceiver(String name) {
        super(name);
    }


    private void handleDetectedActivities(DetectedActivity activity) {
        switch (activity.getType()) {
            case DetectedActivity.IN_VEHICLE: {
                addToData(new ActivityHolder(IN_VEHICLE, activity));
                Log.e(MainActivity.ACTIVITY_TAG, IN_VEHICLE + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.ON_BICYCLE: {
                addToData(new ActivityHolder(ON_BICYCLE, activity));
                Log.e(MainActivity.ACTIVITY_TAG, ON_BICYCLE + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.RUNNING: {
                addToData(new ActivityHolder(RUNNING, activity));
                Log.e(MainActivity.ACTIVITY_TAG, RUNNING + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.STILL: {
                addToData(new ActivityHolder(STILL, activity));
                Log.e(MainActivity.ACTIVITY_TAG, STILL + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.WALKING: {
                addToData(new ActivityHolder(WALKING, activity));
                Log.e(MainActivity.ACTIVITY_TAG, WALKING + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.ON_FOOT: {
                addToData(new ActivityHolder(ON_FOOT, activity));
                Log.e(MainActivity.ACTIVITY_TAG, ON_FOOT + EXTRA + activity.getConfidence());
                break;
            }
            case DetectedActivity.TILTING: {
                addToData(new ActivityHolder(TILTING, activity));
                Log.e(MainActivity.ACTIVITY_TAG, TILTING + EXTRA + activity.getConfidence());
                break;
            }
            default: {
                addToData(new ActivityHolder(UNKNOWN, activity));
                Log.e(MainActivity.ACTIVITY_TAG, UNKNOWN + EXTRA + activity.getConfidence());
            }
        }

    }

    private void addToData(ActivityHolder activity) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        data.put(simpleDateFormat.format(new Date()), activity);
        toUI(simpleDateFormat.format(new Date()), activity);
        writeToCSV(data);
    }

    private void toUI(String format, ActivityHolder activity) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            public void run() {
                MainActivity.textView.append("Timestamp: " + format + "\n Status: " + activity.getActivityName() + ". Confidence: " + activity.getActivity().getConfidence() + "\n");
            }
        });
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            handleDetectedActivities(result.getMostProbableActivity());
        }
    }

    private void writeToCSV(TreeMap<String, ActivityHolder> data) {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DataOPG5.csv");
            CsvWriter csvWriter = new CsvWriter();
            csvWriter.setFieldSeparator(';');
            try (CsvAppender csvAppender = csvWriter.append(file, StandardCharsets.UTF_8)) {

                data.forEach((date, activityHolder) -> {
                    try {
                        csvAppender.appendLine(date, activityHolder.getActivityName(), String.valueOf(activityHolder.getActivity().getConfidence()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            } catch (IOException e) {
                Log.e(WRITE_TAG, e.toString());
            }


        }


    }
}
