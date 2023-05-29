package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Entity defines the LayoutsColumns junction table.
 */
@Entity(
        tableName = "LayoutsColumns",
        primaryKeys = { "layoutID", "columnID" },
        foreignKeys = {
                @ForeignKey(
                        entity = Layout.class,
                        parentColumns = "uid",
                        childColumns = "layoutID",
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
                @Index(value = "layoutID"),
                @Index(value = "columnID")
        }
)
public class LayoutColumn {
    @Expose
    @SerializedName("layoutID")
    @ColumnInfo(name = "layoutID")
    private long layoutID;

    @Expose
    @SerializedName("columnID")
    @ColumnInfo(name = "columnID")
    private long columnID;

    @Expose
    @SerializedName("columnPosition")
    @ColumnInfo(name = "columnPosition")
    private long columnPosition;

    //Non-Default Constructor
    public LayoutColumn(long layoutID, long columnID, long columnPosition) {
        this.layoutID = layoutID;
        this.columnID = columnID;
        this.columnPosition = columnPosition;
    }

    public long getLayoutID() {
        return layoutID;
    }

    public void setLayoutID(long layoutID) {
        this.layoutID = layoutID;
    }

    public long getColumnID() {
        return columnID;
    }

    public void setColumnID(long columnID) {
        this.columnID = columnID;
    }

    public long getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(long columnPosition) {
        this.columnPosition = columnPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayoutColumn that = (LayoutColumn) o;
        return layoutID == that.layoutID && columnID == that.columnID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(layoutID, columnID);
    }
}
