package ca.vdts.voiceselect.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SELECT_FOLDER;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.android.material.slider.Slider;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.adapters.ConfigLayoutsAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.FileUtil;
import ca.vdts.voiceselect.files.Importer;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

/**
 * Configure layouts and their associated columns.
 */
public class ConfigLayoutsActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnValuesActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Layout selectedLayout;
    private int selectedLayoutPosition;
    private Column selectedColumn;
    private LayoutColumn selectedLayoutColumn;
    private Long currentPosition;

    //Lists
    private final List<Layout> layoutList = new ArrayList<>();
    private final List<Column> columnList = new ArrayList<>();
    private final List<LayoutColumn> layoutColumnList = new ArrayList<>();

    //Views
    private Button layoutNewButton;
    private Button layoutResetButton;
    private Button layoutSaveButton;
    private Button layoutDeleteButton;

    private TextView layoutNameEditText;
    private TextView layoutExportCodeEditText;

    private SwitchCompat commentRequiredSwitch;
    private SwitchCompat pictureRequireSwitch;

    private Spinner layoutSpinner;

    private RecyclerView layoutColumnRecyclerView;

    private SwitchCompat columnEnabledSwitch;
    private Slider columnPositionSlider;

    private Button layoutImportButton;
    private Button layoutExportButton;

    //View Model - Adapters
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<Layout> layoutSpinnerAdapter;
    private ConfigLayoutsAdapter configLayoutsAdapter;

    //Iristick Components
    private ConfigLayoutsActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_layout);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        layoutNewButton = findViewById(R.id.layoutNewButton);
        layoutNewButton.setOnClickListener(v -> newLayoutButtonOnClick());

        layoutResetButton = findViewById(R.id.layoutResetButton);
        layoutResetButton.setOnClickListener(v -> resetLayoutButtonOnClick());

        layoutSaveButton = findViewById(R.id.layoutSaveButton);
        layoutSaveButton.setOnClickListener(v -> saveLayoutButtonOnClick());

        layoutDeleteButton = findViewById(R.id.layoutDeleteButton);
        layoutDeleteButton.setOnClickListener(v -> deleteLayoutButtonOnClick());

        layoutNameEditText = findViewById(R.id.layoutNameEditText);
        layoutExportCodeEditText = findViewById((R.id.layoutExportCodeEditText));

        commentRequiredSwitch = findViewById(R.id.commentRequiredSwitch);
        pictureRequireSwitch = findViewById(R.id.pictureRequiredSwitch);

        columnEnabledSwitch = findViewById(R.id.columnEnableSwitch);
        columnEnabledSwitch.setEnabled(false);
        columnEnabledSwitch.setOnClickListener(v -> enableOnClick());

        columnPositionSlider = findViewById(R.id.columnPositionSlider);
        columnPositionSlider.setEnabled(false);
        columnPositionSlider.addOnChangeListener(positionChangeListener);

        layoutImportButton = findViewById(R.id.layoutImportButton);
        layoutImportButton.setOnClickListener(v -> onImportClick());
        layoutExportButton = findViewById(R.id.layoutExportButton);
        layoutExportButton.setOnClickListener(v -> onExportClick());

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Layout Spinner
        layoutSpinner = findViewById(R.id.layoutSpinner);

        //Observe/Update layout list
        vsViewModel.findAllActiveLayoutsLive().observe(
                this,
                layouts -> {
                    layoutList.clear();
                    layoutList.addAll(layouts);

                    layoutList.remove(Layout.LAYOUT_NONE);
                    final Layout newLayout = Layout.LAYOUT_NONE;
                    newLayout.setName("");
                    newLayout.setExportCode("");
                    layoutList.add(0, newLayout);

                    if (selectedLayout != null) {
                        layoutSpinner.setSelection(
                                layoutSpinnerAdapter.getPosition(selectedLayout)
                        );
                    } else {
                        layoutSpinner.setSelection(0);
                    }
                }
        );

        layoutSpinner.setOnItemSelectedListener(layoutSpinnerListener);

        //Recycler View
        layoutColumnRecyclerView = findViewById(R.id.layoutColumnRecyclerView);

        //Observe/Update layoutColumn list
        if (selectedLayout != null) {
            vsViewModel.findAllLayoutColumnsByLayoutLive(selectedLayout.getUid()).observe(
                    this,
                    layoutColumns -> {
                        layoutColumnList.clear();
                        layoutColumnList.addAll(layoutColumns);
                    }
            );
        }

        layoutColumnRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeLayoutList();
    }

    private void initializeLayoutList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            layoutList.clear();
            layoutList.addAll(vsViewModel.findAllActiveLayouts());
            layoutList.remove(Layout.LAYOUT_NONE);
            final Layout newLayout = Layout.LAYOUT_NONE;
            newLayout.setName("");
            newLayout.setExportCode("");
            layoutList.add(0, newLayout);
            handler.post(() -> {
                //todo - do in every other config editor????
                layoutSpinnerAdapter = new VDTSNamedAdapter<>(
                        this,
                        R.layout.adapter_spinner_named,
                        layoutList
                );
                layoutSpinnerAdapter.setToStringFunction((layout, integer) -> layout.getName());

                layoutSpinner.setAdapter(layoutSpinnerAdapter);
                initializeColumnList();
            });
        });
    }

    private void initializeColumnList () {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnList.clear();
            columnList.addAll(vsViewModel.findAllActiveColumns());
            handler.post(() -> {
                if (columnList.size() <= 1) {
                    columnPositionSlider.setEnabled(false);
                } else {
                    columnPositionSlider.setValueTo(columnList.size());
                }

                initializeLayoutColumnList(selectedLayout);
            });
        });
    }

    private void initializeLayoutColumnList(Layout layout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            if (layout != null) {
                layoutColumnList.clear();
                layoutColumnList.addAll(vsViewModel.findAllLayoutColumnsByLayout(layout));
            }

            handler.post(() -> {
                if (selectedLayoutPosition >= 1) {
                    configLayoutsAdapter = new ConfigLayoutsAdapter(
                            this,
                            new VDTSClickListenerUtil(
                                    this::configLayoutsAdapterSelect,
                                    layoutColumnRecyclerView
                            ),
                            columnList,
                            layoutColumnList
                    );
                } else {
                    List<Column> emptyColumnList = new ArrayList<>();
                    List<LayoutColumn> emptyLayoutColumnList = new ArrayList<>();
                    configLayoutsAdapter = new ConfigLayoutsAdapter(
                            this,
                            new VDTSClickListenerUtil(
                                    this::configLayoutsAdapterSelect,
                                    layoutColumnRecyclerView
                            ),
                            emptyColumnList,
                            emptyLayoutColumnList
                    );
                }

                layoutColumnRecyclerView.setAdapter(configLayoutsAdapter);
            });
        });
    }

    /**
     * Click listener for layout spinner.
     */
    private final AdapterView.OnItemSelectedListener layoutSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedLayout = (Layout) parent.getItemAtPosition(position);
                    selectedLayoutPosition = position;

                    if (layoutList.size() <= 1) {
                        YoYo.with(Techniques.Shake)
                                .duration(SHAKE_DURATION)
                                .repeat(SHAKE_REPEAT)
                                .playOn(layoutNewButton);
                        vdtsApplication.displayToast(
                                vdtsApplication.getApplicationContext(),
                                "A layout must be created before it can be customized"
                        );
                    } else {
                        layoutNameEditText.setText(selectedLayout.getName());
                        layoutExportCodeEditText.setText(selectedLayout.getExportCode());
                        commentRequiredSwitch.setChecked(selectedLayout.isCommentRequired());
                        pictureRequireSwitch.setChecked(selectedLayout.isPictureRequired());
                    }

                    initializeLayoutColumnList(selectedLayout);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    /**
     * Select the appropriate column and layout column from the recycler view.
     * @param index - Index of the column and layout column to select.
     */
    private void configLayoutsAdapterSelect(Integer index) {
        if (index >= 0) {
            configLayoutsAdapter.setSelectedColumn(index);

            columnEnabledSwitch.setEnabled(true);
            if (columnList.size() <= 1) {
                String message = "Create more than one column to adjust column position";
                vdtsApplication.displayToast(this, message);
                columnPositionSlider.setEnabled(false);
            } else {
                columnPositionSlider.setEnabled(true);
            }

            Pair<Column, LayoutColumn> columnLayoutColumnPair =
                    configLayoutsAdapter.getSelectedColumnLayoutColumn();
            if (columnLayoutColumnPair != null) {
                if (columnLayoutColumnPair.first != null) {
                    selectedColumn = columnLayoutColumnPair.first;
                    if (columnLayoutColumnPair.second != null) {
                        selectedLayoutColumn = columnLayoutColumnPair.second;

                        columnEnabledSwitch.setChecked(
                                selectedColumn.getUid() == selectedLayoutColumn.getColumnID()
                        );

                        columnPositionSlider.setValue(selectedLayoutColumn.getColumnPosition());
                        currentPosition = selectedLayoutColumn.getColumnPosition();
                    } else {
                        selectedLayoutColumn = null;
                        columnEnabledSwitch.setChecked(false);
                        columnPositionSlider.setValue(1);
                        currentPosition = null;
                    }
                }
            }
        } else {
            /*if (configLayoutsAdapter != null) {
                configLayoutsAdapter.clearSelected();
            }

            columnEnabledSwitch.setEnabled(false);
            columnEnabledSwitch.setChecked(false);
            columnPositionSlider.setEnabled(false);
            columnPositionSlider.setValue(1);*/
            layoutSpinner.setSelection(0);
        }
    }

    private void enableOnClick() {
        if (columnEnabledSwitch.isChecked()) {
            if (selectedLayoutColumn == null) {
                final long position = configLayoutsAdapter.lastPosition() + 1;
                currentPosition = null;
                selectedLayoutColumn = new LayoutColumn(
                        selectedLayout.getUid(),
                        selectedColumn.getUid(),
                        position
                );
                configLayoutsAdapter.addLayoutColumn(selectedLayoutColumn);
                columnPositionSlider.setValue(position);
            }
        } else {
            if (selectedLayoutColumn != null) {
                configLayoutsAdapter.removeLayoutColumn(selectedLayoutColumn);
                currentPosition = null;
                selectedLayoutColumn = null;
                columnPositionSlider.setValue(1);
            }
        }
    }

    private final Slider.OnChangeListener positionChangeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (selectedLayoutColumn != null) {
                if (selectedLayoutColumn.getColumnPosition() != (long) value) {
                    if (value > configLayoutsAdapter.lastPosition()) {
                        columnPositionSlider.setValue(configLayoutsAdapter.lastPosition());
                    } else {
                        selectedLayoutColumn.setColumnPosition((long) value);
                        configLayoutsAdapter.adjustPositions(selectedLayoutColumn, currentPosition);
                        currentPosition = selectedLayoutColumn.getColumnPosition();
                    }
                }
            }
        }
    };

    private void newLayoutButtonOnClick() {
        layoutSpinner.setSelection(0);
        configLayoutsAdapterSelect(-1);
        layoutNameEditText.requestFocus();
    }

    private void resetLayoutButtonOnClick() {
        layoutSpinner.setSelection(layoutSpinnerAdapter.getSelectedEntityIndex());

        layoutNameEditText.setText(selectedLayout.getName());
        layoutExportCodeEditText.setText(selectedLayout.getExportCode());
        configLayoutsAdapterSelect(configLayoutsAdapter.getSelectedColumnIndex());
        layoutNameEditText.requestFocus();
    }

    private void saveLayoutButtonOnClick() {
        if (selectedLayout.getUid() == -9001L &&
                layoutNameEditText.getText().toString().isEmpty() &&
                layoutExportCodeEditText.getText().toString().isEmpty()) {
            String message = "Create or select a layout";
            LOG.info(message);
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutSaveButton);
            vdtsApplication.displayToast(this, message);
        } else if (!layoutNameEditText.getText().toString().isEmpty() &&
                   !layoutExportCodeEditText.getText().toString().isEmpty()) {
            if (selectedLayout.getUid() >= 1) {
                //Update existing layout
                selectedLayout.setName(layoutNameEditText.getText().toString().trim());
                selectedLayout.setExportCode(layoutExportCodeEditText.getText().toString().trim());
                selectedLayout.setCommentRequired(commentRequiredSwitch.isChecked());
                selectedLayout.setPictureRequired(pictureRequireSwitch.isChecked());

                ExecutorService updateLayoutExecutor = Executors.newSingleThreadExecutor();
                Handler updateLayoutHandler = new Handler(Looper.getMainLooper());
                updateLayoutExecutor.execute(() -> {
                    vsViewModel.updateLayout(selectedLayout);
                    updateLayoutHandler.post(() -> {
                        String message = "Updated layout: " + selectedLayout.getName();
                        LOG.info(message);
                        vdtsApplication.displayToast(this, message);

                        layoutSpinner.setSelection(layoutSpinnerAdapter.getPosition(selectedLayout));
                        layoutSpinnerAdapter.notifyDataSetChanged();

                        ExecutorService getCurrentLayoutColumnsExecutor =
                                Executors.newSingleThreadExecutor();
                        Handler getCurrentLayoutColumnsHandler = new Handler(
                                Looper.getMainLooper()
                        );
                        getCurrentLayoutColumnsExecutor.execute(() -> {
                            List<LayoutColumn> currentLayoutColumns =
                                    vsViewModel.findAllLayoutColumnsByLayout(selectedLayout);
                            getCurrentLayoutColumnsHandler.post(() -> {
                                ExecutorService deleteCurrentLayoutColumnsExecutor =
                                        Executors.newSingleThreadExecutor();
                                Handler deleteCurrentLayoutColumnsHandler = new Handler(
                                        Looper.getMainLooper()
                                );
                                deleteCurrentLayoutColumnsExecutor.execute(() -> {
                                    currentLayoutColumns.forEach(lc -> {
                                        if (!configLayoutsAdapter.getLayoutColumnDataset().contains(lc)) {
                                            vsViewModel.deleteLayoutColumn(lc);
                                        }
                                    });
                                    deleteCurrentLayoutColumnsHandler.post(() -> LOG.debug("Layout Columns deleted"));
                                });

                                ExecutorService saveLayoutColumnsExecutor =
                                        Executors.newSingleThreadExecutor();
                                Handler saveLayoutColumnsHandler = new Handler(
                                        Looper.getMainLooper()
                                );
                                saveLayoutColumnsExecutor.execute(() -> {
                                    configLayoutsAdapter.getLayoutColumnDataset().forEach(lc -> {
                                        if (currentLayoutColumns.contains(lc)) {
                                            vsViewModel.updateLayoutColumn(lc);
                                        } else {
                                            vsViewModel.insertLayoutColumn(lc);
                                        }
                                    });
                                    saveLayoutColumnsHandler.post(() -> LOG.debug("Layout Columns saved."));
                                });
                            });
                        });
                    });
                });
            } else {
                //Create new layout
                Layout newLayout = new Layout(
                        currentUser.getUid(),
                        layoutNameEditText.getText().toString(),
                        layoutExportCodeEditText.getText().toString(),
                        commentRequiredSwitch.isChecked(),
                        pictureRequireSwitch.isChecked()
                );

                ExecutorService createLayoutExecutor = Executors.newSingleThreadExecutor();
                Handler createLayoutHandler = new Handler(Looper.getMainLooper());
                createLayoutExecutor.execute(() -> {
                    long uid = vsViewModel.insertLayout(newLayout);
                    newLayout.setUid(uid);
                    createLayoutHandler.post(() -> {
                        layoutSpinnerAdapter.add(newLayout);

                        selectedLayout = newLayout;

                        layoutSpinner.setSelection(layoutSpinnerAdapter.getPosition(newLayout));
                        layoutNameEditText.clearFocus();
                        layoutExportCodeEditText.clearFocus();

                        String message = "Created layout: " + newLayout.getName();
                        LOG.info(message);
                        vdtsApplication.displayToast(this, message);
                    });
                });
            }
        } else {
            String message = "Layout must have a name and export code";
            LOG.info(message);
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutNameEditText);
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutExportCodeEditText);
            vdtsApplication.displayToast(this, message);
        }
    }

    private void deleteLayoutButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            if (selectedLayout != null) {
                selectedLayout.setActive(false);
                layoutSpinnerAdapter.remove(selectedLayout);

                Executor executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    vsViewModel.updateLayout(selectedLayout);

                    handler.post(() -> {
                        List<LayoutColumn> removeLayoutColumnList = new ArrayList<>();
                        for (LayoutColumn layoutColumn : layoutColumnList) {
                            if (selectedLayout.getUid() == layoutColumn.getLayoutID()) {
                                removeLayoutColumnList.add(layoutColumn);
                            }
                        }
                        final LayoutColumn[] removeLayoutColumnArray =
                                removeLayoutColumnList.toArray(new LayoutColumn[0]);

                        selectedLayout = null;

                        new Thread(
                                () -> vsViewModel.deleteAllLayoutColumns(removeLayoutColumnArray)
                        ).start();
                    });
                });
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutDeleteButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can delete columns"
            );
        }
    }

    public void onImportClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutImportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can import layouts"
            );
        } else {
            showImportDialog();
        }
    }

    private void showImportDialog() {
        LOG.info("Showing Choice Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Layouts");
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
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(layoutExportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export layouts"
            );
        } else {
            //saver = Saver.createSaver(ONEDRIVE_APP_ID);
            final Exporter exporter = new Exporter(
                    vsViewModel,
                    vdtsApplication,
                    this
                    //saver
            );
            if (exporter.exportColumnLayout()) {
                vdtsApplication.displayToast(
                        this,
                        "Column layout exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting column layout"
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
                        final Importer importer = new Importer(
                                vsViewModel,
                                this,
                                vdtsApplication
                        );
                        if (importer.importColumnLayout(file)) {
                            configLayoutsAdapterSelect(-1);

                            vdtsApplication.displayToast(
                                    this,
                                    "Layouts imported successfully"
                            );
                        } else {
                            vdtsApplication.displayToast(
                                    this,
                                    "Error importing layouts"
                            );
                        }
                    } else {
                        vdtsApplication.displayToast(
                                this,
                                "Layout file not found"
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    vdtsApplication.displayToast(
                            this,
                            "Layout file not found"
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
            iristickHUD = new ConfigLayoutsActivity.IristickHUD();
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