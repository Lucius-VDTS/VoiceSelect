package ca.vdts.voiceselect.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SELECT_FOLDER;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;
import static ca.vdts.voiceselect.library.utilities.VDTSToolUtil.showKeyboard;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.FileUtil;
import ca.vdts.voiceselect.files.Importer;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

/**
 * Configure column parameters.
 */
public class ConfigColumnsActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private VDTSUser selectedUser;

    //Lists
    private final List<Column> columnList = new ArrayList<>();
    private final List<ColumnSpoken> columnSpokenList = new ArrayList<>();
    private final List<VDTSUser> userList = new ArrayList<>();
    private ArrayList<String> reservedWords;

    //Views
    private Button columnNewButton;
    private Button columnResetButton;
    private Button columnSaveButton;
    private Button columnDeleteButton;

    private EditText columnNameEditText;
    private EditText columnNameCodeEditText;
    private EditText columnExportCodeEditText;
    private EditText columnSpokenEditText;

    private Spinner columnUserSpinner;

    private Button columnImportButton;
    private Button columnExportButton;

    //Recycler View - User Spinner
    private VSViewModel vsViewModel;
    private VDTSIndexedNamedAdapter<Column> columnAdapter;
    private VDTSNamedAdapter<VDTSUser> userAdapter;
    private RecyclerView columnRecyclerView;

    //Iristick Components
    private ConfigColumnsActivity.IristickHUD iristickHUD;

    //Prevent asynchronous list filling issue
    private ReentrantLock adapterLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_columns);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        adapterLock = new ReentrantLock();

        //Views
        columnNewButton = findViewById(R.id.layoutNewButton);
        columnNewButton.setOnClickListener(v -> newColumnButtonOnClick());

        columnResetButton = findViewById(R.id.layoutResetButton);
        columnResetButton.setOnClickListener(v -> resetColumnButtonOnClick());

        columnSaveButton = findViewById(R.id.layoutSaveButton);
        columnSaveButton.setOnClickListener(v -> saveColumnButtonOnClick());

        columnDeleteButton = findViewById(R.id.layoutDeleteButton);
        columnDeleteButton.setOnClickListener(v -> deleteColumnButtonOnClick());

        columnNameEditText = findViewById(R.id.columnNameEditText);
        columnNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(columnNameEditText);
                vdtsApplication.displayToast(
                        this,
                        "Only admin users can set column name"
                );
                columnNameEditText.clearFocus();
            }
        });

        columnNameCodeEditText = findViewById(R.id.columnNameCodeEditText);
        columnNameCodeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(columnNameCodeEditText);
                vdtsApplication.displayToast(
                        this,
                        "Only admin users can set column abbreviation"
                );
                columnNameCodeEditText.clearFocus();
            }
        });

        columnExportCodeEditText = findViewById(R.id.columnExportCodeEditText);
        columnExportCodeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(columnExportCodeEditText);
                vdtsApplication.displayToast(
                        this,
                        "Only an admin can set a column export code"
                );
                columnExportCodeEditText.clearFocus();
            }
        });

        columnSpokenEditText = findViewById(R.id.layoutNameEditText);

        columnImportButton = findViewById(R.id.layoutImportButton);
        columnImportButton.setOnClickListener(v -> importButtonOnClick());

        columnExportButton = findViewById(R.id.layoutExportButton);
        columnExportButton.setOnClickListener(v -> exportButtonOnClick());

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //User Spinner
        columnUserSpinner = findViewById(R.id.layoutSpinner);

        userAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                userList
        );
        userAdapter.setToStringFunction((user, integer) -> user.getName());

        columnUserSpinner.setAdapter(userAdapter);
        columnUserSpinner.setOnItemSelectedListener(userSpinnerListener);

        //Recyclerview
        columnRecyclerView = findViewById(R.id.columnRecyclerView);

        //Observe/Update column list
        vsViewModel.findAllColumnsLive().observe(this, columns -> {
            columnList.clear();
            columnList.addAll(columns);
        });

        //Observe/Update column spoken list
        vsViewModel.findAllColumnSpokensLive().observe(this, columnSpokens -> {
            columnSpokenList.clear();
            columnSpokenList.addAll(columnSpokens);
        });

        columnRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );

        columnAdapter = new VDTSIndexedNamedAdapter<>(
                this,
                new VDTSClickListenerUtil(this::columnAdapterSelect, columnRecyclerView),
                columnList
        );

        columnRecyclerView.setAdapter(columnAdapter);

        reservedWords = new ArrayList<>(
                Arrays.asList(this.getResources().getStringArray(R.array.reserved_words))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeUserList();
        disableViews();
    }

    //Initialize user list then column list
    private void initializeUserList() {
        if (currentUser.getAuthority() <= 0) {
            userList.clear();
            userList.add(currentUser);
            userAdapter.notifyDataSetChanged();
            columnUserSpinner.setSelection(0);
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
                    columnUserSpinner.setSelection(userList.indexOf(currentUser));
                    initializeColumnList();
                });
            });
        }
    }

    private void initializeColumnList() {
        if (selectedUser == null) {
            columnAdapter.setDataset(new ArrayList<>());
            columnAdapterSelect(-1);
        } else {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                adapterLock.lock();
                columnList.clear();
                columnList.addAll(vsViewModel.findAllActiveColumns());
                columnList.remove(Column.COLUMN_NONE);
                handler.post(() -> {
                    columnAdapter.setDataset(columnList);
                    columnAdapterSelect(-1);
                });
                adapterLock.unlock();
            });
        }
    }

    private void disableViews() {
        if (currentUser.getAuthority() <= 0) {
            columnUserSpinner.setEnabled(false);
        }
    }

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
    private void columnAdapterSelect(Integer index) {
        columnAdapter.setSelectedEntity(index);
        if (index >= 0) {
            final Column selectedColumn = columnAdapter.getSelectedEntity();

            if (selectedColumn != null) {
                columnNameEditText.setText(selectedColumn.getName());
                columnNameCodeEditText.setText(selectedColumn.getNameCode());
                columnExportCodeEditText.setText(selectedColumn.getExportCode());

                final List<ColumnSpoken> spokenList = columnSpokenList.stream()
                        .filter(spoken -> spoken.getColumnID() == selectedColumn.getUid())
                        .filter(spoken -> spoken.getUserID() == selectedUser.getUid())
                        .collect(Collectors.toList());

                String spokens = "";
                for (ColumnSpoken columnSpoken : spokenList) {
                    if (!spokens.isEmpty()) {
                        spokens = spokens.concat(", ");
                    }
                    spokens = spokens.concat(columnSpoken.getSpoken());
                }

                columnSpokenEditText.setText(spokens);
            } else {
                columnNameEditText.setText("");
                columnNameCodeEditText.setText("");
                columnExportCodeEditText.setText("");
                columnSpokenEditText.setText("");
            }
        } else {
            columnNameEditText.setText("");
            columnNameCodeEditText.setText("");
            columnExportCodeEditText.setText("");
            columnSpokenEditText.setText("");
        }
    }

    private void clearSelection(){
        columnAdapterSelect(-1);
        resetFocus();
    }
    private void resetFocus(){
        if (currentUser.getAuthority() < 1){
            columnSpokenEditText.requestFocus();
            showKeyboard(columnSpokenEditText,this);
        } else {
            columnNameEditText.requestFocus();
            showKeyboard(columnNameEditText,this);
        }
    }

    private void newColumnButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            clearSelection();
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnNewButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create new columns"
            );
        }
    }

    private void resetColumnButtonOnClick() {
        columnAdapterSelect(columnAdapter.getSelectedEntityIndex());
        resetFocus();
    }

    private void saveColumnButtonOnClick() {
        Column selectedColumn = columnAdapter.getSelectedEntity();
        List<String> spokenList = getFormattedColumnSpokenList(
                columnSpokenEditText.getText().toString(),
                columnNameEditText.getText().toString()
        );

        if (selectedColumn != null) {
            //Update existing column
            Column workColumn = new Column(selectedColumn);
            workColumn.setName(columnNameEditText.getText().toString().trim());
            workColumn.setNameCode(columnNameCodeEditText.getText().toString().trim());
            workColumn.setExportCode(columnExportCodeEditText.getText().toString().trim());

            if (isValidColumn(workColumn)) {
               if (isValidColumnSpoken(workColumn, spokenList)) {
                   ExecutorService executor = Executors.newSingleThreadExecutor();
                   Handler handler = new Handler(Looper.getMainLooper());
                   executor.execute(() -> {
                       vsViewModel.updateColumn(workColumn);
                       updateColumnSpokens(workColumn, spokenList, false);
                       handler.post(() -> columnAdapter.updateEntity(workColumn));
                   });
               } else {
                   LOG.info("Invalid column spoken");
                   YoYo.with(Techniques.Shake)
                           .duration(SHAKE_DURATION)
                           .repeat(SHAKE_REPEAT)
                           .playOn(columnSaveButton);
                   vdtsApplication.displayToast(
                           this,
                           "Invalid column spoken"
                   );
               }
                clearSelection();
            } else {
                resetColumnButtonOnClick();
            }
        } else {
            //Create new column
            Column column = new Column(
                    selectedUser.getUid(),
                    columnNameEditText.getText().toString().trim(),
                    columnNameCodeEditText.getText().toString().trim(),
                    columnExportCodeEditText.getText().toString().trim()
            );

            if (isValidColumn(column)) {
                if (isValidColumnSpoken(column, spokenList)) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        long uid = vsViewModel.insertColumn(column);
                        column.setUid(uid);
                        LOG.info("Added column: {}", column.getName());
                        updateColumnSpokens(column, spokenList, true);

                        handler.post(() -> columnAdapter.addEntity(column));
                    });
                }
                clearSelection();
            }
        }
    }

    private void deleteColumnButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            Column selectedColumn = columnAdapter.getSelectedEntity();
            if (selectedColumn != null) {
                selectedColumn.setActive(false);
                new Thread(() -> vsViewModel.updateColumn(selectedColumn)).start();
                new Thread(() -> {
                    final List<ColumnSpoken> spokenList = vsViewModel
                            .findAllColumnSpokensByColumn(selectedColumn.getUid());

                    //Convert list to array for deleteAllColumnSpokens query
                    final ColumnSpoken[] spokenArray = spokenList.toArray(new ColumnSpoken[0]);
                    vsViewModel.deleteAllColumnSpokens(spokenArray);
                }).start();

                columnAdapter.removeSelectedEntity();
                clearSelection();
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnDeleteButton);
            vdtsApplication.displayToast(
                    this,
                    "Only admin users can delete columns"
            );
        }
    }

    public void importButtonOnClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnImportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only admin users can import columns"
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
                    .playOn(columnExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only admin users can export columns"
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
     * Check if the column is unique, has a name, name code (abbreviated name), and export code.
     * @param column - The column to be checked.
     * @return - True if valid.
     */
    private boolean isValidColumn(Column column) {
        if (column.getName().isEmpty()) {
            LOG.info("Invalid column - no name");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnNameEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have a name"
            );
            return false;
        }

        if (column.getNameCode().isEmpty()) {
            LOG.info("Invalid column - no name code");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnNameCodeEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have an abbreviation,"
            );
            return false;
        }

        if (column.getExportCode().isEmpty()) {
            LOG.info("Invalid column - no export code");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnExportCodeEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have an export code"
            );
        }

        if (columnList.stream()
                .anyMatch(column1 -> column.getUid() != column1.getUid() &&
                        column1.getName().equalsIgnoreCase(column.getName()))) {
            LOG.info("Invalid column - non-unique name");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnNameEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have a unique name"
            );
            return false;
        }

        if (columnList.stream()
                .anyMatch(column1 -> column.getUid() != column1.getUid() &&
                        column1.getNameCode().equalsIgnoreCase(column.getNameCode()))) {
            LOG.info("Invalid column - non-unique name code");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnNameCodeEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have a unique abbreviation"
            );
            return false;
        }

        if (columnList.stream()
                .anyMatch(column1 -> column.getUid() != column1.getUid() &&
                        column1.getExportCode().equalsIgnoreCase(column.getExportCode()))) {
            LOG.info("Invalid column - non-unique export code");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnExportCodeEditText);
            vdtsApplication.displayToast(
                    this,
                    "A column must have a unique export code"
            );
            return false;
        }

        return true;
    }

    /**
     * Check if the column's spokens exist, are unique, and do not contain reserved words.
     * @param column - The column to be checked.
     * @param spokenList - The list of spokens to be checked
     * @return - True if valid.
     */
    private boolean isValidColumnSpoken(Column column, List<String> spokenList) {
        if (!spokenList.isEmpty()) {
            for (ColumnSpoken columnSpoken : columnSpokenList) {
                if (columnSpoken.getColumnID() != column.getUid()) {
                    for (String spoken : spokenList) {
                        if (columnSpoken.getSpoken().equalsIgnoreCase(spoken)) {
                            LOG.info("Column's spokens must be unique");
                            YoYo.with(Techniques.Shake)
                                    .duration(SHAKE_DURATION)
                                    .repeat(SHAKE_REPEAT)
                                    .playOn(columnSpokenEditText);
                            vdtsApplication.displayToast(
                                    this,
                                    "Column's spokens must be unique"
                            );
                            return false;
                        }

                        for (String reserved : reservedWords) {
                            if (spoken.toLowerCase().contains(reserved.toLowerCase())) {
                                LOG.info("Column's spokens contain a reserved word");
                                YoYo.with(Techniques.Shake)
                                        .duration(SHAKE_DURATION)
                                        .repeat(SHAKE_REPEAT)
                                        .playOn(columnSpokenEditText);
                                vdtsApplication.displayToast(
                                        this,
                                        "Column's spokens contain a reserved word"
                                );
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        } else {
            LOG.info("Column must have a spoken term");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(columnSpokenEditText);
            vdtsApplication.displayToast(
                    this,
                    "Column must have a spoken term"
            );
            return false;
        }
    }

    /**
     * Update the spokens associated with a column that is to be created or updated.
     * @param column - The column to be updated.
     * @param spokenList - The list spoken to be updated/created
     * @param isNew - Is the column new.
     */
    private void updateColumnSpokens(Column column, List<String> spokenList, boolean isNew) {
        if (isNew) {
            for (VDTSUser user : userList) {
                for (String spoken : spokenList) {
                    new Thread(
                            () -> vsViewModel.insertColumnSpoken(
                                    new ColumnSpoken(
                                            user.getUid(),
                                            column.getUid(),
                                            spoken
                                    )
                            )
                    ).start();
                }
            }
        } else {
            final List<ColumnSpoken> existingSpokenList = columnSpokenList.stream()
                    .filter(spoken -> spoken.getColumnID() == column.getUid())
                    .filter(spoken -> spoken.getUserID() == selectedUser.getUid())
                    .collect(Collectors.toList());

            //Delete spokens that no longer exist
            for (ColumnSpoken columnSpoken : existingSpokenList) {
                if (spokenList.stream()
                        .noneMatch(spoken -> spoken.equalsIgnoreCase(columnSpoken.getSpoken()))) {
                    new Thread(() -> vsViewModel.deleteColumnSpoken(columnSpoken)).start();
                }
            }

            //Insert new spokens
            if (selectedUser == null) { selectedUser = currentUser; }
            for (String spoken : spokenList) {
                if (existingSpokenList.stream()
                        .noneMatch(oldSpoken -> oldSpoken.getSpoken().equalsIgnoreCase(spoken))) {
                    new Thread(
                            () -> vsViewModel.insertColumnSpoken(
                                    new ColumnSpoken(
                                            selectedUser.getUid(),
                                            column.getUid(),
                                            spoken
                                    )
                            )
                    ).start();
                }
            }
        }
    }

    /**
     * Create a list of strings be separating at the commas the value in the spoken edit field
     * or the name edit field
     * @param spokenText The string from the spoken edit field
     * @param nameText The string from the name edit field
     * @return A list of strings to be used as spoken values
     */
    private List<String> getFormattedColumnSpokenList(String spokenText, String nameText) {
        return !spokenText.isEmpty() ?
                Arrays.stream(
                        spokenText
                                .replaceAll(" ,", ",")
                                .replaceAll(", ", ",")
                                .trim()
                                .split(",")
                ).distinct()
                        .filter(spoken -> !spoken.isEmpty())
                        .collect(Collectors.toList()) :
                Arrays.stream(
                        nameText
                                .replaceAll(" ,", ",")
                                .replaceAll(", ", ",")
                                .trim()
                                .split(",")
                ).distinct()
                        .filter(spoken -> !spoken.isEmpty())
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
                            columnAdapterSelect(-1);

                            vdtsApplication.displayToast(
                                    this,
                                    "Setup imported successfully"
                            );

                            ExecutorService executor = Executors.newSingleThreadExecutor();
                            Handler handler = new Handler(Looper.getMainLooper());
                            executor.execute(() -> {
                                columnList.clear();
                                columnList.addAll(vsViewModel.findAllActiveColumns());
                                columnList.remove(Column.COLUMN_NONE);
                                handler.post(() -> {
                                    columnAdapter.setDataset(columnList);
                                    columnAdapterSelect(-1);
                                });
                            });
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
            iristickHUD = new ConfigColumnsActivity.IristickHUD();
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
