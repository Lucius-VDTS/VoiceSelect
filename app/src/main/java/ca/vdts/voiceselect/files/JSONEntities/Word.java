package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;

public class Word {
    private static final Logger LOG = LoggerFactory.getLogger(Word.class);

    @Expose
    @SerializedName("Spoken Word")
    private String word;

    public Word(ColumnSpoken spoken) {
        LOG.debug("Creating Word for ColumnSpoken {}", spoken.getUid());
        word = spoken.getSpoken();
    }

    public Word(ColumnValueSpoken spoken) {
        word = spoken.getSpoken();
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }
}
