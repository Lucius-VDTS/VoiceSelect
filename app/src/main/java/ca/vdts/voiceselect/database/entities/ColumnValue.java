package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;
import static ca.vdts.voiceselect.database.entities.Column.COLUMN_NONE;
import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_DATE;
import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.Objects;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedInterface;

/**
 * Entity defines the ColumnValues table. Entity associates columns with particular values.
 */
@Entity(
        tableName = "ColumnValues",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Column.class,
                        parentColumns = "uid",
                        childColumns = "columnID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userID"),
                @Index(value = "columnID")
        }
)
public class ColumnValue implements VDTSIndexedNamedInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("columnID")
    @ColumnInfo(name = "columnID")
    private long columnID;

    @Expose
    @SerializedName("createdDate")
    @ColumnInfo(name = "createdDate")
    private LocalDateTime createdDate;

    @Expose
    @SerializedName("name")
    @ColumnInfo(name = "name")
    private String name;

    @Expose
    @SerializedName("nameCode")
    @ColumnInfo(name = "nameCode")
    private String nameCode;

    @Expose
    @SerializedName("exportCode")
    @ColumnInfo(name = "exportCode")
    private String exportCode;

    @Expose
    @SerializedName("active")
    @ColumnInfo(name = "active")
    private boolean active;

    public static final ColumnValue COLUMN_VALUE_NONE = new ColumnValue(
            DEFAULT_UID,
            VDTS_USER_NONE.getUid(),
            COLUMN_NONE.getUid(),
            DEFAULT_DATE,
            "NONE",
            "",
            "",
            false
    );

    public static final ColumnValue COLUMN_VALUE_NOT_FOUND = new ColumnValue(
            DEFAULT_UID - 1,
            VDTS_USER_NONE.getUid(),
            COLUMN_NONE.getUid(),
            DEFAULT_DATE.minusDays(1),
            "NOT_FOUND",
            "",
            "",
            false
    );

    //Non-Default Constructor
    public ColumnValue(long uid, long userID, long columnID, LocalDateTime createdDate, String name,
                       String nameCode, String exportCode, boolean active) {
        this.uid = uid;
        this.userID = userID;
        this.columnID = columnID;
        this.createdDate = createdDate;
        this.name = name;
        this.nameCode = nameCode;
        this.exportCode = exportCode;
        this.active = active;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public ColumnValue(long userID, long columnID, String name,
                       String nameCode, String exportCode) {
        this(
                0L,
                userID,
                columnID,
                LocalDateTime.now(),
                name,
                nameCode,
                exportCode,
                true
        );
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public long getColumnID() {
        return columnID;
    }

    public void setColumnID(long columnID) {
        this.columnID = columnID;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameCode() {
        return nameCode;
    }

    public void setNameCode(String nameCode) {
        this.nameCode = nameCode;
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

    @Override
    @Ignore
    public long id() {
        return uid;
    }

    @Override
    @Ignore
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnValue)) return false;
        ColumnValue that = (ColumnValue) o;
        return getUserID() == that.getUserID() &&
                getColumnID() == that.getColumnID() &&
                Objects.equals(getCreatedDate(), that.getCreatedDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getColumnID(), getCreatedDate());
    }
}
