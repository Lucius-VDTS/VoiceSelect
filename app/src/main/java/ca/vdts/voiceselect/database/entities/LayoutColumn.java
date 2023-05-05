package ca.vdts.voiceselect.database.entities;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import ca.vdts.voiceselect.library.database.entities.VDTSUser;

@Entity(
        tableName = "LayoutsColumns",
        primaryKeys = { "layoutId", "columnId" },
        foreignKeys = {
                @ForeignKey(
                        entity = Layout.class,
                        parentColumns = "uid",
                        childColumns = "layoutId",
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
                @Index(value = "layoutId"),
                @Index(value = "columnId")
        }
)
public class LayoutColumn {
    @Expose
    @SerializedName("layoutId")
    @ColumnInfo(name = "layoutId")
    private long layoutId;

    @Expose
    @SerializedName("columnId")
    @ColumnInfo(name = "columnId")
    private long columnId;

    @Expose
    @SerializedName("columnPosition")
    @ColumnInfo(name = "columnPosition")
    private long columnPosition;

    //Non-Default Constructor
    public LayoutColumn(long layoutId, long columnId, long columnPosition) {
        this.layoutId = layoutId;
        this.columnId = columnId;
        this.columnPosition = columnPosition;
    }

    public long getLayoutId() {
        return layoutId;
    }

    public void setLayoutId(long layoutId) {
        this.layoutId = layoutId;
    }

    public long getColumnId() {
        return columnId;
    }

    public void setColumnId(long columnId) {
        this.columnId = columnId;
    }

    public long getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(long columnPosition) {
        this.columnPosition = columnPosition;
    }
}
