package ca.vdts.voiceselect.files.JSONEntities;


import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

/**
 * The JSON version of a layout
 */
public class JSONLayout {
    private static final Logger LOG = LoggerFactory.getLogger(JSONLayout.class);

    @Expose
    @SerializedName("User Code")
    private String userCode;

    @Expose
    @SerializedName("Create Date")
    private LocalDateTime createDate;

    @Expose
    @SerializedName("Display Name")
    private String displayName;

    @Expose
    @SerializedName("Export Code")
    private String exportCode;

    @Expose
    @SerializedName("Comment Required")
    private boolean commentRequired;

    @Expose
    @SerializedName("Picture Required")
    private boolean pictureRequired;

    @Expose
    @SerializedName("Layout Columns")
    private List<JSONLayoutColumn> layoutColumns;

    @Expose
    @SerializedName("Active")
    private boolean active;

    public JSONLayout(Layout layout, List<LayoutColumn> layoutColumns, List<Column> columns,
                      List<VDTSUser> users) {
        LOG.debug("Creating JSONColumn entity for layout {}", layout.getUid());
        userCode = users.stream()
                .filter(user -> user.getUid() == layout.getUserID())
                .map(VDTSUser::getExportCode)
                .findFirst()
                .orElse(VDTS_USER_NONE.getExportCode());
        createDate = layout.getCreatedDate();
        displayName = layout.getName();
        exportCode = layout.getExportCode();
        commentRequired = layout.isCommentRequired();
        pictureRequired = layout.isPictureRequired();
        active = layout.isActive();

        this.layoutColumns = new ArrayList<>();
        layoutColumns.stream()
                .filter(lc -> lc.getLayoutID() == layout.getUid())
                .forEach(
                        lc -> {
                            List<Column> col = columns.stream()
                                    .filter(c -> c.getUid() == lc.getColumnID())
                                    .collect(Collectors.toList());
                            if (col.size() > 0) {
                                this.layoutColumns.add(
                                        new JSONLayoutColumn(
                                                col.get(0).getExportCode(),
                                                lc.getColumnPosition()
                                        )
                                );
                            }
                        }
                );
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getExportCode() {
        return exportCode;
    }

    public void setExportCode(String exportCode) {
        this.exportCode = exportCode;
    }

    public boolean isCommentRequired() {
        return commentRequired;
    }

    public void setCommentRequired(boolean commentRequired) {
        this.commentRequired = commentRequired;
    }

    public boolean isPictureRequired() {
        return pictureRequired;
    }

    public void setPictureRequired(boolean pictureRequired) {
        this.pictureRequired = pictureRequired;
    }

    public List<JSONLayoutColumn> getLayoutColumns() {
        return layoutColumns;
    }

    public void setLayoutColumns(List<JSONLayoutColumn> layoutColumns) {
        this.layoutColumns = layoutColumns;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
