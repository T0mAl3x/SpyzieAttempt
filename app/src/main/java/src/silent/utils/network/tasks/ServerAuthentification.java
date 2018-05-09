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

public class ServerAuthentification extends AsyncTask<PhoneRegistrationTaskParams, Void, Boolean> {

    private String serverKey;

    public ServerAuthentification() {
        serverKey = "mkl123piu95FEWCW124mmjjlsp284MI1";
    }

    @Override
    protected Boolean doInBackground(PhoneRegistrationTaskParams... params) {
        if (!FileHandler.fileExist(params[0].context, "SecurityToken.enc")) {
            return false;
        }
        String secToken = FileHandler.readFile(params[0].context, "SecurityToken.enc");

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

            JSONObject credentials = new JSONObject();
            credentials.put("IMEI", Base64.encodeToString(params[0].payload[0].getBytes(), Base64.URL_SAFE));
            credentials.put("SecToken", Base64.encodeToString(secToken.getBytes(), Base64.URL_SAFE));

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(credentials.toString());
            outputStream.flush();
            outputStream.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(connection.
                    getInputStream()));
            String response = "";
            for (String line; (line = input.readLine()) != null; response += line) ;
            response = response.replace("\"", "");
            response = new String(Base64.decode(response, Base64.URL_SAFE), "UTF-8");

            String[] keys = response.split(":");git s

            if (keys[0].equals(serverKey) || keys[1].equals(serverKey)) {
                if (!keys[1].equals("0")) {
                    serverKey = keys[1];
                }
                resp = true;
            } else {
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
