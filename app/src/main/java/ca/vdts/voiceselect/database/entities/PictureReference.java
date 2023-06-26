package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

import android.location.Location;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;
import java.util.Objects;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;


@Entity(
        tableName = "PictureReferences",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Entry.class,
                        parentColumns = "uid",
                        childColumns = "entryID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userID"),
                @Index(value = "entryID")
        }
)
public class PictureReference {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "userID")
    private long userID;

    @ColumnInfo(name = "timeStamp")
    private LocalDateTime timeStamp;

    @ColumnInfo(name = "entryID")
    private long entryID;

    @ColumnInfo(name = "path")
    private String path;

    @ColumnInfo(name = "latitude")
    private Double latitude;

    @ColumnInfo(name = "longitude")
    private Double longitude;

    public PictureReference(long uid, long userID, LocalDateTime timeStamp, long entryID,
                            String path, Double latitude, Double longitude) {
        this.uid = uid;
        this.userID = userID;
        this.timeStamp = timeStamp;
        this.entryID = entryID;
        this.path = path;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public PictureReference(long uid, long userID, LocalDateTime timeStamp, long entryID,
                            String path, Location location) {
        this.uid = uid;
        this.userID = userID;
        this.timeStamp = timeStamp;
        this.entryID = entryID;
        this.path = path;
        if (location != null) {
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
        }
    }

    @Ignore
    public PictureReference(long userID, long entryID, String path, Location location) {
        this(0L, userID, LocalDateTime.now(), entryID, path, location);
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

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getEntryID() {
        return entryID;
    }

    public void setEntryID(long entryID) {
        this.entryID = entryID;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PictureReference)) return false;
        PictureReference that = (PictureReference) o;
        return getUserID() == that.getUserID() &&
                getEntryID() == that.getEntryID() &&
                Objects.equals(getTimeStamp(), that.getTimeStamp()) &&
                Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getTimeStamp(), getEntryID(), getPath());
    }
}
