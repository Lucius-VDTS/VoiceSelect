package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.ColumnValueSpokenDAO;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for ColumnValueSpoken entity.
 */
public class ColumnValueSpokenRepository extends VDTSBaseRepository<ColumnValueSpoken> {
    public ColumnValueSpokenRepository(VDTSBaseDAO<ColumnValueSpoken> dao) {
        super(dao);
    }

    public LiveData<List<ColumnValueSpoken>> findAllColumnValueSpokensLive() {
        return ((ColumnValueSpokenDAO) dao).findAllColumnValueSpokensLive();
    }
}
