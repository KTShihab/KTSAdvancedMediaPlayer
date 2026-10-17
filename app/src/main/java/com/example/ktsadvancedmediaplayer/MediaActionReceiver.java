package com.example.ktsadvancedmediaplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

public class MediaActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();

        MainActivity main = MainActivity.getInstance();

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
