package ca.vdts.voiceselect.library.utilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

public class VDTSCustomLifecycle implements LifecycleOwner {
    private final LifecycleRegistry lifecycleRegistry;

    public VDTSCustomLifecycle() {
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.INITIALIZED);
    }

    public void performEvent(Lifecycle.Event event) {
        lifecycleRegistry.handleLifecycleEvent(event);
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}
