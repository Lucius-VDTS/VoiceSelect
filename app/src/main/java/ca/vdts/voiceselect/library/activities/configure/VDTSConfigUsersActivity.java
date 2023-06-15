package ca.vdts.voiceselect.library.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

/**
 * Configure the application's users.
 */
public class VDTSConfigUsersActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSConfigUsersActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;

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
    private final List<VDTSUser> selectableUserList = new ArrayList<>();
    private final List<VDTSUser> allUserList = new ArrayList<>();

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private VDTSConfigUsersActivity.IristickHUD iristickHUD;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_users);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

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
        userNameEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userNameEditText);
                vdtsApplication.displayToast(
                        this,
                        "Must be an admin user to modify",
                        Toast.LENGTH_SHORT
                );
                userNameEditText.clearFocus();
            }
        });

        userPrefixEditText = findViewById(R.id.userPrefixEditText);
        userPrefixEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userPrefixEditText);
                vdtsApplication.displayToast(
                        this,
                        "Must be an admin user to modify",
                        Toast.LENGTH_SHORT
                );
                userPrefixEditText.clearFocus();
            }
        });

        userExportCodeEditText = findViewById(R.id.userExportCodeEditText);
        userExportCodeEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && currentUser.getAuthority() < 1) {
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userExportCodeEditText);
                vdtsApplication.displayToast(
                        this,
                        "Must be an admin user to modify",
                        Toast.LENGTH_SHORT
                );
                userExportCodeEditText.clearFocus();
            }
        });

        userPasswordEditText = findViewById(R.id.userPasswordEditText);
        /*userPasswordEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !userAdminSwitch.isChecked()) {
                vdtsApplication.displayToast(
                        vdtsApplication,
                        "User must be an admin to set password",
                        0);

                userPasswordEditText.clearFocus();
            } else {
                userPasswordEditText.setInputType(1);
            }
        });*/

        userAdminSwitch = findViewById(R.id.userAdminSwitch);
        userAdminSwitch.setOnClickListener(v -> {
            if (currentUser.getAuthority() < 1) {
                userAdminSwitch.setChecked(false);
                userAdminSwitch.clearFocus();
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userAdminSwitch);
                vdtsApplication.displayToast(
                        this,
                        "Must be an admin user to modify",
                        Toast.LENGTH_SHORT
                );
            }
        });
        /*userAdminSwitch.setOnClickListener(v -> {
            if (!userAdminSwitch.isChecked()) {
                userPasswordEditText.setInputType(0);
                userPasswordEditText.setText("");
                userPasswordEditText.clearFocus();
            } else {
                userPasswordEditText.setInputType(1);
                userPasswordEditText.requestFocus();
            }
        });*/

        userPrimarySwitch = findViewById(R.id.userPrimarySwitch);
        userPrimarySwitch.setOnClickListener(v -> {
            if (currentUser.getAuthority() < 1) {
                userPrimarySwitch.setChecked(false);
                userPrimarySwitch.clearFocus();
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userPrimarySwitch);
                vdtsApplication.displayToast(
                        this,
                        "Only an admin user can change the default spoken value",
                        Toast.LENGTH_SHORT
                );
            }
        });

        importButton = findViewById(R.id.userImportButton);
        importButton.setOnClickListener(v -> importButtonOnClick());

        exportButton = findViewById(R.id.userExportButton);
        exportButton.setOnClickListener(v -> exportButtonClick());

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        //Recyclerview
        userRecyclerView = findViewById(R.id.userRecyclerView);

        //Observe/Update user list
        vsViewModel.findAllActiveUsersLive().observe(this, users -> {
            allUserList.clear();
            allUserList.addAll(users);
            allUserList.remove(VDTSUser.VDTS_USER_NONE);

            if (currentUser.getAuthority() > 0) {
                selectableUserList.clear();
                selectableUserList.addAll(allUserList);
                userAdapter.setDataset(selectableUserList);
            }
        });

        if (currentUser.getAuthority() == 0) {
            selectableUserList.clear();
            selectableUserList.add(currentUser);
        }

        userRecyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this,
                        LinearLayoutManager.VERTICAL,
                        false
                )
        );

        userAdapter = new VDTSIndexedNamedAdapter<>(
                this,
                new VDTSClickListenerUtil(this::userAdapterSelect, userRecyclerView),
                selectableUserList
        );

        userRecyclerView.setAdapter(userAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //initializeUserList();
    }

    private void initializeUserList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            selectableUserList.clear();
            if (currentUser.getAuthority() > 0) {
                selectableUserList.addAll(vsViewModel.findAllActiveUsers());
                selectableUserList.remove(VDTSUser.VDTS_USER_NONE);
            } else {
                selectableUserList.add(currentUser);
            }
            handler.post(() -> userAdapter.setDataset(selectableUserList));
        });
    }

    /**
     * Select the appropriate user from the recycler view.
     * @param index - Index of the user to select.
     */
    private void userAdapterSelect(Integer index) {
        userAdapter.setSelectedEntity(index);
        if (index >= 0) {
            VDTSUser selectedUser = userAdapter.getSelectedEntity();
            if (selectedUser != null) {
                final boolean isAdmin = selectedUser.getAuthority() >= 1;
                final boolean isPrimary = selectedUser.isPrimary();

                userNameEditText.setText(selectedUser.getName());
                userPrefixEditText.setText(selectedUser.getSessionPrefix());
                userExportCodeEditText.setText(selectedUser.getExportCode());
                userPasswordEditText.setText(
                        selectedUser.getPassword() != null ? selectedUser.getPassword() : ""
                );
                userAdminSwitch.setChecked(isAdmin);
                userPrimarySwitch.setChecked(isPrimary);
            } else {
                userNameEditText.setText("");
                userPrefixEditText.setText("");
                userExportCodeEditText.setText("");
                userPasswordEditText.setText("");
                userAdminSwitch.setChecked(false);
                userPrimarySwitch.setChecked(false);
            }
        } else {
            userNameEditText.setText("");
            userPrefixEditText.setText("");
            userExportCodeEditText.setText("");
            userPasswordEditText.setText("");
            userAdminSwitch.setChecked(false);
            userPrimarySwitch.setChecked(false);
        }
    }

    public void newUserButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            userAdapterSelect(-1);
            userNameEditText.requestFocus();
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(newUserButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create a new user",
                    Toast.LENGTH_SHORT
            );
        }
    }

    public void resetUserButtonOnClick() {
        userAdapterSelect(userAdapter.getSelectedEntityIndex());
        //userNameEditText.requestFocus();
    }

    public void saveUserButtonOnClick() {
        VDTSUser selectedUser = userAdapter.getSelectedEntity();
        boolean isPrimary = userPrimarySwitch.isChecked();
        final VDTSUser primaryUser = allUserList.stream()
                .filter(VDTSUser::isPrimary)
                .findFirst()
                .orElse(null);

        if (adminPrimaryCheck()) {
            if (selectedUser != null) {
                //Update existing user
                final VDTSUser user = new VDTSUser(selectedUser);
                user.setName(userNameEditText.getText().toString().trim());
                user.setSessionPrefix(userPrefixEditText.getText().toString().trim());
                user.setExportCode(userExportCodeEditText.getText().toString().trim());
                user.setAuthority(userAdminSwitch.isChecked() ? 1 : 0);
                user.setPrimary(userPrimarySwitch.isChecked());

                /*if (!userAdminSwitch.isChecked()) {
                    selectedUser.setPassword("");
                } else {*/
                    final String password = userPasswordEditText.getText().toString().trim();
                    user.setPassword(!password.isEmpty() ? password : null);
                //}

                if (isValidUser(user)) {
                    if (user.getUid() == vdtsApplication.getCurrentUser().getUid()) {
                        vdtsApplication.setCurrentUser(user);
                    }

                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        if (isPrimary &&
                                primaryUser != null &&
                                user.getUid() != primaryUser.getUid()) {
                            primaryUser.setPrimary(false);
                            vsViewModel.updateUser(primaryUser);
                        }

                        vsViewModel.updateUser(user);
                        LOG.info("Updated user: {}", user.getName());
                        vdtsApplication.displayToast(
                                this,
                                "Updated user: " + user.getName(),
                                Toast.LENGTH_SHORT
                        );

                        handler.post(() -> userAdapter.updateSelectedEntity());
                    });
                } else {
                    LOG.error("Unable to update user");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(saveUserButton);
                    vdtsApplication.displayToast(
                            this,
                            "Unable to update user",
                            Toast.LENGTH_SHORT
                    );
                }
            } else {
                //Create new user
                final String password = userPasswordEditText.getText().toString().trim();
                final VDTSUser vdtsUser = new VDTSUser(
                        userNameEditText.getText().toString().trim(),
                        userExportCodeEditText.getText().toString().trim(),
                        userPrefixEditText.getText().toString().trim(),
                        userAdminSwitch.isChecked() ? 1 : 0,
                        userPrimarySwitch.isChecked(),
                        !password.isEmpty() ? password : null
                );

                if (!userAdminSwitch.isChecked()) {
                    vdtsUser.setPassword("");
                }

                if (isValidUser(vdtsUser)) {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        final long userID = vsViewModel.insertUser(vdtsUser);
                        vdtsUser.setUid(userID);
                        LOG.info("Added user: {}", vdtsUser.getName());

                        if (!isPrimary && primaryUser != null) {
                            final List<ColumnSpoken> primaryColumnSpokens = vsViewModel
                                    .findAllColumnSpokensByUser(primaryUser.getUid()
                            );
                            primaryColumnSpokens.forEach(columnSpoken -> {
                                final ColumnSpoken userColumnSpoken = new ColumnSpoken(
                                        userID,
                                        columnSpoken.getColumnID(),
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
                                                userID,
                                                columnValueSpoken.getColumnValueID(),
                                                columnValueSpoken.getSpoken()
                                        );
                                vsViewModel.insertColumnValueSpoken(userColumnValueSpoken);
                            });
                        } else if (isPrimary && primaryUser != null) {
                            primaryUser.setPrimary(false);
                            vsViewModel.updateUser(primaryUser);
                        }

                        handler.post(() -> {
                            if (primaryUser != null) { userAdapter.updateEntity(primaryUser); }

                            userAdapter.addEntity(vdtsUser);

                            if (userAdapter.getItemCount() == 1) {
                                vdtsApplication.setCurrentUser(userAdapter.getEntity(0));
                            }
                        });
                    });

                    newUserButtonOnClick();
                } else {
                    LOG.error("User does not meet requirements");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(saveUserButton);
                    vdtsApplication.displayToast(
                            this,
                            "User does not meet requirements",
                            Toast.LENGTH_SHORT
                    );
                }
            }
        } else {
            LOG.error("Save user failed - admin/default spoken must exist");
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(saveUserButton);
            vdtsApplication.displayToast(
                    this,
                    "Save user failed - admin/default spoken must exist",
                    Toast.LENGTH_SHORT
            );

            resetUserButtonOnClick();
        }
    }

    public void deleteUserButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            VDTSUser selectedUser = userAdapter.getSelectedEntity();
            if (selectedUser != null) {
                if (!adminPrimaryCheck() || !userPrimarySwitch.isChecked()) {
                    selectedUser.setActive(false);
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Handler handler = new Handler(Looper.getMainLooper());
                    executor.execute(() -> {
                        vsViewModel.updateUser(selectedUser);
                        handler.post(() -> {
                            userAdapter.removeSelectedEntity();
                            newUserButtonOnClick();
                            vdtsApplication.setUserCount(userAdapter.getItemCount());
                        });
                    });
                } else {
                    LOG.info("Delete user failed - admin/default spoken must exist");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(deleteUserButton);
                    vdtsApplication.displayToast(
                            this,
                            "Delete user failed - admin/default spoken must exist",
                            Toast.LENGTH_SHORT
                    );

                    resetUserButtonOnClick();
                }
            }
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(deleteUserButton);
            vdtsApplication.displayToast(
                    vdtsApplication,
                    "Only an admin user can delete a user",
                    Toast.LENGTH_SHORT
            );
        }
    }

    public void importButtonOnClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(importButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can import users",
                    Toast.LENGTH_SHORT
            );
        }
    }

    public void exportButtonClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(exportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export users",
                    Toast.LENGTH_SHORT
            );
        }
    }

    /**
     * Check if an admin and primary user exists.
     * @return - True if a user with admin and primary permissions exists.
     */
    public boolean adminPrimaryCheck() {
        VDTSUser selectedUser = userAdapter.getSelectedEntity();
        boolean adminExists = false;
        boolean primaryExists = false;

        if (userAdminSwitch.isChecked()) {
            adminExists = true;
        } else if (allUserList.size() > 0) {
            adminExists = allUserList.stream()
                    .filter(user -> !user.equals(selectedUser))
                    .anyMatch(user -> user.getAuthority() == 1);
        }

        if (userPrimarySwitch.isChecked()) {
            primaryExists = true;
        } else if (allUserList.size() > 0) {
            primaryExists = allUserList.stream()
                    .filter(user -> !user.equals(selectedUser))
                    .anyMatch(VDTSUser::isPrimary);
        }

        return adminExists && primaryExists;
    }

    /**
     * Check if vdtsUser has a name, code, and does not already exist.
     * @param vdtsUser - The vdtsUser to be checked.
     * @return - True if the vdtsUser is valid.
     */
    private boolean isValidUser(VDTSUser vdtsUser) {
        if (userAdminSwitch.isChecked()) {
            return !vdtsUser.getName().isEmpty() &&
                    !vdtsUser.getExportCode().isEmpty() &&
                    //!vdtsUser.getPassword().isEmpty() &&
                    allUserList.stream()
                            .noneMatch(
                                    user1 -> vdtsUser.getUid() != user1.getUid() && (
                                            user1.getName().equalsIgnoreCase(vdtsUser.getName()) ||
                                                    user1.getExportCode().equalsIgnoreCase(
                                                            vdtsUser.getExportCode()
                                                    )
                                    )
                            );
        } else {
            return !vdtsUser.getName().isEmpty() &&
                    !vdtsUser.getExportCode().isEmpty() &&
                    allUserList.stream()
                            .noneMatch(
                                    user1 -> vdtsUser.getUid() != user1.getUid() && (
                                            user1.getName().equalsIgnoreCase(vdtsUser.getName()) ||
                                                    user1.getExportCode().equalsIgnoreCase(
                                                            vdtsUser.getExportCode()
                                                    )
                                    )
                            );
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
     * Initialize elements based on Iristick connection.
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
            setContentView(R.layout.activity_config_hud);

            configOnDeviceText = findViewById(R.id.configHUDText);
            assert configOnDeviceText != null;
            configOnDeviceText.setText(R.string.config_hud_text);
        }
    }
}
