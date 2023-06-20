package ca.vdts.voiceselect.files.JSONEntities;

import static ca.vdts.voiceselect.database.entities.Column.COLUMN_NONE;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;


public class JSONValue {
    private static final Logger LOG = LoggerFactory.getLogger(JSONValue.class);

    @Expose
    @SerializedName("User Code")
    private String userCode;

    @Expose
    @SerializedName("Column Code")
    private String columnCode;

    @Expose
    @SerializedName("Create Date")
    private LocalDateTime createDate;

    @Expose
    @SerializedName("Display Name")
    private String displayName;

    @Expose
    @SerializedName("Display Code")
    private String displayCode;

    @Expose
    @SerializedName("Export Code")
    private String exportCode;

    @Expose
    @SerializedName("Active")
    private boolean active;

    @Expose
    @SerializedName("Default Words")
    private List<Word> defaultWords;

    public JSONValue(List<VDTSUser> users, List<Column> columns, ColumnValue value,
                     List<ColumnValueSpoken> valueSpoken) {
        LOG.debug("Creating JSONValue entity for ColumnValue {}", value.getUid());
        userCode = users.stream()
                .filter(user -> user.getUid() == value.getUserID())
                .map(VDTSUser::getExportCode)
                .findFirst()
                .orElse(VDTS_USER_NONE.getExportCode());
        columnCode = columns.stream()
                .filter(column -> column.getUid() == value.getColumnID())
                .map(Column::getExportCode)
                .findFirst()
                .orElse(COLUMN_NONE.getExportCode());
        createDate = value.getCreatedDate();
        displayName = value.getName();
        displayCode = value.getNameCode();
        exportCode = value.getExportCode();
        active = value.isActive();
        defaultWords = new ArrayList<>();
        valueSpoken.forEach(spoken -> defaultWords.add(new Word(spoken)));
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(String columnCode) {
        this.columnCode = columnCode;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayCode() {
        return displayCode;
    }

    public void setDisplayCode(String displayCode) {
        this.displayCode = displayCode;
    }

    public String getExportCode() {
        return exportCode;
    }

    public void setExportCode(String exportCode) {
        this.exportCode = exportCode;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<Word> getDefaultWords() {
        return defaultWords;
    }

    public void setDefaultWords(List<Word> defaultWords) {
        this.defaultWords = defaultWords;
    }
}
