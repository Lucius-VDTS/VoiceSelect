package ca.vdts.voiceselect.database;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;

//TODO - Probably  busted/not needed
/**
 * Class defines the many to many relationship between layout and column entities
 */
public class VSLayoutColumn {
    @Embedded
    private Layout layout;

    @Relation(
            parentColumn = "layoutId",
            entityColumn = "columnId",
            associateBy = @Junction(LayoutColumn.class)
    )
    private List<Column> columnList;

    public Layout getLayout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public List<Column> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<Column> columnList) {
        this.columnList = columnList;
    }
}
