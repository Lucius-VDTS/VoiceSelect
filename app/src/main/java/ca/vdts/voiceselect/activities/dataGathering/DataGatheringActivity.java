package ca.vdts.voiceselect.activities.dataGathering;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.widget.Toast.LENGTH_LONG;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.WorkerThread;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.adapters.DataGatheringRecyclerAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
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
import ca.vdts.voiceselect.library.utilities.VDTSCustomLifecycle;
import ca.vdts.voiceselect.library.utilities.VDTSImageFileUtils;
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
    private int currentUserFeedbackQueue;

    private Session currentSession;
    private Entry currentEntry;
    private final AtomicBoolean isEntrySelected = new AtomicBoolean(false);
    private ColumnValue selectedColumnValue;
    private int emptyColumnValueIndex = -1;
    private int spokenPosition = 0;
    private int spinnerPosition = 0;
    private boolean isColumnSkipped = false;

    //Lists
    private List<SessionLayout> currentSessionLayoutList;
    private final HashMap<Integer, Column> columnMap = new HashMap<>();
    private final HashMap<Integer, ColumnSpoken> columnSpokenMap = new HashMap<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueMap = new HashMap<>();
    private final HashMap<Integer, List<ColumnValueSpoken>> columnValueSpokenMap = new HashMap<>();
    private final HashMap<Integer, HashMap<String, Long>> positionColumnValueSpokenIDMap = new HashMap<>();
    private final List<Spinner> columnValueSpinnerList = new ArrayList<>();

    private final List<Entry> entryList = new ArrayList<>();
    private LiveData<List<Entry>> entryListLive;
    private final List<EntryValue> entryValueList = new ArrayList<>();
    private final HashMap<Integer, EntryValue> entryValueMap = new HashMap<>();
    private LiveData<List<EntryValue>> entryValueListLive;

    private final List<PictureReference> pictureReferenceList = new ArrayList<>();
    private LiveData<List<PictureReference>> pictureReferencesListLive;
    private final List<PictureReference> currentEntryPhotos = new ArrayList<>();

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
    private VDTSNamedPositionedAdapter vdtsNamedPositionedAdapter;

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private DataGatheringActivity.IristickHUD iristickHUD;
    private final HashMap<Integer, ColumnValue> entryHUDMap = new HashMap<>();
    private IRICamera iriCamera;
    private IRICameraSession iriCameraSession;
    private boolean iriCameraCaptureInProgress = false;

    //GPS Components
    private boolean GPSConnected = false;
    private LocationManager locationManager;
    private Location currentLocation;
    private static final int PERM_CODE_GPS = 2;

    //Device Camera Components
    private PreviewView previewView;
    private VDTSCustomLifecycle cameraLifecycle;
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
    private static final int ZOOM_LEVELS = 5;
    private int zoomLevel = 0;

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
            currentUserFeedbackQueue = 0;
        } else {
            currentUserFeedbackQueue = 1;
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

        cameraLifecycle = new VDTSCustomLifecycle();
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
        long currentSessionID = vdtsApplication.getPreferences().getLong(currentSessionKey);

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

//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Handler handler = new Handler(Looper.getMainLooper());
//        executor.execute(() -> {
//            columnSpokenList = vsViewModel.findAllColumnSpokensByUser(currentUser.getUid());
//
//            handler.post(() -> {
//                if (columnMap.size() != 0) {
//                    for (int index = 0; index < columnMap.size(); index++){
//                        TextView columnText = new TextView(this);
//                        columnText.setMinWidth(minWidthDimen);
//                        columnText.setLayoutParams(layoutParams);
//                        columnText.setPadding(marginPaddingDimen, marginPaddingDimen,
//                                marginPaddingDimen, marginPaddingDimen);
//                        columnText.setGravity(Gravity.CENTER);
//                        columnText.setMaxLines(1);
//                        columnText.setBackground(
//                                ContextCompat.getDrawable(this, R.drawable.text_background));
//                        columnText.setText(currentUser.isAbbreviate() ?
//                                Objects.requireNonNull(columnMap.get(index)).getNameCode() :
//                                Objects.requireNonNull(columnMap.get(index)).getName());
//                        columnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
//                        columnLinearLayout.addView(columnText);
//                    }
//                }
//
//                initializeColumnValuesLayout();
//            });
//        });

        if (columnMap.size() != 0) {
            for (int index = 0; index < columnMap.size(); index++){
                TextView columnText = new TextView(this);
                columnText.setMinWidth(minWidthDimen);
                columnText.setLayoutParams(layoutParams);
                columnText.setPadding(marginPaddingDimen, marginPaddingDimen,
                        marginPaddingDimen, marginPaddingDimen);
                columnText.setGravity(Gravity.CENTER);
                columnText.setMaxLines(1);
                columnText.setBackground(
                        ContextCompat.getDrawable(this, R.drawable.text_background));
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

        if (isHeadsetAvailable) {
            initializeIristickEntryGrammar();
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
                            (VDTSNamedPositionedAdapter) parent.getAdapter();

                    if (isHeadsetAvailable /*&& !entrySelected.get()*/) {
                        updateIristickHUD();
                    }

                    //todo - get last spinner in list
//                    Spinner spinner = columnValueSpinnerList.get(columnValueSpinnerList.size() - 1);
//                    if (!entrySelected.get() && ((Spinner) view).equals(spinner)) {
//                        entrySelected.set(false);
//                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
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
                    iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
                    iristickHUD.sessionEntriesCount.setText(sessionEntriesCount.getText());
                    iristickHUD.sessionValue.setText(sessionValue.getText());
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
            currentEntryPhotos.clear();
            currentEntryPhotos.addAll(dataGatheringRecyclerAdapter.getPictureReferences(index));

            if (isHeadsetAvailable) {
                List<EntryValue> entryValueList = entryValueListLive.getValue();
                if (entryValueList != null) {
                    List<EntryValue> currentEntryValueList = entryValueList.stream()
                            .filter(entryValue -> entryValue.getEntryID() == currentEntry.getUid())
                            .collect(Collectors.toList());

                    List<Long> currentEntryValueColumnValueIDs = currentEntryValueList.stream()
                            .mapToLong(EntryValue::getColumnValueID)
                            .distinct()
                            .boxed()
                            .collect(Collectors.toList());

                    for (int i = 0; i < columnValueSpinnerList.size(); i++) {
                        List<ColumnValue> columnValues = columnValueMap.get(i);
                        emptyColumnValueIndex = i;
                        assert columnValues != null;
                        List<Long> columnValueIDs = columnValues.stream()
                                .mapToLong(ColumnValue::getUid)
                                .distinct()
                                .boxed()
                                .collect(Collectors.toList());

                        //todo - break when no ids in currentEntryColumnValueIDs can be found in columnValueIDs//
//                        if (columnValueIDs.stream().noneMatch(currentEntryValueColumnValueIDs::contains)) {
//                            emptyColumnValueIndex = i;
//                            break;
//                        } else {
//                            emptyColumnValueIndex = -1;
//                        }

                        if (currentEntryValueColumnValueIDs.size() > 0 && columnValueIDs.size() > 0) {
                            if (i < currentEntryValueColumnValueIDs.size()) {
                                if (!Objects.equals(currentEntryValueColumnValueIDs.get(i),
                                        columnValueIDs.get(i))) {
                                    emptyColumnValueIndex = i;
                                    break;
                                }
                            } else {
                                emptyColumnValueIndex = i;
                                break;
                            }
                        } else {
                            emptyColumnValueIndex = -1;
                        }
                    }
                }

                iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
                iristickHUD.entryPicValue.setText(String.valueOf(currentEntryPhotos.size()));
                updateIristickHUD();
            }
        } else {
            newEntry();
        }

        updateViews();
    }

    private void updateViews() {
        isEntrySelected.set(true);
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
            showDeleteConfirmDialogue();
        }
    }

    private void showDeleteConfirmDialogue() {
        LOG.info("Showing Deletion Confirm Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Entry?");
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

    private void deleteEntryFeedback(boolean isDeleteLast) {
        String deleteEntry = "Delete row " + (entryList.size());
        currentUserTTSEngine.speak(
                deleteEntry,
                currentUserFeedbackQueue,
                null,
                null);

        if (isDeleteLast) {
            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Yes", () -> {
                        deleteEntry(true);
                        String rowDeleted = "Row " + (entryList.size()) + " deleted";
                        currentUserTTSEngine.speak(
                                rowDeleted,
                                currentUserFeedbackQueue,
                                null,
                                null);
                    })
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("No", () -> deleteEntry(true))
            );
        }
    }

    private void deleteEntry(boolean isDeleteLast) {
        ExecutorService deletePicturesService = Executors.newSingleThreadExecutor();
        Handler deletePicturesHandler = new Handler(Looper.getMainLooper());
        deletePicturesService.execute(() -> {
            currentEntryPhotos.forEach(this::deletePicture);
            final PictureReference[] pictureReferences = new PictureReference[currentEntryPhotos.size()];
            currentEntryPhotos.toArray(pictureReferences);
            vsViewModel.deleteAllPictureReferences(pictureReferences);
            deletePicturesHandler.post(() -> {
                final long deleteEntryID;
                if (isDeleteLast) {
                    deleteEntryID = entryList.size();
                    currentEntry = entryList.get((int) (deleteEntryID - 1));
                    //todo - add feedback when row deleted
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
                                updateViews();
                            });
                        });
                    });
                });
            });
        });
    }

    private void deletePicture(PictureReference pictureReference) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(
                () -> {
                    String[] projection = { MediaStore.Images.Media._ID };
                    String selection = MediaStore.Images.Media.DATA + " = ?";
                    String[] selectionArgs = new String[] { pictureReference.getPath() };

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
        List<PictureReference> deletePictureReference = currentEntryPhotos.stream()
                .filter(pictureReference -> pictureReference.getUid() == 0)
                .collect(Collectors.toList());
        deletePictureReference.forEach(this::deletePicture);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final PictureReference[] deletePictures = new PictureReference[deletePictureReference.size()];
            deletePictureReference.toArray(deletePictures);
            vsViewModel.deleteAllPictureReferences(deletePictures);
        });

        newEntry();
        entryHUDMap.clear();
        updateViews();
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
                    long currentEntryUid = currentEntry.getUid();
                    if (currentEntryUid == 0L) {
                        final Entry lastEntry = dataGatheringRecyclerAdapter.getEntry(
                                dataGatheringRecyclerAdapter.getItemCount() - 1
                        );
                        assert lastEntry != null;
                        currentEntryUid = lastEntry.getUid();
                    }
                    final long finalEntryID = currentEntryUid;
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
                valueExecutor.execute(() -> vsViewModel.insertAllEntryValues(copyEntryValues));
            });
        });
    }

    private void saveEntryButtonOnClick() {
        if (currentEntry == null) newEntry();
        boolean pictureIssue = currentSession.isPictureRequired() && currentEntryPhotos.isEmpty();
        boolean commentIssue = currentSession.isCommentRequired() &&
                (currentEntry.getComment() == null || currentEntry.getComment().isEmpty());
        if (commentIssue && pictureIssue){
            vdtsApplication.displayToast(
                    this,
                    "This entry requires a comment and a picture."
            );
        } else if (commentIssue){
            vdtsApplication.displayToast(
                    this,
                    "This entry requires a comment."
            );
        } else if (pictureIssue){
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

        isColumnSkipped = false;
        spokenPosition = 0;
    }

    private void savePictureReferences(long entryID) {
        final List<PictureReference> insertPictureReferenceList = new ArrayList<>();
        final List<PictureReference> updatePictureReferenceList = new ArrayList<>();

        currentEntryPhotos.forEach(pictureReference -> {
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
                updateViews();

                entryHUDMap.clear();
                if (isHeadsetAvailable) {
                    iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
                    updateIristickHUD();
                }
            });
        });
    }

    private void endSessionButtonOnClick() {
        if (currentSession!=null) {
            showEndConfirmDialogue();
        }
    }

    private void showEndConfirmDialogue() {
        LOG.info("Showing End Confirm Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("End Session?");
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
                vdtsApplication.getPreferences().setLong(
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
        final Exporter exporter = new Exporter(vsViewModel,vdtsApplication,this);
        boolean CSV = true;
        boolean Excel = true;
        boolean JSON = true;

        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV,false)){
            CSV = exporter.exportSessionCSV(session);
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON,false)){
            JSON= exporter.exportSessionJSON(session);
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX,true)){
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
        emptyColumnValueIndex = -1;
        dataGatheringRecyclerAdapter.clearSelected();
        columnScrollView.setScrollX(0);
        currentEntry = new Entry(currentUser.getUid(), currentSession.getUid());
        selectedColumnValue = null;
        currentEntryPhotos.clear();
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
                (dialogInterface, i) -> {}
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
                    VDTSImageFileUtils.generateFileName("", false),
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
                            new MediaActionSound().play(MediaActionSound.SHUTTER_CLICK);
                            YoYo.with(Techniques.Pulse)
                                    .duration(PULSE_DURATION)
                                    .repeat(PULSE_REPEAT)
                                    .playOn(previewView);

                            VDTSImageFileUtils.addGPS(imageFile.getPath(), currentLocation);
                            if (currentEntry == null) {
                                newEntry();
                            }

                            PictureReference pictureReference = new PictureReference(
                                    currentUser.getUid(),
                                    currentEntry.getUid(),
                                    imageFile.getPath(),
                                    currentLocation
                            );
                            currentEntryPhotos.add(pictureReference);
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
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, currentLocation)) {
            currentLocation = location;
            LOG.debug("{}, {}", location.getLatitude(), location.getLongitude());
            if (!GPSConnected){
                GPSConnected = true;
                vdtsApplication.displayToast(
                        this,
                        "GPS connection found"
                );
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        LOG.debug("onProviderEnabled: {}", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
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
            scrollDetector.onTouchEvent(motionEvent);
            return scaleDetector.onTouchEvent(motionEvent);
        }
    };

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
        initializeIristick(headset);
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
    }

    private void initializeIristick(IRIHeadset headset) {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new DataGatheringActivity.IristickHUD();
            return iristickHUD;
        });

        initializeIristickVoiceCommands();
        initializeIristickCameraGrammar(headset);
    }

    private void initializeIristickVoiceCommands() {
        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("End Session", this::endSessionButtonOnClick)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Navigate Back", this::finish)
        );
    }

    @OptIn(markerClass = Experimental.class)
    private void initializeIristickEntryGrammar() {
        if (isHeadsetAvailable) {
            //Step By Step
            IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
                vg.addAlternativeGroup(stepped -> {
                    //First entry value in row
                    stepped.addAlternativeGroup(start -> {
                        List<ColumnValueSpoken> columnValueSpokenList = columnValueSpokenMap.get(0);
                        if (columnValueSpokenList != null) {
                            for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
                                start.addToken(columnValueSpoken.getSpoken());
                            }
                        }

                        start.addToken("Skip");
                        start.addToken("Delete Last");
                        start.addToken("Repeat Entry");
                        start.addToken("End Session");
                    });

                    //Rest of entry values in row
                    for (int index = 1; index < columnValueSpokenMap.size(); index++) {
                        int finalIndex = index;
                        stepped.addAlternativeGroup(middle -> {
                            List<ColumnValueSpoken> columnValueSpokenList =
                                    columnValueSpokenMap.get(finalIndex);
                            if (columnValueSpokenList != null) {
                                for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
                                    middle.addToken(columnValueSpoken.getSpoken());
                                }
                            }

                            middle.addToken("Skip");
                            middle.addToken("Reset Entry");
                        });
                    }

                    //End of row
                    stepped.addAlternativeGroup(end -> {
                        end.addToken("Make Comment");
                        end.addToken("Open Camera");
                        end.addToken("Reset Entry");
                        end.addToken("Save Entry");
                    });

//                    if (currentSession.isCommentRequired() && currentSession.isPictureRequired()) {
//                        stepped.addAlternativeGroup(comment -> {
//                            comment.addToken("Make Comment");
//                            comment.addToken("Reset Entry");
//                        });
//
//                        stepped.addAlternativeGroup(picture -> {
//                            picture.addToken("Open Camera");
//                            picture.addToken("Reset Entry");
//                        });
//
//                        stepped.addAlternativeGroup(finish -> {
//                            finish.addToken("Reset Entry");
//                            finish.addToken("Save Entry");
//                        });
//                    } else if (currentSession.isCommentRequired()) {
//                        stepped.addAlternativeGroup(end -> {
//                            end.addToken("Make Comment");
//                            end.addToken("Reset Entry");
//                        });
//
//                        stepped.addAlternativeGroup(finish -> {
//                            finish.addToken("Open Camera");
//                            finish.addToken("Reset Entry");
//                            finish.addToken("Save Entry");
//                        });
//                    } else if (currentSession.isPictureRequired()) {
//                        stepped.addAlternativeGroup(end -> {
//                            end.addToken("Open Camera");
//                            end.addToken("Reset Entry");
//                        });
//
//                        stepped.addAlternativeGroup(finish -> {
//                            finish.addToken("Make Comment");
//                            finish.addToken("Reset Entry");
//                            finish.addToken("Save Entry");
//                        });
//                    } else {
//
//                    }
                });

                vg.setListener(((recognizer, tokens, tags) -> {
                    switch (tokens[0]) {
                        case "Skip":

                            spokenPosition++;
                            isColumnSkipped = true;
                            updateIristickHUD();
                            break;
                        case "Reset Entry":
                            spokenPosition = 0;
                            resetEntryButtonOnClick();
                            break;
                        case "Delete Last":
                            spokenPosition = 0;
                            deleteEntryFeedback(true);
                            break;
                        case "Repeat Entry":
                            spokenPosition = 0;
                            repeatEntryButtonOnClick();
                            break;
                        case "Save Entry":
                            spokenPosition = 0;
                            saveEntryButtonOnClick();
                            break;
                        case "End Session":
                            spokenPosition = 0;
                            endSessionButtonOnClick();
                            break;
                        default:
                            enterColumnValueCommand(tokens);
                    }
                }));
            });

            //Chained
