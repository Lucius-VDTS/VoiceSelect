package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;

/**
 * JSON version of a user's columns spoken terms
 */
public class ColumnWords {
    private static final Logger LOG = LoggerFactory.getLogger(ColumnWords.class);

    @Expose
    @SerializedName("Column Code")
    private String code;

    @Expose
    @SerializedName("Words")
    private List<Word> words;

    public ColumnWords(Column column, List<ColumnSpoken> columnSpoken) {
        LOG.debug("Creating ColumnWords entity for column {}", column.getUid());
        code = column.getExportCode();
        words = new ArrayList<>();
        columnSpoken.forEach(spoken -> words.add(new Word(spoken)));
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<Word> getWords() {
        return words;
    }

    public void setWords(List<Word> words) {
        this.words = words;
    }
}
