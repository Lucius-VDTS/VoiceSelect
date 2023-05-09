package ca.vdts.voiceselect.database.repositories;

import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for SessionLayout entity.
 */
public class SessionLayoutRepository extends VDTSBaseRepository<SessionLayout> {
    public SessionLayoutRepository(VDTSBaseDAO<SessionLayout> dao) {
        super(dao);
    }
}
