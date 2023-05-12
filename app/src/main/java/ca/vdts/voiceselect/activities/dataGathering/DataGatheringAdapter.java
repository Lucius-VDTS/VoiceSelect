package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;

public class DataGatheringAdapter extends RecyclerView.Adapter<DataGatheringAdapter.ViewHolder> {
    private final Context context;

    final List<Entry> entriesDataset = new ArrayList<>();
    final List<EntryValue> entryValuesDataset = new ArrayList<>();

    private int selectedIndex = -1;

    public DataGatheringAdapter(Context context, List<Entry> entriesDataset,
                                List<EntryValue> entryValuesDataset) {
        this.context = context;
        this.entriesDataset.addAll(entriesDataset);
        this.entryValuesDataset.addAll(entryValuesDataset);
    }

    @NonNull
    @Override
    public DataGatheringAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                              int viewType) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.adapter_recycler_data_gathering, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull DataGatheringAdapter.ViewHolder holder, int position) {}

    @Override
    public int getItemCount() {
        return entriesDataset.size();
    }

////VIEW_HOLDER_SUBCLASS////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout linearLayout;
        final TextView rvIndexValue;
        final TextView rvCommentValue;
        final TextView rvPhotoValue;

        ViewHolder(View v) {
            super(v);
            linearLayout = (LinearLayout) v;
            rvIndexValue = v.findViewById(R.id.rvIndexValue);
            rvCommentValue = v.findViewById(R.id.rvCommentValue);
            rvPhotoValue = v.findViewById(R.id.rvPhotoValue);
        }
    }
}
