package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.LayoutColumnDAO;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for LayoutColumn entity.
 */
public class LayoutColumnRepository extends VDTSBaseRepository<LayoutColumn> {
    public LayoutColumnRepository(VDTSBaseDAO<LayoutColumn> dao) {
        super(dao);
    }

    public List<LayoutColumn> findAllLayoutColumns() {
        return ((LayoutColumnDAO) dao).findAllLayoutColumns();
    }

    public LiveData<List<LayoutColumn>> findAllLayoutColumnsLive() {
        return ((LayoutColumnDAO) dao).findAllLayoutColumnsLive();
    }

    public LiveData<List<LayoutColumn>> findAllLayoutColumnsByLayoutLive(long layoutID) {
        return ((LayoutColumnDAO) dao).findAllLayoutColumnsByLayoutLive(layoutID);
    }
}