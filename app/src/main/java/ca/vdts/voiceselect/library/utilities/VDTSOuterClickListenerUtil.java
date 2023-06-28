package ca.vdts.voiceselect.library.utilities;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import ca.vdts.voiceselect.library.interfaces.VDTSCallbackInterface;

public class VDTSOuterClickListenerUtil extends VDTSAdapterClickListenerUtil {
    public VDTSOuterClickListenerUtil(VDTSCallbackInterface<Integer> callback, RecyclerView recyclerView) {
        super(callback, recyclerView);
    }

    public void onClick(View v) {
        int position = this.recyclerView.getChildAdapterPosition(v);
        this.callback.callback(position);
    }
}
