package ca.vdts.voiceselect.library.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.BuildConfig;
import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.database.VDTSViewModel;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSClickListenerUtil;

/**
 * Basic login activity for VDTS applications.
 */
public class VDTSLoginActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSLoginActivity.class);

    private VDTSApplication vdtsApplication;
    private TextToSpeech ttsEngine;

    //Recycler View
    private VDTSViewModel vdtsViewModel;
    private VDTSIndexedNamedAdapter<VDTSUser> userAdapter;
    private RecyclerView userRecyclerView;
    private TextView footerUserValue;
    private TextView footerVersionValue;
    private final List<VDTSUser> userList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        vdtsApplication = (VDTSApplication) getApplication();
        ttsEngine = vdtsApplication.getTTSEngine();

        vdtsViewModel = new ViewModelProvider(this).get(VDTSViewModel.class);

        userRecyclerView = findViewById(R.id.loginRecyclerView);
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
                userList
        );

        userRecyclerView.setAdapter(userAdapter);

        VDTSUser currentUser = vdtsApplication.getCurrentUser();
        footerUserValue = findViewById(R.id.footerUserValue);
        footerUserValue.setText(currentUser.getName());

        footerVersionValue = findViewById(R.id.footerVersionValue);
        footerVersionValue.setText(BuildConfig.VERSION_NAME);
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
            userList.clear();
            userList.addAll(vdtsViewModel.findAllActiveUsers());
            userList.remove(VDTSUser.VDTS_USER_NONE);
            handler.post(() -> {
                userAdapter.setDataset(userList);
                userAdapterSelect(-1);
                if (userList.size() == 0) {
                    Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                    startActivity(vdtsMenuActivity);
                }
            });
        });
    }

    /**
     * Select the appropriate user from the recycler view and set as global user. Password must be
     * entered if the user is an admin.
     * @param index - Index of the user.
     */
    private void userAdapterSelect(Integer index) {
        userAdapter.setSelectedEntity(index);
        VDTSUser currentUser = userAdapter.getSelectedEntity();

        if (currentUser != null) {
            if (currentUser.getPassword() != null && !currentUser.getPassword().isEmpty()) {
                final EditText passwordText = new EditText(this);
                passwordText.setInputType(
                        InputType.TYPE_CLASS_NUMBER |
                                InputType.TYPE_NUMBER_VARIATION_PASSWORD
                );

                final AlertDialog.Builder passwordAlert = new AlertDialog.Builder(this)
                        .setTitle(R.string.login_password_title)
                        .setMessage(currentUser.getName())
                        .setCancelable(false)
                        .setView(passwordText);

                passwordAlert.setPositiveButton("Submit", (dialog, which) -> {
                    if (currentUser.getPassword().equals(passwordText.getText().toString().trim())) {
                        vdtsApplication.setCurrentUser(currentUser);
                        LOG.info("User Password: {} validated", currentUser.getName());

                        Intent vdtsMenuActivity = new Intent(
                                vdtsApplication.getApplicationContext(),
                                VDTSMenuActivity.class
                        );
                        startActivity(vdtsMenuActivity);
                    } else {
                        LOG.info("User Password: {} invalid", currentUser.getName());
                        vdtsApplication.displayToast(
                                vdtsApplication.getApplicationContext(),
                                "User Password: " + currentUser.getName() + " invalid",
                                0);
                    }
                });

                passwordAlert.setNegativeButton("Cancel", (dialog, which) -> {
                    userAdapterSelect(-1);
                    dialog.dismiss();
                });

                passwordAlert.show();
                passwordText.requestFocus();
            } else {
                vdtsApplication.setCurrentUser(currentUser);
                Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                startActivity(vdtsMenuActivity);
            }

            //Initialize TTS Engine
            ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
            ttsEngine.setPitch(currentUser.getFeedbackPitch());
        }
    }
}
