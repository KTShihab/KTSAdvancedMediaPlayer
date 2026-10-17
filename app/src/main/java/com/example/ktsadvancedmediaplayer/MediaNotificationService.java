package com.example.ktsadvancedmediaplayer;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;

public class MediaNotificationService extends Service {
    public static final String ACTION_UPDATE = "com.example.ktsadvancedmediaplayer.action.UPDATE_NOTIF";
    public static final String ACTION_STOP = "com.example.ktsadvancedmediaplayer.action.STOP_NOTIF";
    public static final String EXTRA_TITLE = "extra_title";
    public static final String EXTRA_SUBTITLE = "extra_subtitle";
    public static final String EXTRA_IS_PLAYING = "extra_is_playing";
    public static final String EXTRA_TOKEN = "extra_token";

    private MediaNotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = new MediaNotificationManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                } else {
                    stopForeground(true);
                }
            } catch (Exception ignored) {}
            if (notificationManager != null) {
                notificationManager.cancelNotification();
            }
            stopSelf();
            return START_NOT_STICKY;
        }

        String title = intent.getStringExtra(EXTRA_TITLE);
        String subtitle = intent.getStringExtra(EXTRA_SUBTITLE);
        boolean isPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false);
        MediaSession.Token token = intent.getParcelableExtra(EXTRA_TOKEN);

        Notification notification = notificationManager.buildNotification(token, title, subtitle, isPlaying);

        if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(this)) {
            AudioRoutingManager.getInstance().ensureAudioSeparationStable();
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(MediaNotificationManager.NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            notificationManager.showNotificationDirectly(notification);
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (notificationManager != null) {
            notificationManager.cancelNotification();
        }
        super.onDestroy();
    }
}
