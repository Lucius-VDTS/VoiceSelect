package ca.vdts.voiceselect.library.activities.configure;

import static ca.vdts.voiceselect.library.VDTSApplication.SELECT_FOLDER;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;
import static ca.vdts.voiceselect.library.database.entities.VDTSUser.VDTS_USER_NONE;
import static ca.vdts.voiceselect.library.utilities.VDTSToolUtil.showKeyboard;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
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

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.ColumnSpoken;
import ca.vdts.voiceselect.database.entities.ColumnValueSpoken;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.files.FileUtil;
import ca.vdts.voiceselect.files.Importer;
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

    private EditText userPINEditText;
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
    private VDTSConfigUsersActivity.IristickHUD iristickHUD;

    //Prevents asynchronous filling issues
    private ReentrantLock adapterLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config_users);

        IristickSDK.registerListener(this.getLifecycle(), this);

        vdtsApplication = (VDTSApplication) this.getApplication();
        currentUser = vdtsApplication.getCurrentUser();

        adapterLock = new ReentrantLock();

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
                        "Must be an admin user to modify"
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
                        "Must be an admin user to modify"
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
                        "Must be an admin user to modify"
                );
                userExportCodeEditText.clearFocus();
            }
        });

        userPINEditText = findViewById(R.id.userPINEditText);

        userAdminSwitch = findViewById(R.id.userFeedbackSwitch);
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
                        "Must be an admin user to modify"
                );
            }
        });

        userPrimarySwitch = findViewById(R.id.userFlushSwitch);
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
                        "Only an admin user can change the default spoken value"
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
//        vsViewModel.findAllActiveUsersLive().observe(this, users -> {
//            allUserList.clear();
//            allUserList.addAll(users);
//            allUserList.remove(VDTS_USER_NONE);
//
//            if (currentUser.getAuthority() > 0) {
//                selectableUserList.clear();
//                selectableUserList.addAll(allUserList);
//                userAdapter.setDataset(selectableUserList);
//            }
//        });
//
//        if (currentUser.getAuthority() == 0) {
//            selectableUserList.clear();
//            selectableUserList.add(currentUser);
//        }

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
        initializeUserList();
    }

    private void initializeUserList() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            adapterLock.lock();
            allUserList.clear();
            allUserList.addAll(vsViewModel.findAllActiveUsers());
            allUserList.remove(VDTS_USER_NONE);

            selectableUserList.clear();
            if (currentUser.getAuthority() > 0) {
                selectableUserList.addAll(allUserList);
            } else {
                selectableUserList.add(currentUser);
            }
            handler.post(() -> userAdapter.setDataset(selectableUserList));
            adapterLock.unlock();
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
                userPINEditText.setText(
                        selectedUser.getPassword() != null ? selectedUser.getPassword() : ""
                );
                userAdminSwitch.setChecked(isAdmin);
                userPrimarySwitch.setChecked(isPrimary);
            } else {
                userNameEditText.setText("");
                userPrefixEditText.setText("");
                userExportCodeEditText.setText("");
                userPINEditText.setText("");
                userAdminSwitch.setChecked(false);
                userPrimarySwitch.setChecked(false);
            }
        } else {
            userNameEditText.setText("");
            userPrefixEditText.setText("");
            userExportCodeEditText.setText("");
            userPINEditText.setText("");
            userAdminSwitch.setChecked(false);
            userPrimarySwitch.setChecked(false);
        }
    }

    public void newUserButtonOnClick() {
        if (currentUser.getAuthority() > 0) {
            userAdapterSelect(-1);
            userNameEditText.requestFocus();
            showKeyboard(userNameEditText,this);
        } else {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(newUserButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can create a new user"
            );
        }
    }

    public void resetUserButtonOnClick() {
        userAdapterSelect(userAdapter.getSelectedEntityIndex());
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

                final String pin = userPINEditText.getText().toString().trim();
                user.setPassword(!pin.isEmpty() ? pin : null);

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
                                "Updated user: " + user.getName()
                        );

                        handler.post(() -> userAdapter.updateEntity(user));
                    });
                } else {
                    LOG.error("Unable to update user");
                    YoYo.with(Techniques.Shake)
                            .duration(SHAKE_DURATION)
                            .repeat(SHAKE_REPEAT)
                            .playOn(saveUserButton);
                    vdtsApplication.displayToast(
                            this,
                            "Unable to update user"
                    );
                }
            } else {
                //Create new user
                final String pin = userPINEditText.getText().toString().trim();
                final VDTSUser vdtsUser = new VDTSUser(
                        userNameEditText.getText().toString().trim(),
                        userExportCodeEditText.getText().toString().trim(),
                        userPrefixEditText.getText().toString().trim(),
                        userAdminSwitch.isChecked() ? 1 : 0,
                        userPrimarySwitch.isChecked(),
                        !pin.isEmpty() ? pin : null
                );

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

                            final List<ColumnValueSpoken> primaryColumnValueSpokens =
                                    vsViewModel.findAllColumnValueSpokensByUser(primaryUser.getUid()
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
                            "User does not meet requirements"
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
                    "Save user failed - admin/default spoken must exist"
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
                            "Delete user failed - admin/default spoken must exist"
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
                    "Only an admin user can delete a user"
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
                    "Only an admin user can import users"
            );
        }else {
            showImportDialog();
        }
    }

    private void showImportDialog() {
        LOG.info("Showing Choice Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Import Users");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_yes_no,
                null
        );
        builder.setView(customLayout);

        TextView label = customLayout.findViewById(R.id.mainLabel);
        label.setText(R.string.import_dialogue_label);

        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        yesButton.setOnClickListener(v -> {
            finalDialog.dismiss();
            openFilePicker();
        });

        noButton.setOnClickListener(v -> finalDialog.dismiss());
    }

    public void exportButtonClick() {
        if (currentUser.getAuthority() < 1) {
            YoYo.with(Techniques.Shake)
                    .duration(SHAKE_DURATION)
                    .repeat(SHAKE_REPEAT)
                    .playOn(exportButton);
            vdtsApplication.displayToast(
                    this,
                    "Only an admin user can export users"
            );
        } else {
            final Exporter exporter = new Exporter(
                    vsViewModel,
                    vdtsApplication,
                    this
                    //saver
            );
            if (exporter.exportUsers()) {
                vdtsApplication.displayToast(
                        this,
                        "Users exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting users"
                );
            }
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

    public void updateCurrentUser(){
        if (vdtsApplication.getCurrentUser() != null &&
                vdtsApplication.getCurrentUser() != VDTS_USER_NONE) {
            Thread thread = new Thread(() -> vdtsApplication.setCurrentUser(
                    vsViewModel.findUserByID(vdtsApplication.getCurrentUser().getUid()))
            );
            thread.start();
        }
    }

    public void openFilePicker() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            startActivityForResult(intent, SELECT_FOLDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SELECT_FOLDER) {
            if (resultCode == RESULT_OK && data != null) {

                Uri originalUri = data.getData();

                try {
                    File file = FileUtil.from(this,originalUri );
                    LOG.debug("file", "File...:::: uti - "+file .getPath()+" file -" + file + " : " + file .exists());

                    if (file.exists()) {
                        final Importer importer = new Importer(
                                vsViewModel,
                                this,
                                vdtsApplication
                        );
                        if (importer.importUsers(file)) {
                            userAdapter.setSelectedEntity(-1);
                            updateCurrentUser();
                            vdtsApplication.displayToast(
                                    this,
                                    "Users imported successfully"
                            );
                        } else {
                            vdtsApplication.displayToast(
                                    this,
                                    "Error importing users"
                            );
                        }
                    } else {
                        vdtsApplication.displayToast(
                                this,
                                "User file not found"
                        );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    vdtsApplication.displayToast(
                            this,
                            "User file not found"
                    );
                }
            }
        }
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        initializeIristick();
    }

    /**
     * Initialize Iristick HUD and voice commands when connected.
     */
    private void initializeIristick() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new VDTSConfigUsersActivity.IristickHUD();
            return iristickHUD;
        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Navigate Back", this::finish)
        );
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_config_hud);
        }
    }
}
