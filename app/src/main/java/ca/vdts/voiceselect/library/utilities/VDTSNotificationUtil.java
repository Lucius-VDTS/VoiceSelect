package ca.vdts.voiceselect.library.utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

import java.util.Locale;

import ca.vdts.voiceselect.R;

/**
 * Notification service that includes channels for the iristick
 */
public class VDTSNotificationUtil extends Notification.Builder {
    @NonNull
    private final NotificationManager notificationManager;

    final int id;
    private static int nextID = 1;

    public enum Channel {
        ERROR("Errors", NotificationManager.IMPORTANCE_HIGH),
        FIRMWARE("Firmware sync progress", NotificationManager.IMPORTANCE_HIGH),
        DOWNLOAD("Download progress", NotificationManager.IMPORTANCE_HIGH),
        BACKGROUND("Background", NotificationManager.IMPORTANCE_LOW),
        CONNECTED("Iristick connected", NotificationManager.IMPORTANCE_HIGH),
        DISCONNECTED("Iristick disconnected", NotificationManager.IMPORTANCE_HIGH);

        @NonNull
        final String id;
        @NonNull
        final String title;
        final int importance;

        Channel(@NonNull String title, int importance) {
            this.id = name().toLowerCase(Locale.ROOT);
            this.title = title;
            this.importance = importance;
        }
    }

    @MainThread
    public VDTSNotificationUtil(@NonNull Context context, Channel channel) {
        super(context, channel.id);
        id = nextID++;
        notificationManager = context.getSystemService(NotificationManager.class);
        setOnlyAlertOnce(true);
        setSmallIcon(R.drawable.ic_hourglass_empty_black_24dp);
    }

    @MainThread
    public static void init(@NonNull Context context) {
        final NotificationManager notificationManager =
                context.getSystemService(NotificationManager.class);

        for (Channel channel : Channel.values()) {
            notificationManager.createNotificationChannel(new NotificationChannel(
                    channel.id,
                    channel.title,
                    channel.importance));
        }
    }

    @MainThread
    public void show() {
        notificationManager.notify(id, build());
    }

    @MainThread
    public void cancel() {
        notificationManager.cancel(id);
    }
}
