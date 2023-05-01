package ca.vdts.voiceselect.database.daos;

import androidx.room.Dao;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for Column entity
 */
@Dao
public interface ColumnDAO extends VDTSBaseDAO<Column> {}
