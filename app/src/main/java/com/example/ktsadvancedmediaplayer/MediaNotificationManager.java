package com.example.ktsadvancedmediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession;
import android.os.Build;

public class MediaNotificationManager {
    public static final String CHANNEL_ID = "kts_media_playback_channel";
    public static final int NOTIFICATION_ID = 2001;

    public static final String ACTION_REWIND = "com.example.ktsadvancedmediaplayer.ACTION_REWIND";
    public static final String ACTION_PREV = "com.example.ktsadvancedmediaplayer.ACTION_PREV";
    public static final String ACTION_PLAY_PAUSE = "com.example.ktsadvancedmediaplayer.ACTION_PLAY_PAUSE";
    public static final String ACTION_NEXT = "com.example.ktsadvancedmediaplayer.ACTION_NEXT";
    public static final String ACTION_FORWARD = "com.example.ktsadvancedmediaplayer.ACTION_FORWARD";
    public static final String ACTION_STOP = "com.example.ktsadvancedmediaplayer.ACTION_STOP";

    private final Context context;
    private final NotificationManager notificationManager;

    public MediaNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Media Playback",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Audio playback controls and lock screen notification");
            channel.setShowBadge(false);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public Notification buildNotification(
            MediaSession.Token sessionToken,
            String title,
            String subtitle,
            boolean isPlaying
    ) {
        if (title == null || title.isEmpty()) {
            title = "KTS Advanced Media Player";
        }
        if (subtitle == null) {
            subtitle = "";
        }

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        PendingIntent rewindPI = createActionIntent(ACTION_REWIND, 1);
        PendingIntent prevPI = createActionIntent(ACTION_PREV, 2);
        PendingIntent playPausePI = createActionIntent(ACTION_PLAY_PAUSE, 3);
        PendingIntent nextPI = createActionIntent(ACTION_NEXT, 4);
        PendingIntent forwardPI = createActionIntent(ACTION_FORWARD, 5);
        PendingIntent stopPI = createActionIntent(ACTION_STOP, 6);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        builder.setSmallIcon(R.drawable.ic_notif_media)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setContentIntent(contentIntent)
                .setDeleteIntent(stopPI)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setOngoing(isPlaying)
                .setShowWhen(false);

        // 5 actions exactly in order as requested:
        // 0: Rewind
        // 1: Previous
        // 2: Play/Pause
        // 3: Next Track
        // 4: Fast Forwarding
        builder.addAction(new Notification.Action.Builder(
                R.drawable.ic_notif_rewind,
                "Rewind",
                rewindPI
        ).build());

        builder.addAction(new Notification.Action.Builder(
                R.drawable.ic_notif_prev,
                "Previous",
                prevPI
        ).build());

        builder.addAction(new Notification.Action.Builder(
                isPlaying ? R.drawable.ic_notif_pause : R.drawable.ic_notif_play,
                isPlaying ? "Pause" : "Play",
                playPausePI
        ).build());

        builder.addAction(new Notification.Action.Builder(
                R.drawable.ic_notif_next,
                "Next Track",
                nextPI
        ).build());

        builder.addAction(new Notification.Action.Builder(
                R.drawable.ic_notif_forward,
                "Fast Forwarding",
                forwardPI
        ).build());

        Notification.MediaStyle mediaStyle = new Notification.MediaStyle();
        if (sessionToken != null) {
            mediaStyle.setMediaSession(sessionToken);
        }
        // In compact view: Previous (1), Play/Pause (2), Next (3)
        // In expanded view and on Lock Screen: all 5 actions (0, 1, 2, 3, 4) are shown
        mediaStyle.setShowActionsInCompactView(1, 2, 3);
        builder.setStyle(mediaStyle);

        return builder.build();
    }

    private PendingIntent createActionIntent(String action, int requestCode) {
        Intent intent = new Intent(context, MediaActionReceiver.class);
        intent.setAction(action);
        intent.setPackage(context.getPackageName());
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public void showNotificationDirectly(Notification notification) {
        try {
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
        } catch (Exception ignored) {}
    }

    public void cancelNotification() {
        try {
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignored) {}
    }
}
