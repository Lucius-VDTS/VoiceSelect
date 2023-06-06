package ca.vdts.voiceselect.library.utilities;

import android.os.Looper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VDTSTaskUtil {
    private static final Logger LOG = LoggerFactory.getLogger(VDTSTaskUtil.class);

    /**
     * Perform an action on a separate thread and block until the action is finished
     * @param action - Action to perform
     */
    public static void performSynchronousAction(Runnable action) {
        final Thread syncThread = new Thread(action);
        syncThread.start();
        try {
            syncThread.join();
        } catch (InterruptedException e) {
            LOG.error("performSynchronousAction: ", e);
        }
    }

    /**
     * Perform an action on a separate thread and do not block. Fire and forget.
     * @param action - Action to perform
     */
    public static void performAsynchronousAction(Runnable action) {
        new Thread(action).start();
    }

    /**
     * Perform an action on a separate thread and block if the current thread is the main thread.
     * otherwise perform the action on the current thread
     * @param action - Action to perform
     */
    public static void offMainThread(Runnable action, boolean synchronous) {
        if (Looper.getMainLooper().getThread().equals(Thread.currentThread())) {
            if (synchronous) {
                performSynchronousAction(action);
            } else {
                performAsynchronousAction(action);
            }
        } else {
            action.run();
        }
    }
}
