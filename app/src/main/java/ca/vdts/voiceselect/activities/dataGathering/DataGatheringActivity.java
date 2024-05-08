package ca.vdts.voiceselect.activities.dataGathering;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PULSE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.PULSE_REPEAT;
import static ca.vdts.voiceselect.library.utilities.VDTSLocationUtil.isBetterLocation;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Range;
import android.util.Rational;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalExposureCompensation;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.google.common.util.concurrent.ListenableFuture;
import com.iristick.sdk.Experimental;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.camera.IRICamera;
import com.iristick.sdk.camera.IRICameraAFStrategy;
import com.iristick.sdk.camera.IRICameraProfile;
import com.iristick.sdk.camera.IRICameraSession;
import com.iristick.sdk.camera.IRICameraType;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.adapters.DataGatheringRecyclerAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSNamedPositionedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;
import ca.vdts.voiceselect.library.utilities.VDTSCustomLifecycleUtil;
import ca.vdts.voiceselect.library.utilities.VDTSImageFileUtil;
import ca.vdts.voiceselect.library.utilities.VDTSMediaScannerUtil;

/**
 * Gather data for a particular session and its corresponding layout.
 */
@SuppressLint("RestrictedApi")
public class DataGatheringActivity extends AppCompatActivity
        implements IRIListener, ScrollChangeListenerInterface, LocationListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private TextToSpeech currentUserTTSEngine;
    private int currentUserFeedbackMode;

    private Session currentSession;
    private Entry currentEntry;
    private ColumnValue selectedColumnValue;
    private int selectedEntryIndex = 0;
    private int spokenPosition = 0;
    private int spinnerPosition = 0;
    private int listenerCounter = 0;
    private boolean isColumnNext = false;
    private boolean isColumnPrevious = false;
    private boolean isEntrySelected = false;
    private boolean isDeleteLast = false;
    private boolean isVoiceComment = false;
    private boolean isVoiceCommentAppend = false;
    private boolean isRecordVideo = false;

    //Lists
    private List<SessionLayout> currentSessionLayoutList;
    private final HashMap<Integer, Column> columnMap = new HashMap<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueMap = new HashMap<>();
    private final HashMap<Integer, List<ColumnValueSpoken>> columnValueSpokenMap = new HashMap<>();
    private final HashMap<Integer, HashMap<String, Long>> positionColumnValueSpokenIDMap = new HashMap<>();
    private final List<Spinner> columnValueSpinnerList = new ArrayList<>();

    private final List<Entry> entryList = new ArrayList<>();
    private LiveData<List<Entry>> entryListLive;
    private final List<EntryValue> entryValueList = new ArrayList<>();
    private LiveData<List<EntryValue>> entryValueListLive;

    private final List<PictureReference> pictureReferenceList = new ArrayList<>();
    private LiveData<List<PictureReference>> pictureReferencesListLive;
    private final List<PictureReference> currentEntryPhotoList = new ArrayList<>();

    //Views
    private LinearLayout columnLinearLayout;
    public ObservableHorizontalScrollView columnScrollView;

    private TextView columnValueIndexValue;
    private LinearLayout columnValueLinearLayout;
    public ObservableHorizontalScrollView columnValueScrollView;
    private Button columnValueCommentButton;
    private Button columnValuePictureButton;

    private Button entryDeleteButton;
    private Button entryResetButton;
    private Button entryRepeatButton;
    private Button entrySaveButton;

    private TextView sessionValue;
    private TextView sessionEntriesCount;
    private Button sessionEndButton;

    //Recycler View - Spinners
    private VSViewModel vsViewModel;
    private DataGatheringRecyclerAdapter dataGatheringRecyclerAdapter;
    private RecyclerView entryRecyclerView;
    private VDTSNamedPositionedAdapter<?> vdtsNamedPositionedAdapter;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private DataGatheringActivity.IristickHUD iristickHUD;
    private final HashMap<Integer, String> entryHUDMap = new HashMap<>();
    private IRICamera iriCamera;
    private IRICameraSession iriCameraSession;
    private boolean iriCameraCaptureInProgress = false;
    private int cameraType = -1;

    //GPS Components
    private boolean GPSConnected = false;
    private LocationManager locationManager;
    private Location currentLocation;
    private static final int PERM_CODE_GPS = 2;

    //Device Camera Components
    private PreviewView previewView;
    private VDTSCustomLifecycleUtil cameraLifecycle;
    private Camera camera;
    private GestureDetector scrollDetector;
    private ScaleGestureDetector scaleDetector;
    private ImageCapture imageCapture;
    private boolean previewShowing = false;
    private static final int EXPOSURE_LEVELS = 10;
    private int exposureLevel = EXPOSURE_LEVELS / 2;
    private float zoomRatio = 1.0f;
    private float minZoomRatio = 1.0f;
    private float maxZoomRatio = 1.0f;

    //Lock to prevent concurrent list filling issues
    private ReentrantLock adapterLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data_gathering);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();

        currentUser = vdtsApplication.getCurrentUser();
        currentUserTTSEngine = vdtsApplication.getTTSEngine();
        if (currentUser.isFeedbackFlushQueue()) {
            currentUserFeedbackMode = 0;
        } else {
            currentUserFeedbackMode = 1;
        }

        adapterLock = new ReentrantLock();

        columnLinearLayout = findViewById(R.id.columnLinearLayout);
        columnScrollView = findViewById(R.id.columnScrollView);
        columnScrollView.setScrollChangeListener(this);

        columnValueIndexValue = findViewById(R.id.columnValueIndexValue);
        columnValueLinearLayout = findViewById(R.id.columnValueLinearLayout);
        columnValueScrollView = findViewById(R.id.columnValueScrollView);
        columnValueScrollView.setScrollChangeListener(this);
        columnValueCommentButton = findViewById(R.id.columnValueCommentButton);
        columnValueCommentButton.setOnClickListener(v -> commentButtonOnClick());
        columnValuePictureButton = findViewById(R.id.columnValuePhotoButton);
        columnValuePictureButton.setOnClickListener(v -> pictureButtonOnClick());

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
                )
        );

        //Camera
        scaleDetector = new ScaleGestureDetector(this, new ScaleListener());
        scrollDetector = new GestureDetector(this, new ScrollListener());
        previewView = findViewById(R.id.cameraPreview);
        previewView.setOnTouchListener(onTouchListener);

        cameraLifecycle = new VDTSCustomLifecycleUtil();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider
                .getInstance(this);
        cameraProviderFuture.addListener(
                () -> {
                    try {
                        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                        //todo - Add camera existing check
                        //todo - Rework to allow for camera selection
                        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                        imageCapture = new ImageCapture.Builder()
                                .setCameraSelector(cameraSelector)
                                .build();
                        Preview preview = new Preview.Builder().build();
                        preview.setSurfaceProvider(previewView.getSurfaceProvider());
                        camera = cameraProvider.bindToLifecycle(
                                cameraLifecycle,
                                cameraSelector,
                                imageCapture,
                                preview
                        );

                        camera.getCameraInfo().getZoomState().observe(
                                this,
                                observer -> {
                                    zoomRatio = observer.getZoomRatio();
                                    minZoomRatio = observer.getMinZoomRatio();
                                    maxZoomRatio = observer.getMaxZoomRatio();

                                    String logMessage = String.format(
                                            Locale.getDefault(),
                                            "Zoom Ratio: %.2f, Min Zoom Ratio: %.2f, Max Zoom Ratio: %.2f",
                                            zoomRatio,
                                            minZoomRatio,
                                            maxZoomRatio
                                    );
                                    LOG.debug(logMessage);

                                    String message = String.format(
                                            Locale.getDefault(),
                                            "Zoom Level: %.2f",
                                            zoomRatio
                                    );
                                    LOG.debug(message);
                                }
                        );

                        cameraLifecycle.performEvent(Lifecycle.Event.ON_CREATE);
                    } catch (ExecutionException | InterruptedException e) {
                        LOG.error("Error starting preview: ", e);
                    }
                },
                ContextCompat.getMainExecutor(this)
        );
    }

    @Override
    protected void onPause() {
        cameraLifecycle.performEvent(Lifecycle.Event.ON_PAUSE);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraLifecycle.performEvent(Lifecycle.Event.ON_RESUME);
        entryRecyclerView.bringToFront();
        if (previewShowing) {
            showPreview();
        } else {
            hidePreview();
        }

        initializeSession();
    }

    @Override
    protected void onDestroy() {
        cameraLifecycle.performEvent(Lifecycle.Event.ON_DESTROY);
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        enableGPS();
    }

    @Override
    protected void onStop() {
        super.onStop();
        disableGPS();
    }

    @Override
    public void onBackPressed() {
        if (previewShowing) {
            runOnUiThread(
                    () -> vdtsApplication.displayToast(
                            this,
                            "Use long press to exit camera"
                    )
            );
        } else {
            super.onBackPressed();
        }
    }

    private void initializeSession() {
        String currentSessionKey = currentUser.getExportCode().concat("_SESSION");
        long currentSessionID = vdtsApplication.getVDTSPrefKeyValue().getLong(currentSessionKey);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            currentSession = vsViewModel.findSessionByID(currentSessionID);
            handler.post(() -> {
                sessionValue.setText(currentSession.name());
                initializeSessionLayout();
            });
        });
    }

    private void initializeSessionLayout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            adapterLock.lock();
            currentSessionLayoutList =
                    vsViewModel.findAllSessionLayoutsBySession(currentSession.getUid());

            columnMap.clear();
            for (SessionLayout sessionLayout : currentSessionLayoutList) {
                Column column = vsViewModel.findColumnByID(sessionLayout.getColumnID());
                columnMap.put(sessionLayout.getColumnPosition() - 1, column);
            }

            handler.post(this::initializeColumnsLayout);
            adapterLock.unlock();
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

        if (columnMap.size() != 0) {
            for (int index = 0; index < columnMap.size(); index++) {
                TextView columnText = new TextView(this);
                columnText.setMinWidth(minWidthDimen);
                columnText.setLayoutParams(layoutParams);
                columnText.setPadding(marginPaddingDimen, marginPaddingDimen,
                        marginPaddingDimen, marginPaddingDimen);
                columnText.setGravity(Gravity.CENTER);
                columnText.setMaxLines(1);
                columnText.setBackground(
                        ContextCompat.getDrawable(this, R.drawable.edit_text_background));
                columnText.setText(currentUser.isAbbreviate() ?
                        Objects.requireNonNull(columnMap.get(index)).getNameCode() :
                        Objects.requireNonNull(columnMap.get(index)).getName());
                columnText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
                columnLinearLayout.addView(columnText);
            }
        }

        initializeColumnValuesLayout();
    }

    /**
     * Programmatically generate value spinners based on the current session's layout
     */
    private void initializeColumnValuesLayout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            columnValueMap.clear();
            columnValueSpokenMap.clear();

            List<ColumnValueSpoken> columnValueSpokenList;
            for (int index = 0; index < columnMap.size(); index++) {
                columnValueMap.put(
                        index,
                        vsViewModel.findAllActiveColumnValuesByColumn(
                                Objects.requireNonNull(columnMap.get(index)).getUid()
                        )
                );

                columnValueSpokenList = vsViewModel.findAllColumnValueSpokensByColumnAndUser(
                        Objects.requireNonNull(columnMap.get(index)).getUid(),
                        currentUser.getUid());

                columnValueSpokenMap.put(
                        index,
                        columnValueSpokenList
                );

                HashMap<String, Long> columnValueSpokenIDMap = new HashMap<>();
                for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
                    columnValueSpokenIDMap.put(
                            columnValueSpoken.getSpoken(),
                            columnValueSpoken.getColumnValueID());
                }
                positionColumnValueSpokenIDMap.put(index, columnValueSpokenIDMap);
            }

            handler.post(() -> {
                for (int index = 0; index < columnValueMap.size(); index++) {
                    ColumnValueSpinner columnValueSpinner = new ColumnValueSpinner(
                            this,
                            currentUser,
                            columnValueMap.get(index),
                            columnValueSpinnerListener,
                            index
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
            entryListLive = vsViewModel.findAllEntriesBySessionLive(currentSession.getUid());
            handler.post(this::initializeEntryValuesList);
        });
    }

    private void initializeEntryValuesList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            entryValueListLive = vsViewModel.findAllEntryValuesLiveBySession(
                    currentSession.getUid()
            );
            handler.post(this::initializePictureReferenceList);
        });
    }

    private void initializePictureReferenceList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            pictureReferenceList.clear();
            pictureReferenceList.addAll(
                    vsViewModel.findPictureReferencesBySession(currentSession.getUid())
            );
            pictureReferencesListLive = vsViewModel.findPictureReferencesLiveBySession(
                    currentSession.getUid()
            );
            handler.post(this::initializeDGAdapter);
        });
    }

    private void initializeDGAdapter() {
        dataGatheringRecyclerAdapter = new DataGatheringRecyclerAdapter(
                this,
                this,
                currentUser,
                new VDTSClickListenerUtil(this::entryAdapterSelect, entryRecyclerView)
        );

        dataGatheringRecyclerAdapter.setDatasets(
                columnMap,
                columnValueMap,
                entryList,
                entryValueList,
                pictureReferenceList
        );

        entryRecyclerView.setAdapter(dataGatheringRecyclerAdapter);

        entryListLive.observe(this, entryObserver);
        entryValueListLive.observe(this, entryValueObserver);
        pictureReferencesListLive.observe(this, pictureReferenceObserver);

        columnValueIndexValue.setText(
                String.format(
                        Locale.getDefault(),
                        "%d",
                        dataGatheringRecyclerAdapter.getItemCount() + 1
                )
        );

        newEntry();

        if (isHeadsetAvailable) {
            iristickInitializeEntryGrammar();
        }
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
                                int x, int y, int oldx, int oldy) {
        if (observableHorizontalScrollView == columnScrollView) {
            columnValueScrollView.scrollTo(x, y);
            dataGatheringRecyclerAdapter.setXCord(x);
        } else if (observableHorizontalScrollView == columnValueScrollView) {
            columnScrollView.scrollTo(x, y);
            dataGatheringRecyclerAdapter.setXCord(x);
        } else if (observableHorizontalScrollView == null) {
            columnScrollView.scrollTo(x, y);
            columnValueScrollView.scrollTo(x, y);
        }
    }

    private final AdapterView.OnItemSelectedListener columnValueSpinnerListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                                           int position, long id) {
                    selectedColumnValue = (ColumnValue) parent.getItemAtPosition(position);
                    ((TextView) parent.getChildAt(0)).setTextColor(Color.BLACK);

                    vdtsNamedPositionedAdapter =
                            (VDTSNamedPositionedAdapter<?>) parent.getAdapter();

                    if (isHeadsetAvailable) {
                        iristickUpdateHUDViews();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };

    private final Observer<List<Entry>> entryObserver = new Observer<List<Entry>>() {
        @Override
        public void onChanged(List<Entry> entries) {
            if (entries != null) {
                dataGatheringRecyclerAdapter.clearEntries();
                dataGatheringRecyclerAdapter.addAllEntries(entries);

                entryList.clear();
                entryList.addAll(entries);

                columnValueIndexValue.setText(
                        String.format(Locale.getDefault(), "%d", entries.size() + 1)
                );
                sessionEntriesCount.setText(
                        String.format(Locale.getDefault(), "%d", entries.size())
                );

                if (isHeadsetAvailable) {
                    iristickUpdateHUDViews();

                    if (entryList.size() > 0) {
                        iristickInitializeSelectEntryGrammar();
                    }
                }
            }
        }
    };

    private final Observer<List<EntryValue>> entryValueObserver = new Observer<List<EntryValue>>() {
        @Override
        public void onChanged(List<EntryValue> entryValues) {
            if (entryValues != null) {
                dataGatheringRecyclerAdapter.clearEntryValues();
                dataGatheringRecyclerAdapter.addAllEntryValues(entryValues);
            }
        }
    };

    private final Observer<List<PictureReference>> pictureReferenceObserver = new Observer<List<PictureReference>>() {
        @Override
        public void onChanged(List<PictureReference> pictureReferences) {
            if (pictureReferences != null) {
                dataGatheringRecyclerAdapter.clearPictureReferences();
                dataGatheringRecyclerAdapter.addAllPictureReferences(pictureReferences);
            }
        }
    };

    private void entryAdapterSelect(Integer index) {
        if (index != null) {
            dataGatheringRecyclerAdapter.setSelected(index);
            currentEntry = dataGatheringRecyclerAdapter.getEntry(index);
            currentEntryPhotoList.clear();
            currentEntryPhotoList.addAll(dataGatheringRecyclerAdapter.getPictureReferences(index));
            selectedColumnValue = null;
            isEntrySelected = true;
            isColumnNext = false;
            isColumnPrevious = false;
            entryHUDMap.clear();
        } else {
            newEntry();
        }

        connectedDeviceUpdateViews();
        iristickUpdateHUDViews();
    }

    private void connectedDeviceUpdateViews() {
        runOnUiThread(() -> {
            if (currentEntry.getUid() > 0) {
                List<EntryValue> entryValues = entryValueListLive.getValue();
                if (entryValues != null) {
                    List<EntryValue> currentEntryValues = entryValues.stream()
                            .filter(entryValue -> entryValue.getEntryID() == currentEntry.getUid())
                            .collect(Collectors.toList());

                    for (int columnIndex = 0; columnIndex < columnValueSpinnerList.size(); columnIndex++) {
                        List<ColumnValue> columnValueList = columnValueMap.get(columnIndex);
                        if (columnValueList != null) {
                            ColumnValue columnValue = columnValueList.stream()
                                    .filter(
                                            cv -> currentEntryValues.stream()
                                                    .anyMatch(
                                                            ev -> ev.getColumnValueID() == cv.getUid()
                                                    )
                                    ).findFirst()
                                    .orElse(null);

                            Spinner columnSpinner = columnValueSpinnerList.get(columnIndex);
                            if (columnSpinner != null) {
                                columnSpinner.setEnabled(false);
                                if (columnValue == null) {
                                    columnSpinner.setSelection(0);
                                } else {
                                    int position = columnValueList.indexOf(columnValue) + 1;
                                    columnSpinner.setSelection(position);
                                }
                                columnSpinner.setEnabled(true);
                            }
                        }
                    }
                }

                columnValueIndexValue.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d",
                                dataGatheringRecyclerAdapter
                                        .getEntryPosition(currentEntry.getUid()) + 1
                        )
                );
            } else {
                columnValueIndexValue.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d",
                                dataGatheringRecyclerAdapter.getItemCount() + 1
                        )
                );

                for (int columnIndex = 0; columnIndex < columnValueSpinnerList.size(); columnIndex++) {
                    columnValueSpinnerList.get(columnIndex).setSelection(0);
                }
            }
        });
    }

    private void commentButtonOnClick() {
        showCommentDialogue();
    }

    private void pictureButtonOnClick() {
        if (cameraLifecycle.getLifecycle().getCurrentState() == Lifecycle.State.STARTED) {
            hidePreview();
        } else {
            showPreview();
        }
    }

    private void deleteEntryButtonOnClick() {
        if (currentEntry == null || currentEntry.getUid() == 0) {
            resetEntryButtonOnClick();
        } else {
            confirmDeleteEntryDialogue();
        }
    }

    private void confirmDeleteEntryDialogue() {
        LOG.info("Showing Deletion Confirm Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Entry");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_yes_no,
                null
        );
        builder.setView(customLayout);
        TextView label = customLayout.findViewById(R.id.mainLabel);
        label.setText("Entry and associated pictures shall be permanently deleted.\nDelete Entry?");
        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        yesButton.setOnClickListener(view -> {
            finalDialog.dismiss();
            deleteEntry(false);
        });

        noButton.setOnClickListener(view -> finalDialog.dismiss());
    }

    private void deleteEntry(boolean isDeleteLast) {
        ExecutorService deletePicturesService = Executors.newSingleThreadExecutor();
        Handler deletePicturesHandler = new Handler(Looper.getMainLooper());
        deletePicturesService.execute(() -> {
            currentEntryPhotoList.forEach(this::deletePicture);
            final PictureReference[] pictureReferences = new PictureReference[currentEntryPhotoList.size()];
            currentEntryPhotoList.toArray(pictureReferences);
            vsViewModel.deleteAllPictureReferences(pictureReferences);
            deletePicturesHandler.post(() -> {
                final long deleteEntryID;
                if (isDeleteLast) {
                    deleteEntryID = entryList.size();
                    currentEntry = entryList.get((int) (deleteEntryID - 1));
                } else {
                    deleteEntryID = currentEntry.getUid();
                }

                List<EntryValue> entryValueList = entryValueListLive.getValue();
                if (entryValueList != null) {
                    entryValueList = entryValueList.stream()
                            .filter(entryValue -> entryValue.getEntryID() == deleteEntryID)
                            .collect(Collectors.toList());
                } else {
                    entryValueList = new ArrayList<>();
                }
                final EntryValue[] entryValues = new EntryValue[entryValueList.size()];
                entryValueList.toArray(entryValues);

                ExecutorService deleteEntryValueService = Executors.newSingleThreadExecutor();
                Handler deleteEntryValueHandler = new Handler(Looper.getMainLooper());
                deleteEntryValueService.execute(() -> {
                    vsViewModel.deleteAllEntryValues(entryValues);
                    deleteEntryValueHandler.post(() -> {
                        ExecutorService deleteEntryService = Executors.newSingleThreadExecutor();
                        Handler deleteEntryHandler = new Handler(Looper.getMainLooper());
                        deleteEntryService.execute(() -> {
                            vsViewModel.deleteEntry(currentEntry);
                            deleteEntryHandler.post(() -> {
                                dataGatheringRecyclerAdapter.removeEntry(currentEntry);
                                newEntry();
                                connectedDeviceUpdateViews();
                            });
                        });
                    });
                });
            });
        });

        listenerCounter = 0;
    }

    private void deletePicture(PictureReference pictureReference) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                () -> {
                    String[] projection = {MediaStore.Images.Media._ID};
                    String selection = MediaStore.Images.Media.DATA + " = ?";
                    String[] selectionArgs = new String[]{pictureReference.getPath()};

                    Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    ContentResolver contentResolver = getContentResolver();
                    Cursor cursor = contentResolver.query(
                            queryUri,
                            projection,
                            selection,
                            selectionArgs,
                            null
                    );

                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            long id = cursor.getLong(
                                    cursor.getColumnIndexOrThrow(
                                            MediaStore.Images.Media._ID
                                    )
                            );
                            Uri deleteUri = ContentUris.withAppendedId(queryUri, id);
                            contentResolver.delete(
                                    deleteUri,
                                    null,
                                    null
                            );
                        }
                        cursor.close();
                    }
                },
                5000
        );
    }

    private void resetEntryButtonOnClick() {
        List<PictureReference> deletePictureReference = currentEntryPhotoList.stream()
                .filter(pictureReference -> pictureReference.getUid() == 0)
                .collect(Collectors.toList());
        deletePictureReference.forEach(this::deletePicture);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final PictureReference[] deletePictures = new PictureReference[deletePictureReference.size()];
            deletePictureReference.toArray(deletePictures);
            vsViewModel.deleteAllPictureReferences(deletePictures);
        });

        listenerCounter = 0;
        newEntry();
        entryHUDMap.clear();
        connectedDeviceUpdateViews();
    }

    private void repeatEntryButtonOnClick() {
        final Entry copyEntry = new Entry(currentUser.getUid(), currentSession.getUid());
        if (currentLocation != null) {
            copyEntry.setLatitude(currentLocation.getLatitude());
            copyEntry.setLongitude(currentLocation.getLongitude());
        }
        ExecutorService entryExecutor = Executors.newSingleThreadExecutor();
        Handler entryHandler = new Handler(Looper.getMainLooper());
        entryExecutor.execute(() -> {
            final long copyID = vsViewModel.insertEntry(copyEntry);
            entryHandler.post(() -> {
                copyEntry.setUid(copyID);
                List<EntryValue> entryValues = entryValueListLive.getValue();
                if (entryValues != null) {
                    if (currentEntry == null) {
                        newEntry();
                    }
                    assert currentEntry != null;
                    long currentEntryID = currentEntry.getUid();
                    if (currentEntryID == 0L) {
                        final Entry lastEntry = dataGatheringRecyclerAdapter.getEntry(
                                dataGatheringRecyclerAdapter.getItemCount() - 1
                        );
                        assert lastEntry != null;
                        currentEntryID = lastEntry.getUid();
                    }
                    final long finalEntryID = currentEntryID;
                    entryValues = entryValues.stream()
                            .filter(ev -> ev.getEntryID() == finalEntryID)
                            .collect(Collectors.toList());
                } else {
                    entryValues = new ArrayList<>();
                }
                final List<EntryValue> copyValueList = new ArrayList<>();
                entryValues.forEach(entryValue -> {
                    final EntryValue copyValue = new EntryValue(
                            copyID,
                            entryValue.getColumnValueID()
                    );
                    copyValueList.add(copyValue);
                });
                final EntryValue[] copyEntryValues = new EntryValue[entryValues.size()];
                copyValueList.toArray(copyEntryValues);
                ExecutorService valueExecutor = Executors.newSingleThreadExecutor();
                Handler valueHandler = new Handler(Looper.getMainLooper());
                valueExecutor.execute(() -> {
                    vsViewModel.insertAllEntryValues(copyEntryValues);
                    valueHandler.post(this::newEntry);
                });
            });
        });

        listenerCounter = 0;
    }

    private void saveEntryButtonOnClick() {
        if (currentEntry == null) newEntry();
        boolean pictureIssue = currentSession.isPictureRequired() && currentEntryPhotoList.isEmpty();
        boolean commentIssue = currentSession.isCommentRequired() &&
                (currentEntry.getComment() == null || currentEntry.getComment().isEmpty());
        if (commentIssue && pictureIssue) {
            vdtsApplication.displayToast(
                    this,
                    "This entry requires a comment and a picture."
            );
        } else if (commentIssue) {
            vdtsApplication.displayToast(
                    this,
                    "This entry requires a comment."
            );
        } else if (pictureIssue) {
            vdtsApplication.displayToast(
                    this,
                    "This entry requires a picture."
            );
        } else {
            if (currentEntry.getUid() > 0) {
                //Update existing entry
                ExecutorService updateEntryExecutor = Executors.newSingleThreadExecutor();
                Handler updateEntryHandler = new Handler(Looper.getMainLooper());
                updateEntryExecutor.execute(() -> {
                    if (currentLocation != null) {
                        if (currentEntry.getLatitude() == null) {
                            currentEntry.setLatitude(currentLocation.getLatitude());
                        }
                        if (currentEntry.getLongitude() == null) {
                            currentEntry.setLongitude(currentLocation.getLongitude());
                        }
                    }
                    vsViewModel.updateEntry(currentEntry);
                    updateEntryHandler.post(() -> {
                        savePictureReferences(currentEntry.getUid());
                        saveEntryValues(currentEntry.getUid());
                    });
                });
            } else {
                //Save a new entry
                if (currentEntry == null) {
                    newEntry();
                }
                ExecutorService createEntryExecutor = Executors.newSingleThreadExecutor();
                Handler createEntryHandler = new Handler(Looper.getMainLooper());
                createEntryExecutor.execute(() -> {
                    if (currentLocation != null) {
                        currentEntry.setLatitude(currentLocation.getLatitude());
                        currentEntry.setLongitude(currentLocation.getLongitude());
                    }
                    long uid = vsViewModel.insertEntry(currentEntry);
                    currentEntry.setUid(uid);
                    createEntryHandler.post(() -> {
                        savePictureReferences(uid);
                        saveEntryValues(uid);
                    });
                });
            }
        }

        listenerCounter = 0;
        isEntrySelected = false;
        isColumnNext = false;
        isColumnPrevious = false;
    }

    private void savePictureReferences(long entryID) {
        final List<PictureReference> insertPictureReferenceList = new ArrayList<>();
        final List<PictureReference> updatePictureReferenceList = new ArrayList<>();

        currentEntryPhotoList.forEach(pictureReference -> {
            pictureReference.setEntryID(entryID);
            if (pictureReference.getUid() > 0) {
                updatePictureReferenceList.add(pictureReference);
            } else {
                insertPictureReferenceList.add(pictureReference);
            }
        });

        PictureReference[] insertPictureReferences = new
                PictureReference[insertPictureReferenceList.size()];
        PictureReference[] updatePictureReferences = new
                PictureReference[updatePictureReferenceList.size()];

        insertPictureReferenceList.toArray(insertPictureReferences);
        updatePictureReferenceList.toArray(updatePictureReferences);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            if (insertPictureReferences.length > 0) {
                vsViewModel.insertAllPictureReferences(insertPictureReferences);
            }

            if (updatePictureReferences.length > 0) {
                vsViewModel.updateAllPictureReferences(updatePictureReferences);
            }

            handler.post(() -> {
                if (insertPictureReferences.length > 0) {
                    int entryIndex = dataGatheringRecyclerAdapter.getEntryPosition(entryID);
                    if (entryIndex >= 0) {
                        dataGatheringRecyclerAdapter.addPictureReferences(
                                insertPictureReferenceList,
                                entryIndex
                        );
                    }
                }
            });
        });
    }

    private void saveEntryValues(long entryID) {
        List<EntryValue> entryValues = entryValueListLive.getValue();
        if (entryValues != null) {
            entryValues = entryValues.stream()
                    .filter(ev -> ev.getEntryID() == entryID)
                    .collect(Collectors.toList());
        } else {
            entryValues = new ArrayList<>();
        }
        final List<EntryValue> finalEntryValues = entryValues;

        final List<EntryValue> insertEntryValueList = new ArrayList<>();
        final List<EntryValue> updateEntryValueList = new ArrayList<>();
        final List<EntryValue> deleteEntryValueList = new ArrayList<>();

        columnValueMap.forEach(
                (position, columnValues) -> {
                    EntryValue entryValue = finalEntryValues.stream()
                            .filter(
                                    ev -> columnValues.stream()
                                            .anyMatch(cv -> cv.getUid() == ev.getColumnValueID())
                            ).findFirst()
                            .orElse(null);
                    ColumnValue columnValue = (ColumnValue) columnValueSpinnerList
                            .get(position)
                            .getSelectedItem();
                    if (entryValue != null && columnValue != null) {
                        entryValue.setColumnValueID(columnValue.getUid());
                        updateEntryValueList.add(entryValue);
                    } else if (entryValue != null) {
                        deleteEntryValueList.add(entryValue);
                    } else if (columnValue != null) {
                        entryValue = new EntryValue(entryID, columnValue.getUid());
                        insertEntryValueList.add(entryValue);
                    }
                }
        );
        finalEntryValues.forEach(
                entryValue -> {
                    if (insertEntryValueList.stream().noneMatch(ev -> ev.getUid() == entryValue.getUid()) &&
                            updateEntryValueList.stream().noneMatch(ev -> ev.getUid() == entryValue.getUid()) &&
                            deleteEntryValueList.stream().noneMatch(ev -> ev.getUid() == entryValue.getUid())) {
                        deleteEntryValueList.add(entryValue);
                    }
                }
        );

        EntryValue[] insertEntryValues = new EntryValue[insertEntryValueList.size()];
        EntryValue[] updateEntryValues = new EntryValue[updateEntryValueList.size()];
        EntryValue[] deleteEntryValues = new EntryValue[deleteEntryValueList.size()];

        insertEntryValueList.toArray(insertEntryValues);
        updateEntryValueList.toArray(updateEntryValues);
        deleteEntryValueList.toArray(deleteEntryValues);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            if (insertEntryValues.length > 0) vsViewModel.insertAllEntryValues(insertEntryValues);
            if (updateEntryValues.length > 0) vsViewModel.updateAllEntryValues(updateEntryValues);
            if (deleteEntryValues.length > 0) vsViewModel.deleteAllEntryValues(deleteEntryValues);

            handler.post(() -> {
                newEntry();
                connectedDeviceUpdateViews();

                entryHUDMap.clear();
                if (isHeadsetAvailable) {
                    iristickUpdateHUDViews();
                }
            });
        });
    }

    private void endSessionButtonOnClick() {
        if (currentSession != null) {
            confirmEndSessionDialogue();
        }
    }

    private void confirmEndSessionDialogue() {
        LOG.info("Showing End Confirm Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("End Session");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_yes_no,
                null
        );
        builder.setView(customLayout);
        TextView label = customLayout.findViewById(R.id.mainLabel);
        label.setText(R.string.data_gathering_end_session_dialogue);
        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        yesButton.setOnClickListener(view -> {
            currentSession.setEndDate(LocalDateTime.now());
            ExecutorService endExecutor = Executors.newSingleThreadExecutor();
            Handler endHandler = new Handler(Looper.getMainLooper());
            endExecutor.execute(() -> {
                vsViewModel.updateSession(currentSession);
                vdtsApplication.getVDTSPrefKeyValue().setLong(
                        String.format("%s_SESSION", currentUser.getExportCode()),
                        -1L);
                export(currentSession);
                endHandler.post(this::finish);
            });
        });

        noButton.setOnClickListener(view -> finalDialog.dismiss());
    }

    private void export(Session session) {
        //ISaver saver = Saver.createSaver(ONEDRIVE_APP_ID);
        final Exporter exporter = new Exporter(vsViewModel, vdtsApplication, this);
        boolean CSV = true;
        boolean Excel = true;
        boolean JSON = true;

        if (vdtsApplication.getVDTSPrefKeyValue().getBoolean(PREF_EXPORT_CSV, false)) {
            CSV = exporter.exportSessionCSV(session);
        }
        if (vdtsApplication.getVDTSPrefKeyValue().getBoolean(PREF_EXPORT_JSON, false)) {
            JSON = exporter.exportSessionJSON(session);
        }
        if (vdtsApplication.getVDTSPrefKeyValue().getBoolean(PREF_EXPORT_XLSX, true)) {
            Excel = exporter.exportSessionExcel(session);
        }
        if (CSV && Excel && JSON) {
            if (exporter.exportMedia(session)) {
                vdtsApplication.displayToast(
                        this,
                        "Session exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting session photos"
                );
            }
        } else {
            vdtsApplication.displayToast(
                    this,
                    "Error exporting session"
            );
        }
    }

    private void newEntry() {
        dataGatheringRecyclerAdapter.clearSelected();
        columnScrollView.setScrollX(0);
        currentEntry = new Entry(currentUser.getUid(), currentSession.getUid());
        selectedColumnValue = null;
        currentEntryPhotoList.clear();
    }

    private void showCommentDialogue() {
        LOG.info("Showing comment dialogue");

        if (currentEntry == null) {
            newEntry();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(
                vdtsApplication.getResources().getString(R.string.comment_dialogue_edit_comment)
        );

        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_comment,
                null
        );
        builder.setView(customLayout);

        EditText commentView = customLayout.findViewById(R.id.commentValue);
        commentView.setText(currentEntry.getComment() != null ? currentEntry.getComment() : "");

        builder.setPositiveButton(
                vdtsApplication.getResources().getString(R.string.comment_dialogue_enter_label),
                (dialogInterface, i) -> {
                    currentEntry.setComment(commentView.getText().toString());
                    if (isHeadsetAvailable) {
                        iristickHUD.entryCommentValue.setChecked(true);
                    }
                }
        );

        builder.setNegativeButton(
                vdtsApplication.getResources().getString(R.string.comment_dialogue_cancel_label),
                (dialogInterface, i) -> {
                }
        );

        AlertDialog dialog = builder.create();
        assert dialog.getWindow() != null;
        commentView.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showPreview() {
        previewView.bringToFront();
        previewView.setVisibility(View.VISIBLE);
        cameraLifecycle.performEvent(Lifecycle.Event.ON_START);
        previewShowing = true;
        vdtsApplication.displayToast(
                this,
                "Use long press to exit camera"
        );
    }

    private void hidePreview() {
        entryRecyclerView.bringToFront();
        previewView.setVisibility(View.INVISIBLE);
        cameraLifecycle.performEvent(Lifecycle.Event.ON_STOP);
        previewShowing = false;
    }

    private void takePicture() {
        try {
            final File photoDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS),
                    "VoiceSelect/Pictures");

            if (!photoDir.exists()) {
                boolean mkdirResult = photoDir.mkdirs();
                if (!mkdirResult) {
                    LOG.info("Failed to create image directory");
                    return;
                }
            }
            final File imageFile = File.createTempFile(
                    VDTSImageFileUtil.generateFileName("", false),
                    ".jpg",
                    new File(photoDir.getPath())
            );

            ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions
                    .Builder(imageFile)
                    .build();

            imageCapture.setFlashMode(ImageCapture.FLASH_MODE_AUTO);

            imageCapture.takePicture(
                    options,
                    ContextCompat.getMainExecutor(this),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            //todo - use for less verbose audible feedback
                            new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                            YoYo.with(Techniques.Pulse)
                                    .duration(PULSE_DURATION)
                                    .repeat(PULSE_REPEAT)
                                    .playOn(previewView);

                            VDTSImageFileUtil.addGPS(imageFile.getPath(), currentLocation);
                            if (currentEntry == null) {
                                newEntry();
                            }

                            PictureReference pictureReference = new PictureReference(
                                    currentUser.getUid(),
                                    currentEntry.getUid(),
                                    imageFile.getPath(),
                                    currentLocation
                            );
                            currentEntryPhotoList.add(pictureReference);
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            LOG.error("Error taking photo: ", exception);
                        }
                    }
            );

            new VDTSMediaScannerUtil(this, imageFile);
        } catch (IOException e) {
            LOG.error("IO Exception: ", e);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (isBetterLocation(location, currentLocation)) {
            currentLocation = location;
            LOG.debug("{}, {}", location.getLatitude(), location.getLongitude());
            if (!GPSConnected) {
                GPSConnected = true;
                vdtsApplication.displayToast(
                        this,
                        "GPS connection found"
                );
            }
        }
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LOG.debug("onProviderEnabled: {}", provider);
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LOG.debug("onProviderDisabled: {}", provider);
    }

    private void enableGPS() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
            String[] permissions = {ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, CAMERA};
            ActivityCompat.requestPermissions(this, permissions, PERM_CODE_GPS);
            return;
        }

        locationManager.requestLocationUpdates(
                GPS_PROVIDER,
                5000,
                0,
                this
        );
        locationManager.requestLocationUpdates(
                NETWORK_PROVIDER,
                5000,
                0,
                this
        );
        currentLocation = locationManager.getLastKnownLocation(GPS_PROVIDER);
        if (currentLocation == null) {
            currentLocation = locationManager.getLastKnownLocation(NETWORK_PROVIDER);
        }
    }

    private void disableGPS() {
        locationManager.removeUpdates(this);
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newRatio = zoomRatio * scaleFactor;
            newRatio = Math.max(minZoomRatio, Math.min(maxZoomRatio, newRatio));
            LOG.debug(
                    String.format(
                            Locale.getDefault(),
                            "Scale Factor: %.2f, New Ratio: %.2f",
                            scaleFactor,
                            newRatio
                    )
            );
            camera.getCameraControl().setZoomRatio(newRatio);

            return true;
        }
    }

    private class ScrollListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        @OptIn(markerClass = ExperimentalExposureCompensation.class)
        public boolean onScroll(@NonNull MotionEvent e1, @NonNull MotionEvent e2, float distanceX,
                                float distanceY) {
            final String message = String.format(
                    Locale.getDefault(),
                    "onScroll - event1: %s, event2: %s, distance x: %.2f, distance y: %.2f",
                    e1,
                    e2,
                    distanceX,
                    distanceY
            );
            LOG.debug(message);

            Range<Integer> exposureRange = camera.getCameraInfo()
                    .getExposureState()
                    .getExposureCompensationRange();
            Rational exposureStep = camera.getCameraInfo()
                    .getExposureState()
                    .getExposureCompensationStep();

            LOG.debug("Exposure Range: {}, ExposureStep: {}", exposureRange, exposureStep);

            int newExposureLevel = exposureLevel + (int) (distanceY * exposureStep.floatValue());

            newExposureLevel = Math.max(
                    exposureRange.getLower(),
                    Math.min(exposureRange.getUpper(), newExposureLevel)
            );
            LOG.debug("New target exposure level: {}", newExposureLevel);
            int finalNewExposureLevel = newExposureLevel;
            camera.getCameraControl().setExposureCompensationIndex(newExposureLevel).addListener(
                    () -> {
                        exposureLevel = finalNewExposureLevel;
                        LOG.debug(
                                String.format(
                                        Locale.getDefault(),
                                        "Brightness set to %d",
                                        exposureLevel
                                )
                        );
                    },
                    getMainExecutor()
            );

            return true;
        }

        @Override
        public boolean onSingleTapUp(@NonNull MotionEvent e) {
            takePicture();
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            hidePreview();
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            //start/stop Recording
            return true;
        }
    }

    private final View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            //todo - uncomment and test
            //            view.performClick();
            scrollDetector.onTouchEvent(motionEvent);
            return scaleDetector.onTouchEvent(motionEvent);
        }
    };

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
        iristickInitialize(headset);
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
    }

    private void iristickInitialize(IRIHeadset headset) {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new DataGatheringActivity.IristickHUD();
            return iristickHUD;
        });

        iristickInitializeGlobalVoiceCommands();
        iristickInitializeCameraGrammar(headset);
        iristickInitializeCommentGrammar();
    }

    private void iristickInitializeGlobalVoiceCommands() {
        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Navigate Back", () -> {
                    LOG.info("Navigate Back");
                    //todo - get different sound
                    new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                    finish();
                })
        );
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickInitializeEntryGrammar() {
        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
            vg.addAlternativeGroup(ag -> {
                ag.addToken("Next Column");
                ag.addToken("Previous Column");
                ag.addToken("Delete Last");
                ag.addToken("Repeat Last");
                ag.addToken("Reset Entry");
                ag.addToken("Save Entry");
                ag.addToken("Delete Entry");
                ag.addToken("End Session");

                //Available Column Values per column
                for (int index = 0; index < columnValueSpokenMap.size(); index++) {
                    int finalIndex = index;
                    ag.addAlternativeGroup(cv -> {
                        List<ColumnValueSpoken> columnValueSpokenList =
                                columnValueSpokenMap.get(finalIndex);
                        if (columnValueSpokenList != null) {
                            for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
                                cv.addToken(columnValueSpoken.getSpoken());
                            }
                        }
                    });
                }
            });

            vg.setListener(((recognizer, tokens, tags) -> {
                String columnPositionFeedback;
                if (!isDeleteLast && iriCameraSession == null) {
                    switch (tokens[0]) {
                        case "Next Column":
                            if (spokenPosition <= vdtsNamedPositionedAdapter.getSelectedSpinnerPosition()) {
                                LOG.info("Next Column");
                                if (isEntrySelected) {
                                    spokenPosition = vdtsNamedPositionedAdapter
                                            .getSelectedSpinnerPosition() + 1;
                                }

                                if (spokenPosition + 1 < columnMap.size()) {
                                    columnPositionFeedback = Objects.requireNonNull(
                                            columnMap.get(spokenPosition + 1)).getName();
                                } else {
                                    columnPositionFeedback = "End";
                                }

                                if (currentUser.getFeedback() == 1) {
                                    currentUserTTSEngine.speak(
                                            columnPositionFeedback,
                                            currentUserFeedbackMode,
                                            null,
                                            null
                                    );
                                }

                                spokenPosition++;
                                isColumnNext = true;
                                isEntrySelected = false;
                                iristickUpdateHUDViews();
                            }

                            break;
                        case "Previous Column":
                            if (spokenPosition > 0) {
                                LOG.info("Previous Column");
                                if (isEntrySelected) {
                                    spokenPosition = vdtsNamedPositionedAdapter
                                            .getSelectedSpinnerPosition() + 1;
                                }

                                if (spokenPosition - 1 < columnMap.size()) {
                                    columnPositionFeedback = Objects.requireNonNull(
                                            columnMap.get(spokenPosition - 1)).getName();
                                } else {
                                    columnPositionFeedback = "";
                                }

                                if (currentUser.getFeedback() == 1) {
                                    currentUserTTSEngine.speak(
                                            columnPositionFeedback,
                                            currentUserFeedbackMode,
                                            null,
                                            null
                                    );
                                }

                                spokenPosition--;
                                isColumnPrevious = true;
                                isEntrySelected = false;
                                iristickUpdateHUDViews();
                            }

                            break;
                        case "Delete Last":
                            LOG.info("Delete Last");
                            spokenPosition = 0;
                            isDeleteLast = true;
                            iristickConfirmDeleteEntry();
                            break;
                        case "Repeat Last":
                            if (dataGatheringRecyclerAdapter.getItemCount() > 0) {
                                LOG.info("Repeat Entry");
                                if (currentUser.getFeedback() == 1) {
                                    currentUserTTSEngine.speak(
                                            "Entry Repeated",
                                            currentUserFeedbackMode,
                                            null,
                                            null
                                    );
                                    spokenPosition = 0;
                                    repeatEntryButtonOnClick();
                                }
                            }
                            break;
                        case "Reset Entry":
                            LOG.info("Reset Entry");
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Entry Reset",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            spokenPosition = 0;
                            resetEntryButtonOnClick();
                            break;
                        case "Save Entry":
                            LOG.info("Save Entry");
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Entry Saved",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            spokenPosition = 0;
                            saveEntryButtonOnClick();
                            break;
                        case "Delete Entry":
                            LOG.info("Delete Entry");
                            spokenPosition = 0;
                            iristickConfirmDeleteEntry();
                            break;
                        case "End Session":
                            LOG.info("End Session");
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Session Finished",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            spokenPosition = 0;
                            endSessionButtonOnClick();
                            break;
                        default:
                            iristickEnterColumnValueCommands(tokens);
                    }
                }
            }));
        });
    }

    private void iristickConfirmDeleteEntry() {
        String deleteEntry;
        if (isDeleteLast) {
            LOG.info("Delete row {}", entryList.size());
            deleteEntry = "Delete row " + (entryList.size());
        } else {
            LOG.info("Delete row {}", selectedEntryIndex);
            deleteEntry = "Delete row " + selectedEntryIndex;
        }

        if (currentUser.getFeedback() == 1) {
            currentUserTTSEngine.speak(
                    deleteEntry,
                    currentUserFeedbackMode,
                    null,
                    null);
        }

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Yes", () -> {
                    LOG.info("Yes");
                    String rowDeleted;
                    if (isDeleteLast) {
                        rowDeleted = "Row " + entryList.size() + " Deleted";
                    } else {
                        rowDeleted = "Row " + selectedEntryIndex + " Deleted";
                    }

                    if (currentUser.getFeedback() == 1) {
                        currentUserTTSEngine.speak(
                                rowDeleted,
                                currentUserFeedbackMode,
                                null,
                                null);
                    }

                    deleteEntry(isDeleteLast);
                    isDeleteLast = false;
                })
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("No", () -> {
                    LOG.info("No");
                    if (currentUser.getFeedback() == 1) {
                        currentUserTTSEngine.speak(
                                "Delete Aborted",
                                currentUserFeedbackMode,
                                null,
                                null
                        );
                    }

                    isDeleteLast = false;
                })
        );
    }

    private void iristickEnterColumnValueCommands(String[] tokens) {
        HashMap<String, Long> spokenColumnValueIDMap =
                positionColumnValueSpokenIDMap.get(spokenPosition);
        if (spokenColumnValueIDMap != null) {
            if (spokenColumnValueIDMap.get(tokens[0]) != null) {
                Long columnValueID = spokenColumnValueIDMap.get(tokens[0]);

                List<ColumnValue> columnValueList = columnValueMap.get(spokenPosition);
                ColumnValue columnValue = null;
                if (columnValueID != null && columnValueList != null) {
                    columnValue = columnValueList.stream()
                            .filter(cv -> cv.getUid() == columnValueID)
                            .findFirst()
                            .orElse(null);
                }

                Spinner columnValueSpinner = columnValueSpinnerList.get(spokenPosition);
                assert columnValueList != null;
                int position = columnValueList.indexOf(columnValue);
                columnValueSpinner.setSelection(position + 1);
                selectedColumnValue = columnValueList.get(position);
                spokenPosition++;

                String columnValueFeedback;
                if (spokenPosition < columnMap.size()) {
                    columnValueFeedback = tokens[0] + " " +
                            Objects.requireNonNull(columnMap.get(spokenPosition)).getName();
                } else {
                    columnValueFeedback = tokens[0] + " End";
                }

                if (currentUser.getFeedback() == 1) {
                    currentUserTTSEngine.speak(
                            columnValueFeedback,
                            currentUserFeedbackMode,
                            null,
                            null);
                }
            }
        }
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickInitializeSelectEntryGrammar() {
        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
            vg.addToken("Select Row");

            vg.addAlternativeGroup(ag -> {
                for (int index = 0; index < entryList.size(); index++) {
                    ag.addToken(String.valueOf(index + 1));
                }
            });

            vg.setListener((recognizer, tokens, tags) -> {
                if (listenerCounter <= 0) {
                    selectedEntryIndex = Integer.parseInt(tokens[1]);
                    LOG.info("Select Row " + selectedEntryIndex);
                    if (currentUser.getFeedback() == 1) {
                        currentUserTTSEngine.speak(
                                "Row " + selectedEntryIndex + " Selected",
                                currentUserFeedbackMode,
                                null,
                                null
                        );
                    }

                    listenerCounter++;
                    entryAdapterSelect(selectedEntryIndex - 1);
                }
            });
        });
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickInitializeCommentGrammar() {
        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
            vg.addSequentialGroup(sg -> {
                sg.addAlternativeGroup(ag -> {
                    ag.addToken("Make");
                    ag.addToken("Add");
                    ag.addToken("Clear");
                    ag.addToken("Cancel");
                    ag.addToken("Enter");
                });

                sg.addToken("Comment");
            });

            vg.setListener((((recognizer, tokens, tags) -> {
                switch (tokens[0]) {
                    case "Make":
                        LOG.info("Make Comment");

                        iristickHUD.dataGatheringView.setVisibility(View.INVISIBLE);
                        iristickHUD.iriCameraView.setVisibility(View.INVISIBLE);
                        iristickHUD.commentView.setVisibility(View.VISIBLE);

                        isVoiceComment = true;
                        Handler commentHandler = new Handler(Looper.getMainLooper());
                        commentHandler.postDelayed(this::iristickSpeakComment, 1000);
                        break;
                    case "Add":
                        LOG.info("Add Comment");
                        if (isVoiceComment) {
                            isVoiceCommentAppend = true;
                            Handler appendHandler = new Handler(Looper.getMainLooper());
                            appendHandler.postDelayed(this::iristickSpeakComment, 1000);
                        }
                        break;
                    case "Clear":
                        LOG.info("Clear Comment");
                        if (isVoiceComment) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Comment Cleared",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iristickHUD.commentValue.setText("");
                        }
                        break;
                    case "Cancel":
                        LOG.info("Cancel");
                        if (isVoiceComment) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Comment Cancelled",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iristickHUD.commentValue.setText("");
                            iristickHUD.iriCameraView.setVisibility(View.INVISIBLE);
                            iristickHUD.commentView.setVisibility(View.INVISIBLE);
                            iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);

                            isVoiceComment = false;
                        }
                        break;
                    case "Enter":
                        LOG.info("Enter Comment");
                        if (isVoiceComment) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Comment Saved",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            currentEntry.setComment(iristickHUD.commentValue.getText().toString());

                            iristickHUD.commentValue.setText("");
                            iristickHUD.iriCameraView.setVisibility(View.INVISIBLE);
                            iristickHUD.commentView.setVisibility(View.INVISIBLE);
                            iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);
                            iristickHUD.entryCommentValue.setChecked(true);

                            isVoiceComment = false;
                        }
                        break;
                }
            })));
        });
    }

    private void iristickSpeakComment() {
        SpeechRecognizer speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
            }

            @Override
            public void onBeginningOfSpeech() {
            }

            @Override
            public void onRmsChanged(float rmsdB) {
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int error) {
            }

            @Override
            public void onResults(Bundle results) {
                ArrayList<String> recognizedSpeech = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (isVoiceCommentAppend) {
                    iristickHUD.commentValue.append(" " + recognizedSpeech.get(0));
                    isVoiceCommentAppend = false;
                } else {
                    iristickHUD.commentValue.setText(recognizedSpeech.get(0));
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
            }
        });

        speechRecognizer.startListening(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM));
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickInitializeCameraGrammar(IRIHeadset headset) {
        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
            vg.addSequentialGroup(sg -> {
                sg.addAlternativeGroup(ag -> {
                    ag.addToken("Open");
                    ag.addToken("Wide");
                    ag.addToken("Zoom");
                    ag.addToken("Focus");
                    ag.addToken("Show");
                    ag.addToken("Hide");
                    ag.addToken("Close");
                });

                sg.addToken("Camera");
            });

            vg.setListener((((recognizer, tokens, tags) -> {
                switch (tokens[0]) {
                    case "Open":
                        LOG.info("Open Camera");
                        if (currentUser.getFeedback() == 1) {
                            currentUserTTSEngine.speak(
                                    "Camera",
                                    currentUserFeedbackMode,
                                    null,
                                    null
                            );
                        }

                        if (iriCameraSession != null) {
                            iristickCloseCamera();
                        }
                        cameraType = -1;
                        iristickOpenCamera(headset);
                        break;
                    case "Wide":
                        LOG.info("Wide Camera");
                        if (iriCameraSession != null) {
                            iristickCloseCamera();
                        }

                        if (currentUser.getFeedback() == 1) {
                            currentUserTTSEngine.speak(
                                    "Camera Wide",
                                    currentUserFeedbackMode,
                                    null,
                                    null
                            );
                        }

                        cameraType = -1;
                        iristickOpenCamera(headset);
                        break;
                    case "Zoom":
                        LOG.info("Zoom Camera");
                        if (currentUser.getFeedback() == 1) {
                            currentUserTTSEngine.speak(
                                    "Camera Zoom",
                                    currentUserFeedbackMode,
                                    null,
                                    null
                            );
                        }

                        if (iriCameraSession != null) {
                            iristickCloseCamera();
                        }
                        cameraType = 1;
                        iristickOpenCamera(headset);
                        break;
                    case "Focus":
                        LOG.info("Focus Camera");
                        if (iriCameraSession != null) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Camera Focused",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iriCameraSession.triggerAF();
                        }
                        break;
                    case "Show":
                        LOG.info("Show Camera");
                        if (iriCameraSession != null) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Camera Shown",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iristickHUD.dataGatheringView.setVisibility(View.INVISIBLE);
                            iristickHUD.iriCameraView.setVisibility(View.VISIBLE);
                        }
                        break;
                    case "Hide":
                        LOG.info("Hide Camera");
                        if (iriCameraSession != null) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Camera Hidden",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iristickHUD.iriCameraView.setVisibility(View.INVISIBLE);
                            iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);
                        }
                        break;
                    case "Close":
                        LOG.info("Close Camera");
                        if (iriCameraSession != null) {
                            if (currentUser.getFeedback() == 1) {
                                currentUserTTSEngine.speak(
                                        "Camera Closed",
                                        currentUserFeedbackMode,
                                        null,
                                        null
                                );
                            }

                            iristickCloseCamera();
                        }
                        break;
                }
            })));
        });

        //todo - doesn't change zoom - min/max available zoom is 1.0F due to use of full resolution image from both cameras
