package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * JSON version of columns and their values
 */
public class Setup {
    private static final Logger LOG = LoggerFactory.getLogger(Setup.class);

    @Expose
    @SerializedName("Columns")
    private List<JSONColumn> columns;

    @Expose
    @SerializedName("Values")
    private List<JSONValue> values;

    public Setup(List<VDTSUser> users, List<Column> columns, List<ColumnSpoken> columnSpoken,
                 List<ColumnValue> values, List<ColumnValueSpoken> valueSpoken) {
        LOG.debug("Creating Setup entity");
        this.columns = new ArrayList<>();
        columns.forEach(column -> {
            final List<ColumnSpoken> spokenList = columnSpoken.stream()
                    .filter(spoken -> spoken.getColumnID() == column.getUid())
                    .collect(Collectors.toList());
            this.columns.add(new JSONColumn(users, column, spokenList));
        });
        this.values = new ArrayList<>();
        values.forEach(value -> {
            final List<ColumnValueSpoken> spokenList = valueSpoken.stream()
                    .filter(spoken -> spoken.getColumnValueID() == value.getUid())
                    .collect(Collectors.toList());
            this.values.add(new JSONValue(users, columns, value, spokenList));
        });
    }

    public List<JSONColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<JSONColumn> columns) {
        this.columns = columns;
    }

    public List<JSONValue> getValues() {
        return values;
    }

    public void setValues(List<JSONValue> values) {
        this.values = values;
    }
}
