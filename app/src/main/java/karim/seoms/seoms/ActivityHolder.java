package karim.seoms.seoms;

import com.google.android.gms.location.DetectedActivity;

public class ActivityHolder {
    private String activityName;
    private DetectedActivity activity;

    public ActivityHolder(String activityName, DetectedActivity activity) {
        this.activityName = activityName;
        this.activity = activity;
    }

    public String getActivityName() {

        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public DetectedActivity getActivity() {
        return activity;
    }

    public void setActivity(DetectedActivity activity) {
        this.activity = activity;
    }
}
