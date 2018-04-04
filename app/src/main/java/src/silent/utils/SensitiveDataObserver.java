package src.silent.utils;

import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SensitiveDataObserver extends FileObserver {
    private static final int MASK = (FileObserver.OPEN | FileObserver.DELETE |
            FileObserver.CREATE);

    public SensitiveDataObserver(String PATH) {
        super(PATH, MASK);
    }

    @Override
    public void onEvent(int event, @Nullable String path) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String logMessage = dateFormat.format(date);
        switch (event) {
            case FileObserver.OPEN:
                logMessage += " -> File " + path + " was openned\n\n";
                try {
                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory().toString()
                            + "/Log/Fisier.txt", true);
                    writer.write(logMessage);
                    writer.close();
                } catch (Exception ex) {
                    Log.i("EXCEPTION", "Error: " + ex.getMessage());
                }
                break;
            case FileObserver.DELETE:
                logMessage += "File " + path + " was deleted\n\n";
                try {
                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory().toString()
                            + "/Log/Fisier.txt", true);
                    writer.write(logMessage);
                    writer.close();
                } catch (Exception ex) {
                    Log.i("EXCEPTION", "Error: " + ex.getMessage());
                }
                break;
            case FileObserver.CREATE:
                logMessage += "File " + path + " was created\n\n";
                try {
                    FileWriter writer = new FileWriter(Environment.getExternalStorageDirectory().toString()
                            + "/Log/Fisier.txt", true);
                    writer.write(logMessage);
                    writer.close();
                } catch (Exception ex) {
                    Log.i("EXCEPTION", "Error: " + ex.getMessage());
                }
                break;
            default:
                break;
        }
    }
}
