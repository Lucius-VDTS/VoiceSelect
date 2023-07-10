package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;
import static androidx.room.ForeignKey.SET_NULL;
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
 * Entity defines the Layouts table.
 */
@Entity(
        tableName = "Layouts",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = SET_NULL
                ),
        },
        indices = {
                @Index(value = "userID")
        }
)
public class Layout implements VDTSIndexedNamedInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("createdDate")
    @ColumnInfo(name = "createdDate")
    private LocalDateTime createdDate;

    @Expose
    @SerializedName("name")
    @ColumnInfo(name = "name")
    private String name;

    @Expose
    @SerializedName("exportCode")
    @ColumnInfo(name = "exportCode")
    private String exportCode;

    @Expose
    @SerializedName("commentRequired")
    @ColumnInfo(name = "commentRequired")
    private boolean commentRequired;

    @Expose
    @SerializedName("pictureRequired")
    @ColumnInfo(name = "pictureRequired")
    private boolean pictureRequired;

    @Expose
    @SerializedName("active")
    @ColumnInfo(name = "active")
    private boolean active;

    public static final Layout LAYOUT_NONE = new Layout(
            DEFAULT_UID,
            VDTS_USER_NONE.getUid(),
            DEFAULT_DATE,
            "No Layouts",
            "NL",
            false,
            false,
            true
    );

    //Non-Default Constructor
    public Layout(long uid, long userID, LocalDateTime createdDate, String name, String exportCode,
                  boolean commentRequired, boolean pictureRequired, boolean active) {
        this.uid = uid;
        this.userID = userID;
        this.createdDate = createdDate;
        this.name = name;
        this.exportCode = exportCode;
        this.commentRequired = commentRequired;
        this.pictureRequired = pictureRequired;
        this.active = active;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public Layout(long userID, String name, String exportCode, boolean commentRequired,
                  boolean pictureRequired) {
        this(
                0L,
                userID,
                LocalDateTime.now(),
                name,
                exportCode,
                commentRequired,
                pictureRequired,
                true
        );
    }

    public Layout(Layout other){
        this(other.getUid(),other.getUserID(),other.getCreatedDate(),other.getName(),other.getExportCode(),other.isCommentRequired(),other.isPictureRequired(),other.isActive());
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

    public String getExportCode() {
        return exportCode;
    }

    public void setExportCode(String exportCode) {
        this.exportCode = exportCode;
    }

    public boolean isCommentRequired() {
        return commentRequired;
    }

    public void setCommentRequired(boolean commentRequired) {
        this.commentRequired = commentRequired;
    }

    public boolean isPictureRequired() {
        return pictureRequired;
    }

    public void setPictureRequired(boolean pictureRequired) {
        this.pictureRequired = pictureRequired;
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
        return getUserID() == column.getUserID() &&
                Objects.equals(getCreatedDate(), column.getCreatedDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getCreatedDate());
    }
}
