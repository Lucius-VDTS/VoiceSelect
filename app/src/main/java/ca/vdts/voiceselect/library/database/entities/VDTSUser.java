package ca.vdts.voiceselect.library.database.entities;

import static ca.vdts.voiceselect.library.services.VDTSBnfService.sanitizeGrammar;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import ca.vdts.voiceselect.library.interfaces.VDTSBnfGrammarInterface;
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedEntityInterface;

/**
 * Entity defines the Users table.
 */
@Entity (
        tableName = "Users",
        indices = {
                @Index(value = "uid")
        }
)
public class VDTSUser implements VDTSIndexedNamedEntityInterface, VDTSBnfGrammarInterface {
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
    @SerializedName("code")
    @ColumnInfo(name = "code")
    private String code;

    @Expose
    @SerializedName("initials")
    @ColumnInfo(name = "initials")
    private String initials;

    @Expose
    @SerializedName("prefix")
    @ColumnInfo(name = "prefix")
    private String prefix;

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
    @SerializedName("feedback")
    @ColumnInfo(name = "feedback")
    private int feedback;

    @Expose
    @SerializedName("feedbackQueue")
    @ColumnInfo(name = "feedbackQueue")
    private boolean feedbackQueue;

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
            -9001L, "Super", "", "", "", 9001, true,
            "", 1, false, 1f, 1f);

    //Default Constructor
    public VDTSUser(long uid, String name, String code, String initials, String prefix,
                    int authority, boolean primary, String password, int feedback,
                    boolean feedbackQueue, float feedbackRate, float feedbackPitch) {
        this.uid = uid;
        this.name = name;
        this.code = code;
        this.initials = initials;
        this.prefix = prefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.feedback = feedback;
        this.feedbackQueue = feedbackQueue;
        this.feedbackRate = feedbackRate;
        this.feedbackPitch = feedbackPitch;
        this.active = true;
    }

    //Non-Default Constructor
    @Ignore
    public VDTSUser(long uid, String name, String code, String initials, String prefix,
                    int authority, boolean primary, String password, int feedback,
                    boolean feedbackQueue, float feedbackRate, float feedbackPitch,
                    boolean active) {
        this.uid = uid;
        this.name = name;
        this.code = code;
        this.initials = initials;
        this.prefix = prefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.feedback = feedback;
        this.feedbackQueue = feedbackQueue;
        this.feedbackRate = feedbackRate;
        this.feedbackPitch = feedbackPitch;
        this.active = active;
    }

    //Minimal Constructor - id is 0 until entity is saved
    @Ignore
    public VDTSUser(String name, String code) {
        this.uid = 0L;
        this.name = name;
        this.code = code;
        this.active = true;
    }

    //Copy constructor
    @Ignore
    public VDTSUser(VDTSUser copy) {
        this(
            copy.getUid(), copy.getName(), copy.getCode(), copy.getInitials(), copy.getPrefix(),
            copy.getAuthority(), copy.isPrimary(), copy.getPassword(), copy.getFeedback(),
            copy.isFeedbackQueue(), copy.getFeedbackRate(), copy.getFeedbackPitch(), copy.isActive()
        );
    }

    //Typical Constructor (ex - VoiceSelect) - id is 0 until entity is saved
    @Ignore
    public VDTSUser(String name, String code, String prefix, int authority, boolean primary,
                    String password) {
        this.uid = 0L;
        this.name = name;
        this.code = code;
        this.prefix = prefix;
        this.authority = authority;
        this.primary = primary;
        this.password = password;
        this.feedback = 1;
        this.feedbackQueue = false;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
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
        return Objects.equals(getCode(), vdtsUser.getCode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCode());
    }
}
