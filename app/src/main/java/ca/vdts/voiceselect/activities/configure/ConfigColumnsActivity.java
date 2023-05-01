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

import org.apache.commons.lang3.StringUtils;
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
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.services.VDTSClickListenerService;

//todo - add toast when non admin user tries to select only admin editable fields
/**
 * Configure column parameters
 */
public class ConfigColumnsActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private VDTSUser selectedUser;
    private final List<VDTSUser> userList = new ArrayList<>();

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

    //Recycler View
    private VSViewModel vsViewModel;
    private VDTSIndexedNamedAdapter<Column> columnAdapter;
    private RecyclerView columnRecyclerView;
    private final List<Column> columnList = new ArrayList<>();
    private final List<ColumnSpoken> columnSpokenList = new ArrayList<>();
    private ArrayList<String> reservedWords;

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
        columnNewButton = findViewById(R.id.columnNewButton);
        columnNewButton.setOnClickListener(v -> newColumnButtonOnClick());

        columnResetButton = findViewById(R.id.columnResetButton);
        columnResetButton.setOnClickListener(v -> resetColumnButtonOnClick());

        columnSaveButton = findViewById(R.id.columnSaveButton);
        columnSaveButton.setOnClickListener(v -> saveColumnButtonOnClick());

        columnDeleteButton = findViewById(R.id.columnDeleteButton);
        columnDeleteButton.setOnClickListener(v -> deleteColumnButtonOnClick());

        columnNameEditText = findViewById(R.id.columnNameEditText);
        columnNameCodeEditText = findViewById(R.id.columnNameCodeEditText);
        columnExportCodeEditText = findViewById(R.id.columnExportCodeEditText);
        columnSpokenEditText = findViewById(R.id.columnSpokenEditText);

        //User Spinner
        columnUserSpinner = findViewById(R.id.columnUserSpinner);
        columnUserSpinner.setOnItemSelectedListener(userSpinnerSelect);
        if (currentUser.getAuthority() <= 0) {
            userList.clear();
            userList.add(currentUser);
        } else {
            ExecutorService usExecutor = Executors.newSingleThreadExecutor();
            Handler usHandler = new Handler(Looper.getMainLooper());
            usExecutor.execute(() -> {
                userList.clear();
                userList.addAll(vsViewModel.findAllActiveUsers());
                usHandler.post(() -> columnUserSpinner.setSelection(userList.indexOf(currentUser)));
            });
        }

        columnImportButton = findViewById(R.id.columnImportButton);
        columnExportButton = findViewById(R.id.columnExportButton);

        //Recyclerview
        columnRecyclerView = findViewById(R.id.columnRecyclerView);

