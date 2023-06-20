package ca.vdts.voiceselect.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

/**
 * Configure values that can be entered into columns.
 */
public class ConfigColumnValuesActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnValuesActivity.class);

    private VDTSApplication vdtsApplication;
    private Column selectedColumn;
    private VDTSUser currentUser;
    private VDTSUser selectedUser;

    //Lists
    private final List<ColumnValue> columnValueList = new ArrayList<>();
    private final List<ColumnValueSpoken> columnValueSpokenList = new ArrayList<>();
    private final List<Column> columnList = new ArrayList<>();
    private final List<VDTSUser> userList = new ArrayList<>();
    private ArrayList<String> reservedWords;

    //Views
    private Button columnValueNewButton;
    private Button columnValueResetButton;
    private Button columnValueSaveButton;
    private Button columnValueDeleteButton;

    private EditText columnValueNameEditText;
    private EditText columnValueNameCodeEditText;
    private EditText columnValueExportCodeEditText;
    private EditText columnValueSpokenEditText;

    private Spinner columnValueColumnSpinner;
    private Spinner columnValueUserSpinner;

    private RecyclerView columnValueRecyclerView;

    private Button columnValueImportButton;
    private Button columnValueExportButton;

    //View Model - Adapters
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<Column> columnAdapter;
    private VDTSNamedAdapter<VDTSUser> userAdapter;
    private VDTSIndexedNamedAdapter<ColumnValue> columnValueAdapter;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private ConfigColumnValuesActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_column_values);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        selectedColumn = Column.COLUMN_NONE;
        currentUser = vdtsApplication.getCurrentUser();

        //Views
        columnValueNewButton = findViewById(R.id.columnValueNewButton);
        columnValueNewButton.setOnClickListener(v -> newColumnValueButtonOnClick());

        columnValueResetButton = findViewById(R.id.columnValueResetButton);
        columnValueResetButton.setOnClickListener(v -> resetColumnValueButtonOnClick());

        columnValueSaveButton = findViewById(R.id.columnValueSaveButton);
        columnValueSaveButton.setOnClickListener(v -> saveColumnValueButtonOnClick());

        columnValueDeleteButton = findViewById(R.id.columnValueDeleteButton);
        columnValueDeleteButton.setOnClickListener(v -> deleteColumnValueButtonOnClick());

        columnValueNameEditText = findViewById(R.id.columnValueNameEditText);
        columnValueNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(findViewById(R.id.columnValueNameEditText));
                vdtsApplication.displayToast(
                        this,
                        "Only an admin user can set a value name",
                        Toast.LENGTH_SHORT
                );
                columnValueNameEditText.clearFocus();
            }
        });
        columnValueNameCodeEditText = findViewById(R.id.columnValueNameCodeEditText);
        columnValueNameCodeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(findViewById(R.id.columnValueNameCodeEditText));
                vdtsApplication.displayToast(
                        this,
                        "Only an admin user can set a value abbreviation",
                        Toast.LENGTH_SHORT
                );
                columnValueNameCodeEditText.clearFocus();
            }
        });
        columnValueExportCodeEditText = findViewById(R.id.columnValueExportCodeEditText);
        columnValueExportCodeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(findViewById(R.id.columnValueExportCodeEditText));
                vdtsApplication.displayToast(
                        this,
                        "Only an admin user can set a value export code",
                        Toast.LENGTH_SHORT
                );
                columnValueExportCodeEditText.clearFocus();
            }
        });
        columnValueSpokenEditText = findViewById(R.id.columnValueSpokenEditText);

        columnValueImportButton = findViewById(R.id.columnValueImportButton);
        columnValueImportButton.setOnClickListener(v -> importButtonOnClick());
        columnValueExportButton = findViewById(R.id.columnValueExportButton);
        columnValueExportButton.setOnClickListener(v -> exportButtonOnClick());

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Column Spinner
        columnValueColumnSpinner = findViewById(R.id.columnValueColumnSpinner);

        columnAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                columnList
        );
        columnAdapter.setToStringFunction((column, integer) -> column.getName());
        columnValueColumnSpinner.setAdapter(columnAdapter);
        columnValueColumnSpinner.setOnItemSelectedListener(columnSpinnerListener);

        //User Spinner
        columnValueUserSpinner = findViewById(R.id.columnValueUserSpinner);

        userAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                userList
        );
        userAdapter.setToStringFunction((user, integer) -> user.getName());
        columnValueUserSpinner.setAdapter(userAdapter);
        columnValueUserSpinner.setOnItemSelectedListener(userSpinnerListener);

        //Recyclerview
        columnValueRecyclerView = findViewById(R.id.columnValueRecyclerView);

        /*
        //Observe/Update column value list
        vsViewModel.findAllColumnValuesLive().observe(this, columnValues -> {
            columnValueList.clear();
            columnValueList.addAll(columnValues);
        });
         */

        //Observe/Update column spoken list
        vsViewModel.findAllColumnValueSpokensLive().observe(this, columnValueSpokens -> {
            columnValueSpokenList.clear();
            columnValueSpokenList.addAll(columnValueSpokens);
        });

        columnValueRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );

        columnValueAdapter = new VDTSIndexedNamedAdapter<>(
                this,
                new VDTSClickListenerUtil(this::columnValueAdapterSelect, columnValueRecyclerView),
                columnValueList
        );

        columnValueRecyclerView.setAdapter(columnValueAdapter);

        reservedWords = new ArrayList<>(
                Arrays.asList(this.getResources().getStringArray(R.array.reserved_words))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeUserList();
    }

    //Initialize user list then column list
    private void initializeUserList() {
        if (currentUser.getAuthority() <= 0) {
            userList.clear();
            userList.add(currentUser);
            userAdapter.notifyDataSetChanged();
            columnValueUserSpinner.setSelection(0);
            initializeColumnList();
        } else {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                userList.clear();
                userList.addAll(vsViewModel.findAllActiveUsersExcludeDefault());
                userList.remove(VDTSUser.VDTS_USER_NONE);
                handler.post(() -> {
                    userAdapter.notifyDataSetChanged();
                    columnValueUserSpinner.setSelection(userList.indexOf(currentUser));
                    initializeColumnList();
                });
            });
        }
    }

    //Initialize column list then column value list
    private void initializeColumnList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnList.clear();
            columnList.addAll(vsViewModel.findAllActiveColumns());
            handler.post(() -> {
                columnAdapter.notifyDataSetChanged();
                columnValueColumnSpinner.setSelection(0);
                initializeColumnValueList();
            });
        });
    }

    private void initializeColumnValueList() {
        if (selectedColumn == null) {
            columnValueAdapter.setDataset(new ArrayList<>());
            columnValueAdapterSelect(-1);
        } else {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                columnValueList.clear();
                columnValueList.addAll(
                        vsViewModel.findAllActiveColumnValuesByColumn(selectedColumn.getUid())
                );
                handler.post(() -> {
                    columnValueAdapter.setDataset(columnValueList);
                    columnValueAdapterSelect(-1);
                    disableViews();
                });
            });
        }
    }

    /**
     * Disable views based on the current users authority
     */
    private void disableViews() {
        if (currentUser.getAuthority() <= 0) {
            //columnValueNewButton.setEnabled(false);
            //columnValueDeleteButton.setEnabled(false);

            //columnValueNameEditText.setEnabled(false);
            //columnValueNameCodeEditText.setEnabled(false);
            //columnValueExportCodeEditText.setEnabled(false);
            columnValueUserSpinner.setEnabled(false);

            //columnValueImportButton.setEnabled(false);
            //columnValueExportButton.setEnabled(false);

            /*if (columnList.size() == 0) {
                columnValueResetButton.setEnabled(false);
                columnValueSaveButton.setEnabled(false);
            }*/
        }
    }

    /**
     * Click listener for column spinner.
     */
    private final AdapterView.OnItemSelectedListener columnSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                                           long id) {
                    selectedColumn = (Column) parent.getItemAtPosition(position);
                    initializeColumnValueList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    /**
     * Click listener for user spinner.
     */
    private final AdapterView.OnItemSelectedListener userSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                                           long id) {
                    selectedUser = (VDTSUser) parent.getItemAtPosition(position);
                    initializeColumnList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    /**
     * Select the appropriate column from the recycler view.
     * @param index - Index of the column to select.
     */
    private void columnValueAdapterSelect(Integer index) {
        columnValueAdapter.setSelectedEntity(index);
        if (index >= 0) {
            final ColumnValue selectedColumnValue = columnValueAdapter.getSelectedEntity();

            if (selectedColumnValue != null) {
                columnValueNameEditText.setText(selectedColumnValue.getName());
                columnValueNameCodeEditText.setText(selectedColumnValue.getNameCode());
                columnValueExportCodeEditText.setText(selectedColumnValue.getExportCode());

                if (selectedUser == null) { selectedUser = currentUser; }
                final List<ColumnValueSpoken> spokenList = columnValueSpokenList.stream()
                        .filter(spoken -> spoken.getColumnValueID() == selectedColumnValue.getUid())
                        .filter(spoken -> spoken.getUserID() == selectedUser.getUid())
                        .collect(Collectors.toList());

                String spokens = "";
                for (ColumnValueSpoken columnValueSpoken : spokenList) {
                    if (!spokens.isEmpty()) {
                        spokens = spokens.concat(", ");
                    }
                    spokens = spokens.concat(columnValueSpoken.getSpoken());
                }

                columnValueSpokenEditText.setText(spokens);
            } else {
                columnValueNameEditText.setText("");
                columnValueNameCodeEditText.setText("");
                columnValueExportCodeEditText.setText("");
                columnValueSpokenEditText.setText("");
            }
        } else {
            columnValueNameEditText.setText("");
            columnValueNameCodeEditText.setText("");
            columnValueExportCodeEditText.setText("");
            columnValueSpokenEditText.setText("");
        }
    }

    private void clearSelection(){
        columnValueAdapterSelect(-1);
        resetFocus();
    }
    private void resetFocus(){
        if (currentUser.getAuthority() < 1){
            columnValueSpokenEditText.requestFocus();
        } else {
            columnValueNameEditText.requestFocus();
        }
    }

    private void newColumnValueButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            clearSelection();
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueNewButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create new columns",
                    Toast.LENGTH_SHORT
            );
        }
    }



    private void resetColumnValueButtonOnClick() {
        columnValueAdapterSelect(columnValueAdapter.getSelectedEntityIndex());
        resetFocus();
    }

    private void saveColumnValueButtonOnClick() {
        if (selectedColumn != null) {
            ColumnValue selectedColumnValue = columnValueAdapter.getSelectedEntity();

            if (selectedColumnValue != null) {
                //Update existing column value
                selectedColumnValue.setName(columnValueNameEditText.getText().toString().trim());
                selectedColumnValue.setNameCode(
                        columnValueNameCodeEditText.getText().toString().trim()
                );
                selectedColumnValue.setExportCode(
                        columnValueExportCodeEditText.getText().toString().trim()
                );
                selectedColumnValue.setColumnID(selectedColumn.getUid());
                selectedColumnValue.setUserID(selectedUser.getUid());

                if (isValidColumnValue(selectedColumnValue)) {
                    if (isValidColumnValueSpoken(selectedColumnValue)) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());
                        executor.execute(() -> {
                            vsViewModel.updateColumnValue(selectedColumnValue);
                            updateColumnValueSpokens(selectedColumnValue, false);
                            handler.post(() -> columnValueAdapter.updateSelectedEntity());
                        });
                    }
                    clearSelection();
                } else {
                    LOG.info("Invalid column value");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(columnValueSaveButton);
                    vdtsApplication.displayToast(
                            this,
                            "Invalid column value",
                            0
                    );
                    resetColumnValueButtonOnClick();
                }
            } else {
                //Create new column value
                ColumnValue columnValue = new ColumnValue(
                        selectedUser.getUid(),
                        selectedColumn.getUid(),
                        columnValueNameEditText.getText().toString().trim(),
                        columnValueNameCodeEditText.getText().toString().trim(),
                        columnValueExportCodeEditText.getText().toString().trim()
                );

                if (isValidColumnValue(columnValue)) {
                    if (isValidColumnValueSpoken(columnValue)) {
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        Handler handler = new Handler(Looper.getMainLooper());
                        executor.execute(() -> {
                            long uid = vsViewModel.insertColumnValue(columnValue);
                            columnValue.setUid(uid);
                            LOG.info("Added column: {}", columnValue.getName());
                            updateColumnValueSpokens(columnValue, true);
                            handler.post(() -> columnValueAdapter.addEntity(columnValue));
                        });
                    }
                    clearSelection();
                } else {
                    LOG.info("Invalid column value");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(columnValueSaveButton);
                    vdtsApplication.displayToast(
                            this,
                            "Invalid column value",
                            0
                    );
                }
            }
        } else {
            LOG.info("Select a column to create values");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueSaveButton);
            vdtsApplication.displayToast(
                    this,
                    "Select a column to create values",
                    0
            );
        }
    }

    private void deleteColumnValueButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            ColumnValue selectedColumnValue = columnValueAdapter.getSelectedEntity();
            if (selectedColumnValue != null) {
                selectedColumnValue.setActive(false);
                new Thread(() -> vsViewModel.updateColumnValue(selectedColumnValue)).start();
                new Thread(() -> {
                    final List<ColumnSpoken> spokenList =
                            vsViewModel.findAllColumnSpokensByColumn(selectedColumnValue.getUid());

                    //Convert list to array for deleteAllColumnSpokens query
                    final ColumnSpoken[] spokenArray = spokenList.toArray(new ColumnSpoken[0]);
                    vsViewModel.deleteAllColumnSpokens(spokenArray);
                }).start();

                columnValueAdapter.removeSelectedEntity();
                clearSelection();
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueDeleteButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can delete columns",
                    Toast.LENGTH_SHORT
            );
        }
    }

    public void importButtonOnClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueImportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can import columns",
                    Toast.LENGTH_SHORT
            );
        }
    }

    public void exportButtonOnClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export columns",
                    Toast.LENGTH_SHORT
            );
        }
    }

    /**
     * Check if the column value is unique, has a name, name code (abbreviated name),
     * and export code.
     * @param columnValue - The column value to be checked.
     * @return - True if valid.
     */
    private boolean isValidColumnValue(ColumnValue columnValue) {
        return !columnValue.getName().isEmpty() &&
                !columnValue.getNameCode().isEmpty() &&
                !columnValue.getExportCode().isEmpty() &&
                columnList.stream().noneMatch(column1 -> columnValue.getUid() != column1.getUid() &&
                        (column1.getName().equalsIgnoreCase(columnValue.getName()) ||
                                column1.getNameCode().equalsIgnoreCase(columnValue.getName()) ||
                                column1.getExportCode().equalsIgnoreCase(columnValue.getExportCode())
                        )
                );

    }

    /**
     * Check if the column value's spokens exist, are unique, and do not contain reserved words.
     * @param columnValue - The column value to be checked.
     * @return - True if valid.
     */
    private boolean isValidColumnValueSpoken(ColumnValue columnValue) {
        if (!columnValueSpokenEditText.getText().toString().isEmpty()) {
            final List<String> spokenList = getFormattedColumnValueSpokenList();

            for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
                if (columnValueSpoken.getColumnValueID() != columnValue.getUid()) {
                    for (String spoken: spokenList) {
                        if (columnValueSpoken.getSpoken().equalsIgnoreCase(spoken)) {
                            LOG.info("Column value's spokens must be unique");
                            YoYo.with(Techniques.Shake)
                                    .duration(SHAKE_DURATION)
                                    .repeat(SHAKE_REPEAT)
                                    .playOn(columnValueSpokenEditText);
                            vdtsApplication.displayToast(
                                    this,
                                    "Column value's spokens must be unique",
                                    0
                            );
                            return false;
                        }

                        for (String reserved : reservedWords) {
                            if (spoken.toLowerCase().contains(reserved.toLowerCase())) {
                                LOG.info("Column value's spokens contain a reserved word");
                                YoYo.with(Techniques.Shake)
                                        .duration(SHAKE_DURATION)
                                        .repeat(SHAKE_REPEAT)
                                        .playOn(columnValueSpokenEditText);
                                vdtsApplication.displayToast(
                                        this,
                                        "Column value's spokens contain a reserved word",
                                        0
                                );
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } else {
            LOG.info("Column value must have a spoken term");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueSpokenEditText);
            vdtsApplication.displayToast(
                    this,
                    "Column value must have a spoken term",
                    0
            );
            return false;
        }
    }

    private void updateColumnValueSpokens(ColumnValue columnValue, boolean isNew) {
        final List<String> spokenList = getFormattedColumnValueSpokenList();

        if (isNew) {
            for (VDTSUser user : userList) {
                for (String spoken : spokenList) {
                    new Thread(
                            () -> vsViewModel.insertColumnValueSpoken(
                                    new ColumnValueSpoken(
                                            user.getUid(),
                                            columnValue.getUid(),
                                            spoken
                                    )
                            )
                    ).start();
                }
            }
        } else {
            final List<ColumnValueSpoken> existingSpokenList = columnValueSpokenList.stream()
                    .filter(spoken -> spoken.getColumnValueID() == columnValue.getUid())
                    .filter(spoken -> spoken.getUserID() == selectedUser.getUid())
                    .collect(Collectors.toList());

            //Delete spokens that no longer exist
            for (ColumnValueSpoken columnValueSpoken : existingSpokenList) {
                if (spokenList.stream().noneMatch(spoken ->
                        spoken.equalsIgnoreCase(columnValueSpoken.getSpoken()))) {
                    new Thread(
                            () -> vsViewModel.deleteColumnValueSpoken(columnValueSpoken)
                    ).start();
                }
            }

            //Insert new spokens
            if (selectedUser == null) { selectedUser = currentUser; }
            for (String spoken : spokenList) {
                if (existingSpokenList.stream()
                        .noneMatch(oldSpoken -> oldSpoken.getSpoken().equalsIgnoreCase(spoken))) {
                    new Thread(
                            () -> vsViewModel.insertColumnValueSpoken(
                                    new ColumnValueSpoken(
                                            selectedUser.getUid(),
                                            columnValue.getUid(),
                                            spoken
                                    )
                            )
                    ).start();
                }
            }
        }
    }

    /**
     * Create a comma separated list from strings in the spoken text field.
     * @return - A comma separated list of strings.
     */
    private List<String> getFormattedColumnValueSpokenList() {
        return Arrays.stream(
                        columnValueSpokenEditText.getText().toString()
                                .replaceAll(" ,", ",")
                                .replaceAll(", ", ",")
                                .trim()
                                .split(",")
                ).distinct()
                .collect(Collectors.toList());
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
            setContentView(R.layout.activity_config_hud);

            configOnDeviceText = findViewById(R.id.configHUDText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_hud_text);
        }
    }
}