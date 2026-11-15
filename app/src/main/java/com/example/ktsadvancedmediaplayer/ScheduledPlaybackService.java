package com.example.ktsadvancedmediaplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScheduledPlaybackService extends Service {
    private static final String TAG = "ScheduledPlaybackService";
    public static final String ACTION_SCHEDULED_PLAY = "com.example.ktsadvancedmediaplayer.ACTION_SCHEDULED_PLAY";
    public static final String ACTION_STOP_SCHEDULED = "com.example.ktsadvancedmediaplayer.action.STOP_SCHEDULED";

    public static final String EXTRA_SCHEDULED_PATH = "extra_scheduled_path";
    public static final String EXTRA_SCHEDULED_TITLE = "extra_scheduled_title";
    public static final String EXTRA_SCHEDULED_IS_CONTAINER = "extra_scheduled_is_container";
    public static final String EXTRA_SCHEDULED_IS_ZIP = "extra_scheduled_is_zip";

    public static final String CHANNEL_ID = "kts_scheduled_playback_channel";
    public static final int NOTIFICATION_ID = 2005;

    private static ScheduledPlaybackService instance = null;

    private ExoPlayer player;
    private PowerManager.WakeLock wakeLock;
    private String currentPlayingPath;
    private String currentTitle;

    public static ScheduledPlaybackService getInstance() {
        return instance;
    }

    public static boolean isPlaying() {
        return instance != null && instance.player != null && instance.player.isPlaying();
    }

    public String getCurrentPlayingPath() {
        try {
            if (player != null && player.getCurrentMediaItem() != null && player.getCurrentMediaItem().localConfiguration != null) {
                Uri uri = player.getCurrentMediaItem().localConfiguration.uri;
                if (uri != null && uri.getPath() != null) {
                    return uri.getPath();
                }
            }
        } catch (Exception ignored) {}
        return currentPlayingPath;
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Scheduled Playback",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Scheduled audio playback alarm and controls");
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (ACTION_STOP_SCHEDULED.equals(action)) {
            stopPlaybackAndRelease();
            return START_NOT_STICKY;
        }

        if (ACTION_SCHEDULED_PLAY.equals(action)) {
            String path = intent.getStringExtra(EXTRA_SCHEDULED_PATH);
            String title = intent.getStringExtra(EXTRA_SCHEDULED_TITLE);
            boolean isContainer = intent.getBooleanExtra(EXTRA_SCHEDULED_IS_CONTAINER, false);
            boolean isZip = intent.getBooleanExtra(EXTRA_SCHEDULED_IS_ZIP, false);

            acquireWakeLocks();
            startForegroundNotification(title != null ? title : "Scheduled Playback", path, isContainer, isZip);

            MainActivity main = MainActivity.getInstance();
            if (main != null) {
                main.runOnUiThread(() -> main.executeScheduledPlay(path, title, isContainer, isZip));
                new Handler(Looper.getMainLooper()).postDelayed(this::stopPlaybackAndRelease, 2000);
                return START_NOT_STICKY;
            }

            // If MainActivity is not active in memory, play directly in this background service!
            startServicePlayback(path, title, isContainer, isZip);

            // Also try to launch MainActivity so that if permissions allow, the full UI pops up
            try {
                Intent activityIntent = new Intent(this, MainActivity.class);
                activityIntent.setAction(ACTION_SCHEDULED_PLAY);
                activityIntent.putExtra(EXTRA_SCHEDULED_PATH, path);
                activityIntent.putExtra(EXTRA_SCHEDULED_TITLE, title);
                activityIntent.putExtra(EXTRA_SCHEDULED_IS_CONTAINER, isContainer);
                activityIntent.putExtra(EXTRA_SCHEDULED_IS_ZIP, isZip);
                activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(activityIntent);
            } catch (Exception ignored) {}
        }

        return START_NOT_STICKY;
    }

    private void acquireWakeLocks() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                if (wakeLock == null || !wakeLock.isHeld()) {
                    wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KTSMediaPlayer:ScheduledPlaybackWake");
                    wakeLock.acquire(30 * 60 * 1000L); // Hold for up to 30 mins
                }
                @SuppressWarnings("deprecation")
                PowerManager.WakeLock screenLock = pm.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "KTSMediaPlayer:ScheduledPlayScreenWake"
                );
                screenLock.acquire(10000);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error acquiring wake locks", e);
        }
    }

    private void startForegroundNotification(String title, String path, boolean isContainer, boolean isZip) {
        Intent fullScreenIntent = new Intent(this, MainActivity.class);
        fullScreenIntent.setAction(MainActivity.ACTION_SCHEDULED_PLAY);
        fullScreenIntent.putExtra(EXTRA_SCHEDULED_PATH, path);
        fullScreenIntent.putExtra(EXTRA_SCHEDULED_TITLE, title);
        fullScreenIntent.putExtra(EXTRA_SCHEDULED_IS_CONTAINER, isContainer);
        fullScreenIntent.putExtra(EXTRA_SCHEDULED_IS_ZIP, isZip);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent fullScreenPI = PendingIntent.getActivity(this, 9022, fullScreenIntent, flags);

        Intent stopIntent = new Intent(this, ScheduledPlaybackService.class);
        stopIntent.setAction(ACTION_STOP_SCHEDULED);
        PendingIntent stopPI = PendingIntent.getService(this, 9023, stopIntent, flags);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_notif_media)
                .setContentTitle(title)
                .setContentText("Playing scheduled audio")
                .setContentIntent(fullScreenPI)
                .setFullScreenIntent(fullScreenPI, true)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(
                        R.drawable.ic_notif_play,
                        "Stop",
                        stopPI
                ).build());

        Notification notification = builder.build();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start foreground", e);
        }
    }

    private void startServicePlayback(String path, String title, boolean isContainer, boolean isZip) {
        if (path == null || path.isEmpty()) return;
        currentPlayingPath = path;
        currentTitle = title;

        try {
            if (player == null) {
                AudioAttributes audioAttrs = new AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build();

                player = new ExoPlayer.Builder(this)
                        .setAudioAttributes(audioAttrs, true)
                        .setHandleAudioBecomingNoisy(true)
                        .setWakeMode(C.WAKE_MODE_LOCAL)
                        .build();

                player.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int playbackState) {
                        if (playbackState == Player.STATE_ENDED) {
                            stopPlaybackAndRelease();
                        }
                    }
                });
            }

            File target = new File(path);
            if (!target.exists()) {
                Log.e(TAG, "Target file does not exist: " + path);
                return;
            }

            List<MediaItem> items = new ArrayList<>();
            int startIndex = 0;

            if (isZip || ZipMediaManager.isZipFile(target.getName())) {
                File cacheDir = ZipMediaManager.getCacheDirForZip(this, target);
                if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
                    List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
                    for (File t : tracks) {
                        items.add(MediaItem.fromUri(Uri.fromFile(t)));
                    }
                } else {
                    ZipMediaManager.extractZipAsync(this, target, new ZipMediaManager.ExtractionCallback() {
                        @Override
                        public void onProgress(int currentTrack, int totalTracks, String trackName) {}

                        @Override
                        public void onSuccess(File cacheDir, List<File> audioTracks) {
                            if (audioTracks != null && !audioTracks.isEmpty() && player != null) {
                                List<MediaItem> zipItems = new ArrayList<>();
                                for (File f : audioTracks) {
                                    zipItems.add(MediaItem.fromUri(Uri.fromFile(f)));
                                }
                                player.setMediaItems(zipItems, 0, 0);
                                player.prepare();
                                player.play();
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Failed to extract scheduled zip: " + errorMessage);
                        }
                    });
                    return;
                }
            } else if (isContainer || target.isDirectory()) {
                File[] files = target.listFiles();
                if (files != null) {
                    Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                    for (File f : files) {
                        if (!f.isDirectory() && ZipMediaManager.isAudioFile(f.getName())) {
                            items.add(MediaItem.fromUri(Uri.fromFile(f)));
                        }
                    }
                }
            } else {
                File parent = target.getParentFile();
                if (parent != null && parent.exists() && parent.isDirectory()) {
                    File[] files = parent.listFiles();
                    if (files != null) {
                        Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        for (File f : files) {
                            if (!f.isDirectory() && ZipMediaManager.isAudioFile(f.getName())) {
                                if (f.getAbsolutePath().equals(target.getAbsolutePath())) {
                                    startIndex = items.size();
                                }
                                items.add(MediaItem.fromUri(Uri.fromFile(f)));
                            }
                        }
                    }
                }
                if (items.isEmpty()) {
                    items.add(MediaItem.fromUri(Uri.fromFile(target)));
                }
            }

            if (!items.isEmpty()) {
                player.setMediaItems(items, startIndex, 0);
                player.prepare();
                player.play();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting service playback", e);
        }
    }

    public void stopPlaybackAndRelease() {
        try {
            if (player != null) {
                player.stop();
                player.release();
                player = null;
            }
        } catch (Exception ignored) {}

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                wakeLock = null;
            }
        } catch (Exception ignored) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Exception ignored) {}

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(NOTIFICATION_ID);
        }

        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        instance = null;
        stopPlaybackAndRelease();
        super.onDestroy();
    }
}
