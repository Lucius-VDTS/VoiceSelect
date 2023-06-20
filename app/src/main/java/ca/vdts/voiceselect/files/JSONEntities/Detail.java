package ca.vdts.voiceselect.files.JSONEntities;


import static ca.vdts.voiceselect.database.entities.ColumnValue.COLUMN_VALUE_NONE;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

public class Detail {
    private static final Logger LOG = LoggerFactory.getLogger(Detail.class);

    @Expose
    @SerializedName("User Code")
    private String userCode;

    @Expose
    @SerializedName("Time Stamp")
    private LocalDateTime timeStamp;

    @Expose
    @SerializedName("Latitude")
    private Double latitude;

    @Expose
    @SerializedName("Longitude")
    private Double longitude;

    @Expose
    @SerializedName("Entry")
    private int entryNo;

    @Expose
    @SerializedName("Column Code")
    private String columnCode;

    @Expose
    @SerializedName("Value Code")
    private String valueCode;

    public Detail(Entry entry, List<VDTSUser> users, int entryNo, EntryValue entryValue,
                  List<ColumnValue> columnValues, List<Column> columns) {
        LOG.debug("Creating Detail entity for entry {}", entry.getUid());
        userCode = users.stream()
                .filter(user -> user.getUid() == entry.getUserID())
                .map(VDTSUser::getExportCode)
                .findFirst()
                .orElse(VDTS_USER_NONE.getExportCode());
        timeStamp = entry.getCreatedDate();
        latitude = entry.getLatitude();
        longitude = entry.getLongitude();
        this.entryNo = entryNo;
        final ColumnValue value = columnValues.stream()
                .filter(columnValue -> columnValue.getUid() == entryValue.getColumnValueID())
                .findFirst()
                .orElse(COLUMN_VALUE_NONE);
        columnCode = columns.stream()
                .filter(column -> column.getUid() == value.getColumnID())
                .map(Column::getExportCode)
                .findFirst()
                .orElse(COLUMN_VALUE_NONE.getExportCode());
        valueCode = value.getExportCode();
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public int getEntryNo() {
        return entryNo;
    }

    public void setEntryNo(int entryNo) {
        this.entryNo = entryNo;
    }

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(String columnCode) {
        this.columnCode = columnCode;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }
}
