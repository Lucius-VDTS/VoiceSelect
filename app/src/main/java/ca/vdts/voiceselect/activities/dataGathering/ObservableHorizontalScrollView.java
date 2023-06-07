package ca.vdts.voiceselect.activities.dataGathering;

import android.content.Context;
import android.util.AttributeSet;

public class ObservableHorizontalScrollView extends android.widget.HorizontalScrollView {
    private ScrollChangeListenerInterface scrollChangeListenerInterface;

    public ObservableHorizontalScrollView(Context context) {
        super(context);
    }

    public ObservableHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ObservableHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ObservableHorizontalScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setScrollChangeListener(ScrollChangeListenerInterface scrollChangeListenerInterface) {
        this.scrollChangeListenerInterface = scrollChangeListenerInterface;
    }

    @Override
    protected void onScrollChanged(int x, int y, int oldx, int oldy) {
        if (scrollChangeListenerInterface != null) {
            scrollChangeListenerInterface.onScrollChanged(this, x, y, oldx, oldy);
        }
    }
}
