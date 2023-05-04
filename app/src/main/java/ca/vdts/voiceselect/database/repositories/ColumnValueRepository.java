package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.ColumnDAO;
import ca.vdts.voiceselect.database.daos.ColumnValueDAO;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for ColumnValue entity.
 */
public class ColumnValueRepository extends VDTSBaseRepository<ColumnValue> {
    public ColumnValueRepository(VDTSBaseDAO<ColumnValue> dao) {
        super(dao);
    }

    public LiveData<List<ColumnValue>> findAllColumnValuesLive() {
        return ((ColumnValueDAO) dao).findAllColumnValuesLive();
    }
}
