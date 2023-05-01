package ca.vdts.voiceselect.library.database.repositories;

import androidx.annotation.WorkerThread;
import androidx.sqlite.db.SimpleSQLiteQuery;

import java.util.List;

import ca.vdts.voiceselect.library.database.daos.VDTSBaseDAO;

/**
 * Base repository provides data to base dao to perform CRUD operations
 * @param <Ent> - The entity being referenced
 */
public class VDTSBaseRepository<Ent> {
    protected VDTSBaseDAO<Ent> dao;

    public VDTSBaseRepository(VDTSBaseDAO<Ent> dao) { this.dao = dao; }

    @WorkerThread
    public long insert(Ent entity) { return this.dao.insert(entity); }

    @WorkerThread
    public void insertAll(Ent[] entities) { this.dao.insertAll(entities); }

    @WorkerThread
    public void update(Ent entity) {
        this.dao.update(entity);
    }

    @WorkerThread
    public void updateAll(Ent[] entities) {
        this.dao.updateAll(entities);
    }

    @WorkerThread
    public void delete(Ent entity) {
        this.dao.delete(entity);
    }

    @WorkerThread
    public void deleteAll(Ent[] entities) {
        this.dao.deleteAll(entities);
    }

    public Ent find(String query) {
        return this.dao.find(new SimpleSQLiteQuery(query));
    }

    public List<Ent> findAll(String query) {
        return this.dao.findAll(new SimpleSQLiteQuery(query));
    }
}
