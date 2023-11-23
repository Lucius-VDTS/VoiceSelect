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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import ca.vdts.voiceselect.R;
import ca.vdts.voiceselect.database.entities.Session;
import ca.vdts.voiceselect.library.utilities.VDTSAdapterClickListenerUtil;

@SuppressLint("NotifyDataSetChanged")
public class RecallSessionRecyclerAdapter extends RecyclerView.Adapter<RecallSessionRecyclerAdapter.ViewHolder> {
    private final Context context;
    private List<Session> sessionDataset;

    private List<Session> filteredSessionDataset;
    private String filterCriteria = "";
    private boolean filterOpen;

    private final VDTSAdapterClickListenerUtil selectedListener;
    private int selectedIndex = -1;

    public RecallSessionRecyclerAdapter(List<Session> sessions, boolean filterOpen,Context context, VDTSAdapterClickListenerUtil selectedListener) {
        sessionDataset = sessions;
        filteredSessionDataset = new ArrayList<>(sessions);
        this.filterOpen = filterOpen;
        this.context = context;
        this.selectedListener = selectedListener;
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
        if (isFiltered()) {
            session = this.sessionDataset.get(position);
        } else {
            session = this.filteredSessionDataset.get(position);
        }

        int size = sessionDataset.size();

        holder.idView.setText(String.valueOf(position+1));
        holder.sessionView.setText(session.name());
        if (session.getEndDate() ==null ) {
            holder.statusView.setText(R.string.recall_status_available);
        } else {
            holder.statusView.setText(R.string.recall_status_finished);
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

    public boolean isFiltered(){
        return filterOpen || !filterCriteria.isEmpty();
    }

    public void setSessionDataset(final List<Session> sessions) {
        this.sessionDataset = sessions;
        filter(filterCriteria);
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
            if (isFiltered()) {
                return sessionDataset.get(selectedIndex);
            } else {
                return filteredSessionDataset.get(selectedIndex);
            }
        } else {
            return null;
        }
    }

    public void clearSelected() {
        int old = selectedIndex;
        selectedIndex = -1;
        if (old >= 0 && old < sessionDataset.size()) {
            notifyItemChanged(old);
        }
    }

    public void addFilter(String criteria) {
        filterCriteria = criteria != null? criteria : "";
        filter(filterCriteria);
    }

    public boolean isFilterOpen() {
        return filterOpen;
    }

    public void setFilterOpen(boolean filterOpen) {
        this.filterOpen = filterOpen;
        filter(filterCriteria);
    }

    // Filter Class
    private void filter(String charText) {
        selectedIndex = -1;
        charText = charText.toLowerCase(Locale.getDefault());
        filteredSessionDataset.clear();
        List<Session> tempList;
        if (filterOpen){
            tempList = sessionDataset.stream().filter(session -> session.getEndDate() == null).collect(Collectors.toList());
        } else {
            tempList = new ArrayList<>(sessionDataset);
        }

        if (charText.length() == 0) {
            filteredSessionDataset.addAll(tempList);
        } else {
            for (Session wp :tempList) {
                if (wp.name().toLowerCase(Locale.getDefault()).contains(charText)) {
                    filteredSessionDataset.add(wp);
                }
            }
        }
        notifyDataSetChanged();
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
