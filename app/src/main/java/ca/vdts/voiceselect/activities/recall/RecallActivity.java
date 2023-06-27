package ca.vdts.voiceselect.activities.recall;

import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_CSV;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_JSON;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_EXPORT_XLSX;
import static ca.vdts.voiceselect.library.VDTSApplication.PREF_FILTER;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.adapters.SessionAdapter;
import ca.vdts.voiceselect.database.VSViewModel;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.files.Exporter;
import ca.vdts.voiceselect.library.VDTSApplication;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSOuterClickListenerUtil;

public class RecallActivity extends AppCompatActivity {

    private VDTSApplication vdtsApplication;

    private VSViewModel viewModel;

    private List<Session> allSessions = new ArrayList<>();
    private List<Session> openSessions = new ArrayList<>();

    private RecyclerView headerList;
    private SessionAdapter adapter;

    final List<VDTSUser> users = new ArrayList<>();
    final HashMap<Long,VDTSUser> userMap = new HashMap<>();
    private SwitchCompat openCheck;
    private TextView searchView;


    @SuppressLint("UseSparseArrays")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selector_recall);

        vdtsApplication = (VDTSApplication) this.getApplication();

        viewModel = new ViewModelProvider(this).get(VSViewModel.class);

        openCheck = findViewById(R.id.openCheck);
        searchView = findViewById(R.id.searchView);

        openCheck.setChecked(vdtsApplication.getPreferences().getBoolean(PREF_FILTER,false));

        headerList = findViewById(R.id.sesh_list);
        headerList.setHasFixedSize(true);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        headerList.setLayoutManager(layoutManager);


        allSessions = new ArrayList<>();
        adapter = new SessionAdapter(allSessions, this, userMap, new VDTSOuterClickListenerUtil(this::selectCallback, headerList));
        headerList.setAdapter(adapter);

    }

    @Override
    protected void onResume(){
        super.onResume();

        updateSessions();
        updateUsers();
    }


    public void onOpenCheck(View view) {
        if (openSessions != null && allSessions != null) {
            adapter.setDataset(openCheck.isChecked() ? openSessions : allSessions);
        }
        vdtsApplication.getPreferences().setBoolean(PREF_FILTER, openCheck.isChecked());
    }

    private void export() {
        //ISaver saver = Saver.createSaver(ONEDRIVE_APP_ID);
        final Exporter exporter = new Exporter(viewModel,vdtsApplication,this);
        boolean CSV = true;
        boolean Excel = true;
        boolean JSON = true;
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_CSV,false)){
            CSV = exporter.exportSessionCSV(adapter.getSelected());
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_JSON,false)){
            JSON= exporter.exportSessionJSON(adapter.getSelected());
        }
        if (vdtsApplication.getPreferences().getBoolean(PREF_EXPORT_XLSX,false)){
            Excel = exporter.exportSessionExcel(adapter.getSelected());
        }
        if (CSV && Excel && JSON) {
            if (exporter.exportMedia(adapter.getSelected())) {
                vdtsApplication.displayToast(this,"Session exported successfully",0);
            } else {
                vdtsApplication.displayToast(this,"Error exporting session photos",0);
            }
        } else {
            vdtsApplication.displayToast(this,"Error exporting session",0);
        }
    }

    private void updateSessions() {
        new Thread(() -> {
            openSessions.clear();
            openSessions.addAll(viewModel.findAllOpenSessionsOrderByStartDate());
            allSessions.clear();
            allSessions.addAll(viewModel.findAllSessionsOrderByStartDate());
            ArrayList<Session> sessions = new ArrayList<>();
            if (openCheck.isChecked()){
                sessions.addAll(openSessions);
            } else {
                sessions.addAll(allSessions);
            }
            runOnUiThread(()->{
                adapter.setDataset(sessions);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void updateUsers() {
        new Thread(() -> {
            users.clear();
            users.addAll(viewModel.findAllUsers());
            userMap.clear();
            for (VDTSUser user : users){
                userMap.put(user.getUid(),user);
            }
            runOnUiThread(() -> adapter.setUsers(userMap));
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        }).start();
    }

    private void selectCallback(Integer index) {
        adapter.setSelected(index);
       // parseBNFCommand(String.format("Select ID %d", index + 1));
    }

}
