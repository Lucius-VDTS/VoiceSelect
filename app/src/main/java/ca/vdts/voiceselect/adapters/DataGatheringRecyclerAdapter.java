package ca.vdts.voiceselect.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
import java.util.Locale;
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
import ca.vdts.voiceselect.database.entities.PictureReference;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.utilities.VDTSAdapterClickListenerUtil;

@SuppressLint("NotifyDataSetChanged")
public class DataGatheringRecyclerAdapter
        extends RecyclerView.Adapter<DataGatheringRecyclerAdapter.ViewHolder> {
    private final Context context;
    private final DataGatheringActivity dataGatheringActivity;
    private final VDTSUser currentUser;
    private final VDTSAdapterClickListenerUtil selectedListener;
    private final MutableLiveData<Integer> xCord;
    private int selectedIndex = -1;

    //Lists - Maps
    private final List<Column> columnDataset = new ArrayList<>();
    private final HashMap<Integer, List<ColumnValue>> columnValueDataset = new HashMap<>();
    private final List<Entry> entryDataset = new ArrayList<>();
    private final List<EntryValue> entryValueDataset = new ArrayList<>();
    private final List<PictureReference> pictureDataset = new ArrayList<>();

    public DataGatheringRecyclerAdapter(Context context,
                                        DataGatheringActivity dataGatheringActivity,
                                        VDTSUser currentUser,
                                        VDTSAdapterClickListenerUtil selectedListener) {
        this.context = context;
        this.dataGatheringActivity = dataGatheringActivity;
        this.currentUser = currentUser;
        this.selectedListener = selectedListener;

        xCord = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.adapter_recycler_data_gathering, parent, false);

        if (selectedListener != null) {
            view.setOnClickListener(selectedListener);
        }

        return new ViewHolder(view, context, dataGatheringActivity, xCord, columnDataset);
    }

    @Override
    public void onBindViewHolder(@NonNull DataGatheringRecyclerAdapter.ViewHolder holder,
                                 int position) {
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

            if (currentUser.isAbbreviate()) {
                entryValue.setText(columnValue.getNameCode());
            } else {
                entryValue.setText(columnValue.getName());
            }
        }

        holder.entryCommentValue.setChecked(
                entry.getComment() != null && !entry.getComment().isEmpty()
        );

        long pictureCount = pictureDataset.stream()
                .filter(pr -> pr.getEntryID() == entry.getUid())
                .count();

        holder.entryPhotoValue.setText(
                pictureCount > 0 ?
                        String.format(Locale.getDefault(), "%d", pictureCount) :
                        ""
        );

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

    public void setDatasets(HashMap<Integer, Column> columnMap,
                            HashMap<Integer, List<ColumnValue>> columnValueMap,
                            List<Entry> entryList, List<EntryValue> entryValueList,
                            List<PictureReference> pictureReferenceList) {
        columnDataset.clear();
        for (int index = 0; index < columnMap.size(); index++) {
            columnDataset.add(index, columnMap.get(index));
        }

        columnValueDataset.clear();
        for (int index = 0; index < columnValueMap.size(); index++) {
            columnValueDataset.put(index, columnValueMap.get(index));
        }

        entryDataset.clear();
        entryDataset.addAll(entryList);

        entryValueDataset.clear();
        entryValueDataset.addAll(entryValueList);

        pictureDataset.clear();
        pictureDataset.addAll(pictureReferenceList);

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
        notifyItemRangeInserted(startRange, entryValues.size());
    }

    public void addAllPictureReferences(Collection<PictureReference> pictureReferences) {
        final int startRange = pictureDataset.size();
        pictureDataset.addAll(pictureReferences);
        notifyItemRangeInserted(startRange, pictureDataset.size());
    }

    public void clearEntries() {
        entryDataset.clear();
        notifyDataSetChanged();
    }

    public void clearEntryValues() {
        entryValueDataset.clear();
        notifyDataSetChanged();
    }

    public void clearPictureReferences() {
        pictureDataset.clear();
        notifyDataSetChanged();
    }

    public void removeEntry(Entry entry) {
        entryDataset.remove(entry);
    }

    public Entry getEntry(int index) {
        return entryDataset.get(index);
    }

    public int getEntryPosition(long entryID) {
        Entry entry = entryDataset.stream()
                .filter(e -> e.getUid() == entryID)
                .findFirst()
                .orElse(null);
        if (entry != null) {
            return entryDataset.indexOf(entry);
        } else {
            return -1;
        }
    }

    public List<EntryValue> getEntryValues(int index) {
        return entryValueDataset.stream()
                .filter(entryValue -> entryValue.getEntryID() == entryDataset.get(index).getUid())
                .collect(Collectors.toList());
    }

    public void addPictureReferences(List<PictureReference> pictureReferences, int index) {
        List<PictureReference> entryPictureReferences = pictureDataset.stream()
                .filter(
                        pictureReference ->
                                pictureReference.getEntryID() == entryDataset.get(index).getUid()
                ).collect(Collectors.toList());

        pictureReferences.forEach(pictureReference -> {
            if (!entryPictureReferences.contains(pictureReference)) {
                pictureDataset.add(pictureReference);
            }
        });
        notifyItemChanged(index);
    }

    public List<PictureReference> getPictureReferences(int index) {
        return pictureDataset.stream()
                .filter(
                        pictureReference ->
                                pictureReference.getEntryID() == entryDataset.get(index).getUid()
                ).collect(Collectors.toList());
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
        final CheckBox entryCommentValue;
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
            entryPhotoValue = v.findViewById(R.id.entryValuePic);

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
                                    int x, int y, int oldx, int oldy) {
            dataGatheringActivity.onScrollChanged(null, x, y, oldx, oldy);
            xCord.setValue(x);
        }

        private void scrollHorizontally(Integer xCord) {
            if (xCord != null) {
                entryValueScrollView.scrollTo(xCord, 0);
            }
        }
    }
}