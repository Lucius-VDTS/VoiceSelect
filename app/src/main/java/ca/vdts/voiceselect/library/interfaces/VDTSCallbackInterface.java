package ca.vdts.voiceselect.library.interfaces;

/**
 * Interface used to create single argument callback functions
 * @param <Arg>
 */
public interface VDTSCallbackInterface<Arg> {
    void callback(Arg arg);
}
