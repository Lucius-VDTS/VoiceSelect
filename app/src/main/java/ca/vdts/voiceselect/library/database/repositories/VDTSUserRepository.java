package ca.vdts.voiceselect.library.database.repositories;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.daos.VDTSUserDAO;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Repository for VDTSUser entity.
 */
public class VDTSUserRepository extends VDTSBaseRepository<VDTSUser> {
    public VDTSUserRepository(VDTSBaseDAO<VDTSUser> dao) {
        super(dao);
    }

    public VDTSUser findUserByID(long uid) {
        return ((VDTSUserDAO)this.dao).findUserByID(uid);
    }

    public VDTSUser findUserByName(String name) {
        return ((VDTSUserDAO)this.dao).findUserByName(name);
    }

    public VDTSUser findUserByExportCode(String exportCode) {
        return ((VDTSUserDAO)this.dao).findUserByCode(exportCode);
    }

    public List<VDTSUser> findAllActiveUsers() {
        return ((VDTSUserDAO)this.dao).findAllActiveUsers();
    }

    public LiveData<List<VDTSUser>> findAllActiveUsersLive() {
        return ((VDTSUserDAO)this.dao).findAllActiveUsersLive();
    }
}
