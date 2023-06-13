package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

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
 * Entity defines the EntryValues table. Entity represents a value within a row in a
 * gathering table.
 */
@Entity(
        tableName = "EntryValues",
        foreignKeys = {
                @ForeignKey(
                        entity = Entry.class,
                        parentColumns = "uid",
                        childColumns = "entryID",
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
                @Index(value = "entryID"),
                @Index(value = "columnValueID")
        }
)
public class EntryValue {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    private long uid;

    @Expose
    @SerializedName("entryID")
    @ColumnInfo(name = "entryID")
    private long entryID;

    @Expose
    @SerializedName("columnValueID")
    @ColumnInfo(name = "columnValueID")
    private long columnValueID;

    //Non-Default Constructor
    public EntryValue(long uid, long entryID, long columnValueID) {
        this.uid = uid;
        this.entryID = entryID;
        this.columnValueID = columnValueID;
    }

    //Place holder constructor - entity has id 0 until saved to database
    @Ignore
    public EntryValue(long entryID, long columnValueID) {
        this(0L, entryID, columnValueID);
    }

    //Null Column Value Constructor
    @Ignore
    public EntryValue(long entryID) {
        this(0L, entryID, DEFAULT_UID);
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public long getEntryID() {
        return entryID;
    }

    public void setEntryID(long entryID) {
        this.entryID = entryID;
    }

    public long getColumnValueID() {
        return columnValueID;
    }

    public void setColumnValueID(long columnValueID) {
        this.columnValueID = columnValueID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EntryValue)) return false;
        EntryValue that = (EntryValue) o;
        return getEntryID() == that.getEntryID() && getColumnValueID() == that.getColumnValueID();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntryID(), getColumnValueID());
    }
}
