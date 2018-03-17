package src.silent.jobs.utils;

/**
 * Created by all3x on 2/25/2018.
 */

public class ApplicationUsage {
    private String appName;
    private long appReceived;
    private long appTransmitted;
    private long total;

    public ApplicationUsage(String appName, long appReceived, long appTransmitted) {
        this.appName = appName;
        this.appReceived = appReceived;
        this.appTransmitted = appTransmitted;
        total = appReceived + appTransmitted;
    }

    public long getAppReceived() {
        return appReceived;
    }

    public long getAppTransmitted() {
        return appTransmitted;
    }

    public long getTotal() {
        return total;
    }
}
