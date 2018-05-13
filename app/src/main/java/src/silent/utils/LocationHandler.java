package src.silent.utils;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;


/**
 * Created by all3x on 3/20/2018.
 */

public class LocationHandler implements LocationListener {
    private String latitude;
    private String longitude;

    @Override
    public void onLocationChanged(Location location) {
        latitude = String.valueOf(location.getLatitude());
        longitude = String.valueOf(location.getLongitude());

        Looper.myLooper().quitSafely();
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

    public String getLatitude() {
        return latitude;
    }

    public String getLongitude() {
        return longitude;
    }
}