//        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
//            vg.addToken("Zoom");
//
//            vg.addAlternativeGroup(zoomLevel -> {
//                for (int index = 0; index <= 5; index++) {
//                    zoomLevel.addToken(String.valueOf(index));
//                }
//            });
//
//            vg.setListener((((recognizer, tokens, tags) -> {
//                if (iriCameraSession != null) {
//                    int zoomLevel = Integer.parseInt(tokens[1]);
//                    switch (zoomLevel) {
//                        case 0:
//                            LOG.info("Zoom 0");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 0",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.0f));
//                            break;
//                        case 1:
//                            LOG.info("Zoom 1");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 1",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.2f));
//                            break;
//                        case 2:
//                            LOG.info("Zoom 2");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 2",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.4f));
//                            break;
//                        case 3:
//                            LOG.info("Zoom 3");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 3",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.6f));
//                            break;
//                        case 4:
//                            LOG.info("Zoom 4");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 4",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.8f));
//                            break;
//                        case 5:
//                            LOG.info("Zoom 5");
//                            if (currentUser.getFeedback() == 1) {
//                                currentUserTTSEngine.speak(
//                                        "Zoom Level 5",
//                                        currentUserFeedbackMode,
//                                        null,
//                                        null
//                                );
//                            }
//
//                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(2.0f));
//                            break;
//                    }
//                }
//            })));
//        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Take Picture", () -> {
                    LOG.info("Take Picture");
                    if (iriCameraSession != null) {
                        iristickTakePicture();
                    }
                })
        );

