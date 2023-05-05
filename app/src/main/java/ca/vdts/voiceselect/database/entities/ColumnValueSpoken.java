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
import ca.vdts.voiceselect.library.services.VDTSBNFService;

/**
 * Entity defines the ColumnValueSpokens table.
 */
@Entity(
        tableName = "ColumnValueSpokens",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userId",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = ColumnValue.class,
                        parentColumns = "uid",
                        childColumns = "columnValueId",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userId"),
                @Index(value = "columnValueId")
        }
)
public class ColumnValueSpoken {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userId")
    @ColumnInfo(name = "userId")
    private long userId;

    @Expose
    @SerializedName("columnValueId")
    @ColumnInfo(name = "columnValueId")
    private long columnValueId;

    @Expose
    @SerializedName("spoken")
    @ColumnInfo(name = "spoken")
    private String spoken;

    //Non-Default Constructor
    public ColumnValueSpoken(long uid, long userId, long columnValueId, String spoken) {
        this.uid = uid;
        this.userId = userId;
        this.columnValueId = columnValueId;
        this.spoken = spoken;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public ColumnValueSpoken(long userId, long columnValueId, String spoken) {
        this(0L, userId, columnValueId, spoken);
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

    public long getColumnValueId() {
        return columnValueId;
    }

    public void setColumnValueId(long columnValueId) {
        this.columnValueId = columnValueId;
    }

    public String getSpoken() {
        return spoken;
    }

    public void setSpoken(String spoken) {
        this.spoken = spoken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnValueSpoken)) return false;
        ColumnValueSpoken that = (ColumnValueSpoken) o;
        return getUserId() == that.getUserId() &&
                getColumnValueId() == that.getColumnValueId() &&
                Objects.equals(getSpoken(), that.getSpoken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserId(), getColumnValueId(), getSpoken());
    }
}
