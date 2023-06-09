package ca.vdts.voiceselect.library.utilities;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import ca.vdts.voiceselect.library.interfaces.VDTSCallbackInterface;

/**
 * Click listener used by adapter
 */
public class VDTSClickListenerUtil extends VDTSAdapterClickListenerUtil {
    public VDTSClickListenerUtil(VDTSCallbackInterface<Integer> callback,
                                 RecyclerView recyclerView) {
        super(callback, recyclerView);
    }

    public void onClick(View v) {
        final int position = recyclerView.getChildAdapterPosition(v);
        callback.callback(position);
    }
}
