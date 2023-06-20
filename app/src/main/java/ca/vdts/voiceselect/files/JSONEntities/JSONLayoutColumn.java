package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The JSON version of the junction table
 */

public class JSONLayoutColumn {

    @Expose
    @SerializedName("columnCode")
    private String columnCode;

    @Expose
    @SerializedName("columnPosition")
    private long columnPosition;

    public JSONLayoutColumn(String columnId, long columnPosition) {
        this.columnCode = columnCode;
        this.columnPosition = columnPosition;
    }

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(String columnCode) {
        this.columnCode = columnCode;
    }

    public long getColumnPosition() {
        return columnPosition;
    }

    public void setColumnPosition(long columnPosition) {
        this.columnPosition = columnPosition;
    }
}
