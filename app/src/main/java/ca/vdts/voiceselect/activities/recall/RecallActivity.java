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
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.adapters.RecallSessionRecyclerAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSOuterClickListenerUtil;

public class RecallActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {
    private static final Logger LOG = LoggerFactory.getLogger(RecallActivity.class);
    private VDTSApplication vdtsApplication;

    private VDTSUser currentUser;

    private VSViewModel vsViewModel;

    private List<Session> sessionList = new ArrayList<>();

    private RecyclerView headerRecyclerView;
    private RecallSessionRecyclerAdapter recallSessionRecyclerAdapter;
    private SwitchCompat openCheck;
    private SearchView searchView;

    //lock to prevent concurent list filling issues
    private ReentrantLock adapterLock;

    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector_recall);

        vdtsApplication = (VDTSApplication) this.getApplication();

        vsViewModel = new ViewModelProvider(this).get(VSViewModel.class);

        adapterLock = new ReentrantLock();

        openCheck = findViewById(R.id.openCheck);
        openCheck.setOnClickListener(v -> onOpenCheck());
        searchView = findViewById(R.id.sessionSearch);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(this);


        openCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_FILTER,false));

        headerRecyclerView = findViewById(R.id.sesh_list);
        headerRecyclerView.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        headerRecyclerView.setLayoutManager(layoutManager);

        sessionList = new ArrayList<>();
        recallSessionRecyclerAdapter = new RecallSessionRecyclerAdapter(
                sessionList,
                openCheck.isChecked(),
                this,
                new VDTSOuterClickListenerUtil(this::selectCallback, headerRecyclerView)
        );
        headerRecyclerView.setAdapter(recallSessionRecyclerAdapter);
    }


    @Override
    protected void onResume(){
        super.onResume();

        currentUser = vdtsApplication.getCurrentUser();
        updateSessions();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        recallSessionRecyclerAdapter.addFilter(newText);
        return false;
    }


    public void onOpenCheck() {
        vdtsApplication.getPreferences().setBoolean(PREF_FILTER, openCheck.isChecked());
        recallSessionRecyclerAdapter.setFilterOpen(openCheck.isChecked());
    }

    private void export(Session session) {
        //ISaver saver = Saver.createSaver(ONEDRIVE_APP_ID);
        final Exporter exporter = new Exporter(vsViewModel,vdtsApplication,this);
        boolean CSV = true;
        boolean Excel = true;
        boolean JSON = true;

        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV,false)){
            CSV = exporter.exportSessionCSV(session);
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON,false)){
            JSON= exporter.exportSessionJSON(session);
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX,true)){
            Excel = exporter.exportSessionExcel(session);
        }
        if (CSV && Excel && JSON) {
            if (exporter.exportMedia(session)) {
                vdtsApplication.displayToast(
                        this,
                        "Session exported successfully"
                );
            } else {
                vdtsApplication.displayToast(
                        this,
                        "Error exporting session photos"
                );
            }
        } else {
            vdtsApplication.displayToast(
                    this,
                    "Error exporting session"
            );
        }
    }

    private void updateSessions() {
        new Thread(() -> {
            adapterLock.lock();
            recallSessionRecyclerAdapter.clearSelected();

            sessionList.clear();
            sessionList.addAll(vsViewModel.findAllSessionsOrderByStartDate());

            ArrayList<Session> sessions = new ArrayList<>(sessionList);

            runOnUiThread(()-> recallSessionRecyclerAdapter.setSessionDataset(sessions));
            adapterLock.unlock();
        }).start();
    }

    private void selectCallback(Integer index) {
        recallSessionRecyclerAdapter.setSelected(index);
        showOpenDialogue();
    }


    private void showOpenDialogue() {
        LOG.info("Showing Choice Dialog");

        AtomicBoolean keepSelection = new AtomicBoolean(false);

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

        finalDialog.setOnDismissListener(v -> {
                if (!keepSelection.get()) {
                    recallSessionRecyclerAdapter.setSelected(-1);
                }
            });

        openButton.setOnClickListener(v -> {
            vdtsApplication.getPreferences().setLong(
                    String.format("%s_SESSION", currentUser.getExportCode()),
                    recallSessionRecyclerAdapter.getSelected().getUid());
            finalDialog.dismiss();
            Intent resumeActivityIntent = new Intent(this, DataGatheringActivity.class);
            startActivity(resumeActivityIntent);
        });

        exportButton.setOnClickListener(v -> {
            Session session = new Session(recallSessionRecyclerAdapter.getSelected());
            finalDialog.dismiss();
            export(session);
        });

        deleteButton.setOnClickListener(v -> {
            keepSelection.set(true);
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
        label.setText(String.format("Delete Session %s ?", recallSessionRecyclerAdapter.getSelected().name()));
        Button yesButton = customLayout.findViewById(R.id.yesButton);
        Button noButton = customLayout.findViewById(R.id.noButton);

        dialog = builder.create();
        dialog.show();
        AlertDialog finalDialog = dialog;

        finalDialog.setOnDismissListener(v -> recallSessionRecyclerAdapter.setSelected(-1));

        yesButton.setOnClickListener(v -> {
            final Session session = recallSessionRecyclerAdapter.getSelected();
            new Thread(()-> {
                vsViewModel.deleteSession(session);
                updateSessions();
            }).start();
            finalDialog.dismiss();
        });

        noButton.setOnClickListener(v -> finalDialog.dismiss());
    }
}