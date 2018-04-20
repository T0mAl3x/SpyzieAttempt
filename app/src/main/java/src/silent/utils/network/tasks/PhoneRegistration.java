package src.silent.utils.network.tasks;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import src.silent.utils.FileHandler;
import src.silent.utils.models.PhoneRegistrationTaskParams;

public class PhoneRegistration extends AsyncTask<PhoneRegistrationTaskParams, Void, Boolean> {
    @Override
    protected Boolean doInBackground(PhoneRegistrationTaskParams... params) {
        HttpURLConnection connection = null;
        boolean resp = false;
        try {
            URL url = new URL(params[0].urlString);
            connection = (HttpURLConnection) url.openConnection();
            //connection.setReadTimeout(10000);
            //connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Username", Base64.encodeToString(params[0].username.getBytes(),
                    Base64.URL_SAFE));
            jsonObject.put("IMEI", Base64.encodeToString(params[0].payload[0].getBytes(),
                    Base64.URL_SAFE));
            jsonObject.put("Manufacturer", Base64.encodeToString(params[0].payload[1].getBytes(),
                    Base64.URL_SAFE));
            jsonObject.put("Model", Base64.encodeToString(params[0].payload[2].getBytes(),
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

            if (!response.equals("Already registered") && !response.equals("fail")) {
                if (!FileHandler.fileExist(params[0].context, "SecurityToken.enc")) {
                    FileHandler.createFile(params[0].context, "SecurityToken.enc");
                    FileHandler.writeFile(params[0].context, "SecurityToken.enc", response);
                } else {
                    FileHandler.writeFile(params[0].context, "SecurityToken.enc", response);
                }
                resp = true;
            }

            if (response.equals("Already registered")) {
                resp = true;
            }

            if (response.equals("fail")) {
                resp = false;
            }

        } catch (Exception ex) {
            Log.d("MARK", ex.getMessage());
            resp = false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return resp;
    }
}
