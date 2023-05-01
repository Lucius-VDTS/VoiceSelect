package ca.vdts.voiceselect.database;

import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.repositories.ColumnRepository;
import ca.vdts.voiceselect.database.repositories.ColumnSpokenRepository;
import ca.vdts.voiceselect.database.repositories.ColumnValueRepository;
import ca.vdts.voiceselect.database.repositories.ColumnValueSpokenRepository;
import ca.vdts.voiceselect.library.database.VDTSViewModel;

/**
 * VoiceSelect view model allows views to access data from entities
 */
public class VSViewModel extends VDTSViewModel {
    private final ColumnRepository columnRepository;
    private final ColumnSpokenRepository columnSpokenRepository;
    private final ColumnValueRepository columnValueRepository;
    private final ColumnValueSpokenRepository columnValueSpokenRepository;

    public VSViewModel(@NonNull Application application) {
        super(application);
        VSDatabase vsDatabase = VSDatabase.getInstance(getApplication());

        columnRepository = new ColumnRepository(vsDatabase.columnDAO());
        columnSpokenRepository = new ColumnSpokenRepository(vsDatabase.columnSpokenDAO());
        columnValueRepository = new ColumnValueRepository(vsDatabase.columnValueDAO());
        columnValueSpokenRepository = new ColumnValueSpokenRepository(vsDatabase.columnValueSpokenDAO());
    }

    //Column
    public long insertColumn(Column column) {
        return columnRepository.insert(column);
    }

    public void insertAllColumns(Column[] columns) {
        columnRepository.insertAll(columns);
    }

    public void updateColumn (Column column) {
        columnRepository.update(column);
    }

    public void updateAllColumns (Column[] columns) {
        columnRepository.updateAll(columns);
    }

    public void deleteColumn (Column column) {
        columnRepository.delete(column);
    }

    public void deleteAllColumns (Column[] columns) { columnRepository.deleteAll(columns); }

    public Column findColumn(long uid) {
        return columnRepository.find(
                "SELECT * FROM Columns " +
                        "WHERE uid = " + uid
        );
    }

    public Column findFirstColumn(Long userId, String columnName, String columnNameCode,
                                  String columnExportCode) {
        return columnRepository.find(
                "SELECT * FROM Columns " +
                        "WHERE active = 1 " +
                        "AND userId = " + userId + " " +
                        "AND columnName = '" + columnName + "' " +
                        "AND columnNameCode = '" + columnNameCode + "' " +
                        "AND columnExportCode = '" + columnExportCode + "'"
        );
    }

    public List<Column> findAllColumns() {
        return columnRepository.findAll(
                "SELECT * FROM Columns" +
                        "WHERE uid <> " + DEFAULT_UID
        );
    }

    //todo - everywhere
    public LiveData<List<Column>> findAllColumnsLive() {
        return columnRepository.findAllColumnsLive();
    }

    public List<Column> findAllActiveColumns() {
        return columnRepository.findAll(
                "SELECT * FROM Columns " +
                        "WHERE active = 1 " +
                        "AND uid <> " + DEFAULT_UID
        );
    }

    public List<Column> findAllColumnsBySession(long sessionId) {
        return columnRepository.findAll(
                "SELECT DISTINCT C.* FROM Columns AS C " +
                        "LEFT JOIN ColumnValues AS CV ON CV.columnId = C.uid " +
                        "LEFT JOIN EntryValues AS EV ON EV.columnValueId = CV.uid " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryId " +
                        "WHERE E.sessionId = " + sessionId + " " +
                        "AND C.uid <> " + DEFAULT_UID
        );
    }

    //TODO - Is this needed?
    public List<Column> findAllColumnsByHeader(long headerId) {
        return columnRepository.findAll(
                "SELECT C.* FROM Columns AS C " +
                        "LEFT JOIN HeaderColumns AS HC ON HC.columnId = C.uid " +
                        "WHERE HC.headerId = " + headerId + " " +
                        "AND C.uid <> " + DEFAULT_UID
        );
    }

    //ColumnSpoken
    public void insertColumnSpoken(ColumnSpoken columnSpoken) {
        columnSpokenRepository.insert(columnSpoken);
    }

    public void insertAllColumnSpokens(ColumnSpoken[] columnSpoken) {
        columnSpokenRepository.insertAll(columnSpoken);
    }

    public void updateColumnSpoken(ColumnSpoken columnSpoken) {
        columnSpokenRepository.update(columnSpoken);
    }

    public void updateAllColumnSpokens(ColumnSpoken[] columnSpoken) {
        columnSpokenRepository.updateAll(columnSpoken);
    }

    public void deleteColumnSpoken(ColumnSpoken columnSpoken) {
        columnSpokenRepository.delete(columnSpoken);
    }

