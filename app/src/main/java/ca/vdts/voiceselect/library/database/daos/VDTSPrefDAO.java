package ca.vdts.voiceselect.library.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ca.vdts.voiceselect.library.database.entities.VDTSPref;

/**
 * Dao for preference entity.
 */
@Dao
public interface VDTSPrefDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(VDTSPref VDTSPref);

    @Update
    void update(VDTSPref VDTSPref);

    @Delete
    void delete(VDTSPref VDTSPref);

    @Query("SELECT * FROM Preferences WHERE `key` = :key")
    VDTSPref find(String key);

    @Query("SELECT * FROM Preferences")
    List<VDTSPref> findAll();
}
