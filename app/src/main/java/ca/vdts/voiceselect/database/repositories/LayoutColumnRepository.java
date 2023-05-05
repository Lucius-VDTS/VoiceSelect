package ca.vdts.voiceselect.database.repositories;

import java.util.List;

import ca.vdts.voiceselect.database.VSLayoutColumn;
import ca.vdts.voiceselect.database.daos.LayoutColumnDAO;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

public class LayoutColumnRepository extends VDTSBaseRepository<LayoutColumn> {
    public LayoutColumnRepository(VDTSBaseDAO<LayoutColumn> dao) {
        super(dao);
    }

    public List<VSLayoutColumn> findAllLayoutColumns() {
        return ((LayoutColumnDAO) dao).findAllLayoutColumns();
    }
}