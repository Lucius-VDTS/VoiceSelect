package ca.vdts.voiceselect.database.repositories;

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
}
