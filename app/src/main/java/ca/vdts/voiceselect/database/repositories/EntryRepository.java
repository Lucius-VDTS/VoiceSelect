package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.daos.EntryDAO;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for Entry entity.
 */
public class EntryRepository extends VDTSBaseRepository<Entry> {
    public EntryRepository(VDTSBaseDAO<Entry> dao) {
        super(dao);
    }

    public LiveData<List<Entry>> findAllEntriesLive(String query) {
        return ((EntryDAO) dao).findAllEntriesLive(new SimpleSQLiteQuery(query));
    }
}
