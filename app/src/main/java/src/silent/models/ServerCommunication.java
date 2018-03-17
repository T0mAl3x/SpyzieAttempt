package src.silent.models;

import java.net.URL;

/**
 * Created by all3x on 2/22/2018.
 */

public class ServerCommunication {
    private static ServerCommunication myInstance = null;

    private URL url = null;

    private ServerCommunication() {
        try {
            url = new URL("https://192.168.1.24:44381/api/gatherer");
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
