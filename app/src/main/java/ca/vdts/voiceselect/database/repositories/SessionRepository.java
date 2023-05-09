package ca.vdts.voiceselect.database.repositories;

import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;

/**
 * Repository for Session entity.
 */
public class SessionRepository extends VDTSBaseRepository<Session> {
    public SessionRepository(VDTSBaseDAO<Session> dao) {
        super(dao);
    }
}
