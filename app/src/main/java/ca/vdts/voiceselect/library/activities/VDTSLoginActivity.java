package ca.vdts.voiceselect.library.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.adapters.VDTSIndexedNamedAdapter;
import ca.vdts.voiceselect.library.database.VDTSViewModel;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.services.VDTSClickListenerService;

/**
 * Basic login activity for VDTS applications
 */
public class VDTSLoginActivity extends AppCompatActivity {
    //private static final Logger LOG = LoggerFactory.getLogger(VDTSLoginActivity.class);

    private VDTSApplication vdtsApplication;
    private TextToSpeech ttsEngine;

    //Recycler View
    private VDTSViewModel vdtsViewModel;
    private VDTSIndexedNamedAdapter<VDTSUser> userAdapter;
    private RecyclerView userRecyclerView;
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
            userList.addAll(vdtsViewModel.findAllActiveUsers());
            userList.remove(VDTSUser.VDTS_USER_NONE);
            handler.post(() -> {
                userAdapter.setDataset(userList);
                if (userList.size() <= 0) {
                    Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
                    startActivity(vdtsMenuActivity);
                }
            });
        });
    }

    /**
     * Select the appropriate user from the recycler view and set global user
     * @param index - Index of the user
     */
    private void userAdapterSelect(Integer index) {
        userAdapter.setSelectedEntity(index);

        VDTSUser currentUser = userAdapter.getSelectedEntity();
        vdtsApplication.setCurrentUser(currentUser);

        //Initialize TTS Engine
        ttsEngine.setSpeechRate(currentUser.getFeedbackRate());
        ttsEngine.setPitch(currentUser.getFeedbackPitch());

        Intent vdtsMenuActivity = new Intent(this, VDTSMenuActivity.class);
        startActivity(vdtsMenuActivity);
    }
}

