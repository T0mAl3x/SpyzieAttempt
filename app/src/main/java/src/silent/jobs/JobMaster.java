package src.silent.jobs;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;

import src.silent.jobs.tasks.TaskMaster;

/**
 * Created by all3x on 2/22/2018.
 */

public class JobMaster extends JobService {
    private TaskMaster jobExecutioner;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        jobExecutioner = new TaskMaster() {
            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(jobParameters, true);
            }
        };
        jobExecutioner.execute(getApplicationContext());
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobExecutioner.cancel(true);
        return false;
    }
}
