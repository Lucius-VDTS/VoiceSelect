package ca.vdts.voiceselect.library.utilities;

import android.os.Looper;
import android.util.Log;

public class VDTSTaskUtil {
    /**
     * Perform an action on a separate thread and block until the action is finished
     * @param action - Action to perform
     */
    public static void performSynchronousAction(Runnable action){
        final Thread syncThread = new Thread(action);
        syncThread.start();
        try {
            syncThread.join();
        } catch (InterruptedException e) {
            Log.e(VDTSToolUtil.class.getName(), "performSynchronousAction: ", e);
        }
    }

    /**
     * Perform an action on a separate thread and do not block. Fire and forget.
     * @param action - Action to perform
     */
    public static void performAsynchronousAction(Runnable action){
        new Thread(action).start();
    }

    /**
     * Perform an action on a separate thread and block if the current thread is the main thread.
     * otherwise perform the action on the current thread
     * @param action - Sction to perform
     */
    public static void offMainThread(Runnable action,boolean synchronous){
        if(Looper.getMainLooper().getThread() == Thread.currentThread()){
            if(synchronous) {
                performSynchronousAction(action);
            }else{
                performAsynchronousAction(action);
            }
        }else{
            action.run();
        }
    }
}
