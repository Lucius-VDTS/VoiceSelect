package ca.vdts.voiceselect.database.daos;

import androidx.room.Dao;

import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * DAO for Session entity.
 */
@Dao
public interface SessionDAO extends VDTSBaseDAO<Session> {
}
