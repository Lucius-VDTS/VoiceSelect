package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.ColumnDAO;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for Column entity
 */
public class ColumnRepository extends VDTSBaseRepository<Column> {
    public ColumnRepository(VDTSBaseDAO<Column> dao) {
        super(dao);
    }

    public LiveData<List<Column>> findAllColumnsLive() {
        return ((ColumnDAO) dao).findAllColumnsLive();
    }
}
