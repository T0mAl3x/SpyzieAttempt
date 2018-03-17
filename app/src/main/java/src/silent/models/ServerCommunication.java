package src.silent.models;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by all3x on 2/22/2018.
 */

public class ServerCommunication {
    private static ServerCommunication myInstance = null;

    private HttpURLConnection connection = null;

    private ServerCommunication() {
        try {
            URL url = new URL("http://192.168.1.24:58938/api/");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

        } catch (Exception ex) {

        }

    }

    public static ServerCommunication getInstance() {
        if(myInstance == null) {
            myInstance = new ServerCommunication();
        }

        return myInstance;
    }


}
