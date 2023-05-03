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
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Customize features of the application
 */
public class VDTSConfigActivity extends AppCompatActivity implements IRIListener {
    //private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigActivity.class);

    VDTSApplication vdtsApplication;
    VDTSUser currentUser;

    //Views
    private Button configUsersActivityButton;
    private Button configFeedbackActivityButton;
    private Button configColumnsActivityButton;

    private TextView footerUserValue;

    //Iristick Components
    private boolean isHeadsetAvailable = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        configUsersActivityButton = findViewById(R.id.configUsersActivityButton);
        configUsersActivityButton.setOnClickListener(v -> configureUsersActivityButtonOnClick());

        configFeedbackActivityButton = findViewById(R.id.configFeedbackActivityButton);
        configFeedbackActivityButton.setOnClickListener(
                v -> configureFeedbackActivityButtonOnClick());

        configColumnsActivityButton = findViewById(R.id.configColumnsActivityButton);
        configColumnsActivityButton.setOnClickListener(
                v -> configureColumnActivityButtonOnClick());

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

    public void configureUsersActivityButtonOnClick() {
        Intent usersActivityIntent = new Intent(this, VDTSConfigUsersActivity.class);
        startActivity(usersActivityIntent);
    }

    public void configureFeedbackActivityButtonOnClick() {
        Intent feedbackActivityIntent = new Intent(this, VDTSConfigFeedbackActivity.class);
        startActivity(feedbackActivityIntent);
    }

    public void configureColumnActivityButtonOnClick() {
        Intent columnActivityIntent = new Intent(this, ConfigColumnsActivity.class);
        startActivity(columnActivityIntent);
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
                    vc.add("Users", this::configureUsersActivityButtonOnClick));
        }
    }
}
