package ca.vdts.voiceselect.database.repositories;

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
}
