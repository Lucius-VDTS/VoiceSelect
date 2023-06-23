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
}
