package ca.vdts.voiceselect.database;

import static ca.vdts.voiceselect.VSApplication.SHARED_PREFERENCES;
import static ca.vdts.voiceselect.database.VSDatabase.dbCheckpoint;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.library.database.VDTSBackupWorker;
import ca.vdts.voiceselect.library.database.VDTSPrefDatabase;
import ca.vdts.voiceselect.library.utilities.VDTSToolUtil;

/**
 * Periodic database backup worker.
 */
public class VSBackupWorker extends VDTSBackupWorker {
    Context context = getApplicationContext();
    VSApplication vsApplication = (VSApplication) getApplicationContext();

    public VSBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public ListenableWorker.Result doWork() {
        String appName = "VoiceSelect";
        backupDB(vsApplication, VSDatabase.getDbName(), appName);
        backupDB(vsApplication, VDTSPrefDatabase.getDbName(), appName);
        return ListenableWorker.Result.success();
    }

    @Override
    protected void backupDB(final Application application, String dbName, String appName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(
                SHARED_PREFERENCES,
                Context.MODE_PRIVATE);
        long lastBackup = sharedPreferences.getLong("LAST_BACKUP", 1L);

        if (VDTSToolUtil.getTimeStamp().getTime() > lastBackup + 86400000) {
            dbCheckpoint(VSDatabase.getInstance(vsApplication));
            super.backupDB(vsApplication, dbName, appName);
        }
    }
}