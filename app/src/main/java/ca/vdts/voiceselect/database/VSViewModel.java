package ca.vdts.voiceselect.database;

import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.time.LocalDateTime;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.database.repositories.ColumnRepository;
import ca.vdts.voiceselect.database.repositories.ColumnSpokenRepository;
import ca.vdts.voiceselect.database.repositories.ColumnValueRepository;
import ca.vdts.voiceselect.database.repositories.ColumnValueSpokenRepository;
import ca.vdts.voiceselect.database.repositories.EntryRepository;
import ca.vdts.voiceselect.database.repositories.EntryValueRepository;
import ca.vdts.voiceselect.database.repositories.LayoutColumnRepository;
import ca.vdts.voiceselect.database.repositories.LayoutRepository;
import ca.vdts.voiceselect.database.repositories.SessionLayoutRepository;
import ca.vdts.voiceselect.database.repositories.SessionRepository;
import ca.vdts.voiceselect.library.database.VDTSViewModel;

/**
 * VoiceSelect view model allows views to access data from tables.
 */
public class VSViewModel extends VDTSViewModel {
    private final ColumnRepository columnRepository;
    private final ColumnSpokenRepository columnSpokenRepository;
    private final ColumnValueRepository columnValueRepository;
    private final ColumnValueSpokenRepository columnValueSpokenRepository;
    private final LayoutRepository layoutRepository;
    private final LayoutColumnRepository layoutColumnRepository;
    private final SessionRepository sessionRepository;
    private final SessionLayoutRepository sessionLayoutRepository;
    private final EntryRepository entryRepository;
    private final EntryValueRepository entryValueRepository;

    public VSViewModel(@NonNull Application application) {
        super(application);
        VSDatabase vsDatabase = VSDatabase.getInstance(getApplication());

        columnRepository = new ColumnRepository(vsDatabase.columnDAO());
        columnSpokenRepository = new ColumnSpokenRepository(vsDatabase.columnSpokenDAO());
        columnValueRepository = new ColumnValueRepository(vsDatabase.columnValueDAO());
        columnValueSpokenRepository = new ColumnValueSpokenRepository(vsDatabase.columnValueSpokenDAO());
        layoutRepository = new LayoutRepository(vsDatabase.layoutDAO());
        layoutColumnRepository = new LayoutColumnRepository(vsDatabase.layoutColumnDAO());
        sessionRepository = new SessionRepository(vsDatabase.sessionDAO());
        sessionLayoutRepository = new SessionLayoutRepository(vsDatabase.sessionLayoutDAO());
        entryRepository = new EntryRepository(vsDatabase.entryDAO());
        entryValueRepository = new EntryValueRepository(vsDatabase.entryValueDAO());
    }

    //Column
    public long insertColumn(Column column) {
        return columnRepository.insert(column);
    }

    public void insertAllColumns(Column[] columns) {
        columnRepository.insertAll(columns);
    }

    public void updateColumn(Column column) {
        columnRepository.update(column);
    }

    public void updateAllColumns(Column[] columns) {
        columnRepository.updateAll(columns);
    }

    public void deleteColumn(Column column) {
        columnRepository.delete(column);
    }

    public void deleteAllColumns(Column[] columns) { columnRepository.deleteAll(columns); }

    public Column findColumn(long uid) {
        return columnRepository.find(
                "SELECT * FROM Columns " +
                        "WHERE uid = " + uid
        );
    }

    public Column findFirstColumn(Long userID, String columnName, String columnNameCode,
                                  String columnExportCode) {
        return columnRepository.find(
                "SELECT * FROM Columns " +
                        "WHERE active = 1 " +
                        "AND userID = " + userID + " " +
                        "AND columnName = '" + columnName + "' " +
                        "AND columnNameCode = '" + columnNameCode + "' " +
                        "AND columnExportCode = '" + columnExportCode + "'"
        );
    }

    public List<Column> findAllColumns() {
        return columnRepository.findAll(
                "SELECT * FROM Columns"
        );
    }

    public LiveData<List<Column>> findAllColumnsLive() {
        return columnRepository.findAllColumnsLive();
    }

    public List<Column> findAllActiveColumns() {
        return columnRepository.findAll(
                "SELECT * FROM Columns " +
                        "WHERE active = 1"
        );
    }

    public List<Column> findAllActiveColumnsByUser(long userID) {
        return columnRepository.findAll(
                "SELECT * FROM Columns " +
                        "WHERE active = 1 " +
                        "AND userID = " + userID
        );
    }

