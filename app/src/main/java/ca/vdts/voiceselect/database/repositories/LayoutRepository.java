package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.daos.LayoutDAO;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

public class LayoutRepository extends VDTSBaseRepository<Layout> {
    public LayoutRepository(LayoutDAO dao) {
        super(dao);
    }

    public LiveData<List<Layout>> findAllActiveLayoutsLive() {
        return ((LayoutDAO) dao).findAllActiveLayoutsLive();
    }
}
