package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;
import static ca.vdts.voiceselect.database.entities.Layout.LAYOUT_NONE;
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
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedInterface;

/**
 * Entity defines the Sessions table.
 */
@Entity(
        tableName = "Sessions",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userID")
        }
)
public class Session implements VDTSIndexedNamedInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("sessionPrefix")
    @ColumnInfo(name = "sessionPrefix")
    private String sessionPrefix;

    @Expose
    @SerializedName("layoutName")
    @ColumnInfo(name = "layoutName")
    private String layoutName;

    @Expose
    @SerializedName("startDate")
    @ColumnInfo(name = "startDate")
    private LocalDateTime startDate;

    @Expose
    @SerializedName("dateIteration")
    @ColumnInfo(name = "dateIteration")
    private int dateIteration;

    @Expose
    @SerializedName("endDate")
    @ColumnInfo(name = "endDate")
    private LocalDateTime endDate;

    @Ignore
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    public static final Session SESSION_NONE = new Session(
            DEFAULT_UID,
            VDTS_USER_NONE.getUid(),
            VDTS_USER_NONE.getSessionPrefix(),
            LAYOUT_NONE.getName(),
            DEFAULT_DATE,
            0,
            DEFAULT_DATE
    );

    //Non-Default Constructor
    public Session(long uid, long userID, String sessionPrefix, String layoutName,
                   LocalDateTime startDate, int dateIteration, LocalDateTime endDate) {
        this.uid = uid;
        this.userID = userID;
        this.sessionPrefix = sessionPrefix;
        this.layoutName = layoutName;
        this.startDate = startDate;
        this.dateIteration = dateIteration;
        this.endDate = endDate;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public Session(long userID, String userPrefix, String layoutName, int dateIteration) {
        this(0L, userID, userPrefix, layoutName,LocalDateTime.now(), dateIteration, null);
    }

    public Session(Session session) {
        this(session.getUid(), session.getUserID(), session.getSessionPrefix(), session.getLayoutName(),
                session.getStartDate(), session.getDateIteration(), session.getEndDate());
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

    public String getSessionPrefix() {
        return sessionPrefix;
    }

    public void setSessionPrefix(String sessionPrefix) {
        this.sessionPrefix = sessionPrefix;
    }

    public String getLayoutName() {
        return layoutName;
    }

    public void setLayoutName(String layoutName) {
        this.layoutName = layoutName;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public int getDateIteration() {
        return dateIteration;
    }

    public void setDateIteration(int dateIteration) {
        this.dateIteration = dateIteration;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    @Override
    public long id() {
        return 0;
    }

    @Override
    public String name() {
        final DateTimeFormatter datePattern = DateTimeFormatter.ofPattern("yy/MM/dd");
        return String.format(
                Locale.getDefault(),
                "%s%s-%d",
                this.getSessionPrefix() == null && this.getSessionPrefix().isEmpty() ?
                        "" :
                        this.getSessionPrefix().concat("-"),
                datePattern.format(this.getStartDate()),
                this.getDateIteration()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Session)) return false;
        Session header = (Session) o;
        return getUserID() == header.getUserID() &&
                getDateIteration() == header.getDateIteration() &&
                Objects.equals(getStartDate(), header.getStartDate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getStartDate(), getDateIteration());
    }
}