    public void deleteAllColumnSpokens(ColumnSpoken[] columnSpoken) {
        columnSpokenRepository.deleteAll(columnSpoken);
    }

    public List<ColumnSpoken> findAllColumnSpokens() {
        return columnSpokenRepository.findAll("SELECT * FROM ColumnSpokens");
    }

    public LiveData<List<ColumnSpoken>> findAllColumnSpokensLive() {
        return columnSpokenRepository.findAllColumnSpokensLive();
    }

    public List<ColumnSpoken> findAllColumnSpokensByUser(long userId) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE userId = " + userId
        );
    }

    public List<ColumnSpoken> findAllColumnSpokensByColumn(long columnId) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE columnId = " + columnId
        );
    }

    public List<ColumnSpoken> findAllColumnSpokensByColumnAndUser(long columnId, long userId) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE columnId = " + columnId + " " +
                        "AND userId = " + userId
        );
    }

    //ColumnValue
    public long insertColumnValue(ColumnValue columnValue) {
        return columnValueRepository.insert(columnValue);
    }

    public void insertAllColumnValues(ColumnValue[] columnValues) {
        columnValueRepository.insertAll(columnValues);
    }

    public void updateVolumeValue(ColumnValue columnValue) {
        columnValueRepository.update(columnValue);
    }

    public void updateAllColumnValues(ColumnValue[] columnValues) {
        columnValueRepository.updateAll(columnValues);
    }

    public void deleteColumnValue(ColumnValue columnValue) {
        columnValueRepository.delete(columnValue);
    }

    public void deleteAllColumnValues(ColumnValue[] columnValues) {
        columnValueRepository.deleteAll(columnValues);
    }

    public List<ColumnValue> findAllColumnValuesByColumn(long columnId) {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE columnId = " + columnId
        );
    }

    public List<ColumnValue> findAllActiveColumnValuesByColumn(long columnId) {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE active = 1 " +
                        "AND columnId = " + columnId
        );
    }

    public List<ColumnValue> findAllColumnValuesBySession(long sessionId) {
        return columnValueRepository.findAll(
                "SELECT CV.* FROM ColumnValues AS CV " +
                        "LEFT JOIN EntryValues AS EV ON EV.columnValueId = CV.uid " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryId " +
                        "WHERE E.sessionId = " + sessionId + " " +
                        "AND E.uid <> " + DEFAULT_UID
        );
    }

    //ColumnValueSpoken
    public long insert(ColumnValueSpoken columnValueSpoken) {
        return columnValueSpokenRepository.insert(columnValueSpoken);
    }

    public void insertAll(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.insertAll(columnValueSpokens);
    }

    public void update(ColumnValueSpoken columnValueSpoken) {
        columnValueSpokenRepository.update(columnValueSpoken);
    }

    public void updateAll(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.updateAll(columnValueSpokens);
    }

    public void delete(ColumnValueSpoken columnValueSpoken) {
        columnValueSpokenRepository.delete(columnValueSpoken);
    }

    public void deleteAll(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.deleteAll(columnValueSpokens);
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokens() {
        return columnValueSpokenRepository.findAll("SELECT * FROM ColumnValueSpokens");
    }

    public LiveData<List<ColumnValueSpoken>> findAllLiveColumnValueSpokens() {
        return columnValueSpokenRepository.findAllColumnValueSpokensLive();
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumn(long columnId) {
        return columnValueSpokenRepository.findAll(
                "SELECT CVS.* FROM ColumnValueSpokens AS CVS " +
                        "LEFT JOIN ColumnValues AS CV ON CV.uid = CVS.columnValueId " +
                        "LEFT JOIN Columns AS C ON C.uid = CVS.columnId " +
                        "WHERE C.uid = " + columnId
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByUser(long userId) {
        return columnValueSpokenRepository.findAll(
                "SELECT * FROM ColumnValueSpokens " +
                        "WHERE userId = " + userId
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumnValueAndUser(
            long columnValueId, long userId) {
        return columnValueSpokenRepository.findAll(
                "SELECT * FROM ColumnValueSpokens " +
                        "WHERE columnValueId = " + columnValueId + " " +
                        "AND userId = " + userId
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumnAndUser(
            long columnId, long userId) {
        return columnValueSpokenRepository.findAll(
                "SELECT CVS.* FROM ColumnValueSpokens AS CVS " +
                        "LEFT JOIN ColumnValues AS CV ON CV.uid = CVS.columnValueId " +
                        "LEFT JOIN Columns AS C ON C.uid = CVS.columnId " +
                        "WHERE C.uid = " + columnId + " " +
                        "AND VS.userID = " + userId
        );
    }
}
