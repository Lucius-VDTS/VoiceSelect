package ca.vdts.voiceselect.library;

import android.app.Application;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.library.database.VDTSDatabase;
import ca.vdts.voiceselect.library.database.VDTSPrefKeyValue;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.database.repositories.VDTSPrefRepository;
import ca.vdts.voiceselect.library.utilities.VDTSFeedbackUtil;

/**
 * Base VDTS application class.
 */
public class VDTSApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(VSApplication.class);

    public static final String SHARED_PREFERENCES = "SHARED_PREFERENCES";

    public static final String PREF_ENTRY_METHOD = "PREF_ENTRY_METHOD";

    public static final String PREF_AUTOSAVE = "PREF_AUTOSAVE ";
    public static final String PREF_PHOTO_PRINT_NAME = "PREF_PHOTO_PRINT_NAME";
    public static final String PREF_PHOTO_PRINT_GPS = "PREF_PHOTO_PRINT_GPS";
    public static final String PREF_PHOTO_PRINT_TIME = "PREF_PHOTO_PRINT_TIME";
    public static final String PREF_EXPORT_CSV = "PREF_EXPORT_CSV";
    public static final String PREF_EXPORT_JSON = "PREF_EXPORT_JSON";
    public static final String PREF_EXPORT_XLSX = "PREF_EXPORT_XLSX";

    public static final long DEFAULT_UID = -9001L;
    public static final int METHOD_CHAINED = 0;
    public static final int METHOD_STEP = 1;
    public static final int METHOD_FREE = 2;
    public static final LocalDateTime DEFAULT_DATE = LocalDateTime.now();

    //VDTSApplication
    private VDTSApplication vdtsApplication;

    //VDTSDatabase
    private VDTSDatabase vdtsDatabase;

    //VDTSPreferences
    private VDTSPrefKeyValue preferences;
    private VDTSPrefRepository vdtsPrefRepository;

    //VDTSUser
    private VDTSUser currentVDTSUser;
    private int userCount = 0;

    //VDTSFeedback
    private VDTSFeedbackUtil vdtsFeedbackUtil;
    private TextToSpeech ttsEngine;

    //Shake settings
    public static final int SHAKE_DURATION = 200;

    public static final int SHAKE_REPEAT = 2;

    //Strings
    public static final String EXPORT_FILE_USERS = "Users";
    public static final String EXPORT_FILE_SETUP = "Setup";
    public static final String EXPORT_FILE_LAYOUT = "Layout";
    public static final String EXPORT_FILE_OPTIONS = "Settings";
    public static final String FILE_EXTENSION_VDTS = ".vdts";
    public static final String FILE_EXTENSION_JSON = ".json";
    public static final String FILE_EXTENSION_CSV = ".csv";
    public static final String FILE_EXTENSION_XLSX = ".xlsx";
    public static final String FILE_EXTENSION_JPG = ".jpg";
    public static final String FILE_EXTENSION_MP4 = ".mp4";

    //Directory
    public static final String CONFIG_DIRECTORY = "/Configuration";
    public static final String SESSIONS_DIRECTORY = "/Sessions";


    @Override
    public void onCreate() {
        super.onCreate();
        LOG.debug("VDTSApplication onCreate started");

        vdtsApplication = (VDTSApplication) this.getApplicationContext();

        vdtsFeedbackUtil = new VDTSFeedbackUtil(vdtsApplication);
        ttsEngine = vdtsFeedbackUtil.getTTSEngine();

        LOG.debug("VDTSApplication onCreate finished");
    }

    public VDTSApplication getApplicationInstance(Context context) {
        return (VDTSApplication) context.getApplicationContext();
    }

    public TextToSpeech getTTSEngine() {
        return this.ttsEngine;
    }

    //todo - can probably be simplified
    public VDTSUser getCurrentUser() {
        if (currentVDTSUser != null) {
            return currentVDTSUser;
        } else if (getDatabase() == null) {
            LOG.debug("User not found and database was not assigned");
            this.setCurrentUser(VDTSUser.VDTS_USER_NONE);
            return VDTSUser.VDTS_USER_NONE;
        } else {
            LOG.debug("User not found, finding user from database");
            AtomicReference<VDTSUser> foundUser = new AtomicReference<>();

            Thread thread = new Thread(() -> foundUser.set(getDatabase()
                    .getInterfaceInstance(this)
                    .vdtsUserDao()
                    .findUserByID(
                            this.getPreferences().getLong(
                                    "CURRENT_USER", VDTSUser.VDTS_USER_NONE.getUid()))));
            thread.start();

            try {
                thread.join();
            } catch (InterruptedException e) {
                LOG.error("Get VDTSUser interrupted: ", e);
            }

            if (foundUser.get() != null) {
                setCurrentUser(foundUser.get());
                return foundUser.get();
            } else {
                setCurrentUser(VDTSUser.VDTS_USER_NONE);
                return VDTSUser.VDTS_USER_NONE;
            }
        }
    }

    public void setCurrentUser(VDTSUser newVDTSUser) {
        LOG.debug("Setting user to {}", newVDTSUser != null ? newVDTSUser.getName() : "null");
        if (newVDTSUser != null) {
            currentVDTSUser = newVDTSUser;
        } else {
            currentVDTSUser = VDTSUser.VDTS_USER_NONE;
        }

        this.getPreferences().setLong("CURRENT_USER", currentVDTSUser.getUid());
        LOG.debug("VDTSUser set");
    }

    public int getUserCount() { return userCount; }

    public void setUserCount(int userCount) {
        vdtsApplication.userCount = userCount;
        LOG.info("userCount set to {}", userCount);
    }

    public VDTSDatabase getDatabase() {
        return vdtsDatabase;
    }

    public void setDatabase (VDTSDatabase VDTSDatabase) {
        vdtsApplication.vdtsDatabase = VDTSDatabase;
    }

    public VDTSPrefKeyValue getPreferences() {
        if (vdtsPrefRepository == null || preferences == null) {
            vdtsPrefRepository = VDTSPrefRepository.getInstance(getApplicationInstance(this));
            preferences = new VDTSPrefKeyValue(vdtsPrefRepository);
        }

        return preferences;
    }

    @WorkerThread
    public void displayToast(Context context, String message, int length) {
        ContextCompat.getMainExecutor(context).execute(() -> {
            Toast.makeText(context, message, length).show();
        });
    }
}
