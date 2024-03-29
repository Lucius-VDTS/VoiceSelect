package ca.vdts.voiceselect.library.database.entities;

import static ca.vdts.voiceselect.library.utilities.VDTSBNFUtil.sanitizeGrammar;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import ca.vdts.voiceselect.library.interfaces.VDTSBNFGrammarInterface;
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedInterface;

/**
 * Entity defines the Users table.
 */
@Entity (
        tableName = "Users",
        indices = {
                @Index(value = "uid")
        }
)
public class VDTSUser implements VDTSIndexedNamedInterface, VDTSBNFGrammarInterface {
    @Expose
    @SerializedName("uid")
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "uid")
    private long uid;

    @Expose
    @SerializedName("name")
    @ColumnInfo(name = "name")
    private String name;

    @Expose
    @SerializedName("exportCode")
    @ColumnInfo(name = "exportCode")
    private String exportCode;

    @Expose
    @SerializedName("initials")
    @ColumnInfo(name = "initials")
    private String initials;

    @Expose
    @SerializedName("sessionPrefix")
    @ColumnInfo(name = "sessionPrefix")
    private String sessionPrefix;

    @Expose
    @SerializedName("authority")
    @ColumnInfo(name = "authority")
    private int authority;

    @Expose
    @SerializedName("primary")
    @ColumnInfo(name = "primary")
    private boolean primary;

    @Expose
    @SerializedName("password")
    @ColumnInfo(name = "password")
    private String password;

    @Expose
    @SerializedName("autosave")
    @ColumnInfo(name = "autosave")
    private int autosave;

    @Expose
    @SerializedName("abbreviate")
    @ColumnInfo(name = "abbreviate")
    private boolean abbreviate;


    @Expose
    @SerializedName("feedback")
    @ColumnInfo(name = "feedback")
    private int feedback;

    @Expose
    @SerializedName("feedbackQueue")
    @ColumnInfo(name = "feedbackQueue")
    private boolean feedbackFlushQueue;

    @Expose
    @SerializedName("feedbackRate")
    @ColumnInfo(name = "feedbackRate")
    private float feedbackRate;

    @Expose
    @SerializedName("feedbackPitch")
    @ColumnInfo(name = "feedbackPitch")
    private float feedbackPitch;

    @Expose
    @SerializedName("active")
    @ColumnInfo(name = "active")
    private boolean active;

    //Default super user
    public static final VDTSUser VDTS_USER_NONE = new VDTSUser(
            -9001L,
            "",
            "",
            "",
            "",
            9001,
            false,
            "",
            0,
            false,
            1,
            false,
            1f,
            1f
    );

    //Default Constructor
    public VDTSUser(long uid, String name, String exportCode, String initials, String sessionPrefix,
                    int authority, boolean primary, String password, int autosave,
                    boolean abbreviate, int feedback, boolean feedbackFlushQueue, float feedbackRate,
                    float feedbackPitch) {
        this.uid = uid;
        this.name = name;
        this.exportCode = exportCode;
        this.initials = initials;
        this.sessionPrefix = sessionPrefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.autosave = autosave;
        this.abbreviate = abbreviate;
        this.feedback = feedback;
        this.feedbackFlushQueue = feedbackFlushQueue;
        this.feedbackRate = feedbackRate;
        this.feedbackPitch = feedbackPitch;
        this.active = true;
    }

    //Non-Default Constructor
    @Ignore
    public VDTSUser(long uid, String name, String exportCode, String initials, String sessionPrefix,
                    int authority, boolean primary, String password, int autosave, boolean abbreviate,
                    int feedback, boolean feedbackFlushQueue, float feedbackRate, float feedbackPitch,
                    boolean active) {
        this.uid = uid;
        this.name = name;
        this.exportCode = exportCode;
        this.initials = initials;
        this.sessionPrefix = sessionPrefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.autosave = autosave;
        this.abbreviate = abbreviate;
        this.feedback = feedback;
        this.feedbackFlushQueue = feedbackFlushQueue;
        this.feedbackRate = feedbackRate;
        this.feedbackPitch = feedbackPitch;
        this.active = active;
    }

    //Minimal Constructor - id is 0 until entity is saved
    @Ignore
    public VDTSUser(String name, String exportCode) {
        this.uid = 0L;
        this.name = name;
        this.exportCode = exportCode;
        this.active = true;
    }

    //Copy constructor
    @Ignore
    public VDTSUser(VDTSUser copy) {
        this(
                copy.getUid(),
                copy.getName(),
                copy.getExportCode(),
                copy.getInitials(),
                copy.getSessionPrefix(),
                copy.getAuthority(),
                copy.isPrimary(),
                copy.getPassword(),
                copy.getAutosave(),
                copy.isAbbreviate(),
                copy.getFeedback(),
                copy.isFeedbackFlushQueue(),
                copy.getFeedbackRate(),
                copy.getFeedbackPitch(),
                copy.isActive()
        );
    }

    //Typical Constructor (ex - VoiceSelect) - id is 0 until entity is saved
    @Ignore
    public VDTSUser(String name, String exportCode, String sessionPrefix, int authority,
                    boolean primary, String password) {
        this.uid = 0L;
        this.name = name;
        this.exportCode = exportCode;
        this.sessionPrefix = sessionPrefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.autosave = 0;
        this.feedback = 1;
        this.feedbackFlushQueue = false;
        this.feedbackRate = 1f;
        this.feedbackPitch = 1f;
        this.active = true;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
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

    public boolean isAbbreviate() {
        return abbreviate;
    }

    public void setAbbreviate(boolean abbreviate) {
        this.abbreviate = abbreviate;
    }

    public int getFeedback() {
        return feedback;
    }

    public void setFeedback(int feedback) {
        this.feedback = feedback;
    }

    public boolean isFeedbackFlushQueue() {
        return feedbackFlushQueue;
    }

    public void setFeedbackFlushQueue(boolean feedbackFlushQueue) {
        this.feedbackFlushQueue = feedbackFlushQueue;
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

    @Override
    @Ignore
    public long id() {
        return uid;
    }

    @Override
    @Ignore
    public String name() {
        return name;
    }

    @Override
    public String toGrammar(Context context) {
        return sanitizeGrammar(name, context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VDTSUser)) return false;
        VDTSUser vdtsUser = (VDTSUser) o;
        return (getUid() > 0 && vdtsUser.getUid() > 0 && Objects.equals(getUid(), vdtsUser.getUid())) ||
                Objects.equals(getExportCode(), vdtsUser.getExportCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExportCode());
    }
}
