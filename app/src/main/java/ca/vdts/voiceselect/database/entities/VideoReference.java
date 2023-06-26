package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

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
        tableName = "VideoReferences",
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
public class VideoReference {
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @ColumnInfo(name = "userID")
    private long userID;

    @ColumnInfo(name = "timeStamp")
    private LocalDateTime timeStamp;

    @ColumnInfo(name = "sessionID")
    private long sessionID;

    @ColumnInfo(name = "path")
    private String path;

    public VideoReference(long uid, long userID, LocalDateTime timeStamp, long sessionID,
                          String path) {
        this.uid = uid;
        this.userID = userID;
        this.timeStamp = timeStamp;
        this.sessionID = sessionID;
        this.path = path;
    }

    @Ignore
    public VideoReference(long userID, long sessionID, String path) {
        this(0L, userID, LocalDateTime.now(), sessionID, path);
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

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VideoReference)) return false;
        VideoReference that = (VideoReference) o;
        return getUserID() == that.getUserID() &&
                getSessionID() == that.getSessionID() &&
                Objects.equals(getTimeStamp(), that.getTimeStamp()) &&
                Objects.equals(getPath(), that.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getTimeStamp(), getSessionID(), getPath());
    }
}
