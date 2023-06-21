package ca.vdts.voiceselect.files.JSONEntities;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;

public class TotalSession {
    private static final Logger LOG = LoggerFactory.getLogger(TotalSession.class);

    @Expose
    @SerializedName("Header")
    private JSONSession header;

    @Expose
    @SerializedName("Details")
    private List<Detail> details;

    public TotalSession(List<VDTSUser> users, Session totalSession, List<Entry> entries,
                        List<EntryValue> entryValues, List<ColumnValue> columnValues,
                        List<Column> columns) {
        LOG.debug("Creating Session entity");
        this.header = new JSONSession(users, totalSession);
        details = new ArrayList<>();
        entries.sort(Comparator.comparing(Entry::getCreatedDate));
        entries.forEach(entry -> {
            final List<EntryValue> entryValueList = entryValues.stream()
                    .filter(entryValue -> entryValue.getEntryID() == entry.getUid())
                    .collect(Collectors.toList());
            entryValueList.forEach(
                    entryValue -> details.add(
                            new Detail(
                                    entry,
                                    users,
                                    entries.indexOf(entry) + 1,
                                    entryValue,
                                    columnValues,
                                    columns
                            )
                    )
            );
        });
    }

    public JSONSession getHeader() {
        return header;
    }

    public void setHeader(JSONSession header) {
        this.header = header;
    }

    public List<Detail> getDetails() {
        return details;
    }

    public void setDetails(List<Detail> details) {
        this.details = details;
    }
}
