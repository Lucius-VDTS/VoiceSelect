package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.Layout;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.database.entities.SessionLayout;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;


public class JSONColumnLayout {
    private static final Logger LOG = LoggerFactory.getLogger(JSONColumnLayout.class);

    @Expose
    @SerializedName("Layouts")
    private List<JSONLayout> layouts;
    
    public JSONColumnLayout(List<Layout> layouts, List<LayoutColumn> layoutColumns, List<Column> columns, List<VDTSUser> users) {
        LOG.debug("Creating ColumnLayout entity");
        this.layouts = new ArrayList<>();

        for (Layout layout : layouts){
            JSONLayout l = new JSONLayout(layout, layoutColumns,columns, users);
            this.layouts.add(l);
        }
    }

    public List<JSONLayout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<JSONLayout> layouts) {
        this.layouts = layouts;
    }
}
