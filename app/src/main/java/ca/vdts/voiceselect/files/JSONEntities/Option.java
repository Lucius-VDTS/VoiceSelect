package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Option {
    private static final Logger LOG = LoggerFactory.getLogger(Option.class);

    @Expose
    @SerializedName("key")
    private String key;

    @Expose
    @SerializedName("value")
    private String value;

    public Option(String key, String value) {
        LOG.debug("Creating Option entity for option {}", key);
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
