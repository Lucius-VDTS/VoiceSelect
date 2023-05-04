package ca.vdts.voiceselect.library.activities.configure;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnValuesActivity;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
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
    private Button configFeedbackActivityButton;
    private Button configColumnsActivityButton;
    private Button configColumnValuessActivityButton;

    private TextView footerUserValue;

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

        configFeedbackActivityButton = findViewById(R.id.configFeedbackActivityButton);
        configFeedbackActivityButton.setOnClickListener(
                v -> configFeedbackActivityButtonOnClick());

        configColumnsActivityButton = findViewById(R.id.configColumnsActivityButton);
        configColumnsActivityButton.setOnClickListener(
                v -> configColumnActivityButtonOnClick());

        configColumnValuessActivityButton = findViewById(R.id.configColumnValuesActivityButton);
        configColumnValuessActivityButton.setOnClickListener(
                v -> configColumnValuesActivityButtonOnClick());

        footerUserValue = findViewById(R.id.footerUserValue);
        footerUserValue.setText(currentUser.getName());

        disableViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUser = vdtsApplication.getCurrentUser();
        footerUserValue.setText(currentUser.getName());
    }

    private void disableViews() {
        if (currentUser.getAuthority() <= 0) {
            configUsersActivityButton.setEnabled(false);
        }
    }

    public void configUsersActivityButtonOnClick() {
        Intent usersActivityIntent = new Intent(this, VDTSConfigUsersActivity.class);
        startActivity(usersActivityIntent);
    }

    public void configFeedbackActivityButtonOnClick() {
        Intent feedbackActivityIntent = new Intent(this, VDTSConfigFeedbackActivity.class);
        startActivity(feedbackActivityIntent);
    }

    public void configColumnActivityButtonOnClick() {
        Intent columnActivityIntent = new Intent(this, ConfigColumnsActivity.class);
        startActivity(columnActivityIntent);
    }

    public void configColumnValuesActivityButtonOnClick() {
        Intent columnValuesActivityIntent = new Intent(this, ConfigColumnValuesActivity.class);
        startActivity(columnValuesActivityIntent);
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
            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Users", this::configUsersActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Feedback", this::configFeedbackActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Columns", this::configColumnActivityButtonOnClick));

            IristickSDK.addVoiceCommands(this.getLifecycle(), this, vc ->
                    vc.add("Column Values", this::configColumnValuesActivityButtonOnClick));
        }
    }
}