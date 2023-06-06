package ca.vdts.voiceselect.library.database.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.RawQuery;
import androidx.room.Update;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

/**
 * Base data access object that performs CRUD operations on database entities.
 * @param <Entity> - The entity being referenced.
 */
@Dao
public interface VDTSBaseDAO<Entity> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Entity data);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Entity[] data);

    @Update
    void update(Entity data);

    @Update
    void updateAll(Entity[] data);

    @Delete
    void delete(Entity data);

    @Delete
    void deleteAll(Entity[] data);

    @RawQuery
    Entity find(SupportSQLiteQuery data);

    @RawQuery
    List<Entity> findAll(SupportSQLiteQuery data);

    @RawQuery
    int checkpoint(SupportSQLiteQuery data);
}
