package com.example.ktsadvancedmediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.KeyEvent;

public class MediaActionReceiver extends BroadcastReceiver {
    public static final String ACTION_SCHEDULED_PLAY = "com.example.ktsadvancedmediaplayer.ACTION_SCHEDULED_PLAY";
    public static final String EXTRA_SCHEDULED_PATH = "extra_scheduled_path";
    public static final String EXTRA_SCHEDULED_TITLE = "extra_scheduled_title";
    public static final String EXTRA_SCHEDULED_IS_CONTAINER = "extra_scheduled_is_container";
    public static final String EXTRA_SCHEDULED_IS_ZIP = "extra_scheduled_is_zip";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        MainActivity main = MainActivity.getInstance();

        if (ACTION_SCHEDULED_PLAY.equals(action)) {
            String path = intent.getStringExtra(EXTRA_SCHEDULED_PATH);
            String title = intent.getStringExtra(EXTRA_SCHEDULED_TITLE);
            boolean isContainer = intent.getBooleanExtra(EXTRA_SCHEDULED_IS_CONTAINER, false);
            boolean isZip = intent.getBooleanExtra(EXTRA_SCHEDULED_IS_ZIP, false);

            try {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KTSMediaPlayer:ScheduledPlayWakeLock");
                    wl.acquire(60000);
                }
            } catch (Exception ignored) {}

            Intent serviceIntent = new Intent(context, ScheduledPlaybackService.class);
            serviceIntent.setAction(ACTION_SCHEDULED_PLAY);
            serviceIntent.putExtra(EXTRA_SCHEDULED_PATH, path);
            serviceIntent.putExtra(EXTRA_SCHEDULED_TITLE, title);
            serviceIntent.putExtra(EXTRA_SCHEDULED_IS_CONTAINER, isContainer);
            serviceIntent.putExtra(EXTRA_SCHEDULED_IS_ZIP, isZip);

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            } catch (Exception e) {
                android.util.Log.e("MediaActionReceiver", "Failed to start ScheduledPlaybackService", e);
            }
            return;
        }

        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            android.content.SharedPreferences memPrefs = context.getSharedPreferences("media_player_memory", Context.MODE_PRIVATE);
            long schedTime = memPrefs.getLong("scheduled_play_time", 0);
            String path = memPrefs.getString("scheduled_play_path", "");
            String title = memPrefs.getString("scheduled_play_title", "");
            boolean isContainer = memPrefs.getBoolean("scheduled_play_is_container", false);
            boolean isZip = memPrefs.getBoolean("scheduled_play_is_zip", false);
            if (schedTime > System.currentTimeMillis() && !path.isEmpty()) {
                android.app.AlarmManager am = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (am != null) {
                    Intent broadcastIntent = new Intent(context, MediaActionReceiver.class);
                    broadcastIntent.setAction(ACTION_SCHEDULED_PLAY);
                    broadcastIntent.putExtra(EXTRA_SCHEDULED_PATH, path);
                    broadcastIntent.putExtra(EXTRA_SCHEDULED_TITLE, title);
                    broadcastIntent.putExtra(EXTRA_SCHEDULED_IS_CONTAINER, isContainer);
                    broadcastIntent.putExtra(EXTRA_SCHEDULED_IS_ZIP, isZip);

                    int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
                    }
                    android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                            context,
                            9021,
                            broadcastIntent,
                            flags
                    );

                    Intent showIntent = new Intent(context, MainActivity.class);
                    showIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    android.app.PendingIntent showPi = android.app.PendingIntent.getActivity(
                            context,
                            9021,
                            showIntent,
                            flags
                    );

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        android.app.AlarmManager.AlarmClockInfo clockInfo = new android.app.AlarmManager.AlarmClockInfo(schedTime, showPi);
                        am.setAlarmClock(clockInfo, pi);
                    } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        am.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, schedTime, pi);
                    } else {
                        am.setExact(android.app.AlarmManager.RTC_WAKEUP, schedTime, pi);
                    }
                }
            }
            return;
        }

        if (Intent.ACTION_MEDIA_BUTTON.equals(action)) {
            KeyEvent keyEvent = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                int keyCode = keyEvent.getKeyCode();
                if (main != null) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_MEDIA_PLAY:
                        case KeyEvent.KEYCODE_MEDIA_PAUSE:
                        case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                        case KeyEvent.KEYCODE_HEADSETHOOK:
                            main.handleNotificationPlayPause();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_NEXT:
                            main.handleNotificationNext();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                            main.handleNotificationPrev();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                            main.handleNotificationForward();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_REWIND:
                            main.handleNotificationRewind();
                            break;
                        case KeyEvent.KEYCODE_MEDIA_STOP:
                            main.handleNotificationStop();
                            break;
                    }
                }
            }
            return;
        }

        if (main != null) {
            switch (action) {
                case MediaNotificationManager.ACTION_REWIND:
                    main.handleNotificationRewind();
                    break;
                case MediaNotificationManager.ACTION_PREV:
                    main.handleNotificationPrev();
                    break;
                case MediaNotificationManager.ACTION_PLAY_PAUSE:
                    main.handleNotificationPlayPause();
                    break;
                case MediaNotificationManager.ACTION_NEXT:
                    main.handleNotificationNext();
                    break;
                case MediaNotificationManager.ACTION_FORWARD:
                    main.handleNotificationForward();
                    break;
                case MediaNotificationManager.ACTION_STOP:
                    main.handleNotificationStop();
                    break;
            }
        } else {
            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            launchIntent.setAction(action);
            context.startActivity(launchIntent);
        }
    }
}
