package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.interfaces.VDTSBnfGrammarInterface;

/**
 * Entity defines the ColumnSpokens table
 */
@Entity(
        tableName = "ColumnSpokens",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userId",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = Column.class,
                        parentColumns = "uid",
                        childColumns = "columnId",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userId"),
                @Index(value = "columnId")
        }
)
public class ColumnSpoken implements VDTSBnfGrammarInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userId")
    @ColumnInfo(name = "userId")
    private long userId;

    @Expose
    @SerializedName("columnId")
    @ColumnInfo(name = "columnId")
    private long columnId;

    @Expose
    @SerializedName("spoken")
    @ColumnInfo(name = "spoken")
    private String spoken;

    //Non-Default Constructor
    public ColumnSpoken(long uid, long userId, long columnId, String spoken) {
        this.uid = uid;
        this.userId = userId;
        this.columnId = columnId;
        this.spoken = spoken;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public ColumnSpoken(long userID, long columnID, String spoken) {
        this(0L, userID, columnID, spoken);
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

    public long getColumnId() {
        return columnId;
    }

    public void setColumnId(long columnId) {
        this.columnId = columnId;
    }

    public String getSpoken() {
        return spoken;
    }

    public void setSpoken(String spoken) {
        this.spoken = spoken;
    }

    @Override
    public String toGrammar(Context context) {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnSpoken)) return false;
        ColumnSpoken that = (ColumnSpoken) o;
        return getUserId() == that.getUserId() &&
                getColumnId() == that.getColumnId() &&
                Objects.equals(getSpoken(), that.getSpoken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getColumnId(), getSpoken());
    }
}
