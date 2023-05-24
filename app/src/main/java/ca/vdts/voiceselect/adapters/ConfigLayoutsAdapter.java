package ca.vdts.voiceselect.adapters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.Column;
import ca.vdts.voiceselect.database.entities.LayoutColumn;
import ca.vdts.voiceselect.library.utilities.VDTSAdapterClickListenerUtil;

/**
 * Recycler view adapter for layouts editor.
 */
public class ConfigLayoutsAdapter extends RecyclerView.Adapter<ConfigLayoutsAdapter.ViewHolder> {
    private final Context context;
    private final VDTSAdapterClickListenerUtil selectedListener;
    private int selectedIndex = -1;
    private final HashMap<Long, Column> columnHashMap;
    private final List<LayoutColumn> layoutColumnDataset = new ArrayList<>();

    public ConfigLayoutsAdapter(Context context, VDTSAdapterClickListenerUtil selectedListener,
                                HashMap<Long, Column> columnHashMap,
                                List<LayoutColumn> layoutColumnDataset) {
        this.context = context;
        this.selectedListener = selectedListener;
        this.columnHashMap = columnHashMap;
        setDataset(layoutColumnDataset);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.adapter_recycler_config_layout, parent, false);

        if (selectedListener != null) {
            view.setOnClickListener(selectedListener);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LayoutColumn layoutColumn = layoutColumnDataset.get(position);
        int size = layoutColumnDataset.size();

        String columnName = Objects.requireNonNull(
                columnHashMap.get(layoutColumn.getColumnID())).getName();
        holder.columnNameTextView.setText(columnName);

        String isEnabled;
        if (layoutColumn.isColumnEnabled()) {
            isEnabled = "Yes";
        } else {
            isEnabled = "No";
        }
        holder.columnEnabledTextView.setText(isEnabled);

        holder.columnPositionTextView.setText((int) layoutColumn.getColumnPosition());

        if (size == 1) {
            holder.linearLayout.setBackgroundResource(R.drawable.recycler_view_item);
        } else if (position == 0) {
            holder.linearLayout.setBackgroundResource(R.drawable.recycler_view_first_item);
        } else if (position == size - 1) {
            holder.linearLayout.setBackgroundResource(R.drawable.recycler_view_last_item);
        } else {
            holder.linearLayout.setBackgroundResource(R.drawable.recycler_view_middle_item);
        }

        Drawable backgroundResource = holder.linearLayout.getBackground();
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

        holder.linearLayout.setOnClickListener(selectedListener);
    }

    public void setDataset(List<LayoutColumn> layoutColumnDataset) {
        this.layoutColumnDataset.clear();
        this.layoutColumnDataset.addAll(layoutColumnDataset);
        sortDataset(layoutColumnDataset);
        notifyDataSetChanged();
    }

    public void sortDataset(List<LayoutColumn> layoutColumnDataset) {
        layoutColumnDataset.sort((layoutColumn1, layoutColumn2) ->
                (int) (layoutColumn1.getColumnPosition() - layoutColumn2.getColumnPosition()));
    }

    public void addAllLayoutColumns(List<LayoutColumn> layoutColumnList) {
        layoutColumnDataset.addAll(layoutColumnList);
        notifyDataSetChanged();
    }

    public void updateLayoutColumn(LayoutColumn layoutColumn) {
        int index = layoutColumnDataset.indexOf(layoutColumn);
        layoutColumnDataset.remove(layoutColumn);
        layoutColumnDataset.add(index, layoutColumn);
    }

    public void updateSelectedLayoutColumn() {
        notifyItemChanged(this.selectedIndex);
    }

    public void removeAllLayoutColumns(List<LayoutColumn> layoutColumnList) {
        layoutColumnDataset.removeAll(layoutColumnList);
        notifyDataSetChanged();
    }

    public LayoutColumn getLayoutColumn(int index) {
        return layoutColumnDataset.get(index);
    }

    public LayoutColumn getSelectedLayoutColumn() {
        if (selectedIndex >= 0 && selectedIndex < layoutColumnDataset.size()) {
            return layoutColumnDataset.get(selectedIndex);
        }
        return null;
    }

    public void setSelectedLayoutColumn(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < layoutColumnDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        notifyItemChanged(old);
    }

    @Override
    public int getItemCount() {
        return layoutColumnDataset.size();
    }

////VIEW_HOLDER_SUBCLASS////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout linearLayout;
        final TextView columnNameTextView;
        final TextView columnEnabledTextView;
        final TextView columnPositionTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.linearLayout = (LinearLayout) itemView;
            this.columnNameTextView = itemView.findViewById(R.id.columnNameValue);
            this.columnEnabledTextView = itemView.findViewById(R.id.columnEnabledValue);
            this.columnPositionTextView = itemView.findViewById(R.id.columnPositionValue);
        }
    }
}
