package src.silent.utils;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import src.silent.utils.models.DBConstants;

public class LocalDBHandler extends SQLiteOpenHelper {
    public LocalDBHandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + DBConstants.TABELA_LAT_LONG + "(" + DBConstants.ID_COLUMN +
                " integer primary key autoincrement, " + DBConstants.LAT_COLUMN + " text not NULL," +
                DBConstants.LONG_COLUMN + " text not NULL, " + DBConstants.DATE_COLUMN + " text not NULL);");

        db.execSQL("create table " + DBConstants.TABELA_CODES + "(" + DBConstants.ID_COLUMN +
                " integer primary key autoincrement, " + DBConstants.CID_COLUMN + " text not NULL," +
                DBConstants.LAC_COLUMN + " text not NULL, " + DBConstants.MCC_COLUMN + " text not NULL," +
                DBConstants.MNC_COLUMN + " text not NULL," + DBConstants.DATE_COLUMN + " text not NULL);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + DBConstants.TABELA_LAT_LONG);
        db.execSQL("drop table if exists " + DBConstants.TABELA_CODES);
        onCreate(db);
    }
}