    public List<Column> findAllColumnsBySession(long sessionID) {
        return columnRepository.findAll(
                "SELECT DISTINCT C.* FROM Columns AS C " +
                        "LEFT JOIN ColumnValues AS CV ON CV.columnID = C.uid " +
                        "LEFT JOIN EntryValues AS EV ON EV.columnValueID = CV.uid " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryID " +
                        "WHERE E.sessionID = " + sessionID + " " +
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

    public List<ColumnSpoken> findAllColumnSpokensByUser(long userID) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE userID = " + userID
        );
    }

    public List<ColumnSpoken> findAllColumnSpokensByColumn(long columnID) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE columnID = " + columnID
        );
    }

    public List<ColumnSpoken> findAllColumnSpokensByColumnAndUser(long columnID, long userID) {
        return columnSpokenRepository.findAll(
                "SELECT * FROM ColumnSpokens " +
                        "WHERE columnID = " + columnID + " " +
                        "AND userID = " + userID
        );
    }

    //ColumnValue
    public long insertColumnValue(ColumnValue columnValue) {
        return columnValueRepository.insert(columnValue);
    }

    public void insertAllColumnValues(ColumnValue[] columnValues) {
        columnValueRepository.insertAll(columnValues);
    }

    public void updateColumnValue(ColumnValue columnValue) {
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

    public List<ColumnValue> findAllColumnValues() {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE uid <> " + DEFAULT_UID
        );
    }

    public List<ColumnValue> findAllActiveColumnValues() {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE active = 1"
        );
    }



    public LiveData<List<ColumnValue>> findAllColumnValuesLive() {
        return columnValueRepository.findAllColumnValuesLive();
    }

    public List<ColumnValue> findAllColumnValuesByColumn(long columnID) {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE columnID = " + columnID
        );
    }

    public List<ColumnValue> findAllActiveColumnValuesByColumn(long columnID) {
        return columnValueRepository.findAll(
                "SELECT * FROM ColumnValues " +
                        "WHERE active = 1 " +
                        "AND columnID = " + columnID
        );
    }

    public List<ColumnValue> findAllColumnValuesBySession(long sessionID) {
        return columnValueRepository.findAll(
                "SELECT CV.* FROM ColumnValues AS CV " +
                        "LEFT JOIN EntryValues AS EV ON EV.columnValueID = CV.uid " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryID " +
                        "WHERE E.sessionID = " + sessionID + " " +
                        "AND E.uid <> " + DEFAULT_UID
        );
    }

    //ColumnValueSpoken
    public long insertColumnValueSpoken(ColumnValueSpoken columnValueSpoken) {
        return columnValueSpokenRepository.insert(columnValueSpoken);
    }

    public void insertAllColumnValueSpokens(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.insertAll(columnValueSpokens);
    }

    public void updateColumnValueSpoken(ColumnValueSpoken columnValueSpoken) {
        columnValueSpokenRepository.update(columnValueSpoken);
    }

    public void updateAllColumnValueSpokens(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.updateAll(columnValueSpokens);
    }

    public void deleteColumnValueSpoken(ColumnValueSpoken columnValueSpoken) {
        columnValueSpokenRepository.delete(columnValueSpoken);
    }

    public void deleteAllColumnValueSpokens(ColumnValueSpoken[] columnValueSpokens) {
        columnValueSpokenRepository.deleteAll(columnValueSpokens);
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokens() {
        return columnValueSpokenRepository.findAll("SELECT * FROM ColumnValueSpokens");
    }

    public LiveData<List<ColumnValueSpoken>> findAllColumnValueSpokensLive() {
        return columnValueSpokenRepository.findAllColumnValueSpokensLive();
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumn(long columnID) {
        return columnValueSpokenRepository.findAll(
                "SELECT CVS.* FROM ColumnValueSpokens AS CVS " +
                        "LEFT JOIN ColumnValues AS CV ON CV.uid = CVS.columnValueID " +
                        "LEFT JOIN Columns AS C ON C.uid = CVS.columnID " +
                        "WHERE C.uid = " + columnID
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByUser(long userID) {
        return columnValueSpokenRepository.findAll(
                "SELECT * FROM ColumnValueSpokens " +
                        "WHERE userID = " + userID
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumnValueAndUser(
            long columnValueID, long userID) {
        return columnValueSpokenRepository.findAll(
                "SELECT * FROM ColumnValueSpokens " +
                        "WHERE columnValueID = " + columnValueID + " " +
                        "AND userID = " + userID
        );
    }

    public List<ColumnValueSpoken> findAllColumnValueSpokensByColumnAndUser(
            long columnID, long userID) {
        return columnValueSpokenRepository.findAll(
                "SELECT CVS.* FROM ColumnValueSpokens AS CVS " +
                        "LEFT JOIN ColumnValues AS CV ON CV.uid = CVS.columnValueID " +
                        "LEFT JOIN Columns AS C ON C.uid = CVS.columnID " +
                        "WHERE C.uid = " + columnID + " " +
                        "AND VS.userID = " + userID
        );
    }

    //Layout
    public long insertLayout(Layout layout) {
        return layoutRepository.insert(layout);
    }

    public void insertAllLayouts(Layout[] layouts) {
        layoutRepository.insertAll(layouts);
    }

    public void updateLayout(Layout layout) {
        layoutRepository.update(layout);
    }

    public void updateAllLayouts(Layout[] layouts) {
        layoutRepository.updateAll(layouts);
    }

    public void deleteLayout(Layout layout) {
        layoutRepository.delete(layout);
    }

    public void deleteAllLayouts(Layout[] layouts) {
        layoutRepository.deleteAll(layouts);
    }

    public Layout findLayoutByID(long uid) {
        return layoutRepository.find(
                "SELECT * FROM Layouts " +
                        "WHERE uid = " + uid
        );
    }

    public List<Layout> findAllLayouts() {
        return layoutRepository.findAll("SELECT * FROM Layouts");
    }

    public List<Layout> findAllActiveLayouts() {
        return layoutRepository.findAll(
                "SELECT * FROM Layouts " +
                        "WHERE active = 1"
        );
    }

    public LiveData<List<Layout>> findAllLayoutsLive() {
        return layoutRepository.findAllLayoutsLive();
    }

    public LiveData<List<Layout>> findAllActiveLayoutsLive() {
        return layoutRepository.findAllActiveLayoutsLive();
    }

    //LayoutColumn
    public long insertLayoutColumn(LayoutColumn layoutColumn) {
        return layoutColumnRepository.insert(layoutColumn);
    }

    public void insertAllLayoutColumns(LayoutColumn[] layoutColumns) {
        layoutColumnRepository.insertAll(layoutColumns);
    }

    public void updateLayoutColumn(LayoutColumn layoutColumn) {
        layoutColumnRepository.update(layoutColumn);
    }

    public void updateAllLayoutColumns(LayoutColumn[] layoutColumns) {
        layoutColumnRepository.updateAll(layoutColumns);
    }

    public void deleteLayoutColumn(LayoutColumn layoutColumn) {
        layoutColumnRepository.delete(layoutColumn);
    }

    public void deleteAllLayoutColumns(LayoutColumn[] layoutColumns) {
        layoutColumnRepository.deleteAll(layoutColumns);
    }

    public List<LayoutColumn> findAllLayoutColumns() {
        return layoutColumnRepository.findAllLayoutColumns();
    }

    public List<LayoutColumn> findAllLayoutColumnsByLayout(Layout layout) {
        return layoutColumnRepository.findAll(
                "SELECT * FROM LayoutsColumns " +
                        "WHERE layoutID = " + layout.getUid()
        );
    }

    public LiveData<List<LayoutColumn>> findAllLayoutColumnsLive() {
        return layoutColumnRepository.findAllLayoutColumnsLive();
    }

    public LiveData<List<LayoutColumn>> findAllLayoutColumnsByLayoutLive(long layoutID) {
        return layoutColumnRepository.findAllLayoutColumnsByLayoutLive(layoutID);
    }

    //Session
    public long insertSession(Session session) {
        return sessionRepository.insert(session);
    }

    public void insertAllSessions(Session[] sessions) {
        sessionRepository.insertAll(sessions);
    }

    public void updateSession(Session session) {
        sessionRepository.update(session);
    }

    public void updateAllSessions(Session[] sessions) {
        sessionRepository.updateAll(sessions);
    }

    public void deleteSession(Session session) {
        sessionRepository.delete(session);
    }

    public void deleteAllSessions(Session[] sessions) {
        sessionRepository.deleteAll(sessions);
    }

    public Session findSessionByID(long uid) {
        return sessionRepository.find(
                "SELECT * FROM Sessions " +
                        "WHERE uid = " + uid
        );
    }

    public List<Session> findAllSessionsOrderByStartDate() {
        return sessionRepository.findAll(
                "SELECT * FROM Sessions " +
                        "WHERE uid <> " + DEFAULT_UID + " " +
                        "ORDER BY startDate DESC"
        );
    }

    public List<Session> findAllOpenSessionsOrderByStartDate() {
        return sessionRepository.findAll(
                "SELECT * FROM Sessions " +
                        "WHERE uid <> " + DEFAULT_UID + " " +
                        "AND endDate IS NULL " +
                        "ORDER BY startDate DESC"
        );
    }

    public int countSessionsStartedToday() {
        return sessionRepository.findAll(
                "SELECT * FROM Sessions " +
                        "WHERE startDate >= '" + LocalDateTime.now() + "' " +
                        "AND uid <> " + DEFAULT_UID
        ).size();
    }

    //Session Layout
    public long insertSessionLayout(SessionLayout sessionLayout) {
        return sessionLayoutRepository.insert(sessionLayout);
    }

    public void insertAllSessionLayouts(SessionLayout[] sessionLayouts) {
        sessionLayoutRepository.insertAll(sessionLayouts);
    }

    public void updateSessionLayout(SessionLayout sessionLayout) {
        sessionLayoutRepository.update(sessionLayout);
    }

    public void updateAllSessionLayouts(SessionLayout[] sessionLayouts) {
        sessionLayoutRepository.updateAll(sessionLayouts);
    }

    public void deleteSessionLayout(SessionLayout sessionLayout) {
        sessionLayoutRepository.delete(sessionLayout);
    }

    public void deleteAllSessionLayouts(SessionLayout[] sessionLayouts) {
        sessionLayoutRepository.deleteAll(sessionLayouts);
    }

    public List<SessionLayout> findAllSessionLayoutsBySession(long sessionID) {
        return sessionLayoutRepository.findAll(
                "SELECT * FROM SessionLayouts " +
                        "WHERE sessionID = " + sessionID
        );
    }

    //Entry
    public long insertEntry(Entry entry) {
        return entryRepository.insert(entry);
    }

    public void insertAllEntries(Entry[] entries) {
        entryRepository.insertAll(entries);
    }

    public void updateEntry(Entry entry) {
        entryRepository.update(entry);
    }

    public void updateAllEntries(Entry[] entries) {
        entryRepository.updateAll(entries);
    }

    public void deleteEntry(Entry entry) {
        entryRepository.delete(entry);
    }

    public void deleteAllEntries(Entry[] entries) {
        entryRepository.deleteAll(entries);
    }

    public List<Entry> findAllEntriesBySession(long sessionID) {
        return entryRepository.findAll(
                "SELECT * FROM Entries " +
                        "WHERE sessionID = " + sessionID
        );
    }

    public LiveData<List<Entry>> findAllEntriesBySessionLive(long sessionID) {
        return entryRepository.findAllEntriesLive(
                "SELECT * FROM Entries " +
                        "WHERE sessionID = " + sessionID

        );
    }

    //EntryValue
    public long insertEntryValue(EntryValue entryValue) {
        return entryValueRepository.insert(entryValue);
    }

    public void insertAllEntryValues(EntryValue[] entryValues) {
        entryValueRepository.insertAll(entryValues);
    }

    public void updateEntryValue(EntryValue entryValue) {
        entryValueRepository.update(entryValue);
    }

    public void updateAllEntryValues(EntryValue[] entryValues) {
        entryValueRepository.updateAll(entryValues);
    }

    public void deleteEntryValue(EntryValue entryValue) {
        entryValueRepository.delete(entryValue);
    }

    public void deleteAllEntryValues(EntryValue[] entryValues) {
        entryValueRepository.deleteAll(entryValues);
    }

    public List<EntryValue> findAllEntryValuesBySession(long sessionID) {
        return entryValueRepository.findAll(
                "SELECT EV.* FROM EntryValues AS EV " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryID " +
                        "WHERE E.sessionID = " + sessionID
        );
    }

    public LiveData<List<EntryValue>> findAllEntryValuesLiveBySession(long sessionID) {
        return entryValueRepository.findAllEntryValuesLive(
                "SELECT EV.* FROM EntryValues AS EV " +
                        "LEFT JOIN Entries AS E ON E.uid = EV.entryID " +
                        "WHERE E.sessionID = " + sessionID
        );
    }
}