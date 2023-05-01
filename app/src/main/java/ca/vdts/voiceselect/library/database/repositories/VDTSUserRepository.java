package ca.vdts.voiceselect.library.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Repository for User entity
 */
public class VDTSUserRepository extends VDTSBaseRepository<VDTSUser> {
    public VDTSUserRepository(VDTSBaseDAO<VDTSUser> dao) {
        super(dao);
    }

    public VDTSUser findUserById(long uid) {
        return ((VDTSUserDAO)this.dao).findUserById(uid);
    }

    public VDTSUser findUserByCode(String code) {
        return ((VDTSUserDAO)this.dao).findUserByCode(code);
    }

    public List<VDTSUser> findAllActiveUsers() {
        return ((VDTSUserDAO)this.dao).findAllActiveUsers();
    }

    public LiveData<List<VDTSUser>> findAllActiveUsersLive() {
        return ((VDTSUserDAO)this.dao).findAllActiveUsersLive();
    }
}
