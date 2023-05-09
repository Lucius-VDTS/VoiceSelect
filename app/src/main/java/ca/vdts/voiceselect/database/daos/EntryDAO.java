package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for Entry entity.
 */
@Dao
public interface EntryDAO extends VDTSBaseDAO<Entry> {
    @RawQuery(observedEntities = Entry.class)
    LiveData<List<Entry>> findAllEntriesLive(SupportSQLiteQuery query);
}
