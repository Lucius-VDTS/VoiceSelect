package ca.vdts.voiceselect.files.JSONEntities;


import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

public class JSONSession {
    @Expose
    @SerializedName("User Code")
    private String userCode;

    @Expose
    @SerializedName("Name")
    private String name;

    @Expose
    @SerializedName("Start Date")
    private LocalDateTime startDate;

    @Expose
    @SerializedName("End Date")
    private LocalDateTime endDate;

    public JSONSession(List<VDTSUser> users, Session totalSession) {
        userCode = users.stream()
                .filter(user -> user.getUid() == totalSession.getUserID())
                .map(VDTSUser::getExportCode)
                .findFirst()
                .orElse(VDTS_USER_NONE.getExportCode());
        name = totalSession.name();
        startDate = totalSession.getStartDate();
        endDate = totalSession.getEndDate();
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
}
