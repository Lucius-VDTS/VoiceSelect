package ca.vdts.voiceselect.activities.dataGathering;

import android.content.res.Resources;
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
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

public class DataGatheringActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Session currentSession;
    private final boolean isNewSession = true;
    private Entry selectedEntry;
    private ColumnValue selectedColumnValue;

    //Lists
    private final List<Column> columnList = new ArrayList<>();
    private final HashMap<Integer, TextView> columnHashMap = new HashMap<>();

    private final List<ColumnValue> columnValueList = new ArrayList<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueMap = new HashMap<>();

    private final List<Entry> entryList = new ArrayList<>();
    private LiveData<List<Entry>> entryListLive;
    private final List<EntryValue> entryValueList = new ArrayList<>();
    private LiveData<List<EntryValue>> entryValueListLive;

    //Views
    private LinearLayout columnLinearLayout;
    private TextView columnValueIndexValue;
    private LinearLayout columnValueLinearLayout;
    private Button columnValueCommentButton;
    private Button columnValuePhotoButton;

    private Button entryDeleteButton;
    private Button entryResetButton;
    private Button entryRepeatButton;
    private Button entrySaveButton;

    private TextView sessionValue;
    private TextView sessionEntriesValue;

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

        columnLinearLayout = findViewById(R.id.columnsLinearLayout);

        columnValueIndexValue = findViewById(R.id.columnValuesIndexValue);
        columnValueLinearLayout = findViewById(R.id.columnLayoutLinearLayout);
        columnValueCommentButton = findViewById(R.id.columnValuesCommentButton);
        columnValuePhotoButton = findViewById(R.id.columnValuesPhotoButton);

        entryDeleteButton = findViewById(R.id.entryDeleteButton);
        entrySaveButton.setOnClickListener(v -> deleteEntryButtonOnClick());

        entryResetButton = findViewById(R.id.entryResetButton);
        entryResetButton.setOnClickListener(v -> resetEntryButtonOnClick());

        entryRepeatButton = findViewById(R.id.entryRepeatButton);
        entryRepeatButton.setOnClickListener(v -> repeatEntryButtonOnClick());

        entrySaveButton = findViewById(R.id.entrySaveButton);
        entrySaveButton.setOnClickListener(v -> saveEntryButtonOnClick());

        sessionValue = findViewById(R.id.sessionValue);
        sessionEntriesValue = findViewById(R.id.sessionEntriesValue);

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Recycler View
        entryRecyclerView = findViewById(R.id.entryRecyclerView);

        entryRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                ));

        dataGatheringAdapter = new DataGatheringAdapter(
                this,
                columnList,
                columnValueList,
                entryList,
                entryValueList
        );

        entryRecyclerView.setAdapter(dataGatheringAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        initializeSession();
    }

    private void initializeSession() {
        //todo - update isNewSession
        if (isNewSession) {
            //Create new session
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                int dailySessionCount = vsViewModel.countSessionsStartedToday();
                currentSession = new Session(
                        currentUser.getUid(),
                        currentUser.getSessionPrefix(),
                        dailySessionCount + 1);
                long uid = vsViewModel.insertSession(currentSession);
                currentSession.setUid(uid);
                LOG.info("Added session: {}", currentSession.getSessionPrefix());

                handler.post(() -> {
                    LocalDate today = LocalDate.now();
                    DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yy/MM/dd");
                    String formattedDate = today.format(datePattern);
                    String currentSessionString = String.format(
                            "%s %s-%o",
                            currentSession.getSessionPrefix(),
                            formattedDate,
                            currentSession.getDateIteration()
                    );
                    sessionValue.setText(currentSessionString);

                    initializeColumnsLayout();
                });
            });
        } else {
            //Resume existing session
            //todo - resume existing session
        }
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
        int dimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                resources.getDisplayMetrics()
        );
        layoutParams.setMargins(dimen, 0, dimen, 0);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnList.clear();
            columnList.addAll(vsViewModel.findAllActiveColumns());
            columnList.remove(Column.COLUMN_NONE);
            handler.post(() -> {
                int index = 0;
                for (Column column : columnList) {
                    TextView columnText = new TextView(this);
                    columnText.setId(index);
                    columnText.setGravity(Gravity.CENTER);
                    columnText.setLayoutParams(layoutParams);
                    columnText.setPadding(dimen, dimen, dimen, dimen);
                    columnText.setMaxLines(1);        //todo - maybe not a good idea
                    columnText.setText(column.getNameCode());   //todo - use name instead????
                    columnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

                    columnLinearLayout.addView(columnText);
                    index++;
                }

                initializeColumnValuesLayout();
            });
        });
    }

    /**
     * Programmatically generate value spinners based on the current session's columns
     */
    private void initializeColumnValuesLayout() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1
        );
        Resources resources = vdtsApplication.getResources();
        int dimen = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                4,
                resources.getDisplayMetrics()
        );
        layoutParams.setMargins(dimen, 0, dimen, 0);

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
                final List<Spinner> columnValueSpinners = new ArrayList<>();
                final HashMap<Integer, ColumnValueSpinnerAdapter> columnValueSpinnerAdapters = new HashMap<>();
                for (int index = 0; index < columnValueMap.size(); index++) {
                    columnValuesByColumn.clear();
                    columnValuesByColumn.addAll(Objects.requireNonNull(columnValueMap.get(index)));

//                    VDTSNamedAdapter<ColumnValue> columnValueAdapter = new VDTSNamedAdapter<>(
//                            this,
//                            R.layout.adapter_spinner_named,
//                            columnValuesByColumn);
//                    columnValueAdapter.setToStringFunction((columnValue, integer) ->
//                            columnValue.getName());

                    ColumnValueSpinnerAdapter columnValueSpinnerAdapter =
                            new ColumnValueSpinnerAdapter(
                                    this,
                                    columnValuesByColumn
                            );
                    columnValueSpinnerAdapters.put(index, columnValueSpinnerAdapter);

                    Spinner columnValueSpinner = new Spinner(this);
                    columnValueSpinner.setId(index);
                    columnValueSpinner.setGravity(Gravity.CENTER);
                    columnValueSpinner.setLayoutParams(layoutParams);
                    columnValueSpinner.setPadding(dimen, dimen, dimen, dimen);
                    //columnValueSpinner.setAdapter(columnValueAdapter);
                    columnValueSpinner.setAdapter((SpinnerAdapter) columnValueSpinnerAdapters.get(index));
                    columnValueSpinner.setOnItemSelectedListener(columnValueSpinnerListener);
                    columnValueSpinners.add(columnValueSpinner);

                    columnValueLinearLayout.addView(columnValueSpinners.get(index));
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
            handler.post(() -> {
                entryListLive.observe(this, entryObserver);
                initializeEntryValuesList();
            });
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
            handler.post(() -> {
                entryValueListLive.observe(this, entryValueObserver);
                initializeDGAdapter();
            });
        });
    }

    private void initializeDGAdapter() {
        dataGatheringAdapter = new DataGatheringAdapter(
                this,
                columnList,
                columnValueList,
                entryList,
                entryValueList
        );
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
                sessionEntriesValue.setText(String.format(
                        Locale.getDefault(), "%d", entries.size()));
            }
        }
    };

    private final Observer<List<EntryValue>> entryValueObserver = new Observer<List<EntryValue>>() {
        @Override
        public void onChanged(List<EntryValue> entryValues) {
            if (entryValues != null) {
                dataGatheringAdapter.clearValues();
                dataGatheringAdapter.addAllEntryValues(entryValues);
            }
        }
    };

    //todo adapterselect - select entry
