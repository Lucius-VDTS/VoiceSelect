package ca.vdts.voiceselect.activities.recall;

import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_FILTER;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.adapters.SessionAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSOuterClickListenerUtil;

public class RecallActivity extends AppCompatActivity {
    private static final Logger LOG = LoggerFactory.getLogger(RecallActivity.class);
    private VDTSApplication vdtsApplication;

    private VDTSUser currentUser;

    private VSViewModel vsViewModel;

    private List<Session> sessionList = new ArrayList<>();
    private final List<Session> openSessionList = new ArrayList<>();

    private RecyclerView headerRecyclerView;
    private SessionAdapter sessionAdapter;

    final List<VDTSUser> userList = new ArrayList<>();
    final HashMap<Long,VDTSUser> userMap = new HashMap<>();
    private SwitchCompat openCheck;
    private TextView searchView;

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector_recall);

        vdtsApplication = (VDTSApplication) this.getApplication();

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        openCheck = findViewById(R.id.openCheck);
        searchView = findViewById(R.id.searchView);

        openCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_FILTER,false));

        headerRecyclerView = findViewById(R.id.sesh_list);
        headerRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        headerRecyclerView.setLayoutManager(layoutManager);

        sessionList = new ArrayList<>();
        sessionAdapter = new SessionAdapter(
                sessionList,
                this,
                userMap,
                new VDTSOuterClickListenerUtil(this::selectCallback, headerRecyclerView)
        );
        headerRecyclerView.setAdapter(sessionAdapter);
    }

    @Override
    protected void onResume(){
        super.onResume();

        currentUser = vdtsApplication.getCurrentUser();
        updateSessions();
        updateUsers();
    }


    public void onOpenCheck(View view) {
        if (openSessionList != null && sessionList != null) {
            sessionAdapter.setSessionDataset(openCheck.isChecked() ? openSessionList : sessionList);
        }

        vdtsApplication.getPreferences().setBoolean(PREF_FILTER, openCheck.isChecked());
    }

    private void export() {
        //ISaver saver = Saver.createSaver(ONEDRIVE_APP_ID);
        final Exporter exporter = new Exporter(vsViewModel,vdtsApplication,this);
        boolean CSV = true;
        boolean Excel = true;
        boolean JSON = true;

        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV,false)){
            CSV = exporter.exportSessionCSV(sessionAdapter.getSelected());
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON,false)){
            JSON= exporter.exportSessionJSON(sessionAdapter.getSelected());
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX,false)){
            Excel = exporter.exportSessionExcel(sessionAdapter.getSelected());
        }
        if (CSV && Excel && JSON) {
            if (exporter.exportMedia(sessionAdapter.getSelected())) {
                vdtsApplication.displayToast(
                        this,
                        "Session exported successfully",
                        0
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting session photos",
                        0
                );
            }
        } else {
            vdtsApplication.displayToast(
                    this,
                    "Error exporting session",
                    0
            );
        }
    }

    private void updateSessions() {
        new Thread(() -> {
            sessionAdapter.setSelected(-1);

            openSessionList.clear();
            openSessionList.addAll(vsViewModel.findAllOpenSessionsOrderByStartDate());

            sessionList.clear();
            sessionList.addAll(vsViewModel.findAllSessionsOrderByStartDate());

            ArrayList<Session> sessions = new ArrayList<>();
            if (openCheck.isChecked()){
                sessions.addAll(openSessionList);
            } else {
                sessions.addAll(sessionList);
            }

            runOnUiThread(()-> {
                sessionAdapter.setSessionDataset(sessions);
                sessionAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void updateUsers() {
        new Thread(() -> {
            userList.clear();
            userList.addAll(vsViewModel.findAllUsers());

            userMap.clear();
            for (VDTSUser user : userList){
                userMap.put(user.getUid(),user);
            }

            runOnUiThread(() -> sessionAdapter.setUserMap(userMap));
            runOnUiThread(() -> sessionAdapter.notifyDataSetChanged());
        }).start();
    }

    private void selectCallback(Integer index) {
        sessionAdapter.setSelected(index);
        showOpenDialogue();
    }

    private void showOpenDialogue() {
        LOG.info("Showing Choice Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Recall Session");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_recall,
                null
        );
        builder.setView(customLayout);

        Button openButton = customLayout.findViewById(R.id.openButton);
        Button exportButton = customLayout.findViewById(R.id.exportButton);
        Button deleteButton = customLayout.findViewById(R.id.deleteButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        openButton.setOnClickListener(v -> {
            vdtsApplication.getPreferences().setLong(
                    String.format("%s_SESSION", currentUser.getExportCode()),
                    sessionAdapter.getSelected().getUid());
            finalDialog.dismiss();
            Intent resumeActivityIntent = new Intent(this, DataGatheringActivity.class);
            startActivity(resumeActivityIntent);
        });

        exportButton.setOnClickListener(v -> {
            finalDialog.dismiss();
            export();
        });

        deleteButton.setOnClickListener(v -> {
            finalDialog.dismiss();
            showConfirmDialog();
        });
    }

    private void showConfirmDialog() {
        LOG.info("Showing Confirm Dialog");

        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Confirm Deletion");
        final View customLayout = getLayoutInflater().inflate(
                R.layout.dialogue_fragment_yes_no,
                null
        );
        builder.setView(customLayout);

        TextView label = customLayout.findViewById(R.id.mainLabel);
        label.setText(String.format("Delete Session %s ?", sessionAdapter.getSelected().name()));
        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        yesButton.setOnClickListener(v -> {
            final Session session = sessionAdapter.getSelected();
            new Thread(()-> {
                vsViewModel.deleteSession(session);
                updateSessions();
            }).start();
            finalDialog.dismiss();
        });

        noButton.setOnClickListener(v -> finalDialog.dismiss());
    }
}