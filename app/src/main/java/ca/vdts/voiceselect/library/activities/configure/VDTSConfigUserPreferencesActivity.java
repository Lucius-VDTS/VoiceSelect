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
 * Configure user auto save, abbreviation, and feedback preferences.
 */
public class VDTSConfigUserPreferencesActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigUserPreferencesActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private TextToSpeech ttsEngine;
    private String packageName;
    private VSViewModel vsViewModel;

    //Views
    private TextView userText;

    private SwitchCompat autoSaveSwitch;
    private SwitchCompat abbreviateSwitch;
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
    private VDTSConfigUserPreferencesActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_user_preferences);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) getApplication();
        currentUser = vdtsApplication.getCurrentUser();
        ttsEngine = vdtsApplication.getTTSEngine();
        packageName = "com.google.android.tts";

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        userText = findViewById(R.id.userValue);

        autoSaveSwitch = findViewById(R.id.userAutoSaveSwitch);
        abbreviateSwitch = findViewById(R.id.abbreviateSwitch);
        enabledSwitch = findViewById(R.id.userFeedbackSwitch);
        flushQueueSwitch = findViewById(R.id.userFlushSwitch);
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
        defaultFeedbackButton.setOnClickListener(v -> defaultButtonOnClick());

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
        autoSaveSwitch.setChecked(currentUser.getAutosave() == 1);
        abbreviateSwitch.setChecked(currentUser.isAbbreviate());
        enabledSwitch.setChecked(currentUser.getFeedback() == 1);
        flushQueueSwitch.setChecked(currentUser.isFeedbackFlushQueue());
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

    private void defaultButtonOnClick() {
        autoSaveSwitch.setChecked(true);
        abbreviateSwitch.setChecked(true);
        enabledSwitch.setChecked(true);
        flushQueueSwitch.setChecked(false);
        rateSeekBar.setProgress(50);
        pitchSeekBar.setProgress(50);
    }

    private void saveFeedbackButtonOnClick() {
        try {
            if (autoSaveSwitch.isChecked()) {
                currentUser.setAutosave(1);
            } else {
                currentUser.setAutosave(0);
            }

            currentUser.setAbbreviate(abbreviateSwitch.isChecked());

            if (enabledSwitch.isChecked()) {
                currentUser.setFeedback(1);
            } else {
                currentUser.setFeedback(0);
            }

            currentUser.setFeedbackFlushQueue(flushQueueSwitch.isChecked());

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
                    "Preferences saved for: " + currentUser.getName()
            );
        } catch (Exception e) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(saveFeedbackButton);

            LOG.error("{}: Unable to save preferences for {}", e, currentUser.getName());
            vdtsApplication.displayToast(
                    this,
                    "Unable to save preferences for: " + currentUser.getName()
            );
        }
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
                vc -> vc.add("Navigate Back", this::finish)
        );
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_config_hud);
        }
    }
}
