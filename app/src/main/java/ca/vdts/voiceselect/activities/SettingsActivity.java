package ca.vdts.voiceselect.activities;

import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_LAYOUT;
import static ca.vdts.voiceselect.library.VDTSApplication.EXPORT_FILE_OPTIONS;
import static ca.vdts.voiceselect.library.VDTSApplication.FILE_EXTENSION_VDTS;
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
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import java.io.File;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.Importer;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.utilities.VDTSNotificationUtil;

public class SettingsActivity extends AppCompatActivity implements IRIListener {
    private VDTSApplication vdtsApplication;

    //Views
    private SwitchCompat nameOnPhotoCheck;
    private SwitchCompat  gpsOnPhotoCheck;
    private SwitchCompat  timeOnPhotoCheck;

    private SwitchCompat  csvCheck;
    private SwitchCompat  jsonCheck;
    private SwitchCompat excelCheck;

    private RadioGroup entryMethodGroup;

    private Button settingsImportButton;
    private Button settingsExportButton;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private SettingsActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        vdtsApplication = (VDTSApplication) getApplication();

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
        settingsImportButton = findViewById(R.id.prefImportButton);
        settingsImportButton.setOnClickListener(v -> onImportClick());
        settingsExportButton = findViewById(R.id.prefExportButton);
        settingsExportButton.setOnClickListener(v -> onExportClick());
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateControls();
    }

    private void updateControls() {
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

    public void onImportClick() {
        if (vdtsApplication.getCurrentUser().getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(settingsImportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export columns",
                    Toast.LENGTH_SHORT
            );
        } else {
           Uri uri = FileProvider.getUriForFile(
                    this,
                    "ca.vdts.voiceselect",
                    new File(Environment.getExternalStorageDirectory().toString() +
                            "/Documents/VoiceSelect"+EXPORT_FILE_OPTIONS
                            .concat(FILE_EXTENSION_VDTS))
            );
            if (uri != null && uri.getPath() != null) {
                final VSViewModel viewModel = new ViewModelProvider(this).get(VSViewModel.class);
                final Importer importer = new Importer(
                        viewModel,
                        this,
                        vdtsApplication
                );
                if (importer.importOptions(uri)) {
                    // adapterSelect(-1);

                    vdtsApplication.displayToast(this,"Settings imported successfully",Toast.LENGTH_SHORT);
                } else {
                    vdtsApplication.displayToast(this,"Error importing settings",Toast.LENGTH_SHORT);
                }
            } else {
                vdtsApplication.displayToast(this,"Error importing settings",Toast.LENGTH_SHORT);
            }
        }
    }

    public void onExportClick() {
        if (vdtsApplication.getCurrentUser().getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(settingsExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export columns",
                    Toast.LENGTH_SHORT
            );
        } else {
            final VSViewModel viewModel = new ViewModelProvider(this).get(VSViewModel.class);
            //saver = Saver.createSaver(ONEDRIVE_APP_ID);
            final Exporter exporter = new Exporter(
                    viewModel,
                    vdtsApplication,
                    this
                    //saver
            );
            if (exporter.exportOptions()) {
                vdtsApplication.displayToast(this,"Options exported successfully",Toast.LENGTH_SHORT);
            } else {
                vdtsApplication.displayToast(this,"Error exporting options",Toast.LENGTH_SHORT);
            }
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
                iristickHUD = new SettingsActivity.IristickHUD();
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
