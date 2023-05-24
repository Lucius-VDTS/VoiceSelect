package ca.vdts.voiceselect.activities.configure;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
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
    private int selectedLayoutIndex;

    //Lists
    private final List<Layout> layoutList = new ArrayList<>();
    private final HashMap<Long, Column> columnHashMap = new HashMap<>();
    List<Column> columnList = new ArrayList<>();
    private final List<LayoutColumn> layoutColumnList = new ArrayList<>();

    //Views
    private Button layoutNewButton;
    private Button layoutResetButton;
    private Button layoutSaveButton;
    private Button layoutDeleteButton;

    private TextView layoutNameEditText;
    private TextView layoutExportCodeEditText;

    private Spinner layoutSpinner;

    private RecyclerView columnLayoutRecyclerView;

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
        columnLayoutRecyclerView = findViewById(R.id.layoutColumnRecyclerView);

        //Observe/Update layoutColumn list
        vsViewModel.findAllLayoutColumnsLive().observe(this, layoutColumns -> {
            layoutColumnList.clear();
            layoutColumnList.addAll(layoutColumns);
        });

        columnLayoutRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
        ));

        configLayoutsAdapter = new ConfigLayoutsAdapter(
                this,
                new VDTSClickListenerUtil(this::configLayoutColumnAdapterSelect,
                        columnLayoutRecyclerView),
                columnHashMap,
                layoutColumnList
        );

        columnLayoutRecyclerView.setAdapter(configLayoutsAdapter);
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
                initializeLayoutColumnList();
            });
        });
    }

    private void initializeLayoutColumnList() {
        if (selectedLayout.getUid() == -9001L) {
            configLayoutsAdapter.setDataset(new);
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            layoutColumnList.clear();
            layoutColumnList.addAll(vsViewModel.findAllLayoutColumns());
            //todo - initialize column map
        });
    }
//
//    //todo initialize column map
//    private void initializeColumnMap() {
//
//    }

    private final AdapterView.OnItemSelectedListener layoutSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedLayout = (Layout) parent.getItemAtPosition(position);
                    layoutNameEditText.setText(selectedLayout.getName());
                    layoutExportCodeEditText.setText(selectedLayout.getExportCode());
                    selectedLayoutIndex = position;
                    //initializeLayoutColumnList();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    private void configLayoutColumnAdapterSelect(Integer index) {
        configLayoutsAdapter.setSelectedLayoutColumn(index);
        if (index >= 0) {
            final LayoutColumn selectedLayoutColumn =
                    configLayoutsAdapter.getSelectedLayoutColumn();

            if (selectedLayoutColumn != null) {
                columnEnabledSwitch.setChecked(selectedLayoutColumn.isColumnEnabled());
                columnPositionSlider.setValue(selectedLayoutColumn.getColumnPosition());
            } else {
                columnEnabledSwitch.setChecked(false);
                columnPositionSlider.setValue(1);
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
        configLayoutColumnAdapterSelect(-1);
        layoutNameEditText.requestFocus();
    }

    private void resetLayoutButtonOnClick() {
        configLayoutColumnAdapterSelect(selectedLayoutIndex);
        layoutNameEditText.requestFocus();
    }

    private void saveLayoutButtonOnClick() {
        if (selectedLayout.getUid() != -9001L) {
            //Update existing layout
            selectedLayout.setName(layoutNameEditText.getText().toString().trim());
            selectedLayout.setExportCode(layoutExportCodeEditText.getText().toString().trim());

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                vsViewModel.updateLayout(selectedLayout);
                handler.post(() -> {
                    String message = "Updated layout: " + selectedLayout.getName();
                    LOG.info(message);
                    vdtsApplication.displayToast(this, message, 0);
                    layoutAdapter.notifyDataSetChanged();
                });
            });
        } else {
            //Create new layout
            Layout layout = new Layout(
                    currentUser.getUid(),
                    layoutNameEditText.getText().toString(),
                    layoutExportCodeEditText.getText().toString()
            );

            ExecutorService layoutExecutor = Executors.newSingleThreadExecutor();
            Handler layoutHandler = new Handler(Looper.getMainLooper());
            layoutExecutor.execute(() -> {
                long uid = vsViewModel.insertLayout(layout);
                layout.setUid(uid);
                layoutHandler.post(() -> {
                    String message = "Created layout: " + layout.getName();
                    LOG.info(message);
                    vdtsApplication.displayToast(this, message, 0);
                    layoutAdapter.add(layout);
                    layoutSpinner.setSelection(layoutAdapter.getPosition(layout));
                });
            });

            //Create new layout column
            for (Column column : columnList) {
                LayoutColumn layoutColumn = new LayoutColumn(
                        layout.getUid(),
                        column.getUid(),
                        (long) columnPositionSlider.getValue(),
                        columnEnabledSwitch.isChecked()
                );

                ExecutorService layoutColumnExecutor = Executors.newSingleThreadExecutor();
                layoutColumnExecutor.execute(() -> {
                    vsViewModel.insertLayoutColumn(layoutColumn);
                });
                //todo save layoutcolumn
            }
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