package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

@Dao
public interface LayoutDAO extends VDTSBaseDAO<Layout> {
    @Query("SELECT * FROM Layouts WHERE name = :name")
    Layout findLayoutByName(String name);

    @Query("SELECT * FROM Layouts")
    LiveData<List<Layout>> findAllLayoutsLive();

    @Query("SELECT * FROM Layouts WHERE active = 1")
    LiveData<List<Layout>> findAllActiveLayoutsLive();
}
