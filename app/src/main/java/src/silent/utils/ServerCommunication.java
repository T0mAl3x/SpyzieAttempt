package src.silent.utils;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by all3x on 2/22/2018.
 */

public class ServerCommunication {
    public static void executeRegisterPost(final Context context, final String urlString,
                                           final String[] payload, final String username) {
        Thread networkTask = new Thread() {
            public void run() {
                HttpURLConnection connection = null;
                try {
                    URL url = new URL(urlString);
                    connection = (HttpURLConnection) url.openConnection();
                    //connection.setReadTimeout(10000);
                    //connection.setConnectTimeout(15000);
                    connection.setRequestMethod("POST");
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("Username", Base64.encodeToString(username.getBytes(),
                            Base64.URL_SAFE));
                    jsonObject.put("IMEI", Base64.encodeToString(payload[0].getBytes(),
                            Base64.URL_SAFE));
                    jsonObject.put("Manufacturer", Base64.encodeToString(payload[1].getBytes(),
                            Base64.URL_SAFE));
                    jsonObject.put("Model", Base64.encodeToString(payload[2].getBytes(),
                            Base64.URL_SAFE));

                    DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
                    outputStream.writeBytes(jsonObject.toString());
                    outputStream.flush();
                    outputStream.close();

                    BufferedReader input = new BufferedReader(new InputStreamReader(connection.
                            getInputStream()));
                    String response = "";
                    for (String line; (line = input.readLine()) != null; response += line) ;
                    response = response.replace("\"", "");
                    response = new String(Base64.decode(response, Base64.URL_SAFE), "UTF-8");

                    if (!FileHelper.fileExist(context, "SecurityToken.enc")) {
                        FileHelper.createFile(context, "SecurityToken.enc");
                        FileHelper.writeFile(context, "SecurityToken.enc", response);
                    } else {
                        FileHelper.writeFile(context, "SecurityToken.enc", response);
                    }
                } catch (Exception ex) {
                    Log.d("MARK", ex.getMessage());
                } finally {
                    connection.disconnect();
                }
            }
        };

        networkTask.start();
    }

    public static void ExecutePost(Context context, String urlString, String nonBase64Payload,
                                   String action) {
        HttpURLConnection connection = null;
        try {
            //Setting up connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "Text");
            //**********************************************************

            //Write bytes in body
            String body = Base64.encodeToString(nonBase64Payload.getBytes(), Base64.URL_SAFE);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.write(body.getBytes());
            output.flush();
            output.close();
            //**********************************************************

            //Get response
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String base64Response = new String();
            for (String line; (line = br.readLine()) != null; base64Response += line) ;
            String response = new String(Base64.decode(base64Response, Base64.URL_SAFE),
                    "UTF-8");
            //**********************************************************

            //Handle response
            if (action.equals("registration")) {

                if (!FileHelper.fileExist(context, "SecurityToken.txt")) {
                    FileHelper.createFile(context, "SecurityToken.txt");
                    FileHelper.writeFile(context, "SecurityToken.txt", response);
                } else {
                    FileHelper.writeFile(context, "SecurityToken.txt", response);
                }

            }
            //**********************************************************
        } catch (Exception ex) {

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

    }
}
