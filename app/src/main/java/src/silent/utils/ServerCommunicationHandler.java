package src.silent.utils;

import android.content.Context;
import android.util.Base64;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import src.silent.utils.models.PhoneRegistrationTaskParams;
import src.silent.utils.models.TrustAllCerts;
import src.silent.utils.models.UserAuthentificationModel;
import src.silent.utils.network.tasks.PhoneRegistration;
import src.silent.utils.network.tasks.UserAuthentication;

/**
 * Created by all3x on 2/22/2018.
 */

public class ServerCommunicationHandler {
    public static boolean executeUserAuthentification(String urlString, String username,
                                                      String password) {
        UserAuthentificationModel credentials = new UserAuthentificationModel();

        credentials.urlString = urlString;
        credentials.username = username;
        credentials.password = password;

        boolean response;
        try {
            response = new UserAuthentication().execute(credentials).get();
        } catch (Exception ex) {
            response = false;
        }
        return response;
    }

    public static boolean executeRegisterPost(Context context, String urlString,
                                              String[] payload, String username) {
        PhoneRegistrationTaskParams params = new PhoneRegistrationTaskParams();
        params.context = context;
        params.urlString = urlString;
        params.payload = payload;
        params.username = username;

        boolean response;
        try {
            response = new PhoneRegistration().execute(params).get();
        } catch (Exception ex) {
            response = false;
        }
        return response;
    }

    public static void executeDataPost(Context context, String urlString, JSONObject bulkData,
                                       String IMEI) {
        if (!FileHandler.fileExist(context, "SecurityToken.enc")) {
            return;
        }
        String secToken = FileHandler.readFile(context, "SecurityToken.enc");

        HttpsURLConnection connection = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TrustAllCerts.getSSLFactory(), new java.security.SecureRandom());

            URL url = new URL(urlString);
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

            JSONObject credentials = new JSONObject();
            credentials.put("IMEI", Base64.encodeToString(IMEI.getBytes(), Base64.URL_SAFE));
            credentials.put("SecToken", Base64.encodeToString(secToken.getBytes(), Base64.URL_SAFE));
            bulkData.put("Authentication", credentials);

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(bulkData.toString());
            outputStream.flush();
            outputStream.close();

            int code = connection.getResponseCode();
        } catch (Exception ex) {

        } finally {
            connection.disconnect();
        }
    }

    public static String getMask(Context context, String urlString, String IMEI) {
        if (!FileHandler.fileExist(context, "SecurityToken.enc")) {
            return null;
        }
        String secToken = FileHandler.readFile(context, "SecurityToken.enc");

        String response = "";
        HttpsURLConnection connection = null;
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, TrustAllCerts.getSSLFactory(), new java.security.SecureRandom());

            URL url = new URL(urlString);
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

            JSONObject credentials = new JSONObject();
            credentials.put("IMEI", Base64.encodeToString(IMEI.getBytes(), Base64.URL_SAFE));
            credentials.put("SecToken", Base64.encodeToString(secToken.getBytes(), Base64.URL_SAFE));

            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.writeBytes(credentials.toString());
            outputStream.flush();
            outputStream.close();

            BufferedReader input = new BufferedReader(new InputStreamReader(connection.
                    getInputStream()));
            for (String line; (line = input.readLine()) != null; response += line) ;
            response = response.replace("\"", "");
            response = new String(Base64.decode(response, Base64.URL_SAFE), "UTF-8");
        } catch (Exception ex) {

        } finally {
            connection.disconnect();
        }

        return response;
    }
}
