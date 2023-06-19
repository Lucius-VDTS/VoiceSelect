package ca.vdts.voiceselect.activities.dataGathering;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.adapters.DataGatheringAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

public class DataGatheringActivity extends AppCompatActivity
        implements IRIListener, ScrollChangeListenerInterface {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Session currentSession;
    private Entry selectedEntry;
    private ColumnValue selectedColumnValue;

    //Lists
    private final List<Column> columnList = new ArrayList<>();
    private final HashMap<Integer, TextView> columnMap = new HashMap<>();

    private final List<ColumnValue> columnValueList = new ArrayList<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueMap = new HashMap<>();
    private final List<Spinner> columnValueSpinnerList = new ArrayList<>();

    private final List<Entry> entryList = new ArrayList<>();
    private LiveData<List<Entry>> entryListLive;
    private final List<EntryValue> entryValueList = new ArrayList<>();
    private LiveData<List<EntryValue>> entryValueListLive;

    //Views
    private LinearLayout columnLinearLayout;
    public ObservableHorizontalScrollView columnScrollView;

    private TextView columnValueIndexValue;
    private LinearLayout columnValueLinearLayout;
    public ObservableHorizontalScrollView columnValueScrollView;
    private Button columnValueCommentButton;
    private Button columnValuePhotoButton;

    private Button entryDeleteButton;
    private Button entryResetButton;
    private Button entryRepeatButton;
    private Button entrySaveButton;

    private TextView sessionValue;
    private TextView sessionEntriesCount;
    private Button sessionEndButton;

    //Recycler View - Entry Spinners
    private VSViewModel vsViewModel;
    private DataGatheringAdapter dataGatheringAdapter;
    private RecyclerView entryRecyclerView;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private DataGatheringActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data_gathering);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        columnLinearLayout = findViewById(R.id.columnLinearLayout);
        columnScrollView = findViewById(R.id.columnScrollView);
        columnScrollView.setScrollChangeListener(this);

        columnValueIndexValue = findViewById(R.id.columnValueIndexValue);
        columnValueLinearLayout = findViewById(R.id.columnValueLinearLayout);
        columnValueScrollView = findViewById(R.id.columnValueScrollView);
        columnValueScrollView.setScrollChangeListener(this);
        columnValueCommentButton = findViewById(R.id.columnValueCommentButton);
        columnValuePhotoButton = findViewById(R.id.columnValuePhotoButton);

        entryDeleteButton = findViewById(R.id.entryDeleteButton);
        entryDeleteButton.setOnClickListener(v -> deleteEntryButtonOnClick());

        entryResetButton = findViewById(R.id.entryResetButton);
        entryResetButton.setOnClickListener(v -> resetEntryButtonOnClick());

        entryRepeatButton = findViewById(R.id.entryRepeatButton);
        entryRepeatButton.setOnClickListener(v -> repeatEntryButtonOnClick());

        entrySaveButton = findViewById(R.id.entrySaveButton);
        entrySaveButton.setOnClickListener(v -> saveEntryButtonOnClick());

        sessionValue = findViewById(R.id.sessionValue);
        sessionEntriesCount = findViewById(R.id.sessionEntriesCount);

        sessionEndButton = findViewById(R.id.sessionEndButton);
        sessionEndButton.setOnClickListener(v -> endSessionButtonOnClick());

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Recycler View
        entryRecyclerView = findViewById(R.id.entryRecyclerView);
        entryRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                ));
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeSession();
    }

    private void initializeSession() {
        String currentSessionKey = currentUser.getExportCode().concat("_SESSION");
        long currentSessionID = vdtsApplication.getPreferences().getLong(currentSessionKey);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            currentSession = vsViewModel.findSessionByID(currentSessionID);
            handler.post(() -> {
                sessionValue.setText(currentSession.name());
                initializeColumnsLayout();
            });
        });
    }

    /**
     * Programmatically generate column headers based on the current session's layout
     */
    private void initializeColumnsLayout() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        Resources resources = vdtsApplication.getResources();

        int minWidthDimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                96,
                resources.getDisplayMetrics()
        );

        int marginPaddingDimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2,
                resources.getDisplayMetrics()
        );
        layoutParams.setMargins(marginPaddingDimen, 0, marginPaddingDimen, 0);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnList.clear();
            columnList.addAll(vsViewModel.findAllActiveColumns());
            columnList.remove(Column.COLUMN_NONE);
            handler.post(() -> {
                for (Column column : columnList) {
                    TextView columnText = new TextView(this);
                    columnText.setMinWidth(minWidthDimen);
                    columnText.setLayoutParams(layoutParams);
                    columnText.setPadding(marginPaddingDimen, marginPaddingDimen,
                            marginPaddingDimen, marginPaddingDimen);
                    columnText.setGravity(Gravity.CENTER);
                    columnText.setMaxLines(1);
                    columnText.setBackground(
                            ContextCompat.getDrawable(this, R.drawable.text_background));
                    columnText.setText(column.getNameCode());   //todo - base this on setting?
                    columnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    columnLinearLayout.addView(columnText);
                }

                initializeColumnValuesLayout();
            });
        });
    }

    /**
     * Programmatically generate value spinners based on the current session's layout
     */
    private void initializeColumnValuesLayout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            int columnValueMapIndex = 0;
            for (Column column : columnList) {
                columnValueMap.put(
                        columnValueMapIndex,
                        vsViewModel.findAllActiveColumnValuesByColumn(column.getUid()));
                columnValueMapIndex++;
            }

            handler.post(() -> {
                final List<ColumnValue> columnValuesByColumn = new ArrayList<>();
                columnValueSpinnerList.clear();
                for (int index = 0; index < columnValueMap.size(); index++) {
                    columnValuesByColumn.clear();
                    columnValuesByColumn.addAll(Objects.requireNonNull(columnValueMap.get(index)));

                    ColumnValueSpinner columnValueSpinner =
                            new ColumnValueSpinner(
                                    this,
                                    columnValuesByColumn,
                                    columnValueSpinnerListener
                            );

                    columnValueSpinnerList.add(columnValueSpinner.getColumnValueSpinner());
                    columnValueLinearLayout.addView(columnValueSpinner.getColumnValueSpinner());
                }
                initializeEntriesList();
            });
        });
    }

    private void initializeEntriesList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            entryList.clear();
            entryList.addAll(vsViewModel.findAllEntriesBySession(currentSession.getUid()));

