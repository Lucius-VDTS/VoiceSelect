package ca.vdts.voiceselect.adapters;


import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.database.entities.VDTSUser;
import ca.vdts.voiceselect.library.interfaces.VDTSFilterableInterface;
import ca.vdts.voiceselect.library.utilities.VDTSAdapterClickListenerUtil;

@SuppressLint("NotifyDataSetChanged")
public class SessionAdapter extends RecyclerView.Adapter<SessionAdapter.ViewHolder>
        implements VDTSFilterableInterface {
    private final Context context;
    private List<Session> sessionDataset;
    private HashMap<Long, VDTSUser> userMap;

    private List<Session> filteredSessionDataset;
    private String oldCriteria = null;
    private String filterCriteria;

    private final VDTSAdapterClickListenerUtil selectedListener;
    private int selectedIndex = -1;

    public SessionAdapter(List<Session> sessions, Context context, HashMap<Long,
            VDTSUser> userMap, VDTSAdapterClickListenerUtil selectedListener) {
        sessionDataset = sessions;
        this.context = context;
        this.userMap = userMap;
        this.selectedListener = selectedListener;
    }

    public Session getItem(int i) {
        return filterCriteria == null ? sessionDataset.get(i) : filteredSessionDataset.get(i);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.adapter_recycler_session, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Session session;
        if (this.filterCriteria == null) {
            session = this.sessionDataset.get(position);
        } else {
            session = this.filteredSessionDataset.get(position);
        }

        int size = sessionDataset.size();

        holder.idView.setText(String.valueOf(position+1));
        holder.sessionView.setText(session.name());
        if (session.getEndDate() !=null ) {
            holder.statusView.setText(R.string.status_open);
        } else {
            holder.statusView.setText(R.string.status_closed);
        }

        if (size == 1) {
            holder.constraintLayout.setBackgroundResource(R.drawable.recycler_view_item);
        } else if (position == 0) {
            holder.constraintLayout.setBackgroundResource(R.drawable.recycler_view_first_item);
        } else if (position == size - 1) {
            holder.constraintLayout.setBackgroundResource(R.drawable.recycler_view_last_item);
        } else {
            holder.constraintLayout.setBackgroundResource(R.drawable.recycler_view_middle_item);
        }

        Drawable backgroundResource = holder.constraintLayout.getBackground();
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

        holder.constraintLayout.setOnClickListener(selectedListener);
    }

    @Override
    public int getItemCount() {
        return filterCriteria == null ? sessionDataset.size() : filteredSessionDataset.size();
    }

    public void setSessionDataset(final List<Session> sessions) {
        this.sessionDataset = sessions;
        if(filterCriteria!=null)filter_impl(filterCriteria);
        notifyDataSetChanged();
    }

    public void setUserMap(final HashMap<Long, VDTSUser> userMap) {
        this.userMap = userMap;
        notifyDataSetChanged();
    }

    public void setSelected(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        notifyItemChanged(index);
        if (old >= 0 && old < sessionDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public Session getSelected() {
        if (selectedIndex>=0) {
            return sessionDataset.get(selectedIndex);
        } else {
            return null;
        }
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        notifyItemChanged(old);
    }

    /**
     * concatenate the current filter with another filter. If there is no current filter,
     * then this function acts as if {@link #filter(String)} was called
     *
     * @param criteria the string to add to the current filter
     */
    @Override
    public void addFilter(String criteria) {
        oldCriteria = filterCriteria;
        filter(filterCriteria == null ? criteria : filterCriteria + criteria);
    }

    /**
     * Filter entities in the dataset whose names start with the input criteria. If the criteria is
     * the same as the current filter criteria then this function does nothing.
     *
     * @param criteria the string to filter by
     */
    @Override
    public void filter(String criteria) {
        if (filterCriteria == null || !filterCriteria.equals(criteria.toLowerCase())) {
            filter_impl(criteria);
            notifyDataSetChanged();
        }
    }

    private void filter_impl(String criteria) {
        filterCriteria = criteria.toLowerCase();
        filteredSessionDataset = sessionDataset.stream()
                .filter(c->c.name().toLowerCase().startsWith(filterCriteria))
                .collect(Collectors.toList());
        if (filteredSessionDataset.size() < 1){
            if (oldCriteria != null) {
                filter_impl(oldCriteria);
            } else {
                clearFilter();
            }
        }
    }

    @Override
    public void clearFilter() {
        if (filterCriteria != null) {
            filteredSessionDataset = null;
            filterCriteria = null;
            notifyDataSetChanged();
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

////VIEW_HOLDER_SUBCLASS////////////////////////////////////////////////////////////////////////////
    static class ViewHolder extends RecyclerView.ViewHolder {
        final ConstraintLayout constraintLayout;
        final TextView idView;
        final TextView sessionView;
        final TextView statusView;

        ViewHolder(View view) {
            super(view);
            constraintLayout = (ConstraintLayout) view;
            idView = view.findViewById(R.id.sessionIndexValue);
            sessionView = view.findViewById(R.id.selectorSessionValue);
            statusView = view.findViewById(R.id.statusValue);
        }
    }
}
