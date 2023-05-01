package ca.vdts.voiceselect.library;

import android.app.Application;
import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.library.database.VDTSDatabase;
import ca.vdts.voiceselect.library.database.VDTSPref;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.database.repositories.VDTSPrefRepository;
import ca.vdts.voiceselect.library.services.VDTSFeedbackService;

public class VDTSApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(VSApplication.class);

    public static final String SHARED_PREFERENCES = "SHARED_PREFERENCES";
    public static final long DEFAULT_UID = -9001L;
    public static final LocalDateTime DEFAULT_DATE = LocalDateTime.now();

    //VDTSApplication
    private VDTSApplication vdtsApplication;

    //VDTSDatabase
    private VDTSDatabase vdtsDatabase;

    //VDTSPreferences
    private VDTSPref preferences;
    private VDTSPrefRepository vdtsPrefRepository;

    //VDTSUser
    private VDTSUser currentVDTSUser;
    private int userCount = 0;

    //VDTSFeedback
    private VDTSFeedbackService vdtsFeedbackService;
    private TextToSpeech ttsEngine;

    @Override
    public void onCreate() {
        super.onCreate();
        LOG.debug("VDTSApplication onCreate started");

        vdtsApplication = (VDTSApplication) this.getApplicationContext();

        vdtsFeedbackService = new VDTSFeedbackService(vdtsApplication);
        ttsEngine = vdtsFeedbackService.getTTSEngine();

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
                    .findUserById(
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

    public VDTSPref getPreferences() {
        if (vdtsPrefRepository == null || preferences == null) {
            vdtsPrefRepository = VDTSPrefRepository.getInstance(getApplicationInstance(this));
            preferences = new VDTSPref(vdtsPrefRepository);
        }

        return preferences;
    }

    public void displayToast(Context context, String message, int length) {
        Toast.makeText(context, message, length).show();
    }
}