package src.silent.jobs;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.PersistableBundle;

import src.silent.jobs.tasks.TaskMaster;
import src.silent.utils.models.MasterTaskParams;

/**
 * Created by all3x on 2/22/2018.
 */

public class JobMaster extends JobService {
    private TaskMaster jobExecutioner;

    @SuppressLint("StaticFieldLeak")
    @Override
    public boolean onStartJob(final JobParameters jobParameters) {
        PersistableBundle bundle = jobParameters.getExtras();
        MasterTaskParams params = new MasterTaskParams();
        params.IMEI = bundle.getString("IMEI");
        params.context = getApplicationContext();

        jobExecutioner = new TaskMaster() {
            @Override
            protected void onPostExecute(Void aVoid) {
                jobFinished(jobParameters, true);
            }
        };
        jobExecutioner.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        jobExecutioner.cancel(true);
        return false;
    }
}
