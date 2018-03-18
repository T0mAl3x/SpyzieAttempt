package src.silent.utils;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by all3x on 3/18/2018.
 */

public class FileHelper {
    public static void createFile(Context context, String filename) {
        try {
            FileOutputStream outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write("".getBytes());
            outputStream.close();
        } catch (Exception ex) {

        }
    }

    public static Boolean fileExist(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            return file.exists();

        } catch (Exception ex) {
            return false;
        }
    }

    public static String readFile(Context context, String fileName) {
        try {
            File file = new File(context.getFilesDir(), fileName);
            FileInputStream fileInputStream = context.openFileInput(fileName);
            byte[] data = new byte[(int) file.length()];
            fileInputStream.read(data);
            fileInputStream.close();
            return new String(data, "UTF-8");
        } catch (Exception ex) {
            return "";
        }
    }

    public static void writeFile(Context context, String fileName, String data) {
        try {
            FileOutputStream fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(data.getBytes());
            fileOutputStream.close();
        } catch (Exception ex) {

        }
    }
}