//            entryListLive.removeObservers(this);
            entryListLive = vsViewModel.findAllEntriesBySessionLive(currentSession.getUid());
            handler.post(this::initializeEntryValuesList);
        });
    }

    private void initializeEntryValuesList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            entryValueList.clear();
            entryValueList.addAll(vsViewModel.findAllEntryValuesBySession(currentSession.getUid()));

//            entryValueListLive.removeObservers(this);
            entryValueListLive = vsViewModel.findAllEntryValuesLiveBySession(currentSession.getUid());
            handler.post(this::initializeDGAdapter);
        });
    }

    private void initializeDGAdapter() {
        dataGatheringAdapter = new DataGatheringAdapter(
                this,
                this,
                new VDTSClickListenerUtil(this::entryAdapterSelect, entryRecyclerView)
        );

        dataGatheringAdapter.setDatasets(
                columnList,
                columnValueMap,
                entryList,
                entryValueList);

        entryRecyclerView.setAdapter(dataGatheringAdapter);

        entryListLive.observe(this, entryObserver);
        entryValueListLive.observe(this, entryValueObserver);

        columnValueIndexValue.setText(String.format(
                Locale.getDefault(),
                "%d",
                dataGatheringAdapter.getItemCount() + 1));
    }

    /**
     * Observe scroll changes in horizontal scroll views and synchronize their position.
     * @param observableHorizontalScrollView - The scroll view being observed
     * @param x - The horizontal position (pixels) that the view will scroll to
     * @param y - The vertical position (pixels) that the view will scroll to
     * @param oldx - The original horizontal position (pixels) of the scroll view
     * @param oldy - The original vertical position (pixels) of the scroll view
     */
    @Override
    public void onScrollChanged(ObservableHorizontalScrollView observableHorizontalScrollView,
                                int x, int y,
                                int oldx, int oldy) {
//        if (observableHorizontalScrollView == columnScrollView) {
//            columnValueScrollView.smoothScrollTo(x, y);
//            dataGatheringAdapter.setXCord(x);
//        } else if (observableHorizontalScrollView == columnValueScrollView) {
//            columnScrollView.smoothScrollTo(x, y);
//            dataGatheringAdapter.setXCord(x);
//        } else if (observableHorizontalScrollView == null) {
//            columnScrollView.smoothScrollTo(x, y);
//            columnValueScrollView.smoothScrollTo(x, y);
//        }

        if (observableHorizontalScrollView == columnScrollView) {
            columnValueScrollView.scrollTo(x, y);
            dataGatheringAdapter.setXCord(x);
        } else if (observableHorizontalScrollView == columnValueScrollView) {
            columnScrollView.scrollTo(x, y);
            dataGatheringAdapter.setXCord(x);
        } else if (observableHorizontalScrollView == null) {
            columnScrollView.scrollTo(x, y);
            columnValueScrollView.smoothScrollTo(x, y);
        }
    }

    private final AdapterView.OnItemSelectedListener columnValueSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedColumnValue = (ColumnValue) parent.getItemAtPosition(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

    private final Observer<List<Entry>> entryObserver = new Observer<List<Entry>>() {
        @Override
        public void onChanged(List<Entry> entries) {
            if (entries != null) {
                dataGatheringAdapter.clearEntries();
                dataGatheringAdapter.addAllEntries(entries);
                sessionEntriesCount.setText(String.format(
                        Locale.getDefault(), "%d", entries.size()));
            }
        }
    };

    private final Observer<List<EntryValue>> entryValueObserver = new Observer<List<EntryValue>>() {
        @Override
        public void onChanged(List<EntryValue> entryValues) {
            if (entryValues != null) {
                dataGatheringAdapter.clearEntryValues();
                dataGatheringAdapter.addAllEntryValues(entryValues);
            }
        }
    };

    //todo adapterselect - select entry
    private void entryAdapterSelect(int index) {
        if (index >= 0) {
            columnValueIndexValue.setText(index);
            dataGatheringAdapter.setSelected(index - 1);
            entryRecyclerView.scrollToPosition(dataGatheringAdapter.getItemCount() - 1 - index);
            selectedEntry = dataGatheringAdapter.getEntry(index - 1);
            entryValueList.clear();
        } else {

        }
    }

    private void deleteEntryButtonOnClick() {

    }

    private void resetEntryButtonOnClick() {

    }

    private void repeatEntryButtonOnClick() {

    }

    private void endSessionButtonOnClick() {

    }

    private void saveEntryButtonOnClick() {
        if (selectedEntry != null) {
            //Update existing entry
        } else {
            //Create new entry
            Entry newEntry = new Entry(
                    currentUser.getUid(),
                    currentSession.getUid()
            );

            ExecutorService createEntryExecutor = Executors.newSingleThreadExecutor();
            Handler createEntryHandler = new Handler(Looper.getMainLooper());
            createEntryExecutor.execute(() -> {
                long uid = vsViewModel.insertEntry(newEntry);
                newEntry.setUid(uid);
                createEntryHandler.post(() -> {
                    entryValueList.clear();
                    for (int index = 0; index < columnValueSpinnerList.size(); index++) {
                        ColumnValue columnValue =
                                (ColumnValue) columnValueSpinnerList.get(index).getSelectedItem();

                        EntryValue newEntryValue;
                        if (columnValue != null) {
                            newEntryValue = new EntryValue(newEntry.getUid(), columnValue.getUid());
                        } else {
                            newEntryValue = new EntryValue(newEntry.getUid());
                        }
                        entryValueList.add(index, newEntryValue);
                    }

                    EntryValue[] entryValues = new EntryValue[entryValueList.size()];
                    entryValueList.toArray(entryValues);

                    ExecutorService createEntryValuesExecutor = Executors.newSingleThreadExecutor();
                    Handler createEntryValuesHandler = new Handler(Looper.getMainLooper());
                    createEntryValuesExecutor.execute(() -> {
                        vsViewModel.insertAllEntryValues(entryValues);
                        createEntryValuesHandler.post(() -> {
                            columnValueIndexValue.setText(String.format(
                                    Locale.getDefault(),
                                    "%d",
                                    dataGatheringAdapter.getItemCount() + 1)
                            );

//                            dataGatheringAdapter.setXCord(0);
//                            columnValueScrollView.smoothScrollTo(0, 0);
//                            columnScrollView.smoothScrollTo(0, 0);

//                            columnScrollView.smoothScrollTo(0, 0);

//                            dataGatheringAdapter.setXCord(0);

                            columnScrollView.setScrollX(0);
//                            columnScrollView.smoothScrollTo(0, 0);
                        });
                    });
                });
            });
        }
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
                iristickHUD = new DataGatheringActivity.IristickHUD();
                return iristickHUD;
            });
        }
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_data_gathering_hud);

            //HUD Views
        }
    }
}