//            IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
//                vg.addSequentialGroup(sg -> {
//                    //First entry value in row
//                    sg.addAlternativeGroup(start -> {
//                        List<ColumnValueSpoken> columnValueSpokenList = columnValueSpokenMap.get(0);
//                        if (columnValueSpokenList != null) {
//                            for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
//                                start.addToken(columnValueSpoken.getSpoken());
//                            }
//                        }
//
//                        start.addToken("Skip");
//                        start.addToken("Delete Last");
//                        start.addToken("Repeat Entry");
//                        start.addToken("End Session");
//                    });
//
//                    //Rest of entry values in row
//                    for (int index = 1; index < columnValueSpokenMap.size(); index++) {
//                        int finalIndex = index;
//                        sg.addAlternativeGroup(middle -> {
//                            List<ColumnValueSpoken> columnValueSpokenList =
//                                    columnValueSpokenMap.get(finalIndex);
//                            if (columnValueSpokenList != null) {
//                                for (ColumnValueSpoken columnValueSpoken : columnValueSpokenList) {
//                                    middle.addToken(columnValueSpoken.getSpoken());
//                                }
//                            }
//
//                            middle.addToken("Skip");
//                            middle.addToken("Reset Entry");
//                        });
//                    }
//
//                    //End of row
//                    sg.addAlternativeGroup(end -> {
//                        end.addToken("Make Comment");
//                        end.addToken("Open Camera");
//                        end.addToken("Reset Entry");
//                        end.addToken("Save Entry");
//                    });
//                });
//
//                vg.setListener(((recognizer, tokens, tags) -> {
//                    //todo - stuff based on tokens
//                    String test = tokens[0];
//                }));
//            });
        }
    }

    private void enterColumnValueCommand(String[] tokens) {
        HashMap<String, Long> spokenColumnValueIDMap =
                positionColumnValueSpokenIDMap.get(spokenPosition);
        if (spokenColumnValueIDMap != null) {
            if (spokenColumnValueIDMap.get(tokens[0]) != null) {
                Long columnValueID = spokenColumnValueIDMap.get(tokens[0]);

                List<ColumnValue> columnValueList = columnValueMap.get(spokenPosition);
                ColumnValue columnValue = null;
                if (columnValueList != null) {
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
            }
        }
    }

    @OptIn(markerClass = Experimental.class)
    private void initializeIristickCameraGrammar(IRIHeadset headset) {
        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
           vg.addSequentialGroup(sg -> {
               sg.addAlternativeGroup(ag -> {
                   ag.addToken("Open");
                   ag.addToken("Wide");
                   ag.addToken("Zoom");
                   ag.addToken("Show");
                   ag.addToken("Hide");
                   ag.addToken("Close");
               });

               sg.addToken("Camera");
           });

           vg.setListener((((recognizer, tokens, tags) -> {
               switch(tokens[0]) {
                   case "Open":
                   case "Wide":
                       if (iriCameraSession != null) { closeIriCamera(); }
                       openIriCamera(headset, -1);
                       break;
                   case "Zoom":
                       if (iriCameraSession != null) { closeIriCamera(); }
                       openIriCamera(headset, 1);
                       break;
                   case "Show":
                       if (iriCameraSession != null) {
                           iristickHUD.dataGatheringView.setVisibility(View.INVISIBLE);
                           iristickHUD.iriCameraPreview.setVisibility(View.VISIBLE);
                       }
                       break;
                   case "Hide":
                       if (iriCameraSession != null) {
                           iristickHUD.iriCameraPreview.setVisibility(View.INVISIBLE);
                           iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);
                       }
                       break;
                   case "Close":
                       closeIriCamera();
                       break;
               }
           })));
        });

        IristickSDK.addVoiceGrammar(getLifecycle(), getApplicationContext(), vg -> {
            vg.addSequentialGroup(sg -> {
                sg.addToken("Zoom");

                sg.addAlternativeGroup(zoomLevel -> {
                    for (int index = 0; index <= 5; index++) {
                        zoomLevel.addToken(String.valueOf(index));
                    }
                });
            });

            vg.setListener((((recognizer, tokens, tags) -> {
                if (iriCameraSession != null) {
                    int zoomLevel = Integer.parseInt(tokens[1]);
                    switch(zoomLevel) {
                        case 0:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.0f));
                            break;
                        case 1:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.2f));
                            break;
                        case 2:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.4f));
                            break;
                        case 3:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.6f));
                            break;
                        case 4:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(1.8f));
                            break;
                        case 5:
                            iriCameraSession.reconfigure(zoom -> zoom.setZoom(2.0f));
                            break;
                    }
                }
            })));
        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                takePhoto -> takePhoto.add("Take Picture", () -> {
                    if (iriCameraSession != null) { takeIriPicture(); }
                })
        );
    }

    private void updateIristickHUD() {
        if (iristickHUD != null) {
            spinnerPosition = vdtsNamedPositionedAdapter.getSelectedSpinnerPosition();
            if (isColumnSkipped) { spinnerPosition = spokenPosition - 1; }

            if (selectedColumnValue != null ||
                    emptyColumnValueIndex >= 0 ||
                    isColumnSkipped) {
                if (selectedColumnValue != null && !isColumnSkipped) {
                    entryHUDMap.put(spinnerPosition, selectedColumnValue);
                } else if (!isColumnSkipped) {       //todo - select first empty entry
                    spinnerPosition = emptyColumnValueIndex;
                }

                //Set Column values
                if (columnMap.size() > 0) {
                    iristickHUD.columnLastLabel.setText(Objects.requireNonNull(
                                columnMap.get(spinnerPosition)).getName());

                    if (columnMap.size() != spinnerPosition + 1) {
                        iristickHUD.columnNextLabel.setText(Objects.requireNonNull(
                                columnMap.get(spinnerPosition + 1)).getName());
                    } else {
                        iristickHUD.columnNextLabel.setText(
                                R.string.data_gathering_hud_end_of_row);
                        iristickHUD.entryNextValue.setBackground(ContextCompat.getDrawable(
                                this,
                                R.drawable.text_background)
                        );
                    }
                }

                //Set ColumnValue values
                if (isColumnSkipped) {
                    if (entryHUDMap.get(spinnerPosition) != null) {
                        iristickHUD.entryLastValue.setText(
                                Objects.requireNonNull(entryHUDMap.get(spokenPosition)).getName());
                    } else {
                        iristickHUD.entryLastValue.setText("");
                    }
                } else {
                    iristickHUD.entryLastValue.setText(selectedColumnValue != null ?
                            selectedColumnValue.getName() :
                            "");
                }

                if (entryHUDMap.get(spinnerPosition + 1) != null ) {
                    iristickHUD.entryNextValue.setText(Objects.requireNonNull(
                            entryHUDMap.get(spinnerPosition + 1)).getName());
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

        isColumnSkipped = false;
    }

    @OptIn(markerClass = Experimental.class)
    private void openIriCamera(IRIHeadset headset, int cameraType) {
        if (cameraType == 1) {
            if (headset.findCamera(IRICameraType.ZOOM) != null) {
                iriCamera = headset.findCamera(IRICameraType.ZOOM);
                iristickHUD.displayToast(this, "Zoom Camera");
            } else if (headset.findCamera(IRICameraType.WIDE_ANGLE) != null) {
                iriCamera = headset.findCamera(IRICameraType.WIDE_ANGLE);
                iristickHUD.displayToast(this, "Wide Camera");
            }
        } else {
            if (headset.findCamera(IRICameraType.WIDE_ANGLE) != null) {
                iriCamera = headset.findCamera(IRICameraType.WIDE_ANGLE);
                iristickHUD.displayToast(this, "Wide Camera");
            } else if (headset.findCamera(IRICameraType.ZOOM) != null) {
                iriCamera = headset.findCamera(IRICameraType.ZOOM);
                iristickHUD.displayToast(this, "Zoom Camera");
            }
        }

        iriCameraSession = iriCamera.openSession(
                getLifecycle(), IRICameraProfile.STILL_CAPTURE, ic ->
                        ic.addPreview(iristickHUD.iriCameraPreview));

        if (cameraType == 1) {
            iriCameraSession.reconfigure(zoomLevel -> zoomLevel.setZoom(1.0f));
        }

        iristickHUD.dataGatheringView.setVisibility(View.INVISIBLE);
        iristickHUD.iriCameraPreview.setVisibility(View.VISIBLE);
    }

    @OptIn(markerClass = Experimental.class)
    private void closeIriCamera() {
        iriCameraSession.close();
        iriCameraSession = null;
        iristickHUD.iriCameraPreview.setVisibility(View.INVISIBLE);
        iristickHUD.dataGatheringView.setVisibility(View.VISIBLE);
    }

    @OptIn(markerClass = Experimental.class)
    private void takeIriPicture() {
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
                        VDTSImageFileUtils.generateFileName("", false),
                        ".jpg",
                        new File(photoDir.getPath())
                );

                VDTSImageFileUtils.addGPS(imageFile.getPath(), currentLocation);

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

                    currentEntryPhotos.add(pictureReference);
                }

                iristickHUD.entryPicValue.setText(String.valueOf(currentEntryPhotos.size()));

                new VDTSMediaScannerUtil(this, imageFile);

                LOG.info("Capture succeeded");
            } catch (IOException e) {
                LOG.error("Capture failed", e);
            }
        });
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views
        private TextureView iriCameraPreview;

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

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_data_gathering_hud);

            //HUD Views
            iriCameraPreview = findViewById(R.id.iriCameraPreview);

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
        }

        @WorkerThread
        public void displayToast(Context context, String message) {
            ContextCompat.getMainExecutor(context).execute(() ->
                    Toast.makeText(context, message, LENGTH_LONG).show());
        }
    }
}