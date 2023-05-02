package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for ColumnValueSpoken entity.
 */
@Dao
public interface ColumnValueSpokenDAO extends VDTSBaseDAO<ColumnValueSpoken> {
    @Query("SELECT * FROM ColumnValueSpokens")
    LiveData<List<ColumnValueSpoken>> findAllColumnValueSpokensLive();
}
