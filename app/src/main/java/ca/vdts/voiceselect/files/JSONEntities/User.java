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
 * JSON version of a single user,
 * with that user's preferences and spoken terms
 */
public class User {
    private static final Logger LOG = LoggerFactory.getLogger(User.class);

    @Expose
    @SerializedName("name")
    private String name;

    @Expose
    @SerializedName("exportCode")
    private String exportCode;

    @Expose
    @SerializedName("initials")
    private String initials;

    @Expose
    @SerializedName("sessionPrefix")
    private String sessionPrefix;

    @Expose
    @SerializedName("authority")
    private int authority;

    @Expose
    @SerializedName("primary")
    private boolean primary;

    @Expose
    @SerializedName("password")
    private String password;

    @Expose
    @SerializedName("autosave")
    private int autosave;

    @Expose
    @SerializedName("feedback")
    private int feedback;

    @Expose
    @SerializedName("feedbackQueue")
    private boolean feedbackQueue;

    @Expose
    @SerializedName("feedbackRate")
    private float feedbackRate;

    @Expose
    @SerializedName("feedbackPitch")
    private float feedbackPitch;

    @Expose
    @SerializedName("active")
    private boolean active;

    @Expose
    @SerializedName("Spoken Columns")
    private List<ColumnWords> columnWords;

    @Expose
    @SerializedName("Spoken Values")
    private List<ValueWords> valueWords;

    public User(VDTSUser user, List<Column> columns, List<ColumnSpoken> columnSpoken,
                List<ColumnValue> values, List<ColumnValueSpoken> valueSpoken) {
        LOG.debug("Creating User entity for user {}", user.getUid());
        name = user.getName();
        exportCode = user.getExportCode();
        initials = user.getInitials();
        sessionPrefix = user.getSessionPrefix();
        authority = user.getAuthority();
        primary = user.isPrimary();
        password = user.getPassword();
        autosave = user.getAutosave();
        feedback = user.getFeedback();
        feedbackQueue = user.isFeedbackQueue();
        feedbackRate = user.getFeedbackRate();
        feedbackPitch = user.getFeedbackPitch();
        active = user.isActive();
        columnWords = new ArrayList<>();
        columns.forEach(column -> {
            final List<ColumnSpoken> spokenList = columnSpoken.stream()
                    .filter(spoken -> spoken.getColumnID() == column.getUid())
                    .collect(Collectors.toList());
            columnWords.add(new ColumnWords(column, spokenList));
        });
        valueWords = new ArrayList<>();
        values.forEach(value -> {
            final List<ColumnValueSpoken> spokenList = valueSpoken.stream()
                    .filter(spoken -> spoken.getColumnValueID() == value.getUid())
                    .collect(Collectors.toList());
            valueWords.add(new ValueWords(value, columns, spokenList));
        });
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExportCode() {
        return exportCode;
    }

    public void setExportCode(String exportCode) {
        this.exportCode = exportCode;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getSessionPrefix() {
        return sessionPrefix;
    }

    public void setSessionPrefix(String sessionPrefix) {
        this.sessionPrefix = sessionPrefix;
    }

    public int getAuthority() {
        return authority;
    }

    public void setAuthority(int authority) {
        this.authority = authority;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAutosave() {
        return autosave;
    }

    public void setAutosave(int autosave) {
        this.autosave = autosave;
    }

    public int getFeedback() {
        return feedback;
    }

    public void setFeedback(int feedback) {
        this.feedback = feedback;
    }

    public boolean isFeedbackQueue() {
        return feedbackQueue;
    }

    public void setFeedbackQueue(boolean feedbackQueue) {
        this.feedbackQueue = feedbackQueue;
    }

    public float getFeedbackRate() {
        return feedbackRate;
    }

    public void setFeedbackRate(float feedbackRate) {
        this.feedbackRate = feedbackRate;
    }

    public float getFeedbackPitch() {
        return feedbackPitch;
    }

    public void setFeedbackPitch(float feedbackPitch) {
        this.feedbackPitch = feedbackPitch;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ColumnWords> getColumnWords() {
        return columnWords;
    }

    public void setColumnWords(List<ColumnWords> columnWords) {
        this.columnWords = columnWords;
    }

    public List<ValueWords> getValueWords() {
        return valueWords;
    }

    public void setValueWords(List<ValueWords> valueWords) {
        this.valueWords = valueWords;
    }
}
