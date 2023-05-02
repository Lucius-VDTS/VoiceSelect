package ca.vdts.voiceselect.library.activities;

import static android.Manifest.permission.ACCESS_MEDIA_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_MEDIA_AUDIO;
import static android.Manifest.permission.READ_MEDIA_IMAGES;
import static android.Manifest.permission.READ_MEDIA_VIDEO;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.database.VSBackupWorker;
import ca.vdts.voiceselect.database.VSDatabase;

/**
 * Requests permissions, set up database, set current user.
 */
public class VDTSMainActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSMainActivity.class);

    //Views
    private TextView requestPermissionsTextView;
    private Button requestPermissionsButton;
    private TextView settingsPermissionsTextView;
    private Button settingsPermissionsButton;

    //Permissions
    private List<String> permissionsNeeded;
    private int permissionRequests;
    private SharedPreferences storedPermissionRequests;
    private SharedPreferences.Editor storedPermissionRequestsEditor;

    //Backup Worker
    public static final Duration DURATION_DAY = Duration.ofDays(1L);
    private boolean backupInitialized = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionsTextView = findViewById(R.id.requestPermissionsTextView);
        requestPermissionsTextView.setVisibility(View.GONE);
        requestPermissionsButton = findViewById(R.id.requestPermissionsButton);
        requestPermissionsButton.setVisibility(View.GONE);
        requestPermissionsButton.setOnClickListener(v -> requestPermissionsButtonOnClick());

        settingsPermissionsTextView = findViewById(R.id.settingsPermissionsTextView);
        settingsPermissionsTextView.setVisibility(View.GONE);
        settingsPermissionsButton = findViewById(R.id.settingsPermissionsButton);
        settingsPermissionsButton.setVisibility(View.GONE);
        settingsPermissionsButton.setOnClickListener(v -> settingsPermissionsButtonOnClick());

        storedPermissionRequests = this.getPreferences(MODE_PRIVATE);
        storedPermissionRequestsEditor = storedPermissionRequests.edit();

        checkPermissions(this);

        if (permissionsNeeded.isEmpty()) {
            initializeDatabase();
            Intent vdtsLoginActivity = new Intent(this, VDTSLoginActivity.class);
            startActivity(vdtsLoginActivity);
        }
    }

    /**
     * Re-Request permissions if not granted on first pass.
     */
    private void requestPermissionsButtonOnClick() {
        permissionRequests++;
        storedPermissionRequestsEditor.putInt("Requests", permissionRequests);
        storedPermissionRequestsEditor.apply();
        checkPermissions(this);
    }

    /**
     * Go to application settings if permissions not granted on second pass.
     */
    private void settingsPermissionsButtonOnClick() {
        Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        permissionsSettingsLauncher.launch(settingsIntent);
    }

    ActivityResultLauncher<Intent> permissionsSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {}
    );

    private void checkPermissions(Activity activity) {
        String[] permissions = new String[] {
                ACCESS_NETWORK_STATE,
                INTERNET,
                CAMERA,
                RECORD_AUDIO,
                ACCESS_MEDIA_LOCATION
        };

        List<String> permissionList = new ArrayList<>(Arrays.asList(permissions));
        if (Build.VERSION.SDK_INT >= 33) {
            permissionList.add(POST_NOTIFICATIONS);
            permissionList.add(READ_MEDIA_AUDIO);
            permissionList.add(READ_MEDIA_IMAGES);
            permissionList.add(READ_MEDIA_VIDEO);
        } else {
            permissionList.add(READ_EXTERNAL_STORAGE);
        }

        permissionsNeeded = new ArrayList<>();
        for (String permission : permissionList) {
            if (checkSelfPermission(permission) != PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            activity.requestPermissions(permissionsNeeded.toArray(new String[0]), 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean permissionNotGranted = Arrays.stream(grantResults)
                .anyMatch(notGranted -> notGranted == -1);

        if (grantResults.length == permissionsNeeded.size()) {
            if (permissionNotGranted) {
                LOG.info("Permissions not granted");
                Log.i("--", "Permissions not granted");
                if (storedPermissionRequests.getInt("Requests", 0) == 1) {
                    requestPermissionsTextView.setVisibility(View.GONE);
                    requestPermissionsButton.setVisibility(View.GONE);
                    settingsPermissionsTextView.setVisibility(View.VISIBLE);
                    settingsPermissionsButton.setVisibility(View.VISIBLE);
                } else {
                    requestPermissionsTextView.setVisibility(View.VISIBLE);
                    requestPermissionsButton.setVisibility(View.VISIBLE);
                }
            } else {
                LOG.info("Permissions granted");
                Log.i("--", "Permissions granted");
                initializeDatabase();
                Intent vdtsLoginActivity = new Intent(this, VDTSLoginActivity.class);
                startActivity(vdtsLoginActivity);
            }
        }
    }

    /**
     * Prepare database and backup worker.
     */
    private void initializeDatabase() {
        //Prepare db
        VSDatabase.getInstance((VSApplication) getApplication());

        if (!backupInitialized) {
            WorkRequest backupWorkRequest = new PeriodicWorkRequest
                    .Builder(VSBackupWorker.class, DURATION_DAY)
                    .build();

            WorkManager.getInstance(this).enqueue(backupWorkRequest);

            backupInitialized = true;
        }
    }
}