//        IristickSDK.addVoiceCommands(
//                this.getLifecycle(),
//                this,
//                vc -> vc.add("Start Recording", () -> {
//                    LOG.info("Start Recording");
//                    if (iriCameraSession != null) {
//                        isRecordVideo = true;
//                        iristickCloseCamera();
//                        iristickOpenCamera(headset);
//                        iristickRecordVideo()
//                        //start recording from iristick camera
//                    }
//                })
//        );
//
//        IristickSDK.addVoiceCommands(
//                this.getLifecycle(),
//                this,
//                vc -> vc.add("End Recording", () -> {
//                    LOG.info("End Recording");
//                    if (iriCameraSession != null) {
//                        isRecordVideo = false;
//
//                    }
//                })
//        );
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickOpenCamera(IRIHeadset headset) {
        if (cameraType == 1) {
            if (headset.findCamera(IRICameraType.ZOOM) != null) {
                iriCamera = headset.findCamera(IRICameraType.ZOOM);
            } else if (headset.findCamera(IRICameraType.WIDE_ANGLE) != null) {
                iriCamera = headset.findCamera(IRICameraType.WIDE_ANGLE);
            }
        } else {
            if (headset.findCamera(IRICameraType.WIDE_ANGLE) != null) {
                iriCamera = headset.findCamera(IRICameraType.WIDE_ANGLE);
            } else if (headset.findCamera(IRICameraType.ZOOM) != null) {
                iriCamera = headset.findCamera(IRICameraType.ZOOM);
            }
        }

        assert iriCamera != null;
        if (isRecordVideo) {
//            final File videoDir = new File(Environment.getExternalStoragePublicDirectory(
//                    Environment.DIRECTORY_DOCUMENTS),
//                    "VoiceSelect/Pictures");
//
//            if (!videoDir.exists()) {
//                boolean mkdirResult = videoDir.mkdirs();
//                if (!mkdirResult) {
//                    LOG.info("Failed to create image directory");
//                    return;
//                }
//            }
//
//            final ContentResolver resolver = getApplicationContext().getContentResolver();
//            final ContentValues videoDetails = new ContentValues();
//
//            //todo - make video uri and add to set movie uri
////            final Uri videoUri = resolver.insert(videoDir)
//
//            iriCameraSession = iriCamera.openSession(
//                    getLifecycle(), IRICameraProfile.MOVIE_RECORDING, ic -> {
//                        ic.addPreview(iristickHUD.iriCameraView);
//                        ic.setMovieUri();
//                        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
//                                PackageManager.PERMISSION_GRANTED) {
//                            ic.setMovieAudio(true);
//                        }
//                        ic.setAFStrategy(IRICameraAFStrategy.SMOOTH);
//                    });
        } else {
            iriCameraSession = iriCamera.openSession(
                    getLifecycle(), IRICameraProfile.STILL_CAPTURE, ic -> {
                        ic.addPreview(iristickHUD.iriCameraView);
                        ic.setAFStrategy(IRICameraAFStrategy.SMOOTH);
                    });
        }

        iriCameraSession.reconfigure(zoomLevel -> zoomLevel.setZoom(1.0f));
        iriCameraSession.triggerAF();

        iristickHUD.dataGatheringView.setVisibility(View.INVISIBLE);
        iristickHUD.commentView.setVisibility(View.INVISIBLE);
        iristickHUD.iriCameraView.setVisibility(View.VISIBLE);
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickCloseCamera() {
        iriCameraSession.close();
        iriCameraSession = null;
        iristickHUD.iriCameraView.setVisibility(View.INVISIBLE);
        iristickHUD.commentView.setVisibility(View.INVISIBLE);
        iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);
    }

    @OptIn(markerClass = Experimental.class)
    private void iristickTakePicture() {
        if (iriCameraSession == null || iriCameraCaptureInProgress) {
            return;
        }

        final File photoDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS),
                "VoiceSelect/Pictures");

        if (!photoDir.exists()) {
            boolean mkdirResult = photoDir.mkdirs();
            if (!mkdirResult) {
                LOG.info("Failed to create image directory");
                return;
            }
        }

        iriCameraCaptureInProgress = true;
        iriCameraSession.captureStillImage((iriCameraSession, iriPhoto) -> {
            iriCameraCaptureInProgress = false;
            try {
                final File imageFile = File.createTempFile(
                        VDTSImageFileUtil.generateFileName("", false),
                        ".jpg",
                        new File(photoDir.getPath())
                );

                VDTSImageFileUtil.addGPS(imageFile.getPath(), currentLocation);

                OutputStream outputStream = Files.newOutputStream(imageFile.toPath());

                if (iriPhoto != null) {
                    iriPhoto.writeJpeg(outputStream);
                    new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);

                    if (currentEntry == null) {
                        newEntry();
                    }
                    PictureReference pictureReference = new PictureReference(
                            currentUser.getUid(),
                            currentEntry.getUserID(),
                            imageFile.getPath(),
                            currentLocation
                    );

                    currentEntryPhotoList.add(pictureReference);
                }

                iristickHUD.entryPicValue.setText(String.valueOf(currentEntryPhotoList.size()));

                new VDTSMediaScannerUtil(this, imageFile);

                LOG.info("Capture succeeded");
            } catch (IOException e) {
                LOG.error("Capture failed", e);
            }
        });
    }

