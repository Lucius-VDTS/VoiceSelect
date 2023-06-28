package ca.vdts.voiceselect.activities.dataGathering;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.location.LocationManager.GPS_PROVIDER;
import static android.location.LocationManager.NETWORK_PROVIDER;
import static android.widget.Toast.LENGTH_SHORT;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_BRIGHTNESS;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_ZOOM;
import static ca.vdts.voiceselect.library.utilities.VDTSLocationUtil.isBetterLocation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.configure.ConfigColumnsActivity;
import ca.vdts.voiceselect.adapters.DataGatheringRecyclerAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSNamedPositionedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;
import ca.vdts.voiceselect.library.utilities.VDTSCustomLifecycle;
import ca.vdts.voiceselect.library.utilities.VDTSImageFileUtils;

/**
 * Gather data for a particular session and its corresponding layout.
 */
@SuppressLint("RestrictedApi")
public class DataGatheringActivity extends AppCompatActivity
        implements IRIListener, ScrollChangeListenerInterface, LocationListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigColumnsActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private Layout currentLayout;
    private Session currentSession;
    private Entry selectedEntry;
    private Column lastColumn;
    private ColumnValue selectedColumnValue;

    //Lists
    private List<LayoutColumn> currentLayoutColumnList;
    private final List<Column> columnList = new ArrayList<>();
    private final HashMap<Integer, Column> columnMap = new HashMap<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueMap = new HashMap<>();
    private final List<Spinner> columnValueSpinnerList = new ArrayList<>();

    private final List<Entry> entryList = new ArrayList<>();
    private LiveData<List<Entry>> entryListLive;
    private final List<EntryValue> entryValueList = new ArrayList<>();
    private final HashMap<Integer, EntryValue> entryValueMap = new HashMap<>();
    private LiveData<List<EntryValue>> entryValueListLive;

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

    //GPS Components
    private boolean GPSConnected = false;
    private LocationManager locationManager;
    private Location currentLocation;
    private static final int PERM_CODE_GPS = 2;

    //Device Camera Components
    private PreviewView previewView;
    private VDTSCustomLifecycle cameraLifecycle;
    private Camera camera;
    private ImageCapture imageCapture;
    private static final int EXPOSURE_LEVELS = 10; // todo - add controls to control
    private int exposureLevel = EXPOSURE_LEVELS / 2;
    private static final int ZOOM_LEVELS = 5;
    private int zoomLevel = 0;

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
                ));

        //

        //Camera
        previewView = findViewById(R.id.cameraPreview);

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
        initializeIristickHUD();
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

    private void initializeIristickHUD() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new DataGatheringActivity.IristickHUD();
            return iristickHUD;
        });

        initializeLayout();
    }

    private void initializeLayout() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            currentLayout = vsViewModel.findLayoutByID(
                    getIntent().getLongExtra("layout", -9001L));
            currentLayoutColumnList = vsViewModel.findAllLayoutColumnsByLayout(currentLayout);
            handler.post(this::initializeSession);
        });
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
            columnMap.clear();
            for (LayoutColumn layoutColumn : currentLayoutColumnList) {
                Column column = vsViewModel.findColumnByID(layoutColumn.getColumnID());
                columnMap.put((int) layoutColumn.getColumnPosition() - 1, column);
            }

            handler.post(() -> {
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
                        columnText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                        columnLinearLayout.addView(columnText);
                    }
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
            columnValueMap.clear();
            for (int index = 0; index < columnMap.size(); index++) {
                columnValueMap.put(
                        index,
                        vsViewModel.findAllActiveColumnValuesByColumn(
                                Objects.requireNonNull(columnMap.get(index)).getUid())
                );
            }

            handler.post(() -> {
                for (int index = 0; index < columnValueMap.size(); index++) {
                    ColumnValueSpinner columnValueSpinner =
                            new ColumnValueSpinner(
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

//            entryListLive.removeObservers(this);
            entryListLive = vsViewModel.findAllEntriesBySessionLive(currentSession.getUid());
            handler.post(() -> {

                initializeEntryValuesList();
            });
        });
    }

    private void initializeEntryValuesList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            entryValueListLive = vsViewModel.findAllEntryValuesLiveBySession(currentSession.getUid());
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
                entryValueList);

        entryRecyclerView.setAdapter(dataGatheringRecyclerAdapter);

        entryListLive.observe(this, entryObserver);
        entryValueListLive.observe(this, entryValueObserver);

        columnValueIndexValue.setText(String.format(
                Locale.getDefault(),
                "%d",
                dataGatheringRecyclerAdapter.getItemCount() + 1));

        if (isHeadsetAvailable) {
            initializeIristickVoiceCommands();

            iristickHUD.sessionValue.setText(sessionValue.getText());
            iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
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
                                int x, int y,
                                int oldx, int oldy) {
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

                    updateIristickHUD();
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
                sessionEntriesCount.setText(String.format(
                        Locale.getDefault(), "%d", entries.size()));

                if (isHeadsetAvailable) {
                    iristickHUD.sessionEntriesCount.setText(sessionEntriesCount.getText());
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

    //todo - select entry
    private void entryAdapterSelect(int index) {

    }

    private void pictureButtonOnClick() {
        if (cameraLifecycle.getLifecycle().getCurrentState() == Lifecycle.State.STARTED) {
            hidePreview();
        } else {
            showPreview();
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
                    entryValueMap.clear();

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

                        columnValueSpinnerList.get(index).setSelection(0);
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
                                    dataGatheringRecyclerAdapter.getItemCount() + 1)
                            );

                            columnScrollView.setScrollX(0);

                            selectedColumnValue = null;
                            entryHUDMap.clear();
                            if (isHeadsetAvailable) {
                                iristickHUD.entryIndexValue.setText(columnValueIndexValue.getText());
                                updateIristickHUD();
                            }
                        });
                    });
                });
            });
        }
    }

    private void showPreview() {
        previewView.bringToFront();
        cameraLifecycle.performEvent(Lifecycle.Event.ON_START);
    }

    private void hidePreview() {
        entryRecyclerView.bringToFront();
        cameraLifecycle.performEvent(Lifecycle.Event.ON_STOP);
    }

    private void openCamera() {
        Intent openCameraActivityIntent = new Intent(this, IristickCameraActivity.class);
        startActivity(openCameraActivityIntent);
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
            DateTimeFormatter dateTimeWithoutSeconds = DateTimeFormatter.ofPattern("yyyy_MM_dd HH_mm");
            final File imageFile = File.createTempFile(
                    dateTimeWithoutSeconds.format(LocalDateTime.now()),
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
                            VDTSImageFileUtils.addGPS(imageFile.getPath(), currentLocation);
                            PictureReference pictureReference = new PictureReference(
                                    currentUser.getUid(),
                                    selectedEntry.getUid(),
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
        } catch (IOException e) {
            LOG.error("IO Exception: ", e);
        }
    }

    private void zoomIn() {
        if (zoomLevel < ZOOM_LEVELS) {
            ++zoomLevel;
            setCameraZoom(zoomLevel, true);
        }
    }

    private void zoomOut() {
        if (zoomLevel > 0) {
            --zoomLevel;
            setCameraZoom(zoomLevel, true);
        }
    }

    private void setCameraZoom(int zoom, boolean feedback) {
        if (feedback) {
            vdtsApplication.displayToast(this,String.format(Locale.CANADA, "Setting zoom to %d", zoom),0);
        }
        zoomLevel = zoom;
        final float linearZoom = (float) zoom / (float) ZOOM_LEVELS;
        LOG.debug("Zoom level: {}/{}, Linear Zoom: {}", zoom, ZOOM_LEVELS, linearZoom);
        camera.getCameraControl().setLinearZoom(linearZoom);
        vdtsApplication.getPreferences().setInt(PREF_ZOOM, zoom);
    }

    private void increaseExposure() {
        if (exposureLevel < EXPOSURE_LEVELS) {
            ++exposureLevel;
            setExposureLevel(exposureLevel, true);
        }
    }

    private void decreaseExposure() {
        if (exposureLevel > 0) {
            --exposureLevel;
            setExposureLevel(exposureLevel, true);
        }
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalExposureCompensation.class)
    private void setExposureLevel(int exposureLevel, boolean feedback) {
        if (feedback) {
            vdtsApplication.displayToast(this, String.format(Locale.CANADA, "Setting brightness to %d", exposureLevel),0
            );
        }
        this.exposureLevel = exposureLevel;
        camera.getCameraControl().setExposureCompensationIndex(exposureLevel);
        vdtsApplication.getPreferences().setInt(PREF_BRIGHTNESS, exposureLevel);
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
                        "GPS connection found",
                        LENGTH_SHORT
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

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
    }

    private void initializeIristickVoiceCommands() {
        if (isHeadsetAvailable) {
            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Open Camera", this::openCamera)
            );

            IristickSDK.addVoiceCommands(
                    this.getLifecycle(),
                    this,
                    vc -> vc.add("Navigate Back", this::finish)
            );
        }
    }

    private void updateIristickHUD() {
        if (iristickHUD != null) {
            int spinnerPosition = vdtsNamedPositionedAdapter.getSelectedSpinnerPosition();

            if (selectedColumnValue != null) {
                entryHUDMap.put(spinnerPosition, selectedColumnValue);
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
                iristickHUD.entryLastValue.setText(selectedColumnValue.getName());
                if (entryHUDMap.get(spinnerPosition + 1) != null ) {
                    iristickHUD.entryNextValue.setText(Objects.requireNonNull(
                            entryHUDMap.get(spinnerPosition + 1)).getName());
                } else {
                    iristickHUD.entryNextValue.setText("");
                }
            } else {
                iristickHUD.columnLastLabel.setText("");
                iristickHUD.columnNextLabel.setText(Objects.requireNonNull(
                        columnMap.get(1)).getName());
                iristickHUD.entryLastValue.setText("");
                iristickHUD.entryNextValue.setText("");
            }
        }
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views
        private TextView columnLastLabel;
        private TextView columnNextLabel;

        private TextView entryIndexValue;
        private TextView entryLastValue;
        private TextView entryNextValue;
        private TextView entryCommentValue;
        private TextView entryPhotoValue;

        private TextView sessionValue;
        private TextView sessionEntriesCount;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_data_gathering_hud);

            //HUD Views
            columnLastLabel = findViewById(R.id.columnLastLabel);
            columnNextLabel = findViewById(R.id.columnNextLabel);

            entryIndexValue = findViewById(R.id.entryIndexValue);
            entryLastValue = findViewById(R.id.entryValueLast);
            entryNextValue = findViewById(R.id.entryValueNext);
            entryCommentValue = findViewById(R.id.entryValueComment);
            entryPhotoValue = findViewById(R.id.entryValuePhoto);

            sessionValue = findViewById(R.id.sessionValue);
            sessionEntriesCount = findViewById(R.id.sessionEntriesCount);
        }
    }
}
