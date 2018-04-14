package src.silent;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import src.silent.jobs.JobMaster;

public class MainActivity extends AppCompatActivity {

    private static final int jobMaster = 0;
    private JobScheduler jobScheduler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
