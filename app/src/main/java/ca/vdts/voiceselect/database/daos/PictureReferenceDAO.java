package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;


@Dao
public interface PictureReferenceDAO extends VDTSBaseDAO<PictureReference> {

    @RawQuery(observedEntities = PictureReference.class)
    LiveData<List<PictureReference>> findPhotosLive(SupportSQLiteQuery query);
}
