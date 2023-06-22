package ca.vdts.voiceselect.library.activities;

import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_DURATION;
import static ca.vdts.voiceselect.library.VDTSApplication.SHAKE_REPEAT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.iristick.sdk.Experimental;
import com.iristick.sdk.IRIHeadset;
import com.iristick.sdk.IRIListener;
import com.iristick.sdk.IristickSDK;
import com.iristick.sdk.display.IRIWindow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
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
public class VDTSLoginActivity extends AppCompatActivity implements IRIListener {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSLoginActivity.class);

    private VDTSApplication vdtsApplication;
    private VDTSUser currentUser;
    private String[] userName = {""};
    private TextToSpeech ttsEngine;

    private EditText passwordText;

    //Recycler View
    private VDTSViewModel vdtsViewModel;
    private VDTSIndexedNamedAdapter<VDTSUser> userAdapter;
    private RecyclerView userRecyclerView;
    private TextView footerUserValue;
    private TextView footerVersionValue;
    private final List<VDTSUser> userList = new ArrayList<>();

    //Iristick Components
    private boolean isHeadsetAvailable = false;
    private VDTSLoginActivity.IristickHUD iristickHUD;

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
                if (userList.size() == 0) {
                    Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                    startActivity(vdtsMenuActivity);
                } else {
                    userAdapter.setDataset(userList);
                    userAdapterSelect(-1);

                    initializeIristickHUD();
                }
            });
        });
    }

    /**
     * Select the appropriate user from the recycler view and set as global user. Password must be
     * entered if it exists.
     * @param index - Index of the user.
     */
    private void userAdapterSelect(Integer index) {
        userAdapter.setSelectedEntity(index);
        currentUser = userAdapter.getSelectedEntity();
        inputPIN();
    }

    private void inputPIN() {
        if (currentUser != null) {
            if (currentUser.getPassword() != null && !currentUser.getPassword().isEmpty()) {
                passwordText = new EditText(this);
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
                    enterPIN();
                });

                passwordAlert.setNegativeButton("Cancel", (dialog, which) -> {
                    userAdapterSelect(-1);
                    dialog.dismiss();
                });

                passwordAlert.show();
                passwordText.requestFocus();
            } else {
                vdtsApplication.setCurrentUser(currentUser);

                //Initialize TTS Engine
                ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
                ttsEngine.setPitch(currentUser.getFeedbackPitch());

                Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                startActivity(vdtsMenuActivity);
            }
        }
    }

    private void enterPIN() {
        if (passwordText != null) {
            if (currentUser.getPassword().equals(passwordText.getText().toString().trim())) {
                vdtsApplication.setCurrentUser(currentUser);
                LOG.info("User Password: {} validated", currentUser.getName());

                //Initialize TTS Engine
                ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
                ttsEngine.setPitch(currentUser.getFeedbackPitch());

                Intent vdtsMenuActivity = new Intent(
                        vdtsApplication.getApplicationContext(),
                        VDTSMenuActivity.class
                );
                startActivity(vdtsMenuActivity);
            } else {
                LOG.info("User Password: {} invalid", currentUser.getName());
                YoYo.with(Techniques.Shake)
                        .duration(SHAKE_DURATION)
                        .repeat(SHAKE_REPEAT)
                        .playOn(userRecyclerView);
                vdtsApplication.displayToast(
                        vdtsApplication.getApplicationContext(),
                        "User Password: " + currentUser.getName() + " invalid",
                        0);
            }
        } else if (currentUser.getPassword().equals(iristickHUD.enterPINValue.getText().toString().trim())) {
            vdtsApplication.setCurrentUser(currentUser);
            LOG.info("User Password: {} validated", currentUser.getName());

            //Initialize TTS Engine
            ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
            ttsEngine.setPitch(currentUser.getFeedbackPitch());

            Intent vdtsMenuActivity = new Intent(
                    vdtsApplication.getApplicationContext(),
                    VDTSMenuActivity.class
            );
            startActivity(vdtsMenuActivity);
        }
    }

    @Override
    public void onHeadsetAvailable(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = true;
    }

    @Override
    public void onHeadsetDisappeared(@NonNull IRIHeadset headset) {
        IRIListener.super.onHeadsetAvailable(headset);
        isHeadsetAvailable = false;
    }

    private void initializeIristickHUD() {
        IristickSDK.addWindow(this.getLifecycle(), () -> {
            iristickHUD = new IristickHUD();
            return iristickHUD;
        });

        initializeIristickSelectUser();
    }

    @OptIn(markerClass = Experimental.class)
    private void  initializeIristickSelectUser() {
        IristickSDK.addVoiceGrammar(getLifecycle(), this, ac -> {
            ac.addAlternativeGroup(ag -> {
                for (VDTSUser user : userList) {
                    ag.addToken(user.getName());
                }
            });

            ac.setListener((recognizer, tokens, tags) -> {
                for (String token : tokens) {
                    userName[0] = userName[0].concat(token);
                }
                Executor executor = Executors.newSingleThreadExecutor();
                Handler handler = new Handler(Looper.getMainLooper());
                executor.execute(() -> {
                    currentUser = vdtsViewModel.findUserByName(userName[0]);
                    handler.post(() -> {
                        if (!Objects.equals(currentUser.getPassword(), "")) {
                            iristickHUD.selectUserPrompt.setVisibility(View.INVISIBLE);
                            initializeIristickInputPIN();
                        } else {
                            vdtsApplication.setCurrentUser(currentUser);

                            //Initialize TTS Engine
                            ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
                            ttsEngine.setPitch(currentUser.getFeedbackPitch());

                            Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                            startActivity(vdtsMenuActivity);
                        }
                    });
                });
            });
        });
    }

    @OptIn(markerClass = Experimental.class)
    private void initializeIristickInputPIN() {
        iristickHUD.userNameLabel.setVisibility(View.VISIBLE);
        iristickHUD.userNameValue.setVisibility(View.VISIBLE);
        iristickHUD.userNameValue.setText(currentUser.getName());
        iristickHUD.enterPINPrompt.setVisibility(View.VISIBLE);
        iristickHUD.enterPINLabel.setVisibility(View.VISIBLE);
        iristickHUD.enterPINValue.setVisibility(View.VISIBLE);

        String[] pin = {""};
        IristickSDK.addVoiceGrammar(getLifecycle(), this, ac -> {
            ac.addAlternativeGroup(ag -> {
                for (int index = 0; index <= 9; index++) {
                    ag.addToken(String.valueOf(index));
                }
            });

            ac.setListener((recognizer, tokens, tags) -> {
                for (String token : tokens) {
                    pin[0] = pin[0].concat(token);
                }

                iristickHUD.enterPINValue.setText(pin[0]);
            });
        });

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Enter", this::enterPIN)
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Clear", () -> {
                    String empty[] = {""};
                    pin[0] = "";
                    iristickHUD.enterPINValue.setText("");
                })
        );

        IristickSDK.addVoiceCommands(
                this.getLifecycle(),
                this,
                vc -> vc.add("Cancel", () -> {
                    userName[0] = "";

                    iristickHUD.userNameLabel.setVisibility(View.INVISIBLE);
                    iristickHUD.userNameValue.setVisibility(View.INVISIBLE);
                    iristickHUD.userNameValue.setText("");
                    iristickHUD.enterPINPrompt.setVisibility(View.INVISIBLE);
                    iristickHUD.enterPINLabel.setVisibility(View.INVISIBLE);
                    iristickHUD.enterPINValue.setText("");
                    iristickHUD.enterPINValue.setVisibility(View.INVISIBLE);

                    iristickHUD.selectUserPrompt.setVisibility(View.VISIBLE);
                })
        );
    }

////HUD_SUBCLASS////////////////////////////////////////////////////////////////////////////////////
    public static class IristickHUD extends IRIWindow {
        //HUD Views
        private TextView selectUserPrompt;

        private TextView userNameLabel;
        private TextView userNameValue;
        private TextView enterPINPrompt;
        private TextView enterPINLabel;
        private TextView enterPINValue;

        @Override
        protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_login_hud);

            selectUserPrompt = findViewById(R.id.selectUserPrompt);

            userNameLabel = findViewById(R.id.userNameLabel);
            userNameValue = findViewById(R.id.userNameValue);
            enterPINPrompt = findViewById(R.id.enterPinPrompt);
            enterPINLabel = findViewById(R.id.enterPINLabel);
            enterPINValue = findViewById(R.id.enterPINValue);

            userNameLabel.setVisibility(View.INVISIBLE);
            userNameValue.setVisibility(View.INVISIBLE);
            enterPINPrompt.setVisibility(View.INVISIBLE);
            enterPINLabel.setVisibility(View.INVISIBLE);
            enterPINValue.setVisibility(View.INVISIBLE);
        }
    }
}
