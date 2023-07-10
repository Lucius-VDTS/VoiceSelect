package ca.vdts.voiceselect.activities;

import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_CHAINED;
import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_FREE;
import static ca.vdts.voiceselect.library.VDTSApplication.METHOD_STEP;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_ENTRY_METHOD;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_GPS;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_NAME;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_PHOTO_PRINT_TIME;
import static ca.vdts.voiceselect.library.VDTSApplication.SELECT_FOLDER;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.FileUtil;
import ca.vdts.voiceselect.files.Importer;
import ca.vdts.voiceselect.library.VDTSApplication;

public class SettingsActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(SettingsActivity.class);
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
        timeOnPhotoCheck.setOnClickListener(v -> timeOnPictureClick());
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
        nameOnPhotoCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_NAME, false)
        );
        gpsOnPhotoCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_GPS, false)
        );
        timeOnPhotoCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_PHOTO_PRINT_TIME, false)
        );

        csvCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV, false)
        );
        jsonCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON, false)
        );
        excelCheck.setChecked(
                vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX, true)
        );

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
        vdtsApplication.getPreferences().setBoolean(
                PREF_PHOTO_PRINT_NAME,
                nameOnPhotoCheck.isChecked()
        );
    }

    public void gpsOnPictureClick() {
        vdtsApplication.getPreferences().setBoolean(
                PREF_PHOTO_PRINT_GPS,
                gpsOnPhotoCheck.isChecked()
        );
    }

    public void timeOnPictureClick() {
        vdtsApplication.getPreferences().setBoolean(
                PREF_PHOTO_PRINT_TIME,
                timeOnPhotoCheck.isChecked()
        );
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
                    "Only an admin user can export columns"
            );
        } else {
           showImportDialog();
        }
    }

    private void showImportDialog() {
        LOG.info("Showing Choice Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Settings");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_yes_no,
                null
        );
        builder.setView(customLayout);
        TextView label = customLayout.findViewById(R.id.mainLabel);
        label.setText(R.string.import_dialogue_label);
        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);
        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;
        yesButton.setOnClickListener(v -> {
            finalDialog.dismiss();
            openFilePicker();
        });

        noButton.setOnClickListener(v -> finalDialog.dismiss());
    }

    public void onExportClick() {
        if (vdtsApplication.getCurrentUser().getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(settingsExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export columns"
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
                vdtsApplication.displayToast(
                        this,
                        "Options exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting options"
                );
            }
        }
    }

    public void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, SELECT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FOLDER) {
            if (resultCode == RESULT_OK && data != null) {

                Uri originalUri = data.getData();

                try {
                    File file = FileUtil.from(this,originalUri );
                    LOG.debug("file", "File...:::: uti - "+file .getPath()+" file -" + file + " : " + file .exists());

                    if (file.exists()) {
                        final VSViewModel viewModel = new ViewModelProvider(this).get(
                                VSViewModel.class
                        );
                        final Importer importer = new Importer(
                                viewModel,
                                this,
                                vdtsApplication
                        );
                        if (importer.importOptions(file)) {
                            updateControls();

                            vdtsApplication.displayToast(
                                    this,
                                    "Settings imported successfully"
                            );
                        } else {
                            vdtsApplication.displayToast(
                                    this,
                                    "Error importing settings"
                            );
                        }
                    } else {
                        vdtsApplication.displayToast(
                                this,
                                "Setting file not found"
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    vdtsApplication.displayToast(
                            this,
                            "Settings file not found"
                    );
                }
            }
        }
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        initializeIristick();
    }

    /**
     * Initialize Iristick when connected.
     */
    private void initializeIristick() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new SettingsActivity.IristickHUD();
            return iristickHUD;
        });

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
            setContentView(R.layout.activity_settings_hud);
        }
    }
}
