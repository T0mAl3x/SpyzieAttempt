package src.silent;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;

import src.silent.utils.PolicyManager;
import src.silent.utils.SampleDeviceAdminReceiver;
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
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE
    };
    private static final int NEEDED_PERMS_REQUEST = 1;
    //**********************************************************

    private PolicyManager policyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_in);

        //Handling device admin
        policyManager = new PolicyManager(this);
        if (!policyManager.isAdminActive()) {
            Intent activateDeviceAdmin = new Intent(
                    DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(
                    DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    policyManager.getAdminComponent());
            activateDeviceAdmin
                    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                            "After activating admin, you will be able to block application uninstallation.");
            startActivityForResult(activateDeviceAdmin,
                    PolicyManager.DPM_ACTIVATION_REQUEST_CODE);
        }
        //***********************************************************

        Button buttonSettingsAdmin = findViewById(R.id.button4);
        buttonSettingsAdmin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent().setComponent(new ComponentName("com.android.settings",
                        "com.android.settings.DeviceAdminSettings")));
            }
        });

        Button buttonSettings = findViewById(R.id.button3);
        buttonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(myIntent);
            }
        });

        //Requesting permissions at runtime
        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(NEEDED_PERMS, NEEDED_PERMS_REQUEST);
        }
        //**********************************************************

        SharedPreferences sharedPreferences = getSharedPreferences("Credentials", MODE_PRIVATE);
        EditText usernameButton = findViewById(R.id.editTextUsername);
        usernameButton.setText(sharedPreferences.getString("Username", ""));
        EditText passwordButton = findViewById(R.id.editTextPassword);
        passwordButton.setText(sharedPreferences.getString("Password", ""));

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/.Temp/");
        file.mkdirs();

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
                if (ServerCommunicationHandler.executeUserAuthentification(
                        "https://192.168.1.24:443/api/Service/AuthentificateUserFromPhone",
                        username, password)) {
                    SharedPreferences sharedPreferences =
                            getSharedPreferences("Credentials", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("Username", username);
                    editor.putString("Password", password);
                    editor.apply();

                    registerPhone(username);
                    Toast.makeText(getApplicationContext(), "Authentication succeded!",
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Authentication failed!",
                            Toast.LENGTH_LONG).show();
                }
                //**********************************************************
            }
        });
        //**********************************************************
    }

    private void registerPhone(String username) {
        String[] phoneInformation = {"", "", "", ""};
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
                phoneInformation[3] = telephonyManager.getLine1Number();
                if (phoneInformation[3] == null || phoneInformation[3].equals("")) {
                    phoneInformation[3] = "Can't retrieve number";
                }

                if (ServerCommunicationHandler.executeRegisterPost(this,
                        "https://192.168.1.24:443/api/Service/RegisterPhone",
                        phoneInformation, username)) {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.putExtra("IMEI", phoneInformation[0]);
                    startActivity(intent);
                } else {
                    Toast.makeText(getApplicationContext(), "Registration failed!",
                            Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception ex) {
            Toast.makeText(getApplicationContext(), "Registration failed!",
                    Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            boolean ok = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                    break;
                }
            }
            if (!ok) {
                finish();
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGps();
        checkAdmin();
    }

    private void checkGps() {
        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        Button button = findViewById(R.id.button3);
        if (!gps_enabled && !network_enabled) {
            button.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.INVISIBLE);
        }
    }

    private void checkAdmin() {
        DevicePolicyManager mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mAdminName = new ComponentName(this, SampleDeviceAdminReceiver.class);

        Button buttonSettingsAdmin = findViewById(R.id.button4);
        if (mDPM != null &&mDPM.isAdminActive(mAdminName)) {
            buttonSettingsAdmin.setVisibility(View.INVISIBLE);
        } else {
            buttonSettingsAdmin.setVisibility(View.VISIBLE);
        }
    }
}
