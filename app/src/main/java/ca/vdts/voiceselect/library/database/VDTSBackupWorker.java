package ca.vdts.voiceselect.library.database;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Locale;

import ca.vdts.voiceselect.library.services.VDTSUtilService;

/**
 * VDTSDatabase backup service utilized by the application defined backup worker
 */
public class VDTSBackupWorker extends Worker {
    public static Logger LOG = LoggerFactory.getLogger(VDTSBackupWorker.class);

    private final Context context = getApplicationContext();

    SharedPreferences backupSharedPreferences = context.getSharedPreferences(
            "SHARED_PREFERENCES",
            Context.MODE_PRIVATE);

    long lastBackup = backupSharedPreferences.getLong("LAST_BACKUP", 1L);

    public VDTSBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        return Result.success();
    }

    protected void backupDB (Application application, String dbName, String appName) {
        String dbPath = Environment.getDataDirectory().getPath() +
                "/data/" + application.getPackageName() + "/databases/" + dbName;

        String backupDir = Environment.getExternalStorageDirectory().toString() +
                "/Documents/" + appName + "/Backup";

        String backupName = dbName + "_" + String.format(
                Locale.getDefault(),
                "%1$tY-%1$tm-%1$td_%1$tH-%1$tM-%1$tS",
                VDTSUtilService.getTimeStamp()
        ).concat(".db");

        if (VDTSUtilService.getTimeStamp().getTime() > lastBackup + 86400000L) {
            File db = new File(dbPath);

            File dbBackupDir = new File(backupDir);
            if (!dbBackupDir.exists()) {
                boolean mkDirResult = dbBackupDir.mkdirs();
                LOG.info("Created backup directory: {}", mkDirResult);
                if (!mkDirResult) {
                    LOG.info("Failed to create directory: {}", dbBackupDir);
                }
            }

            File dbBackupFile = new File(dbBackupDir, backupName);

            //todo - delete existing backups - keep last two
            if (dbBackupFile.exists()) {
                boolean deleteResult = dbBackupFile.delete();
                if (deleteResult) {
                    LOG.info("Deleted existing backup(s): {}", dbBackupFile);
                } else {
                    LOG.info("Failed to delete existing backup: {}", dbBackupFile);
                }
            }

            //Create placeholder file for backup
            if (!dbBackupFile.exists()) {
                FileOutputStream fos;
                String content = "db backup placeholder";
                try {
                    fos = new FileOutputStream(dbBackupFile);
                    fos.write(content.getBytes());
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                int bufferSize = 8192;
                byte[] buffer = new byte[bufferSize];
                int bytesRead;

                InputStream dbInput = Files.newInputStream(db.toPath());
                OutputStream dbOutput = Files.newOutputStream(dbBackupFile.toPath());

                while ((bytesRead = dbInput.read(buffer, 0, bufferSize)) > 0) {
                    dbOutput.write(buffer, 0, bytesRead);
                }

                dbOutput.flush();
                dbOutput.close();
                dbInput.close();

                backupSharedPreferences.edit().putLong(
                        "LAST_BACKUP",
                        VDTSUtilService.getTimeStamp().getTime()).apply();

                LOG.info("Backup saved to {}", dbBackupDir);
            } catch (IOException e) {
                e.printStackTrace();
                LOG.error("Backup error: ", e);
            }
        }
    }
}
