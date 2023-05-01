package ca.vdts.voiceselect.library.services;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import ca.vdts.voiceselect.library.interfaces.VDTSCallbackInterface;

/**
 * Click listener used by adapters to interact with recycler views
 */
public abstract class VDTSAdapterClickListenerService implements View.OnClickListener{
    final VDTSCallbackInterface<Integer> callback;
    final RecyclerView recyclerView;

    VDTSAdapterClickListenerService(VDTSCallbackInterface<Integer> callback,
                                    RecyclerView recyclerView) {
        this.callback = callback;
        this.recyclerView = recyclerView;
    }
}
