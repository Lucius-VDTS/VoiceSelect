package ca.vdts.voiceselect.library.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * DAO for User entity.
 */
@Dao
public interface VDTSUserDAO extends VDTSBaseDAO<VDTSUser> {
    @Query("SELECT * FROM Users WHERE uid = :uid")
    VDTSUser findUserByID(long uid);

    @Query("SELECT * FROM Users WHERE name = :name")
    VDTSUser findUserByName(String name);

    @Query("SELECT * FROM Users WHERE exportCode = :exportCode")
    VDTSUser findUserByCode(String exportCode);

    @Query("SELECT * FROM Users WHERE active = 1")
    List<VDTSUser> findAllActiveUsers();

    @Query("SELECT * FROM Users WHERE active = 1")
    LiveData<List<VDTSUser>> findAllActiveUsersLive();
}
