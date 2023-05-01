package ca.vdts.voiceselect.library.database;

import android.app.Application;

import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSUserRepository;

public interface VDTSDatabase {
    abstract VDTSUserDAO vdtsUserDao();
    abstract VDTSUserRepository vdtsUserRepository();

    VDTSDatabase getInterfaceInstance(Application appContext);
}