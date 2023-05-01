package ca.vdts.voiceselect.library.database.migrations;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VDTSPrefMigration {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSPrefMigration.class);

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            LOG.info("Migrating from 1 to 2");

        }
    };
}