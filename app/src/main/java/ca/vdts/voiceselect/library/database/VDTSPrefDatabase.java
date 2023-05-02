package ca.vdts.voiceselect.library.database;

import android.app.Application;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SimpleSQLiteQuery;

import ca.vdts.voiceselect.library.database.converters.VDTSConverter;
import ca.vdts.voiceselect.library.database.daos.VDTSPrefDAO;
import ca.vdts.voiceselect.library.database.entities.VDTSPref;

/**
 * Database for VDTS preferences.
 */
@Database(
        entities = {
                VDTSPref.class,
        },
        version = 1,
        exportSchema = false
)
@TypeConverters({
        VDTSConverter.class
})
public abstract class VDTSPrefDatabase extends RoomDatabase {
        private static final String DB_NAME = "vdts_preferences";
        private static VDTSPrefDatabase dbInstance = null;

        Application application;

        public abstract VDTSPrefDAO prefDAO();

        public static VDTSPrefDatabase getInstance(final Application application) {
                if (dbInstance == null) {
                        synchronized (VDTSPrefDatabase.class) {
                                dbInstance = Room.databaseBuilder(
                                                application,
                                                VDTSPrefDatabase.class,
                                                DB_NAME
                                        )//.addMigrations()
                                        .build();

                                new Thread(() ->
                                        dbInstance.query(new SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
                                ).start();

                                dbInstance.application = application;
                        }
                }
                return dbInstance;
        }

        public Application getApplication() {
                return application;
        }

        public static String getDbName() {
                return DB_NAME;
        }
}