//        ExecutorService rvExecutor = Executors.newSingleThreadExecutor();
//        Handler rvHandler = new Handler(Looper.getMainLooper());
//        rvExecutor.execute(() -> {
//            columnList.clear();
//            columnList.addAll(vsViewModel.findAllColumns());
//            rvHandler.post(() -> columnAdapter.setDataset(columnList));
//        });

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //todo - test
        vsViewModel.findAllColumnsLive().observe(this, columns -> {
            columnList.clear();
            columnList.addAll(columns);
        });

        vsViewModel.findAllColumnSpokensLive().observe(this, columnSpokens -> {
            columnSpokenList.clear();
            columnSpokenList.addAll(columnSpokens);
        });

        columnRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                ));

        columnAdapter = new VDTSIndexedNamedAdapter<>(
                new VDTSClickListenerService(this::columnAdapterSelect, columnRecyclerView),
                this,
                columnList
        );

        columnRecyclerView.setAdapter(columnAdapter);

        reservedWords = new ArrayList<>(
                Arrays.asList(this.getResources().getStringArray(R.array.reserved_words))
        );

        disableViews();
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
        }
    }

    public void newColumnButtonOnClick() {
        columnAdapterSelect(-1);
        columnNameEditText.requestFocus();
    }

    public void resetColumnButtonOnClick() {
        columnAdapterSelect(columnAdapter.getSelectedIndex());
        columnNameEditText.requestFocus();
    }

    public void saveColumnButtonOnClick() {
        Column selectedColumn = columnAdapter.getSelectedEntity();

        if (selectedColumn != null) {
            //Update existing column
            selectedColumn.setName(columnNameEditText.getText().toString().trim());
            selectedColumn.setNameCode(columnNameCodeEditText.getText().toString().trim());
            selectedColumn.setExportCode(columnExportCodeEditText.getText().toString().trim());

            if (isValidColumn(selectedColumn)) {
               if (isValidColumnSpoken(selectedColumn)) {
                   new Thread(() -> {
                       vsViewModel.updateColumn(selectedColumn);
                       updateColumnSpokens(selectedColumn, false);
                       updateColumnList();
                   }).start();

               }
            } else {
                LOG.info("Invalid column");
                vdtsApplication.displayToast(this, "Invalid column", 0);
            }
        } else {
            //Create new column
            Column column = new Column(
                    currentUser.getUid(),
                    columnNameEditText.getText().toString().trim(),
                    columnNameCodeEditText.getText().toString().trim(),
                    columnExportCodeEditText.getText().toString().trim()
            );

            if (isValidColumn(column)) {
                if (isValidColumnSpoken(column)) {
                    new Thread(() -> {
                        long uid = vsViewModel.insertColumn(column);
                        column.setUid(uid);
                        updateColumnSpokens(column, true);
                        updateColumnList();
                    }).start();
                }
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
                final List<ColumnSpoken> spokenList =
                        vsViewModel.findAllColumnSpokensByColumn(selectedColumn.getUid());

                //Convert list to array for deleteAllColumns query
                final ColumnSpoken[] spokenArray = spokenList.toArray(new ColumnSpoken[0]);
                vsViewModel.deleteAllColumnSpokens(spokenArray);
            }).start();

            updateColumnList();
        }
    }

    /**
     * Click listener for user spinner
     */
    private final AdapterView.OnItemSelectedListener userSpinnerSelect =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedUser = (VDTSUser) parent.getItemAtPosition(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    /**
     * Select the appropriate column from the recycler view
     * @param index - Index of the column to select
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
                        .filter(spoken -> spoken.getColumnId() == selectedColumn.getUid())
                        .filter(spoken -> spoken.getUserId() == currentUser.getUid())
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

    /**
     * Check if the column is unique, has a name, name code (abbreviated name), and export code
     * @param column - The column to be checked
     * @return - True if valid
     */
    private boolean isValidColumn(Column column) {
        //todo - is refresh of list needed
//        try {
//            columnList.clear();
//            new Thread(() -> columnList.addAll(vsViewModel.findAllColumns())).join();
//        } catch (InterruptedException e) {
//            LOG.error("Getting all columns interrupted: ", e);
//        }

        //todo - split up and provide meaningful toast messages to users
        return !column.getName().isEmpty() &&
               !column.getNameCode().isEmpty() &&
               !column.getExportCode().isEmpty() &&
                columnList.stream().noneMatch(column1 -> column.getUid() != column1.getUid() &&
                        (StringUtils.lowerCase(column1.getName())
                                .equals(StringUtils.lowerCase(column.getName())) ||
                                StringUtils.lowerCase(column1.getNameCode())
                                        .equals(StringUtils.lowerCase(column.getName())) ||
                                StringUtils.lowerCase(column1.getExportCode())
                                        .equals(StringUtils.lowerCase(column.getExportCode()))));
    }

    /**
     * Check if the column's spokens exist, are unique, and do not contain reserved words
     * @param column - The column to be checked
     * @return - True if valid
     */
    private boolean isValidColumnSpoken(Column column) {
        if (!columnSpokenEditText.getText().toString().isEmpty()) {
            final List<String> spokenList = getFormattedColumnSpokenList();

            for (ColumnSpoken columnSpoken : columnSpokenList) {
                if (columnSpoken.getColumnId() != column.getUid()) {
                    for (String spoken : spokenList) {
                        if (columnSpoken.getSpoken().equalsIgnoreCase(spoken)) {
                            LOG.info("Column's spokens must be unique");
                            vdtsApplication.displayToast(
                                    this,
                                    "Column's spokens must be unique",
                                    0);
                            return false;
                        }

                        for (String reserved : reservedWords) {
                            if (spoken.toLowerCase().contains(reserved.toLowerCase())) {
                                LOG.info("Column's spokens contains a reserved word");
                                vdtsApplication.displayToast(
                                        this,
                                        "Column's spokens contains a reserved word",
                                        0);
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
                    0);
            return false;
        }
    }

    /**
     * Update the spokens associated with a column that is to be created or updated
     * @param column - The column to be updated
     * @param isNew - Is the column new
     */
    private void updateColumnSpokens(Column column, boolean isNew) {
        final List<String> spokenList = getFormattedColumnSpokenList();
        if (isNew) {
            if (userList.isEmpty()) userList.add(VDTSUser.VDTS_USER_NONE);
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
                    .filter(spoken -> spoken.getColumnId() == column.getUid())
                    .filter(spoken -> spoken.getUserId() == column.getUserId())
                    .collect(Collectors.toList());

            //Delete spokens that no longer exist
            for (ColumnSpoken columnSpoken : existingSpokenList) {
                if (spokenList.stream().noneMatch(spoken ->
                        spoken.equalsIgnoreCase(columnSpoken.getSpoken()))) {
                    new Thread(() -> vsViewModel.deleteColumnSpoken(columnSpoken)).start();
                }
            }

            //Insert new spokens
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
     * Update the column list after a column has been created or updated
     */
    private void updateColumnList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnList.clear();
            columnList.addAll(vsViewModel.findAllColumns());
            handler.post(() -> {
                columnAdapter.setDataset(columnList);
                columnAdapter.notifyDataSetChanged();
                columnAdapterSelect(-1);
            });
        });
    }

    /**
     * Create a comma separated list from the strings in the spokens text field
     * @return - A comma separated list of strings
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
     * Initialize elements based on Iristick connection
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
            setContentView(R.layout.activity_config_on_device_hud);

            configOnDeviceText = findViewById(R.id.configOnDeviceText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_on_device_text);
        }
    }
}