//    private void entryAdapterSelect(int index) {
//        if (index >= 0) {
//            columnValueIndexValue.setText(index);
//            dataGatheringAdapter.setSelected(index - 1);
//            entryRecyclerView.scrollToPosition(dataGatheringAdapter.getItemCount() - 1 - index);
//            selectedEntry = dataGatheringAdapter.getEntry(index - 1);
//            entryValueList.clear();
//            entryValueList.addAll()
//        } else {
//
//        }
//    }

    private void deleteEntryButtonOnClick() {

    }

    private void resetEntryButtonOnClick() {

    }

    private void repeatEntryButtonOnClick() {

    }

    private void saveEntryButtonOnClick() {
//        if (selectedEntry != null) {
//            //Update existing entry
//            initializeEntriesList();
//        } else {
//            //Create new entry
//            Entry newEntry = new Entry(currentUser.getUid(), currentSession.getUid());
//            dataGatheringAdapter.addEntry(newEntry);
//
//            List<EntryValue> entryValues = new ArrayList<>();
//            for (int index = 0; index < spinnerList.size(); index++) {
//                ColumnValue columnValue = (ColumnValue) spinnerList.get(index).getSelectedItem();
//
//                EntryValue newEntryValue = new EntryValue(newEntry.getUid(), columnValue.getUid());
//                entryValues.add(newEntryValue);
//            }
//
//            dataGatheringAdapter.addAllEntryValues(entryValues);
//
//            index++;
//            columnValueIndexValue.setText(index + "");
//        }
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
