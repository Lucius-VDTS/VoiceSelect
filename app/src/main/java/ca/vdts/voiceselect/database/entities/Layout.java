package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;
import static androidx.room.ForeignKey.SET_NULL;

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
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedEntityInterface;

/**
 * Entity defines the Layouts table.
 */
@Entity(
        tableName = "Layouts",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userId",
                        onUpdate = CASCADE,
                        onDelete = SET_NULL
                ),
        },
        indices = {
                @Index(value = "userId")
        }
)
public class Layout implements VDTSIndexedNamedEntityInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userId")
    @ColumnInfo(name = "userId")
    private long userId;

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

    //Non-Default Constructor
    public Layout(long uid, long userId, LocalDateTime createdDate, String name, String nameCode,
                  String exportCode, boolean active) {
        this.uid = uid;
        this.userId = userId;
        this.createdDate = createdDate;
        this.name = name;
        this.nameCode = nameCode;
        this.exportCode = exportCode;
        this.active = active;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public Layout(long userId, String name, String nameCode, String exportCode) {
        this(
                0L,
                userId,
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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
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
        if (!(o instanceof Layout)) return false;
        Layout column = (Layout) o;
        return getUserId() == column.getUserId() &&
                Objects.equals(getCreatedDate(), column.getCreatedDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getCreatedDate());
    }
}
