package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;
import static ca.vdts.voiceselect.database.entities.Column.COLUMN_NONE;
import static ca.vdts.voiceselect.database.entities.Session.SESSION_NONE;
import static ca.vdts.voiceselect.library.VDTSApplication.DEFAULT_UID;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Entity defines the SessionLayouts table. Entity associates columns and their position with a
 * session so that if it's layout is changed the session maintains it's historical layout.
 */
@Entity(
        tableName = "SessionLayouts",
        foreignKeys = {
                @ForeignKey(
                        entity = Session.class,
                        parentColumns = "uid",
                        childColumns = "sessionID",
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
                @Index(value = "sessionID"),
                @Index(value = "columnID")
        }
)
public class SessionLayout {

    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("sessionID")
    @ColumnInfo(name = "sessionID")
    private long sessionID;

    @Expose
    @SerializedName("columnID")
    @ColumnInfo(name = "columnID")
    private long columnID;

    @Expose
    @SerializedName("columnPosition")
    @ColumnInfo(name = "columnPosition")
    private int columnPosition;

    public static final SessionLayout SESSION_LAYOUT_NONE = new SessionLayout(
            DEFAULT_UID,
            SESSION_NONE.getUid(),
            COLUMN_NONE.getUid(),
            0
    );

    //Non-Default Constructor
    public SessionLayout(long uid, long sessionID, long columnID, int columnPosition) {
        this.uid = uid;
        this.sessionID = sessionID;
        this.columnID = columnID;
        this.columnPosition = columnPosition;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public SessionLayout(long sessionID, long columnID, int columnPosition) {
        this(0L, sessionID, columnID, columnPosition);
    }

    public SessionLayout (SessionLayout sessionLayout) {
        this(sessionLayout.getUid(),sessionLayout.getSessionID(),sessionLayout.getColumnID(), sessionLayout.getColumnPosition());
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getSessionID() {
        return sessionID;
    }

    public void setSessionID(long sessionID) {
        this.sessionID = sessionID;
    }

    public long getColumnID() {
        return columnID;
    }

    public void setColumnID(long columnID) {
        this.columnID = columnID;
    }

    public int getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(int columnPosition) {
        this.columnPosition = columnPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionLayout)) return false;
        SessionLayout that = (SessionLayout) o;
        return getSessionID() == that.getSessionID() &&
                getColumnID() == that.getColumnID() &&
                getColumnPosition() == that.getColumnPosition();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSessionID(), getColumnID(), getColumnPosition());
    }
}
