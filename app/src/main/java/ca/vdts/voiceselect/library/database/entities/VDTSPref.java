package ca.vdts.voiceselect.library.database.entities;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Entity defines the preference table
 */
@Entity (
        tableName = "Preferences",
        indices = {
                @Index(value = "uid")
        }
)
public class VDTSPref {
    @PrimaryKey(autoGenerate = true)
    @Expose
    @SerializedName("uid")
    private long uid;

    @Expose
    @SerializedName("key")
    private final String key;

    @Expose
    @SerializedName("value")
    private String value;

    public VDTSPref(long uid, String key, String value) {
        this.uid = uid;
        this.key = key;
        this.value = value;
    }

    @Ignore
    public VDTSPref(String key, String value) {
        this(0,  key, value);
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
