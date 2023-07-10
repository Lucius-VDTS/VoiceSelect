package ca.vdts.voiceselect.activities.configure;

import static org.apache.poi.ss.util.CellReference.convertColStringToIndex;
import static org.apache.poi.ss.util.CellReference.convertNumToColString;
import static ca.vdts.voiceselect.library.VDTSApplication.SELECT_FOLDER;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;
import static ca.vdts.voiceselect.library.utilities.VDTSBNFUtil.toPhonetic;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.FileUtil;
import ca.vdts.voiceselect.files.Importer;
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
    private Button numRangeButton;
    private Button letterRangeButton;

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
    private ConfigColumnValuesActivity.IristickHUD iristickHUD;

    //lock to prevent concurent list filling issues
    private ReentrantLock adapterLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_column_values);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        selectedColumn = Column.COLUMN_NONE;
        currentUser = vdtsApplication.getCurrentUser();

        adapterLock = new ReentrantLock();

        //Views
        columnValueNewButton = findViewById(R.id.columnValueNewButton);
        columnValueNewButton.setOnClickListener(v -> newColumnValueButtonOnClick());

        columnValueResetButton = findViewById(R.id.columnValueResetButton);
        columnValueResetButton.setOnClickListener(v -> resetColumnValueButtonOnClick());

        columnValueSaveButton = findViewById(R.id.columnValueSaveButton);
        columnValueSaveButton.setOnClickListener(v -> saveColumnValueButtonOnClick(false));

        columnValueDeleteButton = findViewById(R.id.columnValueDeleteButton);
        columnValueDeleteButton.setOnClickListener(v -> deleteColumnValueButtonOnClick());

        numRangeButton = findViewById(R.id.valueNumRangeButton);
        numRangeButton.setOnClickListener(v -> onValueNumberRangeClick());

        letterRangeButton = findViewById(R.id.valueLetterRangeButton);
        letterRangeButton.setOnClickListener(v -> onValueLetterRangeClick());

        columnValueNameEditText = findViewById(R.id.columnValueNameEditText);
        columnValueNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(findViewById(R.id.columnValueNameEditText));
                vdtsApplication.displayToast(
                        this,
                        "Only an admin user can set a value name"
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
                        "Only an admin user can set a value abbreviation"
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
                        "Only an admin user can set a value export code"
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
                adapterLock.lock();
                columnValueList.clear();
                columnValueList.addAll(
                        vsViewModel.findAllActiveColumnValuesByColumn(selectedColumn.getUid())
                );
                handler.post(() -> {
                    columnValueAdapter.setDataset(columnValueList);
                    columnValueAdapterSelect(-1);
                    disableViews();
                });
                adapterLock.unlock();
            });
        }
    }

    /**
     * Disable views based on the current users authority
     */
    private void disableViews() {
        if (currentUser.getAuthority() <= 0) {
            columnValueUserSpinner.setEnabled(false);
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
                    "Only an admin user can create new Values"
            );
        }
    }

    private void resetColumnValueButtonOnClick() {
        columnValueAdapterSelect(columnValueAdapter.getSelectedEntityIndex());
        resetFocus();
    }

    private void saveColumnValueButtonOnClick(boolean automated) {
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
                    if (isValidColumnValueSpoken(selectedColumnValue,automated,false)) {
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
                    if (!automated) {
                        YoYo.with(Techniques.Shake)
                                .duration(SHAKE_DURATION)
                                .repeat(SHAKE_REPEAT)
                                .playOn(columnValueSaveButton);
                        vdtsApplication.displayToast(
                                this,
                                "Invalid column value"
                        );
                        resetColumnValueButtonOnClick();
                    }
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
                    if (isValidColumnValueSpoken(columnValue,automated,true)) {
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
                    if (!automated) {
                        YoYo.with(Techniques.Shake)
                                .duration(SHAKE_DURATION)
                                .repeat(SHAKE_REPEAT)
                                .playOn(columnValueSaveButton);
                        vdtsApplication.displayToast(
                                this,
                                "Invalid column value"
                        );
                    }
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
                    "Select a column to create values"
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
                    "Only an admin user can delete columns"
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
                    "Only an admin user can import columns"
            );
        }else {
            showImportDialog();
        }
    }

    private void showImportDialog() {
        LOG.info("Showing Choice Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Columns and Values");
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

    public void exportButtonOnClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export columns"
            );
        } else {
            LOG.debug("Starting export");
            final Exporter exporter = new Exporter(
                    vsViewModel,
                    vdtsApplication,
                    this
            );
            if (exporter.exportSetup()) {
                vdtsApplication.displayToast(
                        this,
                        "Columns exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting columns"
                );
            }
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
                columnValueList.stream().noneMatch(column1 -> columnValue.getUid() != column1.getUid() &&
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
    private boolean isValidColumnValueSpoken(ColumnValue columnValue,boolean automated, boolean newValue) {
        if (!columnValueSpokenEditText.getText().toString().isEmpty()) {
            final List<String> spokenList = getFormattedColumnValueSpokenList();


            final List<ColumnValueSpoken> existingSpokens = new ArrayList<>();
            if (newValue) {
                Thread thread = new Thread(() -> existingSpokens.addAll(
                        vsViewModel.findAllColumnValueSpokensByColumn(columnValue.getColumnID()))
                );
                thread.start();
                try {
                    thread.join();
                    LOG.info("{} spokenValues found", existingSpokens.size());
                } catch (InterruptedException e) {
                    LOG.error("ValueSpoken Interrupted: ", e);
                }
            } else {
                Thread thread = new Thread(
                        () -> existingSpokens.addAll(
                                vsViewModel.findAllColumnValueSpokensByColumnAndUser(
                                        columnValue.getColumnID(),
                                        selectedUser.getUid()
                                )
                        )
                );
                thread.start();
                try {
                    thread.join();
                    LOG.info("{} spokenValues found", existingSpokens.size());
                } catch (InterruptedException e) {
                    LOG.error("ValueSpoken Interrupted: ", e);
                }
            }

            for (ColumnValueSpoken columnValueSpoken :  existingSpokens) {
                if (columnValueSpoken.getColumnValueID() != columnValue.getUid()) {
                    for (String spoken: spokenList) {
                        if (columnValueSpoken.getSpoken().equalsIgnoreCase(spoken)) {
                            LOG.info("Column value's spokens must be unique");
                            if (!automated) {
                                YoYo.with(Techniques.Shake)
                                        .duration(SHAKE_DURATION)
                                        .repeat(SHAKE_REPEAT)
                                        .playOn(columnValueSpokenEditText);
                                vdtsApplication.displayToast(
                                        this,
                                        "Column value's spokens must be unique"
                                );
                            }
                            return false;
                        }

                        for (String reserved : reservedWords) {
                            if (spoken.toLowerCase().contains(reserved.toLowerCase())) {
                                LOG.info("Column value's spokens contain a reserved word");
                                if (!automated) {
                                    YoYo.with(Techniques.Shake)
                                            .duration(SHAKE_DURATION)
                                            .repeat(SHAKE_REPEAT)
                                            .playOn(columnValueSpokenEditText);
                                    vdtsApplication.displayToast(
                                            this,
                                            "Column value's spokens contain a reserved word"
                                    );
                                }
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
                    "Column value must have a spoken term"
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

    public void onValueNumberRangeClick() {
        if (currentUser.getAuthority() > 0) {
            if (selectedColumn != null) {
                showNumberDialog();
            } else {
                LOG.info("Select a column to create values");
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(columnValueSaveButton);
                vdtsApplication.displayToast(
                        this,
                        "Select a column to create values"
                );
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueNewButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create new Values"
            );
        }
    }

    private void showNumberDialog() {
        LOG.info("Showing Number Dialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(vdtsApplication.getResources().getString(R.string.column_value_number_range_label));
        final View customLayout = getLayoutInflater()
                .inflate(R.layout.dialogue_fragment_integer, null);
        builder.setView(customLayout);

        EditText minView  = customLayout.findViewById(R.id.minValue);

        EditText maxView = customLayout.findViewById(R.id.maxValue);

        EditText multiView = customLayout.findViewById(R.id.multipleValue);

        minView.setText(String.valueOf(1));
        maxView.setText(String.valueOf(10));
        multiView.setText(String.valueOf(1));

        builder.setPositiveButton(vdtsApplication.getResources().getString(R.string.column_values_enter_label), (dialogInterface, i) -> {
            saveNumberRange(minView.getText().toString(),maxView.getText().toString(),multiView.getText().toString());
        });
        builder.setNegativeButton(vdtsApplication.getResources().getString(R.string.column_values_cancel_label), (dialogInterface, i) -> {

        });

        AlertDialog dialog = builder.create();
        assert dialog.getWindow() != null;
        dialog.show();
    }

    private void saveNumberRange(String min, String max, String multiple){
        if (!min.isEmpty() && !max.isEmpty() && !multiple.isEmpty()){

            int start = Integer.parseInt(min);
            int end = Integer.parseInt(max);
            if (start > end){
                int temp = start;
                start = end;
                end = temp;
            }
            int multi = Integer.parseInt(multiple);

            for (int i = start; i <= end ; i = i+multi){
                columnValueAdapterSelect(-1);
                columnValueNameEditText.setText(String.format(Locale.ROOT,"%d",i));
                columnValueNameCodeEditText.setText(String.format(Locale.ROOT,"%d",i));
                columnValueExportCodeEditText.setText(String.format(Locale.ROOT,"%d",i));
                columnValueSpokenEditText.setText(String.format(Locale.ROOT,"%d",i));
                saveColumnValueButtonOnClick(true);
            }
            columnValueAdapterSelect(-1);
        } else {
            vdtsApplication.displayToast(
                    this,
                    "Invalid number range");
        }
    }

    public void onValueLetterRangeClick() {
        if (currentUser.getAuthority() > 0) {
            if (selectedColumn != null) {
                showLetterDialog();
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnValueNewButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create new Values"
            );
        }
    }

    private void showLetterDialog() {
        LOG.info("Showing Letter Dialog");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle( vdtsApplication.getResources().getString(R.string.column_value_letter_range_label));
        final View customLayout = getLayoutInflater()
                .inflate(R.layout.dialogue_fragment_letter, null);
        builder.setView(customLayout);

        EditText minView  = customLayout.findViewById(R.id.minValue);

        EditText maxView = customLayout.findViewById(R.id.maxValue);

        minView.setText("A");
        maxView.setText("Z");

        builder.setPositiveButton(vdtsApplication.getResources().getString(R.string.column_values_enter_label), (dialogInterface, i) -> {
            saveLetterRange(minView.getText().toString(),maxView.getText().toString());
        });
        builder.setNegativeButton(vdtsApplication.getResources().getString(R.string.column_values_cancel_label), (dialogInterface, i) -> {

        });

        AlertDialog dialog = builder.create();
        assert dialog.getWindow() != null;
        dialog.show();
    }

    private void saveLetterRange(String min, String max){
        if (!min.isEmpty() && !max.isEmpty()){

            int start = convertColStringToIndex(min);
            int end = convertColStringToIndex(max);
            if (start > end){
                int temp = start;
                start = end;
                end = temp;
            }

            for (int i = start; i <= end ; i++){
                columnValueAdapterSelect(-1);
                String value = convertNumToColString(i);
                columnValueNameEditText.setText(value);
                columnValueNameCodeEditText.setText(value);
                columnValueExportCodeEditText.setText(value);

                char[] chars = value.toCharArray();
                String spacedValue = "";
                for (char aChar : chars) {
                    spacedValue = spacedValue.concat(toPhonetic(Character.toString(aChar), this).concat(" "));
                }
                spacedValue = spacedValue.trim();
                columnValueSpokenEditText.setText(spacedValue);
                saveColumnValueButtonOnClick(true);
            }
            columnValueAdapterSelect(-1);
        } else {
            vdtsApplication.displayToast(
                    this,
                    "Invalid letter range");
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
                        final Importer importer = new Importer(
                                vsViewModel,
                                this,
                                vdtsApplication
                        );
                        if (importer.importSetup(file)) {
                            columnValueAdapterSelect(-1);

                            vdtsApplication.displayToast(
                                    this,
                                    "Setup imported successfully"
                            );

                            initializeColumnList();
                        } else {
                            vdtsApplication.displayToast(
                                    this,
                                    "Error importing Setup"
                            );
                        }
                    } else {
                        vdtsApplication.displayToast(
                                this,
                                "Setup file not found"
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    vdtsApplication.displayToast(
                            this,
                            "Setup file not found"
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
     * Initialize Iristick HUD and voice commands when connected.
     */
    private void initializeIristick() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new ConfigColumnValuesActivity.IristickHUD();
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