package ca.vdts.voiceselect.activities.configure;

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

import com.google.android.material.slider.Slider;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.adapters.ConfigLayoutsAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

public class ConfigLayoutsActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnValuesActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Layout selectedLayout;
    private Column selectedColumn;
    private LayoutColumn selectedLayoutColumn;

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

    private Spinner layoutSpinner;

    private RecyclerView layoutColumnRecyclerView;

    private SwitchCompat columnEnabledSwitch;
    private Slider columnPositionSlider;

    //View Model - Adapters
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<Layout> layoutAdapter;
    private ConfigLayoutsAdapter configLayoutsAdapter;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private ConfigColumnValuesActivity.IristickHUD iristickHUD;

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

        columnEnabledSwitch = findViewById(R.id.columnEnableSwitch);
        columnEnabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                LayoutColumn layoutColumn = new LayoutColumn(
                        selectedLayout.getUid(),
                        selectedColumn.getUid(),
                        (long) columnPositionSlider.getValue());

                layoutColumnList.add(layoutColumn);
                configLayoutsAdapter.setLayoutColumnDataset(layoutColumnList);
            } else {
                layoutColumnList.remove(selectedLayoutColumn);
                configLayoutsAdapter.setLayoutColumnDataset(layoutColumnList);
            }
        });

        columnPositionSlider = findViewById(R.id.columnPositionSlider);

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Layout Spinner
        layoutSpinner = findViewById(R.id.layoutSpinner);

        //Observe/Update layout list
        vsViewModel.findAllLayoutsLive().observe(this, layouts -> {
            layoutList.clear();
            layoutList.addAll(layouts);
        });

        layoutAdapter = new VDTSNamedAdapter<>(
                this,
                R.layout.adapter_spinner_named,
                layoutList);
        layoutAdapter.setToStringFunction((layout, integer) -> layout.getName());

        layoutSpinner.setAdapter(layoutAdapter);
        layoutSpinner.setOnItemSelectedListener(layoutSpinnerListener);

        //Recycler View
        layoutColumnRecyclerView = findViewById(R.id.layoutColumnRecyclerView);

        //Observe/Update layoutColumn list
        vsViewModel.findAllLayoutColumnsLive().observe(this, layoutColumns -> {
            layoutColumnList.clear();
            layoutColumnList.addAll(layoutColumns);
        });

        layoutColumnRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeLayoutList();
    }

    private void disableViews() {

    }

    private void initializeLayoutList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            layoutList.clear();
            layoutList.addAll(vsViewModel.findAllActiveLayouts());
            handler.post(() -> {
                layoutAdapter.notifyDataSetChanged();
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
                if (columnList.size() >= 1) {
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
            layoutColumnList.clear();
            if (layout != null) {
                layoutColumnList.addAll(vsViewModel.findAllLayoutColumnsByLayout(layout));
            }

            handler.post(() -> {
                configLayoutsAdapter = new ConfigLayoutsAdapter(
                        this,
                        new VDTSClickListenerUtil(this::configLayoutAdapterSelect,
                                layoutColumnRecyclerView),
                        columnList,
                        layoutColumnList
                );

                layoutColumnRecyclerView.setAdapter(configLayoutsAdapter);
            });
        });
    }

    private final AdapterView.OnItemSelectedListener layoutSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedLayout = (Layout) parent.getItemAtPosition(position);
                    layoutNameEditText.setText(selectedLayout.getName());
                    layoutExportCodeEditText.setText(selectedLayout.getExportCode());
                    initializeLayoutColumnList(selectedLayout);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    private void configLayoutAdapterSelect(Integer index) {
        configLayoutsAdapter.setSelectedColumn(index);
        if (index >= 0) {
            Pair<Column, LayoutColumn> columnLayoutColumnPair =
                    configLayoutsAdapter.getSelectedColumnLayoutColumn();
            if (columnLayoutColumnPair != null) {
                if (columnLayoutColumnPair.first != null) {
                    selectedColumn = columnLayoutColumnPair.first;
                    if (columnLayoutColumnPair.second != null) {
                        selectedLayoutColumn = columnLayoutColumnPair.second;

                        if (selectedColumn.getUid() == selectedLayoutColumn.getColumnID()) {
                            columnEnabledSwitch.setChecked(true);
                        } else {
                            columnEnabledSwitch.setChecked(false);
                        }

                        columnPositionSlider.setValue(selectedLayoutColumn.getColumnPosition());
                    } else {
                        columnEnabledSwitch.setChecked(false);
                        columnPositionSlider.setValue(1);
                    }
                }
            }
        } else {
            layoutNameEditText.setText("");
            layoutExportCodeEditText.setText("");
            layoutSpinner.setSelection(0);
            columnEnabledSwitch.setChecked(false);
            columnPositionSlider.setValue(1);
        }
    }

    private void newLayoutButtonOnClick() {
        layoutSpinner.setSelection(0);
        configLayoutAdapterSelect(-1);
        layoutNameEditText.requestFocus();
    }

    private void resetLayoutButtonOnClick() {
        configLayoutAdapterSelect(configLayoutsAdapter.getSelectedColumnIndex());
        layoutNameEditText.requestFocus();
    }

    private void saveLayoutButtonOnClick() {
        if (selectedLayout.getUid() == -9001L &&
                layoutNameEditText.getText().toString().isEmpty() &&
                        layoutExportCodeEditText.getText().toString().isEmpty()) {
            String message = "Create or select a layout";
            LOG.info(message);
            vdtsApplication.displayToast(this, message, 0);
        } else if (!layoutNameEditText.getText().toString().isEmpty() &&
                !layoutExportCodeEditText.getText().toString().isEmpty()) {
            if (selectedLayout.getUid() > 0) {
                //Update existing layout
                selectedLayout.setName(layoutNameEditText.getText().toString().trim());
                selectedLayout.setExportCode(layoutExportCodeEditText.getText().toString().trim());

                ExecutorService updateLayoutExecutor = Executors.newSingleThreadExecutor();
                Handler updateLayoutHandler = new Handler(Looper.getMainLooper());
                updateLayoutExecutor.execute(() -> {
                    vsViewModel.updateLayout(selectedLayout);
                    updateLayoutHandler.post(() -> {
                        String message = "Updated layout: " + selectedLayout.getName();
                        LOG.info(message);
                        vdtsApplication.displayToast(this, message, 0);
                        layoutAdapter.notifyDataSetChanged();

                        //Update existing layout columns
                        //todo - stuff n things
                        if (columnEnabledSwitch.isChecked()) {
                            if (selectedLayoutColumn == null) {
                                selectedLayoutColumn = new LayoutColumn(
                                        selectedLayout.getUid(),
                                        selectedColumn.getUid(),
                                        (long) columnPositionSlider.getValue()
                                );

                                ExecutorService insertLayoutColumnExecutor =
                                        Executors.newSingleThreadExecutor();
                                Handler insertLayoutColumnHandler = new Handler(Looper.getMainLooper());
                                insertLayoutColumnExecutor.execute(() -> {
                                    vsViewModel.insertLayoutColumn(selectedLayoutColumn);
                                    insertLayoutColumnHandler.post(() ->
                                            configLayoutsAdapter.addLayoutColumn(selectedLayoutColumn));
                                });
                            } else {
                                selectedLayoutColumn.setColumnID(selectedColumn.getUid());
                                selectedLayoutColumn.setColumnPosition(
                                        (long) columnPositionSlider.getValue());

                                ExecutorService updateLayoutColumnExecutor =
                                        Executors.newSingleThreadExecutor();
                                Handler updateLayoutColumnHandler = new Handler(Looper.getMainLooper());
                                updateLayoutColumnExecutor.execute(() -> {
                                    vsViewModel.updateLayoutColumn(selectedLayoutColumn);
                                    updateLayoutColumnHandler.post(() -> {
                                        configLayoutsAdapter.updateLayoutColumn(selectedLayoutColumn);
                                    });
                                });
                            }
                        }
                    });
                });
            } else {
                //Create new layout
                Layout layout = new Layout(
                        currentUser.getUid(),
                        layoutNameEditText.getText().toString(),
                        layoutExportCodeEditText.getText().toString()
                );

                ExecutorService createLayoutExecutor = Executors.newSingleThreadExecutor();
                Handler createLayoutHandler = new Handler(Looper.getMainLooper());
                createLayoutExecutor.execute(() -> {
                    long uid = vsViewModel.insertLayout(layout);
                    layout.setUid(uid);
                    createLayoutHandler.post(() -> {
                        String message = "Created layout: " + layout.getName();
                        LOG.info(message);
                        vdtsApplication.displayToast(this, message, 0);
                        layoutAdapter.add(layout);
                        layoutSpinner.setSelection(layoutAdapter.getPosition(layout));
                        layoutNameEditText.clearFocus();
                        layoutExportCodeEditText.clearFocus();
                    });
                });
            }
        } else {
            String message = "Layout must have a name and export code";
            LOG.info(message);
            vdtsApplication.displayToast(this, message, 0);
        }
    }

    private void deleteLayoutButtonOnClick() {
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