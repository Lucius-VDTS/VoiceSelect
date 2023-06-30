package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

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

/**
 * Entity defines the Entries table. Entity represents a row in the data gathering table.
 */
@Entity(
        tableName = "Entries",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Session.class,
                        parentColumns = "uid",
                        childColumns = "sessionID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userID"),
                @Index(value = "sessionID")
        }
)
public class Entry {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("sessionID")
    @ColumnInfo(name = "sessionID")
    private long sessionID;

    @Expose
    @SerializedName("createdDate")
    @ColumnInfo(name = "createdDate")
    private LocalDateTime createdDate;

    @Expose
    @SerializedName("latitude")
    @ColumnInfo(name = "latitude")
    private Double latitude;

    @Expose
    @SerializedName("longitude")
    @ColumnInfo(name = "longitude")
    private Double longitude;

    @Expose
    @SerializedName("comment")
    @ColumnInfo(name = "comment")
    private String comment;

    //Non-Default Constructor
    public Entry(long uid, long userID, long sessionID, LocalDateTime createdDate) {
        this.uid = uid;
        this.userID = userID;
        this.sessionID = sessionID;
        this.createdDate = createdDate;
        this.latitude = null;
        this.longitude = null;
        this.comment = null;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public Entry(long userID, long sessionID) {
        this(0L, userID, sessionID, LocalDateTime.now());
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

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Entry)) return false;
        Entry entry = (Entry) o;
        return getUserID() == entry.getUserID() &&
                getSessionID() == entry.getSessionID() &&
                Objects.equals(getCreatedDate(), entry.getCreatedDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getSessionID(), getCreatedDate());
    }
}
