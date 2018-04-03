package src.silent.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

public class BatteryHandler extends BroadcastReceiver {
    int batteryLevel = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryLevel = (int)((level / (float) scale) * 100);
    }

    public float getBatteryLevel() {
        return batteryLevel;
    }
}
