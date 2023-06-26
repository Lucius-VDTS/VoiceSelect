package ca.vdts.voiceselect.database;

import static ca.vdts.voiceselect.database.VSMigration.MIGRATION_1_2;
import static ca.vdts.voiceselect.database.VSMigration.MIGRATION_2_3;
import static ca.vdts.voiceselect.database.VSMigration.MIGRATION_3_4;

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
import ca.vdts.voiceselect.database.daos.EntryDAO;
import ca.vdts.voiceselect.database.daos.EntryValueDAO;
import ca.vdts.voiceselect.database.daos.LayoutColumnDAO;
import ca.vdts.voiceselect.database.daos.LayoutDAO;
import ca.vdts.voiceselect.database.daos.PictureReferenceDAO;
import ca.vdts.voiceselect.database.daos.SessionDAO;
import ca.vdts.voiceselect.database.daos.SessionLayoutDAO;
import ca.vdts.voiceselect.database.daos.VideoReferenceDAO;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.database.entities.VideoReference;
import ca.vdts.voiceselect.library.database.converters.VDTSConverter;
import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Database for Voice Select.
 */
@Database(
        entities = {
                VDTSUser.class,
                Column.class,
                ColumnSpoken.class,
                ColumnValue.class,
                ColumnValueSpoken.class,
                Layout.class,
                LayoutColumn.class,
                Session.class,
                SessionLayout.class,
                Entry.class,
                EntryValue.class,
                PictureReference.class,
                VideoReference.class
        },
        version = 4
)
@TypeConverters(
        {
                VDTSConverter.class,
        }
)
public abstract class VSDatabase extends RoomDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(VSDatabase.class);

    private static final String DB_NAME = "voiceSelect";
    private static VSDatabase dbInstance = null;

    //DAOs
    public abstract VDTSUserDAO userDAO();
    public abstract ColumnDAO columnDAO();
    public abstract ColumnSpokenDAO columnSpokenDAO();
    public abstract ColumnValueDAO columnValueDAO();
    public abstract ColumnValueSpokenDAO columnValueSpokenDAO();
    public abstract LayoutDAO layoutDAO();
    public abstract LayoutColumnDAO layoutColumnDAO();
    public abstract SessionDAO sessionDAO();
    public abstract SessionLayoutDAO sessionLayoutDAO();
    public abstract EntryDAO entryDAO();
    public abstract EntryValueDAO entryValueDAO();
    public abstract PictureReferenceDAO pictureReferenceDAO();
    public abstract VideoReferenceDAO videoReferenceDAO();


    public static synchronized VSDatabase getInstance(VSApplication vsApplication) {
        LOG.info("Getting database instance");
        if (dbInstance == null) {
            synchronized (VSDatabase.class) {
                dbInstance = Room.databaseBuilder(
                                vsApplication,
                                VSDatabase.class,
                                DB_NAME
                        ).addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4)
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
        final ColumnValueDAO columnValueDAO = dbInstance.columnValueDAO();
        final LayoutDAO layoutDAO = dbInstance.layoutDAO();
        final SessionDAO sessionDAO = dbInstance.sessionDAO();
        final SessionLayoutDAO sessionLayoutDAO = dbInstance.sessionLayoutDAO();
        final EntryValueDAO entryValueDAO = dbInstance.entryValueDAO();

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
                    "SELECT * FROM Columns")).size();
            if (option == PopulateOption.DB_CREATE || columnCount == 0) {
                columnDAO.insert(Column.COLUMN_NONE);
            }

            //ColumnValues
            int columnValueCount = columnValueDAO.findAll(
                    new SimpleSQLiteQuery("SELECT * FROM ColumnValues")).size();
            if (option == PopulateOption.DB_CREATE || columnValueCount == 0) {
                columnValueDAO.insert(ColumnValue.COLUMN_VALUE_NONE);
            }

            //todo - cleanup block - potentially apply to all blocks
            //Layouts
            int layoutCount = layoutDAO.findAll(
                    new SimpleSQLiteQuery("SELECT * FROM Layouts")).size();
            layoutDAO.insert(Layout.LAYOUT_NONE);
//            if (option == PopulateOption.DB_CREATE || layoutCount == 0) {
//                layoutDAO.insert(Layout.LAYOUT_NONE);
//            }

            //Session
            int sessionCount = sessionDAO.findAll(
                    new SimpleSQLiteQuery("SELECT * FROM Sessions")).size();
            if (option == PopulateOption.DB_CREATE || sessionCount == 0) {
                sessionDAO.insert(Session.SESSION_NONE);
            }

            //SessionLayout
            int sessionLayoutCount = sessionLayoutDAO.findAll(
                    new SimpleSQLiteQuery("SELECT * FROM SessionLayouts")).size();
            if (option == PopulateOption.DB_CREATE || sessionLayoutCount == 0) {
                sessionLayoutDAO.insert(SessionLayout.SESSION_LAYOUT_NONE);
            }
        });
    }

    public static void dbCheckpoint(VSDatabase vsDatabase) {
        String checkpoint = "pragma wal_checkpoint(full)";
        vsDatabase.userDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnValueDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.columnValueSpokenDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.layoutDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.layoutColumnDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.sessionDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.sessionLayoutDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.entryDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
        vsDatabase.entryValueDAO().checkpoint(new SimpleSQLiteQuery(checkpoint));
    }

    public static String getDbName() {
        return DB_NAME;
    }
}