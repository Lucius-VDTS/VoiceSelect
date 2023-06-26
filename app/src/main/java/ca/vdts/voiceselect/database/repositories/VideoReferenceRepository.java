package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.daos.VideoReferenceDAO;
import ca.vdts.voiceselect.database.entities.VideoReference;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;


public class VideoReferenceRepository extends VDTSBaseRepository<VideoReference> {

    public VideoReferenceRepository(VDTSBaseDAO<VideoReference> dao) {
        super(dao);
    }

    public LiveData<List<VideoReference>> findAllLive(String query) {
        return ((VideoReferenceDAO) dao).findVideosLive(new SimpleSQLiteQuery(query));
    }
}
