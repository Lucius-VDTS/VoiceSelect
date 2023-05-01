package ca.vdts.voiceselect.database.daos;

import androidx.room.Dao;

import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for ColumnValue entity
 */
@Dao
public interface ColumnValueDAO extends VDTSBaseDAO<ColumnValue> {
}
