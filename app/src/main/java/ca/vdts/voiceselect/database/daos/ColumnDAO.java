package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for Column entity.
 */
@Dao
public interface ColumnDAO extends VDTSBaseDAO<Column> {
    @Query("SELECT * FROM Columns")
    LiveData<List<Column>> findAllColumnsLive();
}
