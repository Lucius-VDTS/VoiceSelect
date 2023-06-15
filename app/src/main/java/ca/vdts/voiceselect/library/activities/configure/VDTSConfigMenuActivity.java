package ca.vdts.voiceselect.library.activities.configure;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.BuildConfig;
import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnValuesActivity;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.activities.configure.ConfigLayoutsActivity;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Customize features of the application
 */
public class VDTSConfigMenuActivity extends AppCompatActivity implements IRIListener {
    //private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigMenuActivity.class);

    VDTSApplication vdtsApplication;
    VDTSUser currentUser;

    //Views
    private Button configUsersActivityButton;
    private Button configPreferencesActivityButton;
    private Button configColumnsActivityButton;
    private Button configColumnValuesActivityButton;
    private Button configLayoutsActivityButton;

    private TextView footerLayoutValue;
    private TextView footerSessionValue;
    private TextView footerUserValue;
    private TextView footerVersionValue;

    //Iristick Components
    private boolean isHeadsetAvailable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_menu);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        configUsersActivityButton = findViewById(R.id.configUsersActivityButton);
        configUsersActivityButton.setOnClickListener(v -> configUsersActivityButtonOnClick());

        configPreferencesActivityButton = findViewById(R.id.configPreferencesActivityButton);
        configPreferencesActivityButton.setOnClickListener(v -> configFeedbackActivityButtonOnClick());

        configColumnsActivityButton = findViewById(R.id.configColumnsActivityButton);
        configColumnsActivityButton.setOnClickListener(v -> configColumnsActivityButtonOnClick());

        configColumnValuesActivityButton = findViewById(R.id.configColumnValuesActivityButton);
        configColumnValuesActivityButton.setOnClickListener(
                v -> configColumnValuesActivityButtonOnClick()
        );

        configLayoutsActivityButton = findViewById(R.id.configLayoutsActivityButton);
        configLayoutsActivityButton.setOnClickListener(v -> configLayoutsActivityButtonOnClick());

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
                currentLayout = viewModel.findLayoutByID(layoutID);
            }
            if (currentLayout != null) {
                final Layout l = currentLayout;
                handler.post(() -> footerLayoutValue.setText(l .getName()));
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
                handler.post(() -> footerSessionValue.setText(c.name()));
            } else {
                handler.post(() -> footerSessionValue.setText(""));
            }
        });

        disableViews();
    }

    private void disableViews() {
        /*if (currentUser.getAuthority() <= 0) {
            configUsersActivityButton.setEnabled(false);
        } else*/ if (currentUser.getUid() == -9001L) {
            configPreferencesActivityButton.setEnabled(false);
            configColumnsActivityButton.setEnabled(false);
            configColumnValuesActivityButton.setEnabled(false);
            configLayoutsActivityButton.setEnabled(false);
        } else {
            configPreferencesActivityButton.setEnabled(true);
            configColumnsActivityButton.setEnabled(true);
            configColumnValuesActivityButton.setEnabled(true);
            configLayoutsActivityButton.setEnabled(true);
        }
    }

    public void configUsersActivityButtonOnClick() {
        Intent usersActivityIntent = new Intent(this, VDTSConfigUsersActivity.class);
        startActivity(usersActivityIntent);
    }

    public void configFeedbackActivityButtonOnClick() {
        Intent feedbackActivityIntent = new Intent(
                this,
                VDTSConfigPreferencesActivity.class
        );
        startActivity(feedbackActivityIntent);
    }

    public void configColumnsActivityButtonOnClick() {
        Intent columnActivityIntent = new Intent(this, ConfigColumnsActivity.class);
        startActivity(columnActivityIntent);
    }

    public void configColumnValuesActivityButtonOnClick() {
        Intent columnValuesActivityIntent = new Intent(
                this,
                ConfigColumnValuesActivity.class
        );
        startActivity(columnValuesActivityIntent);
    }

    public void configLayoutsActivityButtonOnClick() {
        Intent layoutsActivityIntent = new Intent(this, ConfigLayoutsActivity.class);
        startActivity(layoutsActivityIntent);
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
     * Initialize elements based on Iristick connection
     */
    private void initializeIristick() {
        if (isHeadsetAvailable) {
            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Users", this::configUsersActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Feedback", this::configFeedbackActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Columns", this::configColumnsActivityButtonOnClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add(
                            "Column Values",
                            this::configColumnValuesActivityButtonOnClick
                    )
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Layouts", this::configLayoutsActivityButtonOnClick)
            );
        }
    }
}
