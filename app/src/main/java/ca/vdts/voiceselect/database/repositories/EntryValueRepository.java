package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.daos.EntryDAO;
import ca.vdts.voiceselect.database.daos.EntryValueDAO;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for EntryValue entity.
 */
public class EntryValueRepository extends VDTSBaseRepository<EntryValue> {
    public EntryValueRepository(VDTSBaseDAO<EntryValue> dao) {
        super(dao);
    }

    public LiveData<List<EntryValue>> findAllEntryValuesLive(String query) {
        return ((EntryValueDAO) dao).findAllEntryValuesLive(new SimpleSQLiteQuery(query));
    }
}