//    private void iristickRecordVideo() {
//        if (iriCameraSession == null) {
//            return;
//        }
//
//        final File photoDir = new File(Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_DOCUMENTS),
//                "VoiceSelect/Video");
//
//        if (!photoDir.exists()) {
//            boolean mkdirResult = photoDir.mkdirs();
//            if (!mkdirResult) {
//                LOG.info("Failed to create video directory");
//                return;
//            }
//        }
//    }

    private void iristickUpdateHUDViews() {
        if (iristickHUD != null) {
            iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
            iristickHUD.sessionEntriesCount.setText(sessionEntriesCount.getText());
            iristickHUD.sessionValue.setText(sessionValue.getText());

            if (isColumnNext || isColumnPrevious) {
                spinnerPosition = spokenPosition;
            } else {
                spinnerPosition = vdtsNamedPositionedAdapter.getSelectedSpinnerPosition() + 1;
            }

            if (selectedColumnValue != null ||
                    isColumnNext ||
                    isColumnPrevious) {
                if (selectedColumnValue != null &&
                        !isColumnNext &&
                        !isColumnPrevious) {
                    entryHUDMap.put(spinnerPosition - 1, selectedColumnValue.getName());
                }

                //Set Column values
                if (columnMap.size() > 0) {
                    if (spinnerPosition - 1 >= 0) {
                        iristickHUD.columnLastLabel.setText(Objects.requireNonNull(
                                columnMap.get(spinnerPosition - 1)).getName());
                    } else {
                        iristickHUD.columnLastLabel.setText("");
                    }

                    if (columnMap.size() != spinnerPosition) {
                        iristickHUD.columnNextLabel.setText(Objects.requireNonNull(
                                columnMap.get(spinnerPosition)).getName());
                        iristickHUD.entryNextValue.setBackground(ContextCompat.getDrawable(
                                this,
                                R.drawable.spinner_background)
                        );
                    } else {
                        iristickHUD.columnNextLabel.setText(
                                R.string.data_gathering_hud_end_of_row);
                        iristickHUD.entryNextValue.setBackground(ContextCompat.getDrawable(
                                this,
                                R.drawable.hud_text_background)
                        );
                    }
                }

                //Set ColumnValue values
                if (entryHUDMap.get(spinnerPosition - 1) != null) {
                    iristickHUD.entryLastValue.setText(
                            Objects.requireNonNull(entryHUDMap.get(spinnerPosition - 1)));
                } else {
                    iristickHUD.entryLastValue.setText("");
                }

                if (entryHUDMap.get(spinnerPosition) != null ) {
                    iristickHUD.entryNextValue.setText(Objects.requireNonNull(
                            entryHUDMap.get(spinnerPosition)));
                } else {
                    iristickHUD.entryNextValue.setText("");
                }
            } else {
                iristickHUD.columnLastLabel.setText("");
                iristickHUD.columnNextLabel.setText(Objects.requireNonNull(
                        columnMap.get(0)).getName());
                iristickHUD.entryLastValue.setText("");
                iristickHUD.entryNextValue.setText("");
                iristickHUD.entryNextValue.setBackground(ContextCompat.getDrawable(
                        this,
                        R.drawable.spinner_background)
                );
                iristickHUD.entryCommentValue.setChecked(false);
                iristickHUD.entryPicValue.setText("");
            }
        }

        isColumnNext = false;
        isColumnPrevious = false;
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views
        private TextureView iriCameraView;

        private ConstraintLayout dataGatheringView;

        private TextView columnLastLabel;
        private TextView columnNextLabel;

        private TextView entryIndexValue;
        private TextView entryLastValue;
        private TextView entryNextValue;
        private CheckBox entryCommentValue;
        private TextView entryPicValue;

        private TextView sessionValue;
        private TextView sessionEntriesCount;

        private ConstraintLayout commentView;
        private TextView commentValue;
        private TextView addCommentButton;
        private TextView clearCommentButton;
        private TextView cancelCommentButton;
        private TextView enterCommentButton;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_data_gathering_hud);

            //HUD Views
            iriCameraView = findViewById(R.id.iriCameraView);

            dataGatheringView = findViewById(R.id.dataGatheringView);

            columnLastLabel = findViewById(R.id.columnLastLabel);
            columnNextLabel = findViewById(R.id.columnNextLabel);

            entryIndexValue = findViewById(R.id.entryIndexValue);
            entryLastValue = findViewById(R.id.entryValueLast);
            entryNextValue = findViewById(R.id.entryValueNext);
            entryCommentValue = findViewById(R.id.entryValueComment);
            entryPicValue = findViewById(R.id.entryValuePic);

            sessionValue = findViewById(R.id.sessionValue);
            sessionEntriesCount = findViewById(R.id.sessionEntriesCount);

            commentView = findViewById(R.id.commentView);
            commentValue = findViewById(R.id.commentValue);
            addCommentButton = findViewById(R.id.addCommentButton);
            clearCommentButton = findViewById(R.id.clearCommentButton);
            cancelCommentButton = findViewById(R.id.cancelCommentButton);
            enterCommentButton = findViewById(R.id.enterCommentButton);
        }
    }
}