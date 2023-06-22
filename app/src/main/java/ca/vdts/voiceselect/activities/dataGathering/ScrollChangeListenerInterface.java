package ca.vdts.voiceselect.activities.dataGathering;

/**
 * Interface primarily used to setScrollChangeListener on ObservableHorizontalScrollView
 */
public interface ScrollChangeListenerInterface {
    public void onScrollChanged(ObservableHorizontalScrollView observableHorizontalScrollView,
                                int x, int y,
                                int oldx, int oldy);
}
