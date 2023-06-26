package ca.vdts.voiceselect.database.daos;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.RawQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.entities.VideoReference;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;


@Dao
public interface VideoReferenceDAO extends VDTSBaseDAO<VideoReference> {

    @RawQuery(observedEntities = VideoReference.class)
    LiveData<List<VideoReference>> findVideosLive(SupportSQLiteQuery query);
}
