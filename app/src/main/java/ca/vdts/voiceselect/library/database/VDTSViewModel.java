package ca.vdts.voiceselect.library.database;

import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.VSDatabase;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.database.repositories.VDTSUserRepository;

/**
 * Base view model for VDTS applications.
 */
public class VDTSViewModel extends AndroidViewModel {
    private final VDTSUserRepository userRepository;

    public VDTSViewModel(@NonNull Application application) {
        super(application);
        VSDatabase vsDatabase = VSDatabase.getInstance(getApplication());

        userRepository = new VDTSUserRepository(vsDatabase.userDAO());
    }

    //VDTSUser
    public long insertUser(VDTSUser VDTSUser) {
        return userRepository.insert(VDTSUser);
    }

    public void insertAllUsers(VDTSUser[] VDTSUserEntities) {
        userRepository.insertAll(VDTSUserEntities);
    }

    public void updateUser(VDTSUser VDTSUser) {
        userRepository.update(VDTSUser);
    }

    public void updateAllUsers(VDTSUser[] VDTSUserEntities) {
        userRepository.updateAll(VDTSUserEntities);
    }

    public void deleteUser(VDTSUser VDTSUser) {
        userRepository.delete(VDTSUser);
    }

    public void deleteAllUsers(VDTSUser[] VDTSUserEntities) {
        userRepository.deleteAll(VDTSUserEntities);
    }

    public VDTSUser findUserByID(long uid) {
        return userRepository.findUserByID(uid);
    }

    public VDTSUser findUserByName(String name) { return userRepository.findUserByName(name); }

    public VDTSUser findPrimaryUser() {
        List<VDTSUser> users =  userRepository.findAll("SELECT * FROM Users WHERE primary = " + true + "");
        if (users.size()>0){
            return users.get(0);
        } else {
            return VDTSUser.VDTS_USER_NONE;
        }
    }

    public List<VDTSUser> findAllUsers() {
        return userRepository.findAll("SELECT * FROM Users");
    }

    public List<VDTSUser> findAllActiveUsers() {
        return userRepository.findAllActiveUsers();
    }

    public LiveData<List<VDTSUser>> findAllActiveUsersLive() {
        return userRepository.findAllActiveUsersLive();
    }

    public List<VDTSUser> findAllActiveUsersExcludeDefault() {
        return userRepository.findAll(
                "SELECT * FROM Users " +
                        "WHERE active = 1 " +
                        "AND uid <> " + DEFAULT_UID
        );
    }
}