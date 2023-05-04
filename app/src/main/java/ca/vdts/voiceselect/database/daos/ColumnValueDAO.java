package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for ColumnValue entity.
 */
@Dao
public interface ColumnValueDAO extends VDTSBaseDAO<ColumnValue> {
    @Query("SELECT * FROM ColumnValues WHERE active = 1")
    LiveData<List<ColumnValue>> findAllColumnValuesLive();
}
