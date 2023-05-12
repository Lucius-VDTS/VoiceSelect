package ca.vdts.voiceselect.library.database.repositories;

import android.app.Application;
import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.library.database.VDTSPrefDatabase;
import ca.vdts.voiceselect.library.database.entities.VDTSPref;
import ca.vdts.voiceselect.library.utilities.VDTSTaskUtil;

/**
 * Repository for VDTSPref entity.
 */
public class VDTSPrefRepository {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSPrefRepository.class);
    private static volatile VDTSPrefRepository instance = null;
    private final ca.vdts.voiceselect.library.database.daos.VDTSPrefDAO VDTSPrefDAO;

    private VDTSPrefRepository(Application application) {
        VDTSPrefDAO = VDTSPrefDatabase.getInstance(application).prefDAO();
    }

    public static VDTSPrefRepository getInstance(final Application application) {
        if (instance == null) {
            synchronized (VDTSPrefRepository.class) {
                if (instance == null) {
                    instance = new VDTSPrefRepository(application);
                }
            }
        }
        return instance;
    }

    public void insert(final VDTSPref pref) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            VDTSTaskUtil.performAsynchronousAction(() -> VDTSPrefDAO.insert(pref));
        } else {
            VDTSPrefDAO.insert(pref);
        }
    }

    public void update(final VDTSPref pref) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            VDTSTaskUtil.performAsynchronousAction(() -> VDTSPrefDAO.update(pref));
        } else {
            VDTSPrefDAO.update(pref);
        }
    }

    public void delete(String key) {
        final VDTSPref pref = find(key);
        if (pref != null) {
            if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
                VDTSTaskUtil.performAsynchronousAction(() -> VDTSPrefDAO.delete(pref));
            } else {
                VDTSPrefDAO.delete(pref);
            }
        }
    }

    public VDTSPref find(String key) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            final VDTSPref[] VDTSPrefEntities = new VDTSPref[1];
            final Thread fetchThread = new Thread(() -> VDTSPrefEntities[0] = VDTSPrefDAO.find(key));
            fetchThread.start();

            try {
                fetchThread.join();
            } catch (InterruptedException e) {
                LOG.error("Thread Interrupted", e);
            }

            return VDTSPrefEntities[0];
        } else {
            return VDTSPrefDAO.find(key);
        }
    }

    public List<VDTSPref> findAll() {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            final List<VDTSPref> prefs = new ArrayList<>();
            final Thread fetchThread = new Thread(() -> prefs.addAll(VDTSPrefDAO.findAll()));
            fetchThread.start();

            try {
                fetchThread.join();
            } catch (InterruptedException e) {
                LOG.error("Thread Interrupted: ", e);
            }

            return prefs;
        } else {
            return VDTSPrefDAO.findAll();
        }
    }
}
