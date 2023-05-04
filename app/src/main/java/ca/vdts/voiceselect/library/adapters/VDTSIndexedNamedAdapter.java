package ca.vdts.voiceselect.library.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.library.interfaces.VDTSFilterableInterface;
import ca.vdts.voiceselect.library.interfaces.VDTSIndexedNamedEntityInterface;
import ca.vdts.voiceselect.library.services.VDTSAdapterClickListenerService;
import ca.vdts.voiceselect.library.services.VDTSNameOrderService;

/**
 * Generic recycler view adapter for entities with an index and name.
 * @param <Entity>
 */
public class VDTSIndexedNamedAdapter<Entity extends VDTSIndexedNamedEntityInterface>
        extends RecyclerView.Adapter<VDTSIndexedNamedAdapter.ViewHolder>
        implements VDTSFilterableInterface {
    private final VDTSAdapterClickListenerService selectedListener;
    Context context;

    private final List<Entity> dataset = new ArrayList<>();
    private List<Entity> filteredDataset = new ArrayList<>();
    private String oldCriteria;
    private String filterCriteria;
    int selectedIndex = -1;

    private BiFunction<Entity, Integer, String> toStringFunction;

    public VDTSIndexedNamedAdapter(VDTSAdapterClickListenerService selectedListener,
                                   Context context, List<Entity> dataset) {
        this.selectedListener = selectedListener;
        this.context = context;
        setDataset(dataset);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.recycler_view_indexed_named, parent, false);

        if (selectedListener != null) {
            view.setOnClickListener(selectedListener);
        }

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Entity entity;

        if (filterCriteria == null) {
            entity = dataset.get(position);
        } else {
            entity = filteredDataset.get(position);
        }

        holder.indexTextView.setText(String.format(
                Locale.getDefault(),"%d", position + 1));

        if (toStringFunction != null) {
            holder.nameTextView.setText(toStringFunction.apply(entity, position));
        } else {
            holder.nameTextView.setText(entity.name());
        }

        if (position == selectedIndex) {
            holder.linearLayout.setBackgroundColor(
                    context.getColor(R.color.colorBackgroundSelected));
        } else {
            if (position % 2 == 0) {
                holder.linearLayout.setBackgroundColor(
                        context.getColor(R.color.colorBackgroundPrimary));
            } else {
                holder.linearLayout.setBackgroundColor(
                        context.getColor(R.color.colorBackgroundCyan));
            }
        }

        holder.linearLayout.setOnClickListener(selectedListener);
    }

    public void add(Entity entity) {
        this.dataset.add(entity);
        this.notifyItemChanged(dataset.size() - 1);
    }

    /**
     * Filter entities in the dataset whose names start with the input criteria. If the criteria is
     * the same as the current filter criteria then this function does nothing.
     *
     * @param criteria - The string to filter by.
     */
    @Override
    public void filter(String criteria) {
        if (filterCriteria == null || !filterCriteria.equals(criteria.toLowerCase())) {
            filter_impl(criteria);
            setDataset(dataset);
        }
    }

    private void filter_impl(String criteria) {
        filterCriteria = criteria.toLowerCase();
        filteredDataset = dataset.stream()
                .filter(c->c.name().toLowerCase().startsWith(filterCriteria))
                .collect(Collectors.toList());
        if (filteredDataset.size() < 1){
            if (oldCriteria != null) {
                filter_impl(oldCriteria);
            } else {
                clearFilter();
            }
        }
    }

    /**
     * Concatenate the current filter with another filter. If there is no current filter,
     * then this function acts as if {@link #filter(String)} was called.
     *
     * @param criteria - The string to add to the current filter.
     */
    public void addFilter (String criteria) {
        oldCriteria = filterCriteria;
        if (filterCriteria == null) {
            filter(criteria);
        } else {
            filter(filterCriteria + criteria);
        }
    }

    /**
     * Clears the filter, restoring the unfiltered dataset.
     * If there is no filter this function does nothing.
     */
    @Override
    public void clearFilter() {
        if (filterCriteria != null) {
            filteredDataset = null;
            filterCriteria = null;
            setDataset(dataset);
        }
    }

    @Override
    public String getFilter() {
        if (filterCriteria != null) {
            return filterCriteria;
        } else {
            return "";
        }
    }

    public void setDataset(List<Entity> dataset) {
        this.dataset.clear();
        this.dataset.addAll(dataset);
        this.dataset.sort(VDTSNameOrderService.getInstance());
        notifyDataSetChanged();
    }

    public Entity getEntity(int index) {
        if (filterCriteria == null) {
            return dataset.get(index);
        } else {
            return filteredDataset.get(index);
        }
    }

    public void updateEntity(Entity entity) {
        int index = dataset.indexOf(entity);
        dataset.remove(entity);
        dataset.add(index, entity);
        notifyItemChanged(index);
    }

    public Entity getSelectedEntity() {
        if (selectedIndex >= 0 && selectedIndex < dataset.size()) {
            return dataset.get(selectedIndex);
        }
        return null;
    }

    public int getSelectedEntityIndex() {
        return this.selectedIndex;
    }

    public void setSelectedEntity(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < dataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void updateSelectedEntity() {
       notifyItemChanged(this.selectedIndex);
    }

    public void removeSelectedEntity() {
        Entity entity = dataset.get(selectedIndex);
        dataset.remove(entity);
        notifyDataSetChanged();
        //todo - notifyItemRemoved does not reset indices - use diffUtil to recalc indices and remove notifyDataSetChanged
        //notifyItemRemoved(selectedIndex);
        selectedIndex = -1;
        setSelectedEntity(selectedIndex);
    }

    @Override
    public int getItemCount() {
        if (filterCriteria == null) {
            return dataset.size();
        } else {
            return filteredDataset.size();
        }
    }

////VIEWHOLDER_SUBCLASS/////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout linearLayout;
        final TextView indexTextView;
        final TextView nameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.linearLayout = (LinearLayout) itemView;
            this.indexTextView = itemView.findViewById(R.id.index);
            this.nameTextView = itemView.findViewById(R.id.name);
        }
    }
}