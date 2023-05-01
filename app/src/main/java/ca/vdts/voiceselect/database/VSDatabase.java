package ca.vdts.voiceselect.database;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.VSApplication;
import ca.vdts.voiceselect.database.daos.ColumnDAO;
import ca.vdts.voiceselect.database.daos.ColumnSpokenDAO;
import ca.vdts.voiceselect.database.daos.ColumnValueDAO;
import ca.vdts.voiceselect.database.daos.ColumnValueSpokenDAO;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.database.converters.VDTSConverters;
import ca.vdts.voiceselect.library.database.converters.VDTSDateConverter;
import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

@Database(
        entities = {
                VDTSUser.class,
                Column.class,
                ColumnSpoken.class,
                ColumnValue.class,
                ColumnValueSpoken.class
        },
        version = 1)
@TypeConverters(
        {
                VDTSConverters.class,
                VDTSDateConverter.class
        }
)

public abstract class VSDatabase extends RoomDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(VSDatabase.class);

    private static final String DB_NAME = "voice_select";
    private static VSDatabase dbInstance = null;

    //DAOs
    public abstract VDTSUserDAO userDAO();
    public abstract ColumnDAO columnDAO();
    public abstract ColumnSpokenDAO columnSpokenDAO();
    public abstract ColumnValueDAO columnValueDAO();
    public abstract ColumnValueSpokenDAO columnValueSpokenDAO();


    public static synchronized VSDatabase getInstance(VSApplication vsApplication) {
        LOG.info("Getting database instance");
        if (dbInstance == null) {
            synchronized (VSDatabase.class) {
                dbInstance = Room.databaseBuilder(
                                vsApplication,
                                VSDatabase.class,
                                DB_NAME
                        )//.addMigrations()
                        .addCallback(dbPopulateCallback)
                        .build();
            }
        }
        return dbInstance;
    }

    private enum PopulateOption {
        DB_CREATE,
        DB_OPEN
    }

    private static final Callback dbPopulateCallback = new Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
            populateDB(PopulateOption.DB_CREATE);
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.query(new SimpleSQLiteQuery("pragma wal_checkpoint(full)"));
            populateDB(PopulateOption.DB_OPEN);
        }
    };

    private static void populateDB(PopulateOption option) {
        LOG.info("Populating database");

        ExecutorService executor = Executors.newSingleThreadExecutor();

        final VDTSUserDAO VDTSUserDao = dbInstance.userDAO();
        final ColumnDAO columnDAO = dbInstance.columnDAO();
        final ColumnSpokenDAO columnSpokenDAO = dbInstance.columnSpokenDAO();
        final ColumnValueDAO columnValueDAO = dbInstance.columnValueDAO();
        final ColumnValueSpokenDAO columnValueSpokenDAO = dbInstance.columnValueSpokenDAO();

        executor.execute(() -> {
            //Users
            int userCount = VDTSUserDao.findAll(new SimpleSQLiteQuery(
                    "SELECT * FROM Users")).size();
            if (option == PopulateOption.DB_CREATE || userCount == 0) {
                VDTSUserDao.insert(
                        VDTSUser.VDTS_USER_NONE);
            }

            //Columns
            int columnCount = columnDAO.findAll(new SimpleSQLiteQuery(
                    "select * from Columns")).size();
            if (option == PopulateOption.DB_CREATE || columnCount == 0) {
                columnDAO.insert(Column.COLUMN_NONE);
            }

            //ColumnValues
            int columnValueCount = columnValueDAO.findAll(
                    new SimpleSQLiteQuery("select * from ColumnValues")).size();
            if (option == PopulateOption.DB_CREATE || columnValueCount == 0) {
                columnValueDAO.insert(ColumnValue.COLUMN_VALUE_NONE);
            }
        });
    }

    public static void dbCheckpoint(VSDatabase vsDatabase) {
        String checkpoint = "pragma wal_checkpoint(full)";
        vsDatabase.userDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnValueDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnValueSpokenDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
    }

    public static String getDbName() {
        return DB_NAME;
    }
}