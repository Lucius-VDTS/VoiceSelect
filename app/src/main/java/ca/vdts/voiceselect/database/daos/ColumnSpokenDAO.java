package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for ColumnSpoken entity.
 */
@Dao
public interface ColumnSpokenDAO extends VDTSBaseDAO<ColumnSpoken> {
    @Query("SELECT * FROM ColumnSpokens")
    LiveData<List<ColumnSpoken>> findAllColumnSpokensLive();
}
