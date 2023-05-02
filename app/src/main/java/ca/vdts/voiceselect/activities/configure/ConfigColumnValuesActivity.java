package ca.vdts.voiceselect.activities.configure;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Configure values that can be entered into columns.
 */
public class ConfigColumnValuesActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnValuesActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;

    //Views
    private Button newColumnValueButton;
    private Button resetColumnValueButton;
    private Button saveColumnValueButton;
    private Button deleteColumnValueButton;

    private EditText columnValueNameEditText;
    private EditText columnValueNameCodeEditText;
    private EditText columnValueExportCodeEditText;
    private EditText columnValueSpokenEditText;

    private Spinner columnValueColumnSpinner;
    private Spinner columnValueUserSpinner;

    private Button importColumnValueButton;
    private Button exportColumnValueButton;

    //Recycler View
    private VSViewModel vsViewModel;
    private VDTSIndexedNamedAdapter<Column> columnValueAdapter;
    private RecyclerView columnValueRecyclerView;
    private final List<Column> columnValueList = new ArrayList<>();

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private ConfigColumnValuesActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_column_values);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();

//        //Views
//        newColumnValueButton = findViewById(R.id.columnValueNewButton);
//        newColumnValueButton.setOnClickListener(v -> newColumnValueButtonOnClick());
//
//        resetColumnValueButton = findViewById(R.id.columnValueResetButton);
//        resetColumnValueButton.setOnClickListener(v -> resetColumnValueButtonOnClick());
//
//        saveColumnValueButton = findViewById(R.id.columnValueSaveButton);
//        saveColumnValueButton.setOnClickListener(v -> saveColumnValueButtonOnClick());
//
//        deleteColumnValueButton = findViewById(R.id.columnValueDeleteButton);
//        deleteColumnValueButton.setOnClickListener(v -> deleteColumnValueButtonOnClick());

        columnValueNameEditText = findViewById(R.id.columnValueNameEditText);
        columnValueNameCodeEditText = findViewById(R.id.columnValueNameCodeEditText);
        columnValueExportCodeEditText = findViewById(R.id.columnValueExportCodeEditText);
        columnValueSpokenEditText = findViewById(R.id.columnValueSpokenEditText);

        columnValueColumnSpinner = findViewById(R.id.columnValueColumnSpinner);

        columnValueUserSpinner = findViewById(R.id.columnValueUserSpinner);
        if (currentUser.getAuthority() <= 0) {
            columnValueUserSpinner.setVisibility(View.GONE);
        }

        importColumnValueButton = findViewById(R.id.columnValueImportButton);
        exportColumnValueButton = findViewById(R.id.columnValueExportButton);

        //Recyclerview
        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);
        //todo
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
            IristickSDK.addWindow(this.getLifecycle(), () -> {
                iristickHUD = new ConfigColumnValuesActivity.IristickHUD();
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
            setContentView(R.layout.activity_config_on_device_hud);

            configOnDeviceText = findViewById(R.id.configOnDeviceText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_on_device_text);
        }
    }
}
