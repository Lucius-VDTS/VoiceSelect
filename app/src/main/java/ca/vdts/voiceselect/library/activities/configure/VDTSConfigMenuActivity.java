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
import com.iristick.sdk.display.IRIWindow;

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
    private VDTSConfigMenuActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_menu);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        configUsersActivityButton = findViewById(R.id.configUsersActivityButton);
        configUsersActivityButton.setOnClickListener(v -> configUsersActivityButtonOnClick());

        configPreferencesActivityButton = findViewById(R.id.configUserPreferencesActivityButton);
        configPreferencesActivityButton.setOnClickListener(v -> configUserPreferencesButtonOnClick());

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
        if (currentUser.getUid() == -9001L) {
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

    public void configUserPreferencesButtonOnClick() {
        Intent userPreferencesActivityIntent = new Intent(
                this,
                VDTSConfigUserPreferencesActivity.class
        );
        startActivity(userPreferencesActivityIntent);
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
        initializeIristick();
    }

    /**
     * Initialize Iristick HUD and voice commands when connected.
     */
    private void initializeIristick() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new IristickHUD();
            return iristickHUD;
        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Users", this::configUsersActivityButtonOnClick)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("User Preferences", this::configUserPreferencesButtonOnClick)
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
                        "Values",
                        this::configColumnValuesActivityButtonOnClick
                )
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Layouts", this::configLayoutsActivityButtonOnClick)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Navigate Back", this::finish)
        );
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
       @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_config_menu_hud);
        }
    }
}
