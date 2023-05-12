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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

public class DataGatheringActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Session currentSession;
    private boolean isNewSession = true;
    private Entry currentEntry;
    private ColumnValue selectedColumnValue;

    //Views
    private LinearLayout headerColumnLinearLayout;

    private TextView entryIndexValue;
    private LinearLayout entryValueLinearLayout;
    private Button entryCommentButton;
    private Button entryPhotoButton;

    private TextView sessionValue;
    private TextView sessionEntriesValue;
    private Button entrySaveButton;

    //Recycler View - Entry Spinners
    private VSViewModel vsViewModel;
    private VDTSNamedAdapter<ColumnValue> entryValueAdapter;

    //todo - Camera Stuff

    //Lists
    private final List<Column> headerColumnList = new ArrayList<>();
    private final List<ColumnValue> entryValueList = new ArrayList<>();

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

        headerColumnLinearLayout = findViewById(R.id.headerColumnLinearLayout);

        entryIndexValue = findViewById(R.id.entryIndexValue);
        entryValueLinearLayout = findViewById(R.id.entryValueLinearLayout);
        entryCommentButton = findViewById(R.id.entryCommentButton);
        entryPhotoButton = findViewById(R.id.entryPhotoButton);

        sessionValue = findViewById(R.id.sessionValue);
        sessionEntriesValue = findViewById(R.id.sessionEntriesValue);
        entrySaveButton = findViewById(R.id.entrySaveButton);

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        initializeSession();
        initializeHeaderColumns();
    }

    private void initializeSession() {
        //todo - update isNewSession
        if (isNewSession) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                int dailySessionCount = vsViewModel.countSessionsStartedToday();
                currentSession = new Session(
                        currentUser.getUid(),
                        currentUser.getSessionPrefix(),
                        dailySessionCount + 1);

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
                });
            });
        } else {
            //todo - resume existing session
        }
    }

    private void initializeHeaderColumns() {
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
            headerColumnList.clear();
            headerColumnList.addAll(vsViewModel.findAllActiveColumns());
            headerColumnList.remove(Column.COLUMN_NONE);
            handler.post(() -> {
                int index = 0;
                for (Column column : headerColumnList) {
                    TextView headerColumnText = new TextView(this);
                    headerColumnText.setId(index);
                    headerColumnText.setGravity(Gravity.CENTER);
                    headerColumnText.setLayoutParams(layoutParams);
                    headerColumnText.setPadding(dimen, dimen, dimen, dimen);
                    headerColumnText.setMaxLines(1);        //todo - maybe not a good idea
                    headerColumnText.setText(column.getNameCode());   //todo - use name instead????
                    headerColumnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

                    headerColumnLinearLayout.addView(headerColumnText);
                    index++;
                }

                initializeEntryValues();
            });
        });
    }

    private void initializeEntryValues() {
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

        int index = 0;
        for (Column column : headerColumnList) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            int finalIndex = index;
            executor.execute(() -> {
                entryValueList.clear();
                entryValueList.addAll(
                        vsViewModel.findAllActiveColumnValuesByColumn(column.getUid()));
                entryValueList.remove(ColumnValue.COLUMN_VALUE_NONE);
                handler.post(() -> {
                    entryValueAdapter = new VDTSNamedAdapter<>(
                            this,
                            R.layout.adapter_spinner_named,
                            entryValueList);
                    entryValueAdapter.setToStringFunction((columnValue, integer) ->
                            columnValue.getName());

                    Spinner entryValueSpinner = new Spinner(this);
                    entryValueSpinner.setId(finalIndex);
                    entryValueSpinner.setGravity(Gravity.CENTER);
                    entryValueSpinner.setLayoutParams(layoutParams);
                    entryValueSpinner.setPadding(dimen, dimen, dimen, dimen);

                    entryValueSpinner.setAdapter(entryValueAdapter);
                    entryValueSpinner.setOnItemSelectedListener(entryValueSpinnerListener);

                    entryValueLinearLayout.addView(entryValueSpinner);
                });
            });

            index++;
        }
    }

    private final AdapterView.OnItemSelectedListener entryValueSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedColumnValue = (ColumnValue) parent.getItemAtPosition(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            };

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
            //HUD layout
        }
    }
}
