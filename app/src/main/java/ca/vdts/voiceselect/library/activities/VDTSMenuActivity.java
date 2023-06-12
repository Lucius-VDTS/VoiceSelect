package ca.vdts.voiceselect.library.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IRIState;
import com.iristick.sdk.IristickSDK;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.BuildConfig;
import ca.vdts.voiceselect.R;
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
    //private static final Logger LOG = LoggerFactory.getLogger(VDTSMainActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;

    private Layout selectedLayout;

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

    //View Model - Adapters
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<Layout> layoutSpinnerAdapter;

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

        //Observe/Update layout list
        vsViewModel.findAllLayoutsLive().observe(this, layouts -> {
            layoutList.clear();
            layoutList.addAll(layouts);
            layoutList.remove(Layout.LAYOUT_NONE);
            layoutSpinner.setEnabled(layoutList.size() > 1);
            layoutSpinnerAdapter.notifyDataSetChanged();
        });

        layoutSpinnerAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                layoutList);
        layoutSpinnerAdapter.setToStringFunction((layout, integer) -> layout.getName());

        layoutSpinner.setAdapter(layoutSpinnerAdapter);
        layoutSpinner.setOnItemSelectedListener(layoutSpinnerListener);

        startActivityButton = findViewById(R.id.startActivityButton);
        startActivityButton.setOnClickListener(v -> startActivityButtonOnClick());

        resumeActivityButton = findViewById(R.id.resumeActivityButton);
        resumeActivityButton.setOnClickListener(v -> resumeActivityButtonOnClick());

        configureActivityButton = findViewById(R.id.configureActivityButton);
        configureActivityButton.setOnClickListener(v -> configureActivityButtonOnClick());

        settingsActivityButton = findViewById(R.id.settingsActivityButton);

        TextToSpeech textToSpeech = vdtsApplication.getTTSEngine();
        settingsActivityButton.setOnClickListener(
                v -> textToSpeech.speak(
                        "Good day " + currentUser.getName(),
                        0,
                        null,
                        null
                )
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

        VSViewModel viewModel = new VSViewModel(vdtsApplication);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
                    final String layoutKey = currentUser.getExportCode().concat("_Layout");
                    final long layoutID = vdtsApplication.getPreferences().getLong(
                            layoutKey,
                            -1L
                    );
                    Layout currentLayout = null;
                    if (layoutID > 0) {
                        currentLayout = viewModel.findLayout(layoutID);
                    }
                    if (currentLayout != null) {
                        final Layout l = currentLayout;
                        handler.post(() -> {
                            footerLayoutValue.setText(l .getName());
                            layoutSpinner.setSelection(layoutSpinnerAdapter.getPosition(l));
                        });
                    } else {
                        handler.post(() -> footerLayoutValue.setText(""));
                    }

                    final String sessionKey = currentUser.getExportCode().concat("_Session");
                    final long sessionID = vdtsApplication.getPreferences().getLong(
                            sessionKey,
                            -1L
                    );
                    Session currentSession = null;
                    if (sessionID > 0) {
                        currentSession = viewModel.findSessionByID(sessionID);
                    }
                    if (currentSession != null) {
                        final Session c = currentSession;
                        handler.post(() -> {
                            resumeActivityButton.setEnabled(true);
                            footerSessionValue.setText(c.name());
                        });
                    } else {
                        handler.post(() -> {
                            resumeActivityButton.setEnabled(false);
                            footerSessionValue.setText("");
                        });
                    }
                });

        disableViews();
    }

    private void disableViews() {
        if (currentUser.getUid() == -9001L) {
            startActivityButton.setEnabled(false);
            //resumeActivityButton.setEnabled(false);
            settingsActivityButton.setEnabled(false);
            changeUserActivityButton.setEnabled(false);
        } else {
            startActivityButton.setEnabled(true);
            //resumeActivityButton.setEnabled(true);
            settingsActivityButton.setEnabled(true);
            changeUserActivityButton.setEnabled(true);
        }
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
        Intent configureActivityIntent = new Intent(
                this,
                VDTSConfigMenuActivity.class
        );
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

    private final AdapterView.OnItemSelectedListener layoutSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedLayout = (Layout) parent.getItemAtPosition(position);

                    if (layoutList.size() <=1) {
                        layoutSpinner.setEnabled(false);
                    } else {
                        layoutSpinner.setEnabled(true);
                    }
                    final String layoutKey = currentUser.getExportCode().concat("_Layout");
                    vdtsApplication.getPreferences().setLong(layoutKey,selectedLayout.getUid());
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };


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
                    vc -> vc.add("About Iris Stick", this::aboutActivityButtonOnClick)
            );

            if (currentUser.getUid() != -9001) {
                IristickSDK.addVoiceCommands(
                        this.getLifecycle(),
                        this,
                        vc -> vc.add("Change User", this::changeUserActivityButtonOnClick)
                );
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
}
