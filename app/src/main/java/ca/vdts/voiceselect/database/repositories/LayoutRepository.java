package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.daos.LayoutDAO;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

public class LayoutRepository extends VDTSBaseRepository<Layout> {
    public LayoutRepository(LayoutDAO dao) {
        super(dao);
    }

    public Layout findLayoutByName(String name) {
        return ((LayoutDAO) dao).findLayoutByName(name);
    }

    public LiveData<List<Layout>> findAllLayoutsLive() {
        return ((LayoutDAO) dao).findAllLayoutsLive();
    }

    public LiveData<List<Layout>> findAllActiveLayoutsLive() {
        return ((LayoutDAO) dao).findAllActiveLayoutsLive();
    }
}
