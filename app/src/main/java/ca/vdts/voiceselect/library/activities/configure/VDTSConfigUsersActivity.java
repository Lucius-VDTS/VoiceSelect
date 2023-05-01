package ca.vdts.voiceselect.library.activities.configure;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.services.VDTSClickListenerService;

/**
 * Configure the application's users
 */
public class VDTSConfigUsersActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigUsersActivity.class);

    private VDTSApplication vdtsApplication;

    //Views
    private Button newUserButton;
    private Button resetUserButton;
    private Button saveUserButton;
    private Button deleteUserButton;

    private EditText userNameEditText;
    private EditText userPrefixEditText;
    private EditText userExportCodeEditText;

    private EditText userPasswordEditText;
    private SwitchCompat userAdminSwitch;
    private SwitchCompat userPrimarySwitch;

    private Button importButton;
    private Button exportButton;

    //Recycler View
    private VSViewModel vsViewModel;
    private VDTSIndexedNamedAdapter<VDTSUser> userAdapter;
    private RecyclerView userRecyclerView;
    private final List<VDTSUser> userList = new ArrayList<>();

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private VDTSConfigUsersActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_users);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();

        //Views
        newUserButton = findViewById(R.id.userNewButton);
        newUserButton.setOnClickListener(v -> newUserButtonOnClick());

        resetUserButton = findViewById(R.id.userResetButton);
        resetUserButton.setOnClickListener(v -> resetUserButtonOnClick());

        saveUserButton = findViewById(R.id.userSaveButton);
        saveUserButton.setOnClickListener(v -> saveUserButtonOnClick());

        deleteUserButton = findViewById(R.id.userDeleteButton);
        deleteUserButton.setOnClickListener(v -> deleteUserButtonOnClick());

        userNameEditText = findViewById(R.id.userNameEditText);
        userPrefixEditText = findViewById(R.id.userPrefixEditText);
        userExportCodeEditText = findViewById(R.id.userExportCodeEditText);
        userPasswordEditText = findViewById(R.id.userPasswordEditText);

        userPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !userAdminSwitch.isChecked()) {
                vdtsApplication.displayToast(
                        vdtsApplication,
                        "User must be an admin to set password",
                        0);
                userPasswordEditText.clearFocus();
            }
        });

        userAdminSwitch = findViewById(R.id.userAdminSwitch);
        userAdminSwitch.setOnClickListener(v -> {
            if (!userAdminSwitch.isChecked()) {
                userPasswordEditText.clearFocus();
                userPasswordEditText.setText("");
            } else {
                userPasswordEditText.requestFocus();
            }
        });

        userPrimarySwitch = findViewById(R.id.userPrimarySwitch);

        importButton = findViewById(R.id.userImportButton);
        exportButton = findViewById(R.id.userExportButton);

        //Recyclerview
        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        userRecyclerView = findViewById(R.id.userRecyclerView);
        userRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                ));

        userAdapter = new VDTSIndexedNamedAdapter<>(
                new VDTSClickListenerService(this::userAdapterSelect, userRecyclerView),
                this,
                userList
        );

        userRecyclerView.setAdapter(userAdapter);

        //Update recycler view once user list has been generated
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            userList.addAll(vsViewModel.findAllActiveUsers());
            userList.remove(VDTSUser.VDTS_USER_NONE);
            handler.post(() -> userAdapter.setDataset(userList));
        });
    }

    public void newUserButtonOnClick() {
        userAdapterSelect(-1);
        userNameEditText.requestFocus();
    }

    public void resetUserButtonOnClick() {
        userAdapterSelect(userAdapter.getSelectedIndex());
        userNameEditText.requestFocus();
    }

    public void saveUserButtonOnClick() {
        VDTSUser selectedUser = userAdapter.getSelectedEntity();
        if (adminPrimaryCheck()) {
            if (selectedUser != null) {
                //Update existing user
                selectedUser.setName(userNameEditText.getText().toString().trim());
                selectedUser.setPrefix(userPrefixEditText.getText().toString().trim());
                selectedUser.setCode(userExportCodeEditText.getText().toString().trim());
                selectedUser.setPassword(userPasswordEditText.getText().toString().trim());
                selectedUser.setAuthority(userAdminSwitch.isChecked() ? 1 : 0);
                selectedUser.setPrimary(userPrimarySwitch.isChecked());

                if (isValidUser(selectedUser)) {
                    userAdapter.updateSelectedEntity();
                    if (selectedUser.getUid() ==  vdtsApplication.getCurrentUser().getUid()) {
                        vdtsApplication.setCurrentUser(selectedUser);
                    }

                    if (userPrimarySwitch.isChecked()) {
                        clearPrimaries();
                        LOG.info("VDTSUser Primary: {}", selectedUser.getName());
                        selectedUser.setPrimary(true);
                    } else {
                        setOldestAsPrimary();
                    }

                    new Thread(() -> {
                        vsViewModel.updateUser(selectedUser);
                        LOG.info("Updated VDTSUser: {}", selectedUser);
                        vdtsApplication.displayToast(
                                this,
                                "Updated VDTSUser: " + selectedUser,
                                0);
                    }).start();
                } else {
                    LOG.error("Unable to update user");
                    vdtsApplication.displayToast(
                            this, "Unable to update user", 0);
                }
            } else {
                //Create new user
                final VDTSUser vdtsUser = new VDTSUser(
                        userNameEditText.getText().toString().trim(),
                        userExportCodeEditText.getText().toString().trim(),
                        userPrefixEditText.getText().toString().trim(),
                        userAdminSwitch.isChecked() ? 1 : 0,
                        userPrimarySwitch.isChecked(),
                        userPasswordEditText.getText().toString().trim()
                );

                if (isValidUser(vdtsUser)) {
                    boolean isPrimary = userPrimarySwitch.isChecked();
                    userAdapter.add(vdtsUser);

                    new Thread(() -> {
                        final long userId = vsViewModel.insertUser(vdtsUser);
                        vdtsUser.setUid(userId);
                        LOG.info("Added vdtsUser: {}", vdtsUser);

                        final VDTSUser primaryUser = userList.stream()
                                .filter(VDTSUser::isPrimary)
                                .findFirst()
                                .orElse(null);

                        if (!isPrimary && primaryUser != null) {
                            final List<ColumnSpoken> primaryColumnSpokens = vsViewModel
                                    .findAllColumnSpokensByUser(primaryUser.getUid()
                            );
                            primaryColumnSpokens.forEach(columnSpoken -> {
                                final ColumnSpoken userColumnSpoken = new ColumnSpoken(
                                        userId,
                                        columnSpoken.getColumnId(),
                                        columnSpoken.getSpoken()
                                );
                                vsViewModel.insertColumnSpoken(userColumnSpoken);
                            });

                            final List<ColumnValueSpoken> primaryColumnValueSpokens = vsViewModel
                                    .findAllColumnValueSpokensByUser(primaryUser.getUid()
                            );
                            primaryColumnValueSpokens.forEach(columnValueSpoken -> {
                                final ColumnValueSpoken userColumnValueSpoken =
                                        new ColumnValueSpoken(
                                                userId,
                                                columnValueSpoken.getColumnValueId(),
                                                columnValueSpoken.getSpoken()
                                );
                                vsViewModel.insert(userColumnValueSpoken);
                            });
                        } else if (isPrimary && primaryUser != null) {
                            primaryUser.setPrimary(false);
                            vsViewModel.updateUser(primaryUser);
                        }
                    }).start();
                } else {
                    LOG.error("vdtsUser does not meet requirements");
                    vdtsApplication.displayToast(
                            this, "vdtsUser does not meet requirements", 0);
                }
            }

            if (userAdapter.getItemCount() == 1) {
                vdtsApplication.setCurrentUser(userAdapter.getEntity(0));
            } else if (userAdapter.getItemCount() == 0) {
                vdtsApplication.setCurrentUser(VDTSUser.VDTS_USER_NONE);
            }

            userAdapterSelect(-1);
        } else {
            LOG.error("Save user failed - admin/primary user must exist");
            vdtsApplication.displayToast(
                    this,
                    "Save user failed - admin/primary user must exist",
                    0);
        }
    }

    public void deleteUserButtonOnClick() {
        VDTSUser selectedUser = userAdapter.getSelectedEntity();
        if (adminPrimaryCheck()) {
            selectedUser.setActive(false);
            new Thread(() -> vsViewModel.updateUser(selectedUser)).start();
            userAdapter.removeSelectedEntity();
            userAdapterSelect(-1);
            vdtsApplication.setUserCount(userAdapter.getItemCount());
        } else {
            LOG.error("Delete selectedUser failed - admin/primary selectedUser must exist");
            vdtsApplication.displayToast(
                    this,
                    "Delete selectedUser failed - admin/primary selectedUser must exist",
                    0);

            userAdapterSelect(userAdapter.getSelectedIndex());
        }
    }

    /**
     * Select the appropriate user from the recycler view
     * @param index - Index of the user to select
     */
    private void userAdapterSelect(Integer index) {
        userAdapter.setSelectedEntity(index);
        if (index >= 0) {
            VDTSUser selectedUser = userAdapter.getSelectedEntity();
            if (selectedUser != null) {
                final boolean isAdmin = selectedUser.getAuthority() >= 1;
                final boolean isPrimary = selectedUser.isPrimary();

                userNameEditText.setText(selectedUser.getName());
                userPrefixEditText.setText(selectedUser.getPrefix());
                userExportCodeEditText.setText(selectedUser.getCode());
                userPasswordEditText.setText(selectedUser.getPassword());
                userPasswordEditText.setEnabled(isAdmin);
                userAdminSwitch.setChecked(isAdmin);
                userPrimarySwitch.setChecked(isPrimary);
            } else {
                userNameEditText.setText("");
                userPrefixEditText.setText("");
                userExportCodeEditText.setText("");
                userPasswordEditText.setText("");
                userPasswordEditText.setEnabled(false);
                userAdminSwitch.setChecked(false);
                userPrimarySwitch.setChecked(false);
            }
        } else {
            userNameEditText.setText("");
            userPrefixEditText.setText("");
            userExportCodeEditText.setText("");
            userPasswordEditText.setText("");
            userPasswordEditText.setEnabled(false);
            userAdminSwitch.setChecked(false);
            userPrimarySwitch.setChecked(false);
        }
    }

    /**
     * Check if an admin and primary user exists
     * @return - True if a user with admin and primary permissions exists
     */
    public boolean adminPrimaryCheck() {
        VDTSUser selectedUser = userAdapter.getSelectedEntity();
        boolean adminExists = false;
        boolean primaryExists = false;

        if (userAdminSwitch.isChecked()) {
            adminExists = true;
        } else if (userList.size() > 0) {
            adminExists = userList.stream()
                    .filter(user -> !user.equals(selectedUser))
                    .anyMatch(user -> user.getAuthority() == 1);
        }

        if (userPrimarySwitch.isChecked()) {
            primaryExists = true;
        } else if (userList.size() > 0) {
            primaryExists = userList.stream()
                    .filter(user -> !user.equals(selectedUser))
                    .anyMatch(VDTSUser::isPrimary);
        }

        return adminExists && primaryExists;
    }

    /**
     * Check if vdtsUser has a name, code, and does not already exist
     * @param vdtsUser - The vdtsUser to be checked
     * @return - True if the vdtsUser is valid
     */
    private boolean isValidUser(VDTSUser vdtsUser) {
        try {
            new Thread(() -> userList.addAll(vsViewModel.findAllUsers())).join();
        } catch (InterruptedException e) {
            LOG.error("Getting all users interrupted: ", e);
        }

        return !vdtsUser.getName().isEmpty() &&
               !vdtsUser.getCode().isEmpty() &&
               userList.stream().noneMatch(user1 -> vdtsUser.getUid() != user1.getUid() &&
                    (StringUtils.lowerCase(user1.getName())
                            .equals(StringUtils.lowerCase(vdtsUser.getName())) ||
                                    StringUtils.lowerCase(user1.getCode())
                                            .equals(StringUtils.lowerCase(vdtsUser.getCode()))));

    }

    /**
     * Set all user's primary field to false
     */
    private void clearPrimaries() {
        for (VDTSUser user : userList) {
            user.setPrimary(false);
        }
    }

    /**
     * Set oldest user as the primary if none exist
     */
    private void setOldestAsPrimary() {
        if (userList.stream().noneMatch(VDTSUser::isPrimary)) {
            VDTSUser oldestPrimary = userList.stream()
                    .min(Comparator.comparing(VDTSUser::getUid))
                    .orElse(null);
            if (oldestPrimary != null) {
                oldestPrimary.setPrimary(true);
                vsViewModel.updateUser(oldestPrimary);
            }
        }
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
        initializeIristick();
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
        initializeIristick();
    }

    /**
     * Initialize elements based on Iristick connection
     */
    private void initializeIristick() {
        if (isHeadsetAvailable) {
            IristickSDK.addWindow(this.getLifecycle(), () -> {
                iristickHUD = new VDTSConfigUsersActivity.IristickHUD();
                return iristickHUD;
            });
        }
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        private TextView configOnDeviceText;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_config_on_device_hud);

            configOnDeviceText = findViewById(R.id.configOnDeviceText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_on_device_text);
        }
    }
}
