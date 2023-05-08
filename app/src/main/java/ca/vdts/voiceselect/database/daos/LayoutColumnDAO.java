package ca.vdts.voiceselect.database.daos;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.VSLayoutColumn;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

@Dao
public interface LayoutColumnDAO extends VDTSBaseDAO<LayoutColumn> {
    @Query("SELECT * FROM LayoutsColumns")
    List<LayoutColumn> findAllLayoutColumns();
}
