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
import ca.vdts.voiceselect.library.interfaces.VDTSBNFGrammarInterface;

/**
 * Entity defines the ColumnSpokens table. Entity associates columns with their spoken terms.
 */
@Entity(
        tableName = "ColumnSpokens",
        foreignKeys = {
                @ForeignKey(
                        entity = VDTSUser.class,
                        parentColumns = "uid",
                        childColumns = "userID",
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
                @Index(value = "userID"),
                @Index(value = "columnID")
        }
)
public class ColumnSpoken implements VDTSBNFGrammarInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("userID")
    @ColumnInfo(name = "userID")
    private long userID;

    @Expose
    @SerializedName("columnID")
    @ColumnInfo(name = "columnID")
    private long columnID;

    @Expose
    @SerializedName("spoken")
    @ColumnInfo(name = "spoken")
    private String spoken;

    //Non-Default Constructor
    public ColumnSpoken(long uid, long userID, long columnID, String spoken) {
        this.uid = uid;
        this.userID = userID;
        this.columnID = columnID;
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

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    public long getColumnID() {
        return columnID;
    }

    public void setColumnID(long columnID) {
        this.columnID = columnID;
    }

    public String getSpoken() {
        return spoken;
    }

    public void setSpoken(String spoken) {
        this.spoken = spoken;
    }

    @Override
    public String toGrammar(Context context) {
        return spoken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColumnSpoken)) return false;
        ColumnSpoken that = (ColumnSpoken) o;
        return getUserID() == that.getUserID() &&
                getColumnID() == that.getColumnID() &&
                Objects.equals(getSpoken(), that.getSpoken());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUserID(), getColumnID(), getSpoken());
    }
}
