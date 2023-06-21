package ca.vdts.voiceselect.files.JSONEntities;


import static ca.vdts.voiceselect.database.entities.Column.COLUMN_NONE;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;


public class ValueWords {
    private static final Logger LOG = LoggerFactory.getLogger(ValueWords.class);

    @Expose
    @SerializedName("ColumnCode")
    private String columnCode;

    @Expose
    @SerializedName("Value Code")
    private String valueCode;

    @Expose
    @SerializedName("Words")
    private List<Word> words;

    public ValueWords(ColumnValue value, List<Column> columns, List<ColumnValueSpoken> valueSpoken) {
        LOG.debug("Creating ValueWords entity for ColumnValue {}", value.getUid());
        columnCode = columns.stream()
                .filter(column -> column.getUid() == value.getColumnID())
                .findFirst()
                .orElse(COLUMN_NONE)
                .getExportCode();
        valueCode = value.getExportCode();
        words = new ArrayList<>();
        valueSpoken.forEach(spoken -> words.add(new Word(spoken)));
    }

    public String getColumnCode() {
        return columnCode;
    }

    public void setColumnCode(String columnCode) {
        this.columnCode = columnCode;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}
