package ca.vdts.voiceselect.database.repositories;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.database.daos.PictureReferenceDAO;
import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;
import ca.vdts.voiceselect.library.database.repositories.VDTSBaseRepository;


public class PictureReferenceRepository extends VDTSBaseRepository<PictureReference> {

    public PictureReferenceRepository(VDTSBaseDAO<PictureReference> dao) {
        super(dao);
    }

    public LiveData<List<PictureReference>> findAllLive(String query) {
        return ((PictureReferenceDAO) dao).findPhotosLive(new SimpleSQLiteQuery(query));
    }
}
