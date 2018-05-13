package src.silent.utils.network.tasks;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import src.silent.utils.models.TrustAllCerts;
import src.silent.utils.models.UserAuthentificationModel;

public class UserAuthentication extends AsyncTask<UserAuthentificationModel, Void, Boolean> {

    @Override
    protected Boolean doInBackground(UserAuthentificationModel... credentials) {
        HttpsURLConnection connection = null;
        boolean resp;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TrustAllCerts.getSSLFactory(), new java.security.SecureRandom());


            URL url = new URL(credentials[0].urlString);
            connection = (HttpsURLConnection) url.openConnection();
            //connection.setReadTimeout(10000);
            //connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setSSLSocketFactory(sc.getSocketFactory());
            connection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Username", Base64.encodeToString(credentials[0].username.getBytes(),
                    Base64.URL_SAFE));
            jsonObject.put("Password", Base64.encodeToString(credentials[0].password.getBytes(),
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

            if (response.equals("success")) {
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
