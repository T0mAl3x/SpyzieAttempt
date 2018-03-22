package src.silent;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import src.silent.utils.ServerCommunicationHandler;

public class LogInActivity extends AppCompatActivity {

    //List of needed permissions
    private static final String[] NEEDED_PERMS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int NEEDED_PERMS_REQUEST = 1;
    //**********************************************************

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        //Requesting permissions at runtime
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(NEEDED_PERMS, NEEDED_PERMS_REQUEST);
        }
        //**********************************************************

        //Setting up log in event
        Button button = findViewById(R.id.buttonLogIn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Gathering username and password from edittexts
                EditText user = findViewById(R.id.editTextUsername);
                String username = user.getText().toString();
                EditText pass = findViewById(R.id.editTextPassword);
                String password = pass.getText().toString();
                //**********************************************************

                //Validating credentials and handle event
                if (validateCredentials(username, password)) {
                    registerPhone(username);
                }
                //**********************************************************
            }
        });
        //**********************************************************
    }

    private boolean validateCredentials(String username, String password) {
        return true;
    }

    private void registerPhone(String username) {
        String[] phoneInformation = {"", "", ""};
        try {
            TelephonyManager telephonyManager = (TelephonyManager) this.
                    getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager != null && ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= 26) {
                    phoneInformation[0] = telephonyManager.getImei();
                } else {
                    phoneInformation[0] = telephonyManager.getDeviceId();
                }
                phoneInformation[1] = Build.MANUFACTURER;
                phoneInformation[2] = Build.MODEL;

                ServerCommunicationHandler.executeRegisterPost(this,
                        "http://192.168.1.24:58938/api/Service/RegisterPhone",
                        phoneInformation, username);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }

        } catch (Exception ex) {

        }
    }
}
