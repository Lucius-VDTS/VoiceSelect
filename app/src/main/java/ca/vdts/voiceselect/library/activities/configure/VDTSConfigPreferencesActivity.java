package ca.vdts.voiceselect.library.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.textfield.TextInputEditText;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Config user's feedback params.
 */
public class VDTSConfigPreferencesActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigPreferencesActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private TextToSpeech ttsEngine;
    private String packageName;
    private VSViewModel vsViewModel;

    //Views
    private TextView userText;

    private SwitchCompat enabledSwitch;
    private SwitchCompat flushQueueSwitch;
    private SeekBar rateSeekBar;
    private SeekBar pitchSeekBar;
    private TextInputEditText sampleTextInput;

    private Button testFeedbackButton;
    private Button resetFeedbackButton;
    private Button defaultFeedbackButton;
    private Button saveFeedbackButton;

    //Iristick components
    private boolean isHeadsetAvailable = false;
    private VDTSConfigPreferencesActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_preferences);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();
        ttsEngine = vdtsApplication.getTTSEngine();
        packageName = "com.google.android.tts";

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        userText = findViewById(R.id.userText);

        enabledSwitch = findViewById(R.id.userAdminSwitch);
        flushQueueSwitch = findViewById(R.id.userPrimarySwitch);
        rateSeekBar = findViewById(R.id.rateSeekBar);
        pitchSeekBar = findViewById(R.id.pitchSeekBar);

        sampleTextInput = findViewById(R.id.sampleTextInput);
        sampleTextInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                testFeedbackButton.setEnabled(s.length() != 0);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        testFeedbackButton = findViewById(R.id.testFeedbackButton);
        testFeedbackButton.setOnClickListener(v -> testFeedbackButtonOnClick());

        resetFeedbackButton = findViewById(R.id.resetFeedbackButton);
        resetFeedbackButton.setOnClickListener(v -> resetFeedbackButtonOnClick());

        defaultFeedbackButton = findViewById(R.id.defaultFeedbackButton);
        defaultFeedbackButton.setOnClickListener(v -> defaultFeedbackButtonOnClick());

        saveFeedbackButton = findViewById(R.id.saveFeedbackButton);
        saveFeedbackButton.setOnClickListener(v -> saveFeedbackButtonOnClick());

        initializeUserSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentUser = vdtsApplication.getCurrentUser();
        userText.setText(currentUser.getName());
    }

    /**
     * Initialize views with feedback values from the current user.
     */
    private void initializeUserSettings() {
        userText.setText(currentUser.getName());
        enabledSwitch.setChecked(currentUser.getFeedback() == 1);
        flushQueueSwitch.setChecked(currentUser.isFeedbackQueue());
        rateSeekBar.setProgress((int) (currentUser.getFeedbackRate() * 50));
        pitchSeekBar.setProgress((int) (currentUser.getFeedbackPitch() * 50));
    }

    /**
     * Test the TTS engine's feedback.
     */
    public void testFeedbackButtonOnClick() {
        String sampleTextOutput = Objects.requireNonNull(sampleTextInput.getText()).toString();

        int queueMode;
        if (flushQueueSwitch.isChecked()) {
            queueMode = TextToSpeech.QUEUE_FLUSH;
        } else {
            queueMode = TextToSpeech.QUEUE_ADD;
        }

        float rate = (float) rateSeekBar.getProgress() / 50;
        if (rate < 0.1) { rate = 0.1f; }
        ttsEngine.setSpeechRate(rate);

        float pitch = (float) pitchSeekBar.getProgress() / 50;
        if (pitch < 0.1) { pitch = 0.1f; }
        ttsEngine.setPitch(pitch);

        ttsEngine.speak(sampleTextOutput, queueMode, null, null);
    }

    private void resetFeedbackButtonOnClick() {
        initializeUserSettings();
    }

    private void defaultFeedbackButtonOnClick() {
        enabledSwitch.setChecked(true);
        flushQueueSwitch.setChecked(false);
        rateSeekBar.setProgress(50);
        pitchSeekBar.setProgress(50);
    }

    private void saveFeedbackButtonOnClick() {
        try {
            if (enabledSwitch.isChecked()) {
                currentUser.setFeedback(1);
            } else {
                currentUser.setFeedback(0);
            }

            currentUser.setFeedbackQueue(flushQueueSwitch.isChecked());

            float rate = (float) rateSeekBar.getProgress() / 50;
            if (rate < 0.1) { rate = 0.1f; }
            currentUser.setFeedbackRate(rate);

            float pitch = (float) pitchSeekBar.getProgress() / 50;
            if (pitch < 0.1) { pitch = 0.1f; }
            currentUser.setFeedbackPitch(pitch);

            new Thread(() -> vsViewModel.updateUser(currentUser)).start();

            ttsEngine.setSpeechRate(rate);
            ttsEngine.setPitch(pitch);

            LOG.info("Feedback settings saved for: {}", currentUser.getName());
            vdtsApplication.displayToast(
                    this,
                    "Feedback settings saved for: " + currentUser.getName(),
                    0);
        } catch (Exception e) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(saveFeedbackButton);
            vdtsApplication.displayToast(
                    this,
                    "Unable to save feedback settings for: " + currentUser.getName(),
                    0);
            LOG.error("{}: Unable to save feedback settings for {}", e, currentUser.getName());
        }
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
    }

    /**
     * Initialize elements based on Iristick connection.
     */
    private void initializeIristick() {
        if (isHeadsetAvailable) {
            IristickSDK.addWindow(this.getLifecycle(), () -> {
                iristickHUD = new IristickHUD();
                return iristickHUD;
            });
        }
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        private TextView configOnDeviceText;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_config_hud);

            configOnDeviceText = findViewById(R.id.configHUDText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_hud_text);
        }
    }
}
