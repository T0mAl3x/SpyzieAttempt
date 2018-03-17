package src.silent;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Build;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import src.silent.jobs.JobMaster;

public class MainActivity extends AppCompatActivity {
    private static final int jobMaster = 0;
    private JobScheduler jobScheduler;

    private static final String[] NEEDED_PERMS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int NEEDED_PERMS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(NEEDED_PERMS, NEEDED_PERMS_REQUEST);
        }
        jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);

    }

    public void scheduleMasterJob(View view) {
        JobInfo.Builder builder = new JobInfo.Builder(jobMaster, new ComponentName(this,
                JobMaster.class));
        builder.setPersisted(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(5000);
        } else {
            builder.setPeriodic(5000);
        }

        builder.setRequiresDeviceIdle(false);
        builder.setRequiresCharging(false);

        PersistableBundle bundle = new PersistableBundle();
        builder.setExtras(bundle);

        builder.setBackoffCriteria(1000, JobInfo.BACKOFF_POLICY_LINEAR);

        jobScheduler.schedule(builder.build());
    }

    public void clearMasterJob(View view) {
        jobScheduler.cancel(jobMaster);
    }
}
