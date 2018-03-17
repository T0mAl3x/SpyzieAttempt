package src.silent.jobs.utils;

import android.app.job.JobScheduler;
import android.content.Context;

/**
 * Created by all3x on 2/23/2018.
 */

public class UtilMasterTaskParams {
    private JobScheduler jobScheduler;
    private Context contextParam;

    public UtilMasterTaskParams(JobScheduler jobScheduler, Context context) {
        this.jobScheduler = jobScheduler;
        this.contextParam = context;
    }

    public JobScheduler getJobScheduler() {
        return jobScheduler;
    }

    public Context getContextParam() {
        return contextParam;
    }
}
