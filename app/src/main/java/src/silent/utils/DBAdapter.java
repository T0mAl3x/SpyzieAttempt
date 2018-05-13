package src.silent.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import src.silent.utils.models.DBConstants;

public class DBAdapter {
    private SQLiteDatabase db;
    private SQLiteOpenHelper helper;

    public DBAdapter(Context context, String databaseName) {
        helper = new LocalDBHandler(context, databaseName, null, 1);
    }

    public void startConnection() {
        db = helper.getWritableDatabase();
    }

    public void closeConnection() {
        db.close();
        helper.close();
    }

    public void insertLatLong(String latitude, String longitude, String date) {
        ContentValues cv = new ContentValues();
        cv.put(DBConstants.LAT_COLUMN, latitude);
        cv.put(DBConstants.LONG_COLUMN, longitude);
        cv.put(DBConstants.DATE_COLUMN, date);

        db.insert(DBConstants.TABELA_LAT_LONG, null, cv);
    }

    public void insertCodes(String cid, String lac, String date, String mnc, String mcc) {
        ContentValues cv = new ContentValues();
        cv.put(DBConstants.CID_COLUMN, cid);
        cv.put(DBConstants.LAC_COLUMN, lac);
        cv.put(DBConstants.MNC_COLUMN, mnc);
        cv.put(DBConstants.MCC_COLUMN, mcc);
        cv.put(DBConstants.DATE_COLUMN, date);

        db.insert(DBConstants.TABELA_CODES, null, cv);
    }

    public List<String[]> selectAllLatLong() {
        List<String[]> latLongList = new ArrayList<>();
        Cursor cursor = db.query(DBConstants.TABELA_LAT_LONG, new String[]{DBConstants.LAT_COLUMN,
                        DBConstants.LONG_COLUMN, DBConstants.DATE_COLUMN}, null, null,
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String[] latLong = {cursor.getString(0), cursor.getString(1),
                cursor.getString(2)};
                latLongList.add(latLong);
            } while (cursor.moveToNext());
        }

        db.execSQL("delete from  " + DBConstants.TABELA_LAT_LONG);
        return latLongList;
    }

    public List<String[]> selectAllCodes() {
        List<String[]> codesList = new ArrayList<>();
        Cursor cursor = db.query(DBConstants.TABELA_CODES, new String[]{DBConstants.CID_COLUMN,
                        DBConstants.LAC_COLUMN, DBConstants.MNC_COLUMN, DBConstants.MCC_COLUMN,
                        DBConstants.DATE_COLUMN}, null, null,
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String[] codes = {cursor.getString(0), cursor.getString(1),
                cursor.getString(2), cursor.getString(3), cursor.getString(4)};
                codesList.add(codes);
            } while (cursor.moveToNext());
        }

        db.execSQL("delete from  " + DBConstants.TABELA_CODES);
        return codesList;
    }
}
