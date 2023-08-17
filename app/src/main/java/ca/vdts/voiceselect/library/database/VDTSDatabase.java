package ca.vdts.voiceselect.library.database;

import android.app.Application;

import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;

/**
 * Base database for VDTS applications.
 */
public interface VDTSDatabase {
    VDTSUserDAO vdtsUserDao();

    VDTSDatabase getInterfaceInstance(Application appContext);
}