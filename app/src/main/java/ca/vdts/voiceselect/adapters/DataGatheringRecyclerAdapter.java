package ca.vdts.voiceselect.adapters;

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
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.activities.dataGathering.DataGatheringActivity;
import ca.vdts.voiceselect.activities.dataGathering.ObservableHorizontalScrollView;
import ca.vdts.voiceselect.activities.dataGathering.ScrollChangeListenerInterface;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.ColumnValue;
import ca.vdts.voiceselect.database.entities.Entry;
import ca.vdts.voiceselect.database.entities.EntryValue;
import ca.vdts.voiceselect.library.utilities.VDTSAdapterClickListenerUtil;

public class DataGatheringRecyclerAdapter extends RecyclerView.Adapter<DataGatheringRecyclerAdapter.ViewHolder> {
    private final Context context;
    private final DataGatheringActivity dataGatheringActivity;
    private final VDTSAdapterClickListenerUtil selectedListener;
    private final MutableLiveData<Integer> xCord;
    private int selectedIndex = -1;

    //Lists - Maps
    final List<Column> columnDataset = new ArrayList<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueDataset = new HashMap<>();
    final List<Entry> entryDataset = new ArrayList<>();
    final List<EntryValue> entryValueDataset = new ArrayList<>();

    public DataGatheringRecyclerAdapter(Context context,
                                        DataGatheringActivity dataGatheringActivity,
                                        VDTSAdapterClickListenerUtil selectedListener) {
        this.context = context;
        this.dataGatheringActivity = dataGatheringActivity;
        this.selectedListener = selectedListener;

        xCord = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.adapter_recycler_data_gathering, parent, false),
                context,
                dataGatheringActivity,
                xCord,
                columnDataset);
    }

    @Override
    public void onBindViewHolder(@NonNull DataGatheringRecyclerAdapter.ViewHolder holder, int position) {
        final Entry entry = entryDataset.get(position);
        int size = entryDataset.size();

        final List<EntryValue> entryValues = entryValueDataset.stream()
                .filter(entryValue -> entryValue.getEntryID() == entry.getUid())
                .collect(Collectors.toList());

        holder.entryIndexValue.setText(String.valueOf(position + 1));

        for (int index = 0; index < holder.entryValueLinearLayout.getChildCount(); index++) {
            TextView entryValue = (TextView) holder.entryValueLinearLayout.getChildAt(index);

            List<ColumnValue> columnValueList = new ArrayList<>(
                    Objects.requireNonNull(columnValueDataset.get(index))
            );

            ColumnValue columnValue = columnValueList.stream()
                    .filter(
                            columnVal -> entryValues.stream()
                                .anyMatch(
                                    entryVal -> entryVal.getColumnValueID() == columnVal.getUid()
                            )
                    ).findFirst()
                    .orElse(ColumnValue.COLUMN_VALUE_NONE);

            entryValue.setText(columnValue.getName());
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

        Drawable backgroundResource = holder.entryLinearLayout.getBackground();
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
        return entryDataset.size();
    }

    public void setDatasets(List<Column> columnList,
                            HashMap<Integer, List<ColumnValue>> columnValueMap,
                            List<Entry> entryList,
                            List<EntryValue> entryValueList) {
        columnDataset.clear();
        columnDataset.addAll(columnList);

        columnValueDataset.clear();
        for (int index = 0; index < columnValueMap.size(); index++) {
            columnValueDataset.put(index, columnValueMap.get(index));
        }

        entryDataset.clear();
        entryDataset.addAll(entryList);

        entryValueDataset.clear();
        entryValueDataset.addAll(entryValueList);

        notifyDataSetChanged();
    }

    public void addEntry(Entry entry) {
        entryDataset.add(entry);
        notifyDataSetChanged();
    }

    public void addAllEntries(Collection<Entry> entries) {
        final int startRange = entryDataset.size();
        entryDataset.addAll(entries);
        notifyItemRangeInserted(startRange, entries.size());
    }

    public void addAllEntryValues(Collection<EntryValue> entryValues) {
        final int startRange = entryDataset.size();
        entryValueDataset.addAll(entryValues);
        notifyItemRangeInserted(startRange, entryDataset.size());
    }

    public void clearEntries() {
        entryDataset.clear();
        notifyDataSetChanged();
    }

    public void clearEntryValues() {
        entryValueDataset.clear();
        notifyDataSetChanged();
    }

    public Entry getEntry(int index) {
        return entryDataset.get(index);
    }

    public List<EntryValue> getEntryValues(int index) {
        return entryValueDataset.stream()
                .filter(entryValue ->
                        entryValue.getEntryID() == entryDataset.get(index).getUid())
                .collect(Collectors.toList());
    }

    public void setSelected(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < entryDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        notifyItemChanged(old);
    }

    public void setXCord(int xCord) {
        this.xCord.setValue(xCord);
    }

////VIEW_HOLDER_SUBCLASS////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder
        implements ScrollChangeListenerInterface {
        Context context;
        DataGatheringActivity dataGatheringActivity;
        MutableLiveData<Integer> xCord;
        List<Column> columnList;

        final LinearLayout entryLinearLayout;
        final TextView entryIndexValue;
        final ObservableHorizontalScrollView entryValueScrollView;
        final LinearLayout entryValueLinearLayout;
        final TextView entryCommentValue;
        final TextView entryPhotoValue;

        ViewHolder(View v, Context context, DataGatheringActivity dataGatheringActivity,
                   MutableLiveData<Integer> xCord, List<Column> columnList) {
            super(v);
            this.context = context;
            this.dataGatheringActivity = dataGatheringActivity;
            this.xCord = xCord;
            this.columnList = columnList;

            entryLinearLayout = v.findViewById(R.id.entryLinearLayout);
            entryIndexValue = v.findViewById(R.id.entryIndexValue);
            entryValueScrollView = v.findViewById(R.id.entryValueScrollView);
            entryValueScrollView.setScrollChangeListener(this);
            entryValueLinearLayout = v.findViewById(R.id.entryValueLinearLayout);
            entryCommentValue = v.findViewById(R.id.entryValueComment);
            entryPhotoValue = v.findViewById(R.id.entryValuePhoto);

            initializeEntryValuesLayout();
            xCord.observe(dataGatheringActivity, this::scrollHorizontally);
        }

        private void initializeEntryValuesLayout() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1
            );
            Resources resources = context.getResources();

            int minWidthDimen = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    96,
                    resources.getDisplayMetrics()
            );

            int marginDimen = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    2,
                    resources.getDisplayMetrics()
            );

            int paddingLRDimen = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    4,
                    resources.getDisplayMetrics()
            );

            int paddingTBDimen = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    8,
                    resources.getDisplayMetrics()
            );

            layoutParams.setMargins(marginDimen, 0, marginDimen, 0);

            for (Column ignored : columnList) {
                TextView entryValueText = new TextView(context);
                entryValueText.setMinWidth(minWidthDimen);
                entryValueText.setLayoutParams(layoutParams);
                entryValueText.setPadding(paddingLRDimen, paddingTBDimen,
                        paddingLRDimen, paddingTBDimen);
                entryValueText.setGravity(Gravity.CENTER);
                entryValueText.setMaxLines(1);
                entryValueText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);

                entryValueLinearLayout.addView(entryValueText);
            }
        }

        /**
         * Observe scroll changes in horizontal scroll views and synchronize their position.
         * @param observableHorizontalScrollView - The scroll view being observed
         * @param x - The horizontal position (pixels) that the view will scroll to
         * @param y - The vertical position (pixels) that the view will scroll to
         * @param oldx - The original horizontal position (pixels) of the scroll view
         * @param oldy - The original vertical position (pixels) of the scroll view
         */
        @Override
        public void onScrollChanged(ObservableHorizontalScrollView observableHorizontalScrollView,
                                    int x, int y,
                                    int oldx, int oldy) {
            dataGatheringActivity.onScrollChanged(
                    null,
                    x, y,
                    oldx, oldy);
            xCord.setValue(x);
        }

        private void scrollHorizontally(Integer xCord) {
            if (xCord != null) {
                entryValueScrollView.scrollTo(xCord, 0);
            }
        }
    }
}