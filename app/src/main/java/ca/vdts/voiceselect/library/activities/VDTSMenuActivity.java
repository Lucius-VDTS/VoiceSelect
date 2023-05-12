package ca.vdts.voiceselect.library.activities;

import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IRIState;
import com.iristick.sdk.IristickSDK;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.library.activities.configure.VDTSConfigMenuActivity;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSNotificationUtil;

/**
 * VDTS application's main menu.
 */
public class VDTSMenuActivity extends AppCompatActivity implements IRIListener {
    //private static final Logger LOG = LoggerFactory.getLogger(VDTSMainActivity.class);

    VDTSApplication vdtsApplication;
    VDTSUser currentUser;

    //Views
    private Button startActivityButton;
    private Button resumeActivityButton;
    private Button configureActivityButton;
    private Button settingsActivityButton;
    private Button changeUserActivityButton;
    private Button aboutActivityButton;

    private TextView footerUserValue;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    @Nullable
    private VDTSNotificationUtil errorIristickNotifService;
    @Nullable
    private VDTSNotificationUtil firmwareIristickNotifService;
    @Nullable
    private VDTSNotificationUtil connectedIristickNotifService;
    @Nullable
    private VDTSNotificationUtil disconnectedIristickNotifService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        startActivityButton = findViewById(R.id.startActivityButton);
        startActivityButton.setOnClickListener(v -> startActivityButtonOnClick());

        //todo - disabled if data gathering has not been started
        resumeActivityButton = findViewById(R.id.resumeActivityButton);
        resumeActivityButton.setOnClickListener(v -> resumeActivityButtonOnClick());

        configureActivityButton = findViewById(R.id.configureActivityButton);
        configureActivityButton.setOnClickListener(v -> configureActivityButtonOnClick());

        settingsActivityButton = findViewById(R.id.settingsActivityButton);

        TextToSpeech textToSpeech = vdtsApplication.getTTSEngine();
        settingsActivityButton.setOnClickListener(v -> {
            textToSpeech.speak("Good day " + currentUser.getName(), 0, null, null);
        });

        changeUserActivityButton = findViewById(R.id.changeUserActivityButton);
        changeUserActivityButton.setOnClickListener(v -> changeUserActivityButtonOnClick());
        if (currentUser.getUid() == -9001) { changeUserActivityButton.setEnabled(false); }

        aboutActivityButton = findViewById(R.id.aboutActivityButton);
        aboutActivityButton.setVisibility(View.GONE);

        footerUserValue = findViewById(R.id.footerUserValue);
        footerUserValue.setText(currentUser.getName());
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUser = vdtsApplication.getCurrentUser();
        footerUserValue.setText(currentUser.getName());
    }

    public void startActivityButtonOnClick() {
        Intent startActivityIntent = new Intent(this, DataGatheringActivity.class);
        startActivity(startActivityIntent);
    }

    public void resumeActivityButtonOnClick() {
        Intent resumeActivityIntent = new Intent(this, DataGatheringActivity.class);
        startActivity(resumeActivityIntent);
    }

    public void configureActivityButtonOnClick() {
        Intent configureActivityIntent = new Intent(this, VDTSConfigMenuActivity.class);
        startActivity(configureActivityIntent);
    }

    public void changeUserActivityButtonOnClick() {
        Intent changeUserActivityIntent = new Intent(this, VDTSLoginActivity.class);
        startActivity(changeUserActivityIntent);
    }

    /**
     * Show Iristick custom about screen - required for legal reasons.
     */
    public void aboutActivityButtonOnClick() {
        IristickSDK.showAbout(VDTSMenuActivity.this);
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
        initializeIristick();
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
        initializeIristick();
    }

    /**
     * Initialize elements based on Iristick connection.
     */
    private void initializeIristick() {
        if (isHeadsetAvailable) {
            aboutActivityButton.setVisibility(View.VISIBLE);
            aboutActivityButton.setOnClickListener(v -> aboutActivityButtonOnClick());

            VDTSNotificationUtil.init(this);

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Start", this::startActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Resume", this::resumeActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Configure", this::configureActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("About Iris Stick", this::aboutActivityButtonOnClick));

            if (currentUser.getUid() != -9001) {
                IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                        vc.add("Change User", this::changeUserActivityButtonOnClick));
            }
        } else {
            aboutActivityButton.setVisibility(View.GONE);
        }
    }

    /**
     * Provide notifications to the user based on Iristick state.
     * @param state The new/current state.
     */
    @Override
    public void onIristickState(@NonNull IRIState state) {
        if (state.isError()) {
            if (errorIristickNotifService == null) {
                errorIristickNotifService = new VDTSNotificationUtil(
                        getApplicationContext(), VDTSNotificationUtil.Channel.ERROR);
            }
            errorIristickNotifService.setContentTitle("Error: " + state);
            errorIristickNotifService.show();
        } else {
            if (errorIristickNotifService != null) {
                errorIristickNotifService.cancel();
                errorIristickNotifService = null;
            }
        }

        if (state == IRIState.SYNCHRONIZING_FIRMWARE) {
            if (firmwareIristickNotifService == null) {
                firmwareIristickNotifService = new VDTSNotificationUtil(
                        getApplicationContext(), VDTSNotificationUtil.Channel.FIRMWARE);
                firmwareIristickNotifService.setOngoing(true);
                firmwareIristickNotifService.setContentTitle("Firmware sync in progress");
                firmwareIristickNotifService.setProgress(100, 0, true);
                firmwareIristickNotifService.show();
            }
        } else {
            if (firmwareIristickNotifService != null) {
                firmwareIristickNotifService.cancel();
                firmwareIristickNotifService = null;
            }
        }

        if (state == IRIState.HEADSET_CONNECTED) {
            if (connectedIristickNotifService == null) {
                connectedIristickNotifService = new VDTSNotificationUtil(
                        getApplicationContext(), VDTSNotificationUtil.Channel.CONNECTED);
            }
            connectedIristickNotifService.setContentTitle("Iristick connected");
            connectedIristickNotifService.show();
        } else {
            if (connectedIristickNotifService != null) {
                connectedIristickNotifService.cancel();
                connectedIristickNotifService = null;
            }
        }

        if (state == IRIState.DISCONNECTED) {
            if (disconnectedIristickNotifService == null) {
                disconnectedIristickNotifService = new VDTSNotificationUtil(
                        getApplicationContext(), VDTSNotificationUtil.Channel.DISCONNECTED);
            }
            disconnectedIristickNotifService.setContentTitle("Iristick disconnected");
            disconnectedIristickNotifService.show();
        } else {
            if (disconnectedIristickNotifService != null) {
                disconnectedIristickNotifService.cancel();
                disconnectedIristickNotifService = null;
            }
        }
    }

    /**
     * Used when a firmware sync notification is sent to the user
     * @param progress The current relative progress between 0.0 and 1.0 inclusive.
     */
    @Override
    public void onFirmwareSyncProgress(float progress) {
        if (isHeadsetAvailable) {
            if (firmwareIristickNotifService != null) {
                firmwareIristickNotifService.setProgress(
                        100, (int) (progress * 100), false);
                firmwareIristickNotifService.show();
            }
        }
    }
}
