package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;

public class DataGatheringAdapter extends RecyclerView.Adapter<DataGatheringAdapter.ViewHolder> {
    private Context context;
    private int selectedIndex = -1;

    //Lists - Maps
    final List<Column> columnList = new ArrayList<>();
    final List<ColumnValue> columnValueList = new ArrayList<>();
    final List<Entry> entriesDataset = new ArrayList<>();
    final List<EntryValue> entryValuesDataset = new ArrayList<>();

    public DataGatheringAdapter(Context context,
                                List<Column> columnList,
                                List<ColumnValue> columnValueList,
                                List<Entry> entryList,
                                List<EntryValue> entryValueList) {
        this.context = context;
        this.columnList.addAll(columnList);
        this.columnValueList.addAll(columnValueList);
        this.entriesDataset.addAll(entryList);
        this.entryValuesDataset.addAll(entryValueList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.adapter_recycler_indexed_named, parent, false),
                context,
                columnList);
    }

    @Override
    public void onBindViewHolder(@NonNull DataGatheringAdapter.ViewHolder holder, int position) {
        int size = entriesDataset.size();
        final Entry entry = entriesDataset.get(position);

        final List<EntryValue> entryValues = entryValuesDataset.stream()
                .filter(entryValue -> entryValue.getEntryID() == entry.getUid())
                .collect(Collectors.toList());

        holder.entryIndexValue.setText(position + 1);

        for (int i = 0; i <= holder.entryLinearLayout.getChildCount(); i++) {
            TextView currentEntry = (TextView) holder.entryLinearLayout.getChildAt(i);

            ColumnValue columnValue = columnValueList.stream().filter(
                    columnVal -> entryValuesDataset.stream()
                            .anyMatch(
                                    entryVal -> entryVal.getColumnValueID() == columnVal.getUid()
                            )
                    ).findFirst()
                    .orElse(ColumnValue.COLUMN_VALUE_NONE);

            //build hashmap of hashmap with column and with column values and ids
            //pass into adapter
            //

            currentEntry.setText(columnValue.getName());
        }

        if (size == 1) {
            holder.entryLinearLayout.setBackgroundResource(R.drawable.recycler_view_item);
        } else if (position == 0) {
            holder.entryLinearLayout.setBackgroundResource(R.drawable.recycler_view_first_item);
        } else if (position == size - 1) {
            holder.entryLinearLayout.setBackgroundResource(R.drawable.recycler_view_last_item);
        } else {
            holder.entryLinearLayout.setBackgroundResource(R.drawable.recycler_view_middle_item);
        }

        Drawable backgroundResource = holder.entryLinearLayout.getBackground();;
        int backgroundColor;
        if (position == selectedIndex) {
            backgroundColor = ContextCompat.getColor(context, R.color.colorBackgroundSelected);
            backgroundResource.setTint(backgroundColor);
        } else {
            if (position % 2 == 0) {
                backgroundColor = ContextCompat.getColor(context, R.color.colorBackgroundPrimary);
                backgroundResource.setTint(backgroundColor);
            } else {
                backgroundColor = ContextCompat.getColor(context, R.color.colorBackgroundSecondary);
                backgroundResource.setTint(backgroundColor);
            }
        }
    }

    @Override
    public int getItemCount() {
        return entriesDataset.size();
    }

    public void addAllEntries(Collection<Entry> entries) {
        final int startRange = entriesDataset.size();
        entriesDataset.addAll(entries);
        notifyItemRangeInserted(startRange, entries.size());
    }

    public void addAllEntryValues(Collection<EntryValue> entryValues) {
        final int startRange = entriesDataset.size();
        entryValuesDataset.addAll(entryValues);
        notifyItemRangeInserted(startRange, entriesDataset.size());
    }

    public void clearEntries() {
        entriesDataset.clear();
        notifyDataSetChanged();
    }

    public void clearValues() {
        entryValuesDataset.clear();
        notifyDataSetChanged();
    }

    public Entry getEntry(int index) {
        return entriesDataset.get(index);
    }

    public List<EntryValue> getEntryValues(int index) {
        return entryValuesDataset.stream()
                .filter(entryValue ->
                        entryValue.getEntryID() == entriesDataset.get(index).getUid())
                .collect(Collectors.toList());
    }

    public void setSelected(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < entriesDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        notifyItemChanged(old);
    }

////VIEW_HOLDER_SUBCLASS////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder {
        Context context;
        List<Column> columnList;

        final TextView entryIndexValue;
        final LinearLayout entryLinearLayout;
        final TextView entryCommentValue;
        final TextView entryPhotoValue;

        ViewHolder(View v, Context context, List<Column> columnList) {
            super(v);
            this.context = context;
            this.columnList = columnList;
            entryIndexValue = v.findViewById(R.id.entryIndexValue);
            entryLinearLayout = v.findViewById(R.id.entryLinearLayout);
            entryCommentValue = v.findViewById(R.id.entryCommentValue);
            entryPhotoValue = v.findViewById(R.id.entryPhotoValue);
            initializeEntryValuesLayout();
        }

        private void initializeEntryValuesLayout() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
            );
            Resources resources = context.getResources();
            int dimen = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4,
                    resources.getDisplayMetrics()
            );
            layoutParams.setMargins(dimen, 0, dimen, 0);

            for (int i = 0; i <= columnList.size(); i++ ) {
                TextView entryValueText = new TextView(context);
                entryValueText.setId(i);
                entryValueText.setGravity(Gravity.CENTER);
                entryValueText.setLayoutParams(layoutParams);
                entryValueText.setPadding(dimen, dimen, dimen, dimen);
                entryValueText.setMaxLines(1);        //todo - maybe not a good idea
                entryValueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

                entryLinearLayout.addView(entryValueText);
            }
        }
    }
}
