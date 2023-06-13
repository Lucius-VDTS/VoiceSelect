package ca.vdts.voiceselect.activities;

import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_CHAINED;
import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_FREE;
import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_STEP;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_AUTOSAVE;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_ENTRY_METHOD;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_GPS;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_NAME;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_TIME;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.utilities.VDTSNotificationUtil;

public class SettingsMenu extends AppCompatActivity implements IRIListener {
    private VDTSApplication vdtsApplication;

    private SwitchCompat autoCheck;

    private SwitchCompat nameOnPhotoCheck;
    private SwitchCompat  gpsOnPhotoCheck;
    private SwitchCompat  timeOnPhotoCheck;

    private SwitchCompat  csvCheck;
    private SwitchCompat  jsonCheck;
    private SwitchCompat excelCheck;

    private RadioGroup entryMethodGroup;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private SettingsMenu.IristickHUD iristickHUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        vdtsApplication = (VDTSApplication) getApplication();

        autoCheck = findViewById(R.id.autoSaveCheck);
        autoCheck.setOnClickListener(v -> autosaveClick());
        nameOnPhotoCheck = findViewById(R.id.printNameOnPictureCheck);
        nameOnPhotoCheck.setOnClickListener(v -> nameOnPictureClick());
        gpsOnPhotoCheck = findViewById(R.id.printGPSOnPictureCheck);
        gpsOnPhotoCheck.setOnClickListener(v -> gpsOnPictureClick());
        timeOnPhotoCheck = findViewById(R.id.printTimeOnPictureCheck);
        timeOnPhotoCheck.setOnClickListener(v -> gpsOnPictureClick());
        csvCheck = findViewById(R.id.CSVCheck);
        csvCheck.setOnClickListener(v -> csvClick());
        jsonCheck = findViewById(R.id.JSONCheck);
        jsonCheck.setOnClickListener(v -> jsonClick());
        excelCheck = findViewById(R.id.excelCheck);
        excelCheck.setOnClickListener(v -> excelClick());
        entryMethodGroup = findViewById(R.id.entryModeGroup);
        entryMethodGroup.setOnCheckedChangeListener((group, checkedId) -> {
            View radioButton = entryMethodGroup.findViewById(checkedId);
            int index = entryMethodGroup.indexOfChild(radioButton);
            switch (index) {
                case 0: // first button
                    setEntryMethod(METHOD_CHAINED);
                    break;
                case 1: // second button
                    setEntryMethod(METHOD_STEP);
                    break;
                case 2: // third button
                    setEntryMethod(METHOD_FREE);
                    break;
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateControls();
    }

    private void updateControls() {
        autoCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_AUTOSAVE, false));

        nameOnPhotoCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_NAME, false));
        gpsOnPhotoCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_GPS, false));
        timeOnPhotoCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_TIME, false));

        csvCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV, false));
        jsonCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON, false));
        excelCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX, false));

        switch (vdtsApplication.getPreferences().getInt(PREF_ENTRY_METHOD, -1)) {
            case METHOD_CHAINED:
            default:
                ((RadioButton) findViewById(R.id.chainedRadio)).setChecked(true);
                break;
            case METHOD_STEP:
                ((RadioButton) findViewById(R.id.stepRadio)).setChecked(true);
                break;
            case METHOD_FREE:
                ((RadioButton) findViewById(R.id.freeRadio)).setChecked(true);
                break;
        }
    }

    private void setEntryMethod(int order) {
        vdtsApplication.getPreferences().setInt(PREF_ENTRY_METHOD, order);
    }

    private void autosaveClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_AUTOSAVE, autoCheck.isChecked());
    }

    public void nameOnPictureClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_PHOTO_PRINT_NAME, nameOnPhotoCheck.isChecked());
    }

    public void gpsOnPictureClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_PHOTO_PRINT_GPS, gpsOnPhotoCheck.isChecked());
    }

    public void timeOnPictureClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_PHOTO_PRINT_TIME, timeOnPhotoCheck.isChecked());
    }

    public void csvClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_EXPORT_CSV, csvCheck.isChecked());
    }

    public void jsonClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_EXPORT_JSON, jsonCheck.isChecked());
    }

    public void excelClick() {
        vdtsApplication.getPreferences().setBoolean(PREF_EXPORT_XLSX, excelCheck.isChecked());
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
            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Combined",()->setEntryMethod(METHOD_CHAINED))
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Step By Step",()->setEntryMethod(METHOD_STEP))
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Free Form",()->setEntryMethod(METHOD_FREE))
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Print Name", this::nameOnPictureClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Print Time", this::timeOnPictureClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Print GPS", this::gpsOnPictureClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("CSV", this::csvClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("JSON", this::jsonClick)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("XLSX", this::excelClick)
            );


            IristickSDK.addWindow(this.getLifecycle(), () -> {
                iristickHUD = new SettingsMenu.IristickHUD();
                return iristickHUD;
            });
        }
    }

    ////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_data_gathering_hud);

            //HUD Views
        }
    }
}
