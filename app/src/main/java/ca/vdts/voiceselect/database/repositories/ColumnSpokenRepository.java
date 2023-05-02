package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.ColumnSpokenDAO;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for ColumnSpoken entity.
 */
public class ColumnSpokenRepository extends VDTSBaseRepository<ColumnSpoken> {
    public ColumnSpokenRepository(VDTSBaseDAO<ColumnSpoken> dao) {
        super(dao);
    }

    public LiveData<List<ColumnSpoken>> findAllColumnSpokensLive() {
        return ((ColumnSpokenDAO) dao).findAllColumnSpokensLive();
    }
}
