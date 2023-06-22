package ca.vdts.voiceselect.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    private Column selectedColumn;
    private int selectedIndex = -1;
    private final List<Column> columnDataset = new ArrayList<>();
    private final List<LayoutColumn> layoutColumnDataset = new ArrayList<>();

    public ConfigLayoutsAdapter(Context context, VDTSAdapterClickListenerUtil selectedListener,
                                List<Column> columnList,
                                List<LayoutColumn> layoutColumnList) {
        this.context = context;
        this.selectedListener = selectedListener;
        setColumnDataset(columnList);
        setLayoutColumnDataset(layoutColumnList);
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
        selectedColumn = columnDataset.get(position);
        int size = columnDataset.size();

        if (selectedColumn != null) {
            //Name
            holder.columnNameTextView.setText(selectedColumn.getName());

            //Enabled - Position
            LayoutColumn enabledLayoutColumn = layoutColumnDataset.stream()
                    .filter(column -> column.getColumnID() == selectedColumn.getUid())
                    .findFirst()
                    .orElse(null);

            if (enabledLayoutColumn == null) {
                holder.columnEnabledTextView.setText(R.string.layout_column_enabled_no);
                holder.columnPositionTextView.setText("");
            } else {
                holder.columnEnabledTextView.setText(R.string.layout_column_enabled_yes);
                holder.columnPositionTextView.setText(
                        String.format(
                                Locale.getDefault(),
                                "%d",
                                enabledLayoutColumn.getColumnPosition()
                        )
                );
            }
        } else {
            holder.columnNameTextView.setText("");
            holder.columnEnabledTextView.setText("");
            holder.columnPositionTextView.setText("");
        }

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

    public void setColumnDataset(List<Column> columnDataset) {
        this.columnDataset.clear();
        this.columnDataset.addAll(columnDataset);
        sortColumns();
    }

    public void sortColumnDataset(List<LayoutColumn> layoutColumnDataset) {
        layoutColumnDataset.sort((layoutColumn1, layoutColumn2) ->
                (int) (layoutColumn1.getColumnPosition() - layoutColumn2.getColumnPosition()));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void sortColumns() {
        final Column selectedColumn = selectedIndex >= 0 ? columnDataset.get(selectedIndex) : null;

        columnDataset.sort(
                (c1, c2) -> {
                    LayoutColumn lc1 = layoutColumnDataset.stream()
                            .filter(column -> column.getColumnID() == c1.getUid())
                            .findFirst()
                            .orElse(null);
                    LayoutColumn lc2 = layoutColumnDataset.stream()
                            .filter(column -> column.getColumnID() == c2.getUid())
                            .findFirst()
                            .orElse(null);
                    if (lc1 != null && lc2 != null) {
                        return Long.compare(lc1.getColumnPosition(), lc2.getColumnPosition());
                    } else if (lc1 != null) {
                        return -1;
                    } else if (lc2 != null) {
                        return 1;
                    } else {
                        return c1.getName().compareTo(c2.getName());
                    }
                }
        );

        if (selectedColumn != null) {
            selectedIndex = columnDataset.indexOf(selectedColumn);
        } else {
            selectedIndex = -1;
        }

        notifyDataSetChanged();
    }

    public void adjustPositions(LayoutColumn selectedLayoutColumn, Long currentPosition) {
        if (currentPosition == null || currentPosition <= 0) {
            layoutColumnDataset.stream()
                    .filter(lc -> !lc.equals(selectedLayoutColumn))
                    .filter(lc -> lc.getColumnPosition() >= selectedLayoutColumn.getColumnPosition())
                    .forEach(lc -> lc.setColumnPosition(lc.getColumnPosition() + 1));
        } else if (selectedLayoutColumn.getColumnPosition() < currentPosition) {
            layoutColumnDataset.stream()
                    .filter(lc -> !lc.equals(selectedLayoutColumn))
                    .filter(lc -> lc.getColumnPosition() >= selectedLayoutColumn.getColumnPosition())
                    .filter(lc -> lc.getColumnPosition() < currentPosition)
                    .forEach(lc -> lc.setColumnPosition(lc.getColumnPosition() + 1));
        } else if (selectedLayoutColumn.getColumnPosition() > currentPosition) {
            layoutColumnDataset.stream()
                    .filter(lc -> !lc.equals(selectedLayoutColumn))
                    .filter(lc -> lc.getColumnPosition() <= selectedLayoutColumn.getColumnPosition())
                    .filter(lc -> lc.getColumnPosition() > currentPosition)
                    .forEach(lc -> lc.setColumnPosition(lc.getColumnPosition() - 1));
        }

        columnDataset.stream()
                .filter(column -> column.getUid() == selectedLayoutColumn.getColumnID())
                .findFirst()
                .ifPresent(
                        selectedColumn -> selectedIndex = columnDataset.indexOf(selectedColumn)
                );

        sortColumns();
    }

    public long lastPosition() {
        return layoutColumnDataset.stream()
                .mapToLong(LayoutColumn::getColumnPosition)
                .max()
                .orElse(0L);
    }

    public void addAllColumns(List<Column> columnList) {
        columnDataset.addAll(columnList);
        notifyDataSetChanged();
    }

    public void updateColumn(Column column) {
        int index = columnDataset.indexOf(column);
        columnDataset.remove(column);
        columnDataset.add(index, column);
    }

    public void updateSelectedColumn() {
        notifyItemChanged(selectedIndex);
    }

    public void removeAllColumns(List<Column> columnList) {
        columnDataset.removeAll(columnList);
        notifyDataSetChanged();
    }

    public Column getColumn(int index) {
        return columnDataset.get(index);
    }

    public int getSelectedColumnIndex() {
        return selectedIndex;
    }

    public void setSelectedColumn(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < columnDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void setLayoutColumnDataset(List<LayoutColumn> layoutColumnDataset) {
        this.layoutColumnDataset.clear();
        this.layoutColumnDataset.addAll(layoutColumnDataset);
        sortColumns();
    }

    public List<LayoutColumn> getLayoutColumnDataset() {
        return layoutColumnDataset;
    }

    public void addLayoutColumn(LayoutColumn layoutColumn) {
        layoutColumnDataset.add(layoutColumn);
        columnDataset.stream()
                .filter(column -> column.getUid() == layoutColumn.getColumnID())
                .findFirst()
                .ifPresent(
                        selectedColumn -> selectedIndex = columnDataset.indexOf(selectedColumn)
                );
        sortColumns();
    }

    public void updateLayoutColumn(LayoutColumn layoutColumn) {
        int index = layoutColumnDataset.indexOf(layoutColumn);
        layoutColumnDataset.remove(layoutColumn);
        layoutColumnDataset.add(index, layoutColumn);
        sortColumns();
    }

    public void removeLayoutColumn(LayoutColumn layoutColumn) {
        layoutColumnDataset.remove(layoutColumn);
        layoutColumnDataset.stream()
                .filter(lc -> lc.getColumnPosition() > layoutColumn.getColumnPosition())
                .forEach(lc -> lc.setColumnPosition(lc.getColumnPosition() - 1));
        columnDataset.stream()
                .filter(column -> column.getUid() == layoutColumn.getColumnID())
                .findFirst()
                .ifPresent(
                        selectedColumn -> selectedIndex = columnDataset.indexOf(selectedColumn)
                );
        sortColumns();
    }

    public Pair<Column, LayoutColumn> getSelectedColumnLayoutColumn() {
        if (selectedIndex >= 0 && selectedIndex < columnDataset.size()) {
            LayoutColumn layoutColumn = layoutColumnDataset.stream()
                    .filter(column -> column.getColumnID() == getColumn(selectedIndex).getUid())
                    .findFirst()
                    .orElse(null);

            return new Pair<>(getColumn(selectedIndex), layoutColumn);
        }
        return null;
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        notifyItemChanged(old);
    }

    @Override
    public int getItemCount() {
        return columnDataset.size();
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
