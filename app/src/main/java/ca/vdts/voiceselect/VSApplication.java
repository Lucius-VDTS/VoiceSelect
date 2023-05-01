package ca.vdts.voiceselect;

import com.iristick.sdk.IristickSDK;

import ca.vdts.voiceselect.library.VDTSApplication;

public class VSApplication extends VDTSApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        IristickSDK.setup(this);
    }
}