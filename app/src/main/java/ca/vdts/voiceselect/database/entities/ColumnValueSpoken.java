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

import java.util.Objects;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * Entity defines the ColumnValueSpokens table. Entity associates values that can be placed in
 * particular columns with their spoken terms.
 */
@Entity(
        tableName = "ColumnValueSpokens",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                ),
                @ForeignKey(
                        entity = ColumnValue.class,
                        parentColumns = "uid",
                        childColumns = "columnValueID",
                        onUpdate = CASCADE,
                        onDelete = CASCADE
                )
        },
        indices = {
                @Index(value = "userID"),
                @Index(value = "columnValueID")
        }
)
public class ColumnValueSpoken {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("columnValueID")
    @ColumnInfo(name = "columnValueID")
    private long columnValueID;

    @Expose
    @SerializedName("spoken")
    @ColumnInfo(name = "spoken")
    private String spoken;

    //Non-Default Constructor
    public ColumnValueSpoken(long uid, long userID, long columnValueID, String spoken) {
        this.uid = uid;
        this.userID = userID;
        this.columnValueID = columnValueID;
        this.spoken = spoken;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public ColumnValueSpoken(long userID, long columnValueID, String spoken) {
        this(0L, userID, columnValueID, spoken);
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

    public long getColumnValueID() {
        return columnValueID;
    }

    public void setColumnValueID(long columnValueID) {
        this.columnValueID = columnValueID;
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
        return getUserID() == that.getUserID() &&
                getColumnValueID() == that.getColumnValueID() &&
                Objects.equals(getSpoken(), that.getSpoken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getColumnValueID(), getSpoken());
    }
}
