package ca.vdts.voiceselect.activities.configure;

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
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

//todo - add toast when non admin user tries to select only admin editable fields
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
    private boolean isHeadsetAvailable = false;
    private ConfigColumnsActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_columns);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

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
        columnNameCodeEditText = findViewById(R.id.columnNameCodeEditText);
        columnExportCodeEditText = findViewById(R.id.columnExportCodeEditText);
        columnSpokenEditText = findViewById(R.id.layoutNameEditText);

        columnImportButton = findViewById(R.id.layoutImportButton);
        columnExportButton = findViewById(R.id.layoutExportButton);

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //User Spinner
        columnUserSpinner = findViewById(R.id.layoutSpinner);

        userAdapter = new VDTSNamedAdapter<>(this, R.layout.adapter_spinner_named, userList);
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
                columnList.clear();
                columnList.addAll(vsViewModel.findAllActiveColumnsByUser(currentUser.getUid()));
                columnList.remove(Column.COLUMN_NONE);
                handler.post(() -> {
                    columnAdapter.setDataset(columnList);
                    columnAdapterSelect(-1);
                });
            });
        }
    }

    private void disableViews() {
        if (currentUser.getAuthority() <= 0) {
            columnNewButton.setEnabled(false);
            columnDeleteButton.setEnabled(false);

            columnNameEditText.setEnabled(false);
            columnNameCodeEditText.setEnabled(false);
            columnExportCodeEditText.setEnabled(false);
            columnUserSpinner.setEnabled(false);

            columnImportButton.setEnabled(false);
            columnExportButton.setEnabled(false);

            if (columnList.size() == 0) {
                columnResetButton.setEnabled(false);
                columnSaveButton.setEnabled(false);
            }
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
                        .filter(spoken -> spoken.getUserID() == selectedUser.getUid())      //todo maybe??????
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

    private void newColumnButtonOnClick() {
        columnAdapterSelect(-1);
        columnNameEditText.requestFocus();
    }

    private void resetColumnButtonOnClick() {
        columnAdapterSelect(columnAdapter.getSelectedEntityIndex());
        columnNameEditText.requestFocus();
    }

    private void saveColumnButtonOnClick() {
        Column selectedColumn = columnAdapter.getSelectedEntity();

        if (selectedColumn != null) {
            //Update existing column
            selectedColumn.setName(columnNameEditText.getText().toString().trim());
            selectedColumn.setNameCode(columnNameCodeEditText.getText().toString().trim());
            selectedColumn.setExportCode(columnExportCodeEditText.getText().toString().trim());

            if (isValidColumn(selectedColumn)) {
               if (isValidColumnSpoken(selectedColumn)) {
                   ExecutorService executor = Executors.newSingleThreadExecutor();
                   Handler handler = new Handler(Looper.getMainLooper());
                   executor.execute(() -> {
                       vsViewModel.updateColumn(selectedColumn);
                       updateColumnSpokens(selectedColumn, false);
                       handler.post(() -> columnAdapter.updateSelectedEntity());
                   });
               } else {
                   LOG.info("Invalid column spoken");
                   vdtsApplication.displayToast(
                           this,
                           "Invalid column spoken",
                           0
                   );
               }
               newColumnButtonOnClick();
            } else {
                LOG.info("Invalid column");
                vdtsApplication.displayToast(this, "Invalid column", 0);
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
                if (isValidColumnSpoken(column)) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        long uid = vsViewModel.insertColumn(column);
                        column.setUid(uid);
                        LOG.info("Added column: {}", column.getName());
                        updateColumnSpokens(column, true);

                        handler.post(() -> columnAdapter.addEntity(column));
                    });
                }
                newColumnButtonOnClick();
            } else {
                LOG.info("Invalid column");
                vdtsApplication.displayToast(this, "Invalid column", 0);
            }
        }
    }

    private void deleteColumnButtonOnClick() {
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
            newColumnButtonOnClick();
        }
    }

    /**
     * Check if the column is unique, has a name, name code (abbreviated name), and export code.
     * @param column - The column to be checked.
     * @return - True if valid.
     */
    private boolean isValidColumn(Column column) {
        //todo - split up and provide meaningful toast messages to users - similar to isValidColumnSpoken
        return !column.getName().isEmpty() &&
               !column.getNameCode().isEmpty() &&
               !column.getExportCode().isEmpty() &&
                columnList.stream().noneMatch(column1 -> column.getUid() != column1.getUid() &&
                        (column1.getName().equalsIgnoreCase(column.getName()) ||
                                column1.getNameCode().equalsIgnoreCase(column.getName()) ||
                                column1.getExportCode().equalsIgnoreCase(column.getExportCode())));
    }

    /**
     * Check if the column's spokens exist, are unique, and do not contain reserved words.
     * @param column - The column to be checked.
     * @return - True if valid.
     */
    private boolean isValidColumnSpoken(Column column) {
        if (!columnSpokenEditText.getText().toString().isEmpty()) {
            final List<String> spokenList = getFormattedColumnSpokenList();

            for (ColumnSpoken columnSpoken : columnSpokenList) {
                if (columnSpoken.getColumnID() != column.getUid()) {
                    for (String spoken : spokenList) {
                        if (columnSpoken.getSpoken().equalsIgnoreCase(spoken)) {
                            LOG.info("Column's spokens must be unique");
                            vdtsApplication.displayToast(
                                    this,
                                    "Column's spokens must be unique",
                                    0
                            );
                            return false;
                        }

                        for (String reserved : reservedWords) {
                            if (spoken.toLowerCase().contains(reserved.toLowerCase())) {
                                LOG.info("Column's spokens contain a reserved word");
                                vdtsApplication.displayToast(
                                        this,
                                        "Column's spokens contain a reserved word",
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
            LOG.info("Column must have a spoken term");
            vdtsApplication.displayToast(
                    this,
                    "Column must have a spoken term",
                    0
            );
            return false;
        }
    }

    /**
     * Update the spokens associated with a column that is to be created or updated.
     * @param column - The column to be updated.
     * @param isNew - Is the column new.
     */
    private void updateColumnSpokens(Column column, boolean isNew) {
        final List<String> spokenList = getFormattedColumnSpokenList();
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
                if (spokenList.stream().noneMatch(spoken ->
                        spoken.equalsIgnoreCase(columnSpoken.getSpoken()))) {
                    new Thread(() -> vsViewModel.deleteColumnSpoken(columnSpoken)).start();
                }
            }

            //Insert new spokens
            if (selectedUser == null) { selectedUser = currentUser; }
            for (String spoken : spokenList) {
                if (existingSpokenList.stream().noneMatch(oldSpoken ->
                        oldSpoken.getSpoken().equalsIgnoreCase(spoken))) {
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
     * Create a comma separated list from strings in the spoken text field.
     * @return - A comma separated list of strings.
     */
    private List<String> getFormattedColumnSpokenList() {
        return Arrays.stream(
                        columnSpokenEditText.getText().toString()
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
                iristickHUD = new ConfigColumnsActivity.IristickHUD();
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
