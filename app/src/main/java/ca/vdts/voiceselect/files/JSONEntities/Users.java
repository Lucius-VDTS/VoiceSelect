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
 * JSON version of the user list
 */
public class Users {
    private static final Logger LOG = LoggerFactory.getLogger(Users.class);

    @Expose
    @SerializedName("Users")
    private List<User> users;

    public Users(List<VDTSUser> users,  List<Column> columns, List<ColumnSpoken> columnSpoken,
                 List<ColumnValue> values, List<ColumnValueSpoken> valueSpoken) {
        LOG.debug("Creating Users entity");
        this.users = new ArrayList<>();
        users.forEach(user -> {
            final List<ColumnSpoken> columnSpokenList = columnSpoken.stream()
                    .filter(spoken -> spoken.getUserID() == user.getUid())
                    .collect(Collectors.toList());
            final List<ColumnValueSpoken> valueSpokenList = valueSpoken.stream()
                    .filter(spoken -> spoken.getUserID() == user.getUid())
                    .collect(Collectors.toList());
            this.users.add(
                    new User(
                            user,
                            columns,
                            columnSpokenList,
                            values,
                            valueSpokenList
                    )
            );
        });
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
