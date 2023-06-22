package ca.vdts.voiceselect.library.activities;

import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IRIState;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.BuildConfig;
import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.SettingsActivity;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.activities.configure.VDTSConfigMenuActivity;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSNotificationUtil;

/**
 * VDTS application's main menu.
 */
public class VDTSMenuActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSMainActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Session currentSession;
    private Layout currentLayout;

    //Views
    private Spinner layoutSpinner;
    private Button startActivityButton;
    private Button resumeActivityButton;
    private Button configureActivityButton;
    private Button settingsActivityButton;
    private Button changeUserActivityButton;
    private Button aboutActivityButton;

    private TextView footerLayoutValue;
    private TextView footerSessionValue;
    private TextView footerUserValue;
    private TextView footerVersionValue;

    //Layout Spinner
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<Layout> layoutAdapter;

    //Lists
    private final List<Layout> layoutList = new ArrayList<>();

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    @Nullable
    private VDTSNotificationUtil errorIristickNotificationService;
    @Nullable
    private VDTSNotificationUtil firmwareIristickNotificationService;
    @Nullable
    private VDTSNotificationUtil connectedIristickNotificationService;
    @Nullable
    private VDTSNotificationUtil disconnectedIristickNotificationService;
    private VDTSMenuActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        //Layout Spinner
        layoutSpinner = findViewById(R.id.layoutSpinner);

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        layoutAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                layoutList
        );
        layoutAdapter.setToStringFunction((layout, integer) -> layout.getName());

        layoutSpinner.setAdapter(layoutAdapter);
        layoutSpinner.setOnItemSelectedListener(layoutSpinnerListener);

        startActivityButton = findViewById(R.id.startActivityButton);
        startActivityButton.setOnClickListener(v -> startActivityButtonOnClick());

        resumeActivityButton = findViewById(R.id.resumeActivityButton);
        resumeActivityButton.setOnClickListener(v -> resumeActivityButtonOnClick());

        configureActivityButton = findViewById(R.id.configureActivityButton);
        configureActivityButton.setOnClickListener(v -> configureActivityButtonOnClick());

        settingsActivityButton = findViewById(R.id.settingsActivityButton);

        settingsActivityButton.setOnClickListener(
                v -> settingsActivityButtonOnClick()
        );

        changeUserActivityButton = findViewById(R.id.changeUserActivityButton);
        changeUserActivityButton.setOnClickListener(v -> changeUserActivityButtonOnClick());

        aboutActivityButton = findViewById(R.id.aboutActivityButton);
        aboutActivityButton.setVisibility(View.GONE);

        footerLayoutValue = findViewById(R.id.footerLayoutValue);

        footerSessionValue = findViewById(R.id.footerSessionValue);

        footerUserValue = findViewById(R.id.footerUserValue);
        footerUserValue.setText(currentUser.getName());

        footerVersionValue = findViewById(R.id.footerVersionValue);
        footerVersionValue.setText(BuildConfig.VERSION_NAME);
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUser = vdtsApplication.getCurrentUser();
        footerUserValue.setText(currentUser.getName());

        initializeCurrentSession();
    }

    private void initializeCurrentSession() {
        String currentSessionKey = currentUser.getExportCode().concat("_SESSION");
        long currentSessionID = vdtsApplication.getPreferences().getLong(
                currentSessionKey,
                -1L);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            currentSession = vsViewModel.findSessionByID(currentSessionID);
            handler.post(this::initializeCurrentLayout);
        });
    }

    private void initializeCurrentLayout() {
        final String layoutKey = currentUser.getExportCode().concat("_LAYOUT");
        final long layoutID = vdtsApplication.getPreferences().getLong(
                 layoutKey,
                -1L
        );

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            layoutList.clear();
            layoutList.addAll(vsViewModel.findAllActiveLayouts());

            if (layoutList.size() > 1) {
                layoutList.remove(Layout.LAYOUT_NONE);
                currentLayout = vsViewModel.findLayoutByID(layoutID);
            }
            handler.post(() -> {
                layoutAdapter.notifyDataSetChanged();

                if (currentLayout == null) {
                    layoutSpinner.setSelection(0);
                } else {
                    layoutSpinner.setSelection(layoutList.indexOf(currentLayout));
                }

                if (currentSession != null) {
                    footerLayoutValue.setText(currentSession.getLayoutName());
                    footerSessionValue.setText(currentSession.name());
                } else {
                    footerLayoutValue.setText("");
                    footerSessionValue.setText("");
                }

                disableViews();
            });
        });
    }

    private void disableViews() {
        if (currentUser.getUid() == -9001L) {
            startActivityButton.setEnabled(false);
            resumeActivityButton.setEnabled(false);
            settingsActivityButton.setEnabled(false);
            changeUserActivityButton.setEnabled(false);
        }
    }

    private final AdapterView.OnItemSelectedListener layoutSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    currentLayout = (Layout) parent.getItemAtPosition(position);
                    if (currentLayout.getUid() == -9001L) {
                        YoYo.with(Techniques.Shake)
                                .duration(SHAKE_DURATION)
                                .repeat(SHAKE_REPEAT)
                                .playOn(configureActivityButton);
                        vdtsApplication.displayToast(
                                vdtsApplication,
                                "Create a layout to start a session",
                                0
                        );

                        layoutSpinner.setEnabled(false);
                    } else {
                        layoutSpinner.setEnabled(true);
                        final String layoutKey = currentUser.getExportCode().concat("_LAYOUT");
                        vdtsApplication.getPreferences().setLong(layoutKey, currentLayout.getUid());
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    public void startActivityButtonOnClick() {
        if (currentLayout.getUid() == -9001L) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(startActivityButton);
            vdtsApplication.displayToast(
                    this,
                    "Select a layout to start a session",
                    0);
        } else {
            //Create new session
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                int dailySessionCount = vsViewModel.countSessionsStartedToday();
                currentSession = new Session(
                        currentUser.getUid(),
                        currentUser.getSessionPrefix(),
                        currentLayout.getName(),
                        dailySessionCount + 1);
                long uid = vsViewModel.insertSession(currentSession);
                currentSession.setUid(uid);
                LOG.info("Added session: {}", currentSession.getSessionPrefix());

                handler.post(() -> {
                    //Attach new session to current user
                    vdtsApplication.getPreferences().setLong(
                            String.format("%s_SESSION", currentUser.getExportCode()),
                            currentSession.getUid());
                    LOG.info("Attached session to: {}", currentUser.getName());
                });
            });

            Intent startActivityIntent = new Intent(this, DataGatheringActivity.class);
            startActivity(startActivityIntent);
        }
    }

    public void resumeActivityButtonOnClick() {
        if (currentSession == null) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(resumeActivityButton);
            vdtsApplication.displayToast(
                    this,
                    "A session has not been started",
                    0);
        } else {
            Intent resumeActivityIntent = new Intent(this, DataGatheringActivity.class);
            startActivity(resumeActivityIntent);
        }
    }

    public void configureActivityButtonOnClick() {
        Intent configureActivityIntent = new Intent(
                this,
                VDTSConfigMenuActivity.class
        );
        startActivity(configureActivityIntent);
    }

    private void settingsActivityButtonOnClick() {
        Intent changeUserActivityIntent = new Intent(this, SettingsActivity.class);
        startActivity(changeUserActivityIntent);
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

            IristickSDK.addWindow(this.getLifecycle(), () -> {
                iristickHUD = new IristickHUD();
                return iristickHUD;
            });

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Start", this::startActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Resume", this::resumeActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Configure", this::configureActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Settings", this::settingsActivityButtonOnClick)
            );

            if (currentUser.getUid() != -9001) {
                IristickSDK.addVoiceCommands(
                        this.getLifecycle(),
                        this,
                        vc -> vc.add("Change User", this::changeUserActivityButtonOnClick)
                );
            }

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("About Iris Stick", this::aboutActivityButtonOnClick)
            );
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
            if (errorIristickNotificationService == null) {
                errorIristickNotificationService = new VDTSNotificationUtil(
                        getApplicationContext(),
                        VDTSNotificationUtil.Channel.ERROR
                );
            }
            errorIristickNotificationService.setContentTitle("Error: " + state);
            errorIristickNotificationService.show();
        } else {
            if (errorIristickNotificationService != null) {
                errorIristickNotificationService.cancel();
                errorIristickNotificationService = null;
            }
        }

        if (state.equals(IRIState.SYNCHRONIZING_FIRMWARE)) {
            if (firmwareIristickNotificationService == null) {
                firmwareIristickNotificationService = new VDTSNotificationUtil(
                        getApplicationContext(),
                        VDTSNotificationUtil.Channel.FIRMWARE
                );
                firmwareIristickNotificationService.setOngoing(true);
                firmwareIristickNotificationService.setContentTitle("Firmware sync in progress");
                firmwareIristickNotificationService.setProgress(
                        100,
                        0,
                        true
                );
                firmwareIristickNotificationService.show();
            }
        } else {
            if (firmwareIristickNotificationService != null) {
                firmwareIristickNotificationService.cancel();
                firmwareIristickNotificationService = null;
            }
        }

        if (state.equals(IRIState.HEADSET_CONNECTED)) {
            if (connectedIristickNotificationService == null) {
                connectedIristickNotificationService = new VDTSNotificationUtil(
                        getApplicationContext(),
                        VDTSNotificationUtil.Channel.CONNECTED
                );
            }
            connectedIristickNotificationService.setContentTitle("Iristick connected");
            connectedIristickNotificationService.show();
        } else {
            if (connectedIristickNotificationService != null) {
                connectedIristickNotificationService.cancel();
                connectedIristickNotificationService = null;
            }
        }

        if (state.equals(IRIState.DISCONNECTED)) {
            if (disconnectedIristickNotificationService == null) {
                disconnectedIristickNotificationService = new VDTSNotificationUtil(
                        getApplicationContext(),
                        VDTSNotificationUtil.Channel.DISCONNECTED
                );
            }
            disconnectedIristickNotificationService.setContentTitle("Iristick disconnected");
            disconnectedIristickNotificationService.show();
        } else {
            if (disconnectedIristickNotificationService != null) {
                disconnectedIristickNotificationService.cancel();
                disconnectedIristickNotificationService = null;
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
            if (firmwareIristickNotificationService != null) {
                firmwareIristickNotificationService.setProgress(
                        100,
                        (int) (progress * 100),
                        false
                );
                firmwareIristickNotificationService.show();
            }
        }
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login_hud);
        }
    }
}
