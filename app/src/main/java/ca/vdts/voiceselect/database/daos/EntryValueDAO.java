package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for EntryValue entity
 */
@Dao
public interface EntryValueDAO extends VDTSBaseDAO<EntryValue> {
    @RawQuery(observedEntities = EntryValue.class)
    LiveData<List<EntryValue>> findAllEntryValuesLive(SupportSQLiteQuery query);
}
