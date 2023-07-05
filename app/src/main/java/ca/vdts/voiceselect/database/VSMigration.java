package ca.vdts.voiceselect.database;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for Voice Select database migration
 */
public class VSMigration {
    private static final Logger LOG = LoggerFactory.getLogger(VSMigration.class);

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LOG.debug("Migrating from 1 to 2");

            database.execSQL("ALTER TABLE 'Layouts' ADD 'commentRequired' INTERGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE 'Layouts' ADD 'pictureRequired' INTERGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LOG.debug("Migrating from 2 to 3");

            database.execSQL("ALTER TABLE 'Users' ADD 'abbreviate' INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LOG.debug("Migrating database from version 3 to 4");
            database.execSQL(
                    "CREATE TABLE PictureReferences " +
                            "(uid INTEGER NOT NULL, userID INTEGER NOT NULL, timeStamp TEXT," +
                            "entryID INTEGER NOT NULL, path TEXT, latitude REAL, longitude REAL, " +
                            "PRIMARY KEY(uid), " +
                            "FOREIGN KEY(userID) REFERENCES Users(uid) ON UPDATE CASCADE ON DELETE CASCADE, " +
                            "FOREIGN KEY(entryID) REFERENCES Entries(uid) ON UPDATE CASCADE ON DELETE CASCADE)"
            );

            database.execSQL("CREATE INDEX index_PictureReferences_userID on PictureReferences(userID)");
            database.execSQL("CREATE INDEX index_PictureReferences_entryID ON PictureReferences(entryID)");


            database.execSQL(
                    "CREATE TABLE VideoReferences " +
                            "(uid INTEGER NOT NULL, userID INTEGER NOT NULL, timeStamp TEXT," +
                            "sessionID INTEGER NOT NULL, path TEXT, " +
                            "PRIMARY KEY(uid), " +
                            "FOREIGN KEY(userID) REFERENCES Users(uid) ON UPDATE CASCADE ON DELETE CASCADE, " +
                            "FOREIGN KEY(sessionID) REFERENCES Sessions(uid) ON UPDATE CASCADE ON DELETE CASCADE)"
            );

            database.execSQL("CREATE INDEX index_VideoReferences_userID on VideoReferences(userID)");
            database.execSQL("CREATE INDEX index_VideoReferences_sessionID ON VideoReferences(sessionID)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LOG.debug("Migrating database from version 4 to 5");

            database.execSQL("ALTER TABLE 'Entries' ADD 'comment' TEXT");
        }
    };
}
