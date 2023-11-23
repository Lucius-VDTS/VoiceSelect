package ca.vdts.voiceselect.activities.dataGathering;

/**
 * Interface used to setScrollChangeListener on ObservableHorizontalScrollView
 */
public interface ScrollChangeListenerInterface {
    void onScrollChanged(ObservableHorizontalScrollView observableHorizontalScrollView,
                                int x, int y,
                                int oldx, int oldy);
}
