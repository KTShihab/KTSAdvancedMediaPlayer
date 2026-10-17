package com.example.ktsadvancedmediaplayer;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.PowerManager;
import android.media.VolumeProvider;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.Virtualizer;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.app.ProgressDialog;
import android.media.MediaScannerConnection;
import android.webkit.MimeTypeMap;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.extractor.DefaultExtractorsFactory;

import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

@SuppressWarnings({"deprecation", "SpellCheckingInspection"})
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "InlinedApi", "DefaultLocale"})
@OptIn(markerClass = UnstableApi.class)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "KTSPlayer";

    private static final int ACTION_SHOW_TIME_DETAILS = 2001;
    private static final int ACTION_CONFIG_SEEK = 2002;
    private static final int ACTION_VIEW_BOOKMARKS = 2003;
    private static final int ACTION_SHOW_FILE_OPTIONS = 2004;

    private LinearLayout containerPlayer, containerLibrary, eqBandsContainer;
    private ScrollView containerSettings;
    private Button tabPlayer, tabLibrary, tabSettings;
    private View rootLayout;
    private Button btnAppTheme;
    private TextView headerAppearance, headerPlayback, headerAudioEffects, headerVolShortcuts, headerStorage, headerShortcuts, headerUpdates, tvCurrentVersionInfo;
    private Button btnCheckUpdate, btnAutoCheckUpdateToggle;
    private LinearLayout layoutTopTabBar, cardNowPlaying, cardPlaybackControls, layoutLibHeader;
    private int currentSelectedTab = 1;

    private TextView tvNowPlaying, tvCurrentFolder;
    private Button btnResumeFolder;
    private final Stack<String> folderBackStack = new Stack<>();
    private long lastPeriodicSaveTime = 0;
    private EditText inputSearch;
    private Button btnSort, btnStream, btnQueue, btnPlayMode, btnPrev, btnPlay, btnNext;
    private Button btnRewind, btnForward, btnSpeed, btnJump, btnLoop, btnBookmark;
    private SeekBar seekBarPlayback;
    private TextView tvCurrentProgressTime, tvTotalProgressTime;
    private Button btnSliderStepSetting;
    private boolean isUserTrackingSeekBar = false;
    private int sliderStepPercent = 5;
    private final Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updatePlaybackProgressUI();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                handler.postDelayed(this, 500);
            }
        }
    };
    private Button btnResumeToggle, btnBgPlaybackToggle, btnFilterMediaFolders, btnAudioRoute, btnEqToggle, btnEarSpeakerMode;

    public static final int EAR_MODE_OFF = 0;
    public static final int EAR_MODE_ON = 1;
    public static final int EAR_MODE_AUTO = 2;
    private int currentEarMode = EAR_MODE_OFF;
    private boolean isEarSpeakerRouted = false;
    private boolean isNearEar = false;
    private boolean isProximitySensorRegistered = false;
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;
    private PowerManager.WakeLock proximityWakeLock;

    private Button btnVirtualizerToggle, btnFadeConfig, btnGapConfig, btnSkipSilence;
    private SeekBar seekVirtualizer;

    private Button btnHist, btnFav, btnPlaylists, btnBookShelf, btnClear, btnLibBack, btnLibNew;
    private LinearLayout layoutHistoryFilter;
    private Button btnHistFilterTracks, btnHistFilterFolders, btnHistFilterPlaylists;
    private TextView tvLibTitle, tvLibEmpty;

    private Button btnVolUpLong, btnVolUpDouble, btnVolDownLong, btnVolDownDouble;
    private Button btnVolShortcutsMasterToggle, btnScreenOffVolToggle, btnSleepTimer;
    private Button btnPinResumeShortcut, btnPinShelfShortcut, btnPinFavShortcut, btnPinHistShortcut;

    private int libraryMode = 1;
    private int historyFilterMode = 0; // 0 = Tracks, 1 = Folders, 2 = Playlists
    private String currentPlaylistName = "";
    private String currentShelfName = "";

    private SeekBar seekLoudness;
    private ListView listViewFiles, listViewLibrary;

    private ExoPlayer mediaPlayer;
    private ValueAnimator volumeAnimator;

    private String currentDir = "";
    private String currentPlayingFile = "";
    private int playMode = 0;

    private Button btnClearZipCache, btnClearSharedMediaCache, btnClearAllCache, btnAutoPlayNextFolder;
    private String currentZipName = "";
    private String currentZipFilePath = "";
    private boolean isBrowsingZip = false;
    private String zipSourceFolderPath = "";

    private String currentDriveFolderName = "";
    private String currentDriveFolderUrl = "";
    private final List<GoogleDriveManager.DriveTrack> currentDriveTracks = new ArrayList<>();
    private boolean isBrowsingDrive = false;
    private String driveSourceFolderPath = "";

    private int seekStep = 10000;
    private int currentSeekIndex = 1;
    private float currentSpeed = 1.0f;
    private int currentSortMode = 0;

    private boolean isSkipSilenceEnabled = false;

    private float currentFadeMultiplier = 1.0f;
    private int currentFadeIndex = 2;
    private final String[] fadeOptions = {"Off", "Fast", "Normal", "Slow", "Very Slow"};
    private final float[] fadeValues = {0.0f, 0.5f, 1.0f, 2.0f, 3.0f};

    private long targetSeekPosition = -1;
    private Runnable pendingSeekFadeInRunnable = null;
    private boolean isTrackFadingOutAtEnd = false;

    private int currentGapIndex = 0;
    private final String[] gapOptions = {"0 Seconds (Fastest)", "0.5 Seconds", "1.0 Second", "1.5 Seconds", "2.0 Seconds"};
    private final long[] gapValues = {0, 500, 1000, 1500, 2000};

    private int abState = 0;
    private int pointA = 0;
    private int pointB = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loopRunnable;
    private SharedPreferences bkPrefs, memPrefs, libPrefs;

    private final List<String> currentDisplayNames = new ArrayList<>();
    private final List<String> currentDisplayPaths = new ArrayList<>();
    private final List<String> currentAudioList = new ArrayList<>();
    private ArrayAdapter<String> fileListAdapter;

    public static class QueueItem {
        public static final int TYPE_TRACK = 0;
        public static final int TYPE_FOLDER = 1;
        public static final int TYPE_ZIP = 2;

        public final int type;
        public final String path;
        public final String title;

        public QueueItem(int type, String path, String title) {
            this.type = type;
            this.path = path;
            this.title = title;
        }
    }

    private final List<QueueItem> playQueue = new ArrayList<>();

    private final List<String> globalAudioNames = new ArrayList<>();
    private final List<String> globalAudioPaths = new ArrayList<>();
    private final Set<String> knownMediaFoldersSet = new HashSet<>();
    private final Map<String, Boolean> mediaFolderCache = new HashMap<>();

    private final List<String> libraryDisplayNames = new ArrayList<>();
    private final List<String> libraryDisplayPaths = new ArrayList<>();
    private ArrayAdapter<String> libraryAdapter;

    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<Intent> manageStorageLauncher;
    private boolean hadManageStorageAccess = false;

    private AudioManager audioManager;
    private MediaSession mediaSession;

    private Equalizer equalizer;
    private LoudnessEnhancer loudnessEnhancer;
    private Virtualizer virtualizer;

    private boolean isEqEnabled = false;
    private int currentLoudnessProgress = 0;
    private short[] savedBandLevels = null;

    private boolean isVirtualizerEnabled = false;
    private int currentVirtualizerStrength = 0;
    private int currentAudioSessionId = 0;

    // Volume Shortcut Variables
    private boolean isVolShortcutsMasterEnabled = true;
    private int volUpLongAction = 4;
    private int volUpDoubleAction = 2;
    private int volDownLongAction = 5;
    private int volDownDoubleAction = 3;
    private final String[] shortcutOptions = {"None", "Play / Pause", "Next Track", "Previous Track", "Forward", "Rewind"};

    private static final long FOREGROUND_DOUBLE_TAP_TIMEOUT_MS = 200L;
    private static final long FOREGROUND_LONG_PRESS_TIMEOUT_MS = 380L;
    private static final long BACKGROUND_DOUBLE_TAP_WINDOW_MS = 230L;
    private static final long BACKGROUND_LONG_PRESS_WINDOW_MS = 550L;

    private long lastVolUpTime = 0;
    private long lastVolDownTime = 0;
    private final Handler volHandler = new Handler(Looper.getMainLooper());

    private final Runnable volUpSingleRunnable = () -> {
        lastVolUpTime = 0;
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        }
    };

    private final Runnable volDownSingleRunnable = () -> {
        lastVolDownTime = 0;
        if (audioManager != null) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        }
    };

    // Foreground Volume Key Tracking
    private boolean isAppInForeground = true;
    private boolean isVolLongPressTriggered = false;
    private Runnable volLongPressRunnable = null;

    // Screen-Off Volume & Sleep Timer Variables
    private boolean isScreenOffVolEnabled = true;
    private ContentObserver volumeContentObserver;
    private int previousStreamVolume = -1;
    private Vibrator vibrator;

    // Background gesture tracking (system volume observer)
    private int gestureBaseVolume = -1;
    private int bgTapCount = 0;
    private int bgTapDirection = 0;
    private long gestureStartTime = 0;
    private long lastBgTapTime = 0;
    private boolean isRestoringVolume = false;
    private boolean isLongPressTriggered = false;
    private final Runnable bgResetGestureRunnable = () -> {
        gestureBaseVolume = -1;
        bgTapCount = 0;
        bgTapDirection = 0;
        gestureStartTime = 0;
        lastBgTapTime = 0;
        isLongPressTriggered = false;
    };

    private final Handler sleepTimerHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepTimerRunnable;
    private long sleepTimerEndTimeMs = 0;
    private boolean isSleepTimerActive = false;
    private boolean isSleepEndOfTrack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, windowInsets) -> {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
                return windowInsets;
            });
        }

        instance = this;
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
            }
        }

        bkPrefs = getSharedPreferences("KTS_Media_Bookmarks", Context.MODE_PRIVATE);
        memPrefs = getSharedPreferences("KTS_Media_Memory", Context.MODE_PRIVATE);
        libPrefs = getSharedPreferences("KTS_Media_Library", Context.MODE_PRIVATE);

        currentFadeMultiplier = bkPrefs.getFloat("fade_multiplier", 1.0f);
        currentFadeIndex = bkPrefs.getInt("fade_index", 2);
        currentGapIndex = bkPrefs.getInt("gap_index", 0);
        isSkipSilenceEnabled = bkPrefs.getBoolean("skip_silence", false);

        isVirtualizerEnabled = bkPrefs.getBoolean("virtualizer_enabled", false);
        currentVirtualizerStrength = bkPrefs.getInt("virtualizer_strength", 0);
        currentLoudnessProgress = bkPrefs.getInt("loudness_gain", 0);
        isVolShortcutsMasterEnabled = bkPrefs.getBoolean("vol_shortcuts_master_enabled", true);
        isScreenOffVolEnabled = bkPrefs.getBoolean("screen_off_vol_enabled", true);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        playMode = bkPrefs.getInt("persistent_play_mode", 0);

        initViews();
        setupShortcutsUI();
        setupMediaSession();
        setupAudioEffectsUI();
        setupAudioRouting();
        setupMediaPlayer();
        updateExoPlayerPlayMode(false);
        setupEarSpeakerMode();
        setupListeners();
        setupAdvancedFeatures();
        setupLibraryFeatures();
        setupBackNavigation();
        setupUniversalSearch();

        updateSeekButtons();
        updateResumeToggleUI();
        updateBgPlaybackToggleUI();
        updateFilterMediaFoldersToggleUI();
        restoreLastPlayedInfo();
        updateAppShortcuts();
        applyCurrentTheme();

        manageStorageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            hadManageStorageAccess = true;
                            indexAllAudioForSearch();
                            loadDirectory(currentDir != null && !currentDir.isEmpty() ? currentDir : Environment.getExternalStorageDirectory().getAbsolutePath());
                        } else {
                            Toast.makeText(this, "Limited storage access. Some folders may be hidden.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean storageGranted = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_MEDIA_AUDIO))
                                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
                    } else {
                        storageGranted = Boolean.TRUE.equals(result.get(Manifest.permission.READ_EXTERNAL_STORAGE))
                                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
                    }

                    if (storageGranted) {
                        indexAllAudioForSearch();
                        loadDirectory(Environment.getExternalStorageDirectory().getAbsolutePath());
                        handleIntent(getIntent());
                        checkManageStoragePermission();
                    } else {
                        Toast.makeText(this, "Storage permission is required to browse audio files.", Toast.LENGTH_LONG).show();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            Toast.makeText(this, "Notification permission is needed for lock screen controls", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        switchTab(1);
        checkStoragePermission();

        // Background check for updates if enabled
        if (AppUpdateManager.isAutoCheckEnabled(this)) {
            handler.postDelayed(() -> {
                if (!isFinishing()) {
                    AppUpdateManager.getInstance().checkForUpdates(this, false);
                }
            }, 3000);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (!isVolShortcutsMasterEnabled) {
            return super.dispatchKeyEvent(event);
        }
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                return onKeyDown(keyCode, event);
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                return onKeyUp(keyCode, event);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (!isVolShortcutsMasterEnabled) {
            return super.onKeyDown(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.getRepeatCount() == 0) {
                isVolLongPressTriggered = false;
                if (volLongPressRunnable != null) {
                    volHandler.removeCallbacks(volLongPressRunnable);
                }
                final int currentKeyCode = keyCode;
                volLongPressRunnable = () -> {
                    isVolLongPressTriggered = true;
                    volHandler.removeCallbacks(volUpSingleRunnable);
                    volHandler.removeCallbacks(volDownSingleRunnable);
                    lastVolUpTime = 0;
                    lastVolDownTime = 0;
                    int action = (currentKeyCode == KeyEvent.KEYCODE_VOLUME_UP) ? volUpLongAction : volDownLongAction;
                    if (action != 0) {
                        executeShortcutAction(action);
                        vibrateFeedback();
                        announceAccessibility("Action Triggered");
                    }
                };
                volHandler.postDelayed(volLongPressRunnable, FOREGROUND_LONG_PRESS_TIMEOUT_MS);
            } else if (event.getRepeatCount() >= 1 && !isVolLongPressTriggered) {
                if (volLongPressRunnable != null) {
                    volHandler.removeCallbacks(volLongPressRunnable);
                    volLongPressRunnable = null;
                }
                isVolLongPressTriggered = true;
                volHandler.removeCallbacks(volUpSingleRunnable);
                volHandler.removeCallbacks(volDownSingleRunnable);
                lastVolUpTime = 0;
                lastVolDownTime = 0;
                int action = (keyCode == KeyEvent.KEYCODE_VOLUME_UP) ? volUpLongAction : volDownLongAction;
                if (action != 0) {
                    executeShortcutAction(action);
                    vibrateFeedback();
                    announceAccessibility("Action Triggered");
                }
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (!isVolShortcutsMasterEnabled) {
            return super.onKeyUp(keyCode, event);
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volLongPressRunnable != null) {
                volHandler.removeCallbacks(volLongPressRunnable);
                volLongPressRunnable = null;
            }
            if (isVolLongPressTriggered) {
                isVolLongPressTriggered = false;
                lastVolUpTime = 0;
                lastVolDownTime = 0;
                return true;
            }

            long currentTime = System.currentTimeMillis();
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                if (currentTime - lastVolUpTime <= FOREGROUND_DOUBLE_TAP_TIMEOUT_MS && lastVolUpTime > 0) {
                    volHandler.removeCallbacks(volUpSingleRunnable);
                    lastVolUpTime = 0;
                    if (volUpDoubleAction != 0) {
                        executeShortcutAction(volUpDoubleAction);
                        vibrateFeedback();
                        announceAccessibility("Action Triggered");
                    }
                } else {
                    lastVolUpTime = currentTime;
                    volHandler.postDelayed(volUpSingleRunnable, FOREGROUND_DOUBLE_TAP_TIMEOUT_MS);
                }
                return true;
            } else {
                if (currentTime - lastVolDownTime <= FOREGROUND_DOUBLE_TAP_TIMEOUT_MS && lastVolDownTime > 0) {
                    volHandler.removeCallbacks(volDownSingleRunnable);
                    lastVolDownTime = 0;
                    if (volDownDoubleAction != 0) {
                        executeShortcutAction(volDownDoubleAction);
                        vibrateFeedback();
                        announceAccessibility("Action Triggered");
                    }
                } else {
                    lastVolDownTime = currentTime;
                    volHandler.postDelayed(volDownSingleRunnable, FOREGROUND_DOUBLE_TAP_TIMEOUT_MS);
                }
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    private void executeShortcutAction(int actionType) {
        if (actionType == 0 || mediaPlayer == null) return;
        switch (actionType) {
            case 1: btnPlay.performClick(); break;
            case 2: btnNext.performClick(); break;
            case 3: btnPrev.performClick(); break;
            case 4: btnForward.performClick(); break;
            case 5: btnRewind.performClick(); break;
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        String shortcutAction = intent.getStringExtra("shortcut_action");

        if (MediaNotificationManager.ACTION_PLAY_PAUSE.equals(action)) {
            handleNotificationPlayPause();
            return;
        }
        if (MediaNotificationManager.ACTION_NEXT.equals(action)) {
            handleNotificationNext();
            return;
        }
        if (MediaNotificationManager.ACTION_PREV.equals(action)) {
            handleNotificationPrev();
            return;
        }
        if (MediaNotificationManager.ACTION_REWIND.equals(action)) {
            handleNotificationRewind();
            return;
        }
        if (MediaNotificationManager.ACTION_FORWARD.equals(action)) {
            handleNotificationForward();
            return;
        }
        if (MediaNotificationManager.ACTION_STOP.equals(action)) {
            handleNotificationStop();
            return;
        }

        if ("com.example.ktsadvancedmediaplayer.action.RESUME_LAST".equals(action) || "resume_last".equals(shortcutAction)) {
            switchTab(1);
            resumeGlobalLastPlayed();
            return;
        }

        if ("com.example.ktsadvancedmediaplayer.action.OPEN_BOOKSHELF".equals(action) || "bookshelf".equals(shortcutAction)) {
            switchTab(2);
            libraryMode = 4;
            loadBookShelfList();
            announceAccessibility("Showing Book Shelves");
            return;
        }

        if ("com.example.ktsadvancedmediaplayer.action.OPEN_FAVORITES".equals(action) || "favorites".equals(shortcutAction)) {
            switchTab(2);
            libraryMode = 1;
            loadLibraryData("favorites");
            announceAccessibility("Showing Favorites");
            return;
        }

        if ("com.example.ktsadvancedmediaplayer.action.OPEN_HISTORY".equals(action) || "history".equals(shortcutAction)) {
            switchTab(2);
            libraryMode = 0;
            historyFilterMode = 0;
            loadHistoryData();
            announceAccessibility("Showing History");
            return;
        }

        if (Intent.ACTION_SEND.equals(action) || Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText == null) {
                CharSequence cs = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
                if (cs != null) sharedText = cs.toString();
            }
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                String extractedUrl = extractUrlFromText(sharedText);
                if (extractedUrl != null && GoogleDriveManager.isGoogleDriveUrl(extractedUrl)) {
                    switchTab(1);
                    handleGoogleDriveUrl(extractedUrl);
                    return;
                } else if (GoogleDriveManager.isGoogleDriveUrl(sharedText)) {
                    switchTab(1);
                    handleGoogleDriveUrl(sharedText.trim());
                    return;
                } else if (extractedUrl != null && (extractedUrl.startsWith("http://") || extractedUrl.startsWith("https://"))) {
                    switchTab(1);
                    playNetworkStream(extractedUrl);
                    return;
                }
            }

            ArrayList<Uri> streamUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (streamUris != null && !streamUris.isEmpty()) {
                handleIncomingAudioUris(streamUris);
                return;
            }

            if (intent.getClipData() != null && intent.getClipData().getItemCount() > 0) {
                List<Uri> clipUris = new ArrayList<>();
                for (int i = 0; i < intent.getClipData().getItemCount(); i++) {
                    Uri u = intent.getClipData().getItemAt(i).getUri();
                    if (u != null) clipUris.add(u);
                }
                if (!clipUris.isEmpty()) {
                    handleIncomingAudioUris(clipUris);
                    return;
                }
            }

            Uri streamUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (streamUri == null && intent.getData() != null) {
                streamUri = intent.getData();
            }
            if (streamUri != null) {
                handleIncomingAudioUris(Collections.singletonList(streamUri));
                return;
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri audioUri = intent.getData();
            if (audioUri != null) {
                String uriStr = audioUri.toString();

                if (GoogleDriveManager.isGoogleDriveUrl(uriStr)) {
                    switchTab(1);
                    handleGoogleDriveUrl(uriStr);
                    return;
                }

                String lowerUri = uriStr.toLowerCase();
                String type = intent.getType();
                boolean isZip = lowerUri.endsWith(".zip") ||
                        (type != null && (type.contains("zip") || type.contains("octet-stream")));

                if (isZip) {
                    switchTab(1);
                    handleZipIntent(audioUri);
                } else if (uriStr.startsWith("http://") || uriStr.startsWith("https://")) {
                    switchTab(1);
                    playNetworkStream(uriStr);
                } else {
                    handleIncomingAudioUris(Collections.singletonList(audioUri));
                }
            }
        }
    }

    private String extractUrlFromText(String text) {
        if (text == null) return null;
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = urlPattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    private void handleGoogleDriveUrl(String url) {
        if (GoogleDriveManager.isDriveFolder(url)) {
            loadGoogleDriveFolder(url, 0);
        } else if (GoogleDriveManager.isDriveFile(url)) {
            String fileId = GoogleDriveManager.extractDriveId(url);
            if (fileId != null) {
                playDriveSingleFile(fileId, url);
            } else {
                playNetworkStream(url);
            }
        } else {
            loadGoogleDriveFolder(url, 0);
        }
    }

    private void loadGoogleDriveFolder(String folderUrl, int startIndex) {
        AlertDialog progressDialog = createExtractionProgressDialog("Loading Google Drive Folder");
        progressDialog.show();
        announceAccessibility("Loading Google Drive folder. Please wait.");

        GoogleDriveManager.fetchFolderTracksAsync(this, folderUrl, new GoogleDriveManager.DriveFolderCallback() {
            @Override
            public void onTracksLoaded(String folderName, List<GoogleDriveManager.DriveTrack> tracks) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                libPrefs.edit().putString("title_" + folderUrl, "📁 " + folderName).apply();
                playDriveTracks(folderName, folderUrl, tracks, startIndex);
                populateDriveDirectory(folderName, tracks);
            }

            @Override
            public void onError(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility(errorMessage);
            }
        });
    }

    private void populateDriveDirectory(String folderName, List<GoogleDriveManager.DriveTrack> tracks) {
        isBrowsingDrive = true;
        driveSourceFolderPath = currentDir;
        currentDir = "gdrive://" + folderName;
        tvCurrentFolder.setText("Google Drive: " + folderName);
        currentDisplayNames.clear();
        currentDisplayPaths.clear();

        currentDisplayNames.add("Up");
        currentDisplayPaths.add(driveSourceFolderPath);

        currentDisplayNames.add("⚙️ More Options");
        currentDisplayPaths.add("DRIVE_FOLDER_OPTIONS");

        for (int i = 0; i < tracks.size(); i++) {
            GoogleDriveManager.DriveTrack track = tracks.get(i);
            currentDisplayNames.add((i + 1) + ". " + track.title);
            currentDisplayPaths.add(track.streamUrl);
        }
        fileListAdapter.notifyDataSetChanged();
    }

    private void showDriveFolderOptionsDialog() {
        String[] options = {
                "⭐ Add to Favorites",
                "📚 Add to Book Shelf",
                "🎵 Add to Playlist"
        };
        new AlertDialog.Builder(this)
                .setTitle("Google Drive: " + currentDriveFolderName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        addToLibrary("favorites", currentDriveFolderUrl);
                        libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                        announceAccessibility(currentDriveFolderName + " added to favorites");
                        Toast.makeText(this, "Folder added to Favorites", Toast.LENGTH_SHORT).show();
                    } else if (which == 1) {
                        libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                        showBookShelfSelectionDialog(currentDriveFolderUrl);
                    } else if (which == 2) {
                        libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                        showPlaylistSelectionDialog(currentDriveFolderUrl);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void playDriveTracks(String folderName, String folderUrl, List<GoogleDriveManager.DriveTrack> tracks, int startIndex) {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
            fadeAudio(-1f, 0.0f, 200, () -> executePlayDriveTracks(folderName, folderUrl, tracks, startIndex));
        } else {
            executePlayDriveTracks(folderName, folderUrl, tracks, startIndex);
        }
    }

    private void executePlayDriveTracks(String folderName, String folderUrl, List<GoogleDriveManager.DriveTrack> tracks, int startIndex) {
        try {
            saveCurrentPosition();
            currentDriveFolderName = folderName;
            currentDriveFolderUrl = folderUrl;
            if (tracks != currentDriveTracks) {
                currentDriveTracks.clear();
                if (tracks != null) {
                    currentDriveTracks.addAll(tracks);
                }
            }

            if (currentDriveTracks.isEmpty()) {
                announceAccessibility("No tracks to play");
                return;
            }

            currentZipName = "";
            currentZipFilePath = "";

            currentAudioList.clear();
            List<MediaItem> mediaItems = new ArrayList<>();
            for (GoogleDriveManager.DriveTrack t : currentDriveTracks) {
                currentAudioList.add(t.streamUrl);
                mediaItems.add(new MediaItem.Builder()
                        .setUri(t.streamUrl)
                        .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(t.title).build())
                        .build());
            }

            int finalIndex = startIndex;
            if (finalIndex < 0 || finalIndex >= currentDriveTracks.size()) finalIndex = 0;

            mediaPlayer.stop();
            mediaPlayer.clearMediaItems();
            mediaPlayer.setMediaItems(mediaItems, finalIndex, C.TIME_UNSET);
            mediaPlayer.prepare();

            mediaPlayer.setVolume(0f);
            mediaPlayer.play();
            fadeAudio(0.0f, 1.0f, 500, null);

            resetAB();
            updateMediaSessionState(PlaybackState.STATE_PLAYING);
            mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
            updateExoPlayerPlayMode();

            String trackTitle = currentDriveTracks.get(finalIndex).title;
            tvNowPlaying.setText("🎵 [Drive: " + folderName + "] " + trackTitle);
            announceAccessibility("Playing Google Drive folder " + folderName + ". Track " + (finalIndex + 1) + " of " + currentDriveTracks.size() + ": " + trackTitle);

            libPrefs.edit().putString("title_" + folderUrl, "📁 " + folderName).apply();
            addToLibrary("history", folderUrl);

        } catch (Exception e) {
            Log.e(TAG, "Error playing Google Drive tracks", e);
            announceAccessibility("Cannot play Google Drive tracks");
        }
    }

    private void playDriveSingleFile(String fileId, String originalUrl) {
        currentDriveFolderName = "";
        currentDriveFolderUrl = "";
        currentDriveTracks.clear();
        String streamUrl = GoogleDriveManager.getDirectStreamUrl(fileId);
        playNetworkStream(streamUrl);
        tvNowPlaying.setText("🎵 Google Drive File");
        announceAccessibility("Playing Google Drive file");
        libPrefs.edit().putString("title_" + originalUrl, "🎵 Google Drive File").apply();
        addToLibrary("history", originalUrl);
    }

    private void handleZipIntent(Uri uri) {
        String uriStr = uri.toString();
        String savedCache = libPrefs.getString("zip_cache_" + uriStr, "");
        if (!savedCache.isEmpty()) {
            File cDir = new File(savedCache);
            if (ZipMediaManager.isZipAlreadyExtracted(cDir)) {
                List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cDir);
                if (!tracks.isEmpty()) {
                    String zName = libPrefs.getString("zip_name_" + uriStr, "External ZIP");
                    playZipTracks(zName, uriStr, tracks, 0);
                    return;
                }
            }
        }
        File altDir = ZipMediaManager.findExtractedCacheDir(this, uriStr);
        if (altDir != null && ZipMediaManager.isZipAlreadyExtracted(altDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(altDir);
            if (!tracks.isEmpty()) {
                String zName = libPrefs.getString("zip_name_" + uriStr, "External ZIP");
                libPrefs.edit()
                        .putString("zip_cache_" + uriStr, altDir.getAbsolutePath())
                        .putString("zip_name_" + uriStr, zName)
                        .putString("title_" + uriStr, "📦 " + zName)
                        .apply();
                playZipTracks(zName, uriStr, tracks, 0);
                return;
            }
        }

        AlertDialog progressDialog = createExtractionProgressDialog("Opening ZIP Archive");
        progressDialog.show();
        announceAccessibility("Extracting ZIP archive. Please wait.");

        ZipMediaManager.extractZipFromUriAsync(this, uri, new ZipMediaManager.ExtractionCallback() {
            @Override
            public void onProgress(int currentTrack, int totalTracks, String trackName) {
                if (progressDialog.isShowing()) {
                    TextView tvMsg = progressDialog.findViewById(android.R.id.message);
                    if (tvMsg != null) {
                        tvMsg.setText("Extracting track " + currentTrack + ": " + trackName);
                    }
                }
            }

            @Override
            public void onSuccess(File cacheDir, List<File> audioTracks) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                updateZipCacheUI();
                String uriStr = uri.toString();
                String zipName = "External ZIP";
                try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if (nameIndex != -1) {
                            String n = cursor.getString(nameIndex);
                            if (n != null && !n.isEmpty()) zipName = n;
                        }
                    }
                } catch (Exception ignored) {}
                libPrefs.edit()
                        .putString("zip_cache_" + uriStr, cacheDir.getAbsolutePath())
                        .putString("zip_name_" + uriStr, zipName)
                        .putString("title_" + uriStr, "📦 " + zipName)
                        .apply();
                playZipTracks(zipName, uriStr, audioTracks, 0);
            }

            @Override
            public void onError(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility(errorMessage);
            }
        });
    }

    private void initViews() {
        containerPlayer = findViewById(R.id.containerPlayer);
        containerLibrary = findViewById(R.id.containerLibrary);
        containerSettings = findViewById(R.id.containerSettings);
        eqBandsContainer = findViewById(R.id.eqBandsContainer);

        layoutTopTabBar = findViewById(R.id.layoutTopTabBar);
        cardNowPlaying = findViewById(R.id.cardNowPlaying);
        cardPlaybackControls = findViewById(R.id.cardPlaybackControls);
        layoutLibHeader = findViewById(R.id.layoutLibHeader);

        tabPlayer = findViewById(R.id.tabPlayer);
        tabLibrary = findViewById(R.id.tabLibrary);
        tabSettings = findViewById(R.id.tabSettings);

        btnAppTheme = findViewById(R.id.btnAppTheme);
        if (btnAppTheme != null) {
            btnAppTheme.setOnClickListener(v -> showThemeSelectionDialog());
        }
        headerAppearance = findViewById(R.id.headerAppearance);
        headerPlayback = findViewById(R.id.headerPlayback);
        headerAudioEffects = findViewById(R.id.headerAudioEffects);
        headerVolShortcuts = findViewById(R.id.headerVolShortcuts);
        headerStorage = findViewById(R.id.headerStorage);
        headerShortcuts = findViewById(R.id.headerShortcuts);

        tvNowPlaying = findViewById(R.id.tvNowPlaying);
        tvCurrentFolder = findViewById(R.id.tvCurrentFolder);
        tvCurrentFolder.setOnClickListener(v -> showStorageSelectionDialog());
        tvCurrentFolder.setOnLongClickListener(v -> {
            showFolderTimeDetails();
            return true;
        });
        btnResumeFolder = findViewById(R.id.btnResumeFolder);
        if (btnResumeFolder != null) btnResumeFolder.setVisibility(View.GONE);

        inputSearch = findViewById(R.id.inputSearch);
        btnSort = findViewById(R.id.btnSort);
        btnStream = findViewById(R.id.btnStream);
        btnQueue = findViewById(R.id.btnQueue);
        if (btnQueue != null) {
            btnQueue.setOnClickListener(v -> showQueueDialog());
            updateQueueButtonText();
        }

        btnPlayMode = findViewById(R.id.btnPlayMode);
        btnPrev = findViewById(R.id.btnPrev);
        btnPlay = findViewById(R.id.btnPlay);
        btnNext = findViewById(R.id.btnNext);

        btnRewind = findViewById(R.id.btnRewind);
        btnForward = findViewById(R.id.btnForward);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnJump = findViewById(R.id.btnJump);
        btnLoop = findViewById(R.id.btnLoop);
        btnBookmark = findViewById(R.id.btnBookmark);
        seekBarPlayback = findViewById(R.id.seekBarPlayback);
        tvCurrentProgressTime = findViewById(R.id.tvCurrentProgressTime);
        tvTotalProgressTime = findViewById(R.id.tvTotalProgressTime);

        btnHist = findViewById(R.id.btnHist);
        btnFav = findViewById(R.id.btnFav);
        btnPlaylists = findViewById(R.id.btnPlaylists);
        btnBookShelf = findViewById(R.id.btnBookShelf);
        btnClear = findViewById(R.id.btnClear);
        btnLibBack = findViewById(R.id.btnLibBack);
        btnLibNew = findViewById(R.id.btnLibNew);
        tvLibTitle = findViewById(R.id.tvLibTitle);
        tvLibEmpty = findViewById(R.id.tvLibEmpty);

        layoutHistoryFilter = findViewById(R.id.layoutHistoryFilter);
        btnHistFilterTracks = findViewById(R.id.btnHistFilterTracks);
        btnHistFilterFolders = findViewById(R.id.btnHistFilterFolders);
        btnHistFilterPlaylists = findViewById(R.id.btnHistFilterPlaylists);

        if (btnHistFilterTracks != null) {
            btnHistFilterTracks.setOnClickListener(v -> {
                historyFilterMode = 0;
                loadHistoryData();
                announceAccessibility("History filtered by tracks");
            });
        }
        if (btnHistFilterFolders != null) {
            btnHistFilterFolders.setOnClickListener(v -> {
                historyFilterMode = 1;
                loadHistoryData();
                announceAccessibility("History filtered by folders");
            });
        }
        if (btnHistFilterPlaylists != null) {
            btnHistFilterPlaylists.setOnClickListener(v -> {
                historyFilterMode = 2;
                loadHistoryData();
                announceAccessibility("History filtered by playlists");
            });
        }

        btnResumeToggle = findViewById(R.id.btnResumeToggle);
        btnBgPlaybackToggle = findViewById(R.id.btnBgPlaybackToggle);
        btnFilterMediaFolders = findViewById(R.id.btnFilterMediaFolders);
        btnSliderStepSetting = findViewById(R.id.btnSliderStepSetting);
        sliderStepPercent = bkPrefs.getInt("slider_step_percent", 5);
        if (btnSliderStepSetting != null) {
            btnSliderStepSetting.setText("Slider Step: " + sliderStepPercent + "%");
            btnSliderStepSetting.setOnClickListener(v -> showSliderStepDialog());
        }
        btnAudioRoute = findViewById(R.id.btnAudioRoute);
        btnEarSpeakerMode = findViewById(R.id.btnEarSpeakerMode);
        btnEqToggle = findViewById(R.id.btnEqToggle);

        btnVirtualizerToggle = findViewById(R.id.btnVirtualizerToggle);
        seekVirtualizer = findViewById(R.id.seekVirtualizer);

        btnFadeConfig = findViewById(R.id.btnFadeConfig);
        btnGapConfig = findViewById(R.id.btnGapConfig);
        btnAutoPlayNextFolder = findViewById(R.id.btnAutoPlayNextFolder);
        btnSkipSilence = findViewById(R.id.btnSkipSilence);
        seekLoudness = findViewById(R.id.seekLoudness);

        btnVolShortcutsMasterToggle = findViewById(R.id.btnVolShortcutsMasterToggle);
        btnScreenOffVolToggle = findViewById(R.id.btnScreenOffVolToggle);
        btnVolUpLong = findViewById(R.id.btnVolUpLong);
        btnVolUpDouble = findViewById(R.id.btnVolUpDouble);
        btnVolDownLong = findViewById(R.id.btnVolDownLong);
        btnVolDownDouble = findViewById(R.id.btnVolDownDouble);
        btnSleepTimer = findViewById(R.id.btnSleepTimer);
        btnPinResumeShortcut = findViewById(R.id.btnPinResumeShortcut);
        btnPinShelfShortcut = findViewById(R.id.btnPinShelfShortcut);
        btnPinFavShortcut = findViewById(R.id.btnPinFavShortcut);
        btnPinHistShortcut = findViewById(R.id.btnPinHistShortcut);

        if (btnPinResumeShortcut != null) {
            btnPinResumeShortcut.setOnClickListener(v -> pinAppShortcut("shortcut_resume"));
        }
        if (btnPinShelfShortcut != null) {
            btnPinShelfShortcut.setOnClickListener(v -> pinAppShortcut("shortcut_bookshelf"));
        }
        if (btnPinFavShortcut != null) {
            btnPinFavShortcut.setOnClickListener(v -> pinAppShortcut("shortcut_favorites"));
        }
        if (btnPinHistShortcut != null) {
            btnPinHistShortcut.setOnClickListener(v -> pinAppShortcut("shortcut_history"));
        }

        listViewFiles = findViewById(R.id.listViewFiles);
        fileListAdapter = new ArrayAdapter<String>(this, R.layout.item_media_file, R.id.itemText, currentDisplayNames) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tvText = view.findViewById(R.id.itemText);
                TextView tvSubText = view.findViewById(R.id.itemSubText);
                TextView tvIcon = view.findViewById(R.id.itemIcon);
                TextView tvStatus = view.findViewById(R.id.itemStatus);

                if (tvText != null) tvText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvSubText != null) tvSubText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvIcon != null) tvIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvStatus != null) tvStatus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

                if (position >= 0 && position < currentDisplayNames.size()) {
                    String name = currentDisplayNames.get(position);
                    String path = (position < currentDisplayPaths.size()) ? currentDisplayPaths.get(position) : "";
                    String displayName = name;

                    boolean isZip = name.startsWith("📦 ") || (path != null && ZipMediaManager.isZipFile(path));
                    boolean isUp = "Up".equals(name) || name.startsWith("Up (");
                    boolean isStorage = name.startsWith("💾") || "STORAGE_ROOT".equals(path);
                    boolean isDir = path != null && (new File(path).isDirectory() || (isBrowsingZip && !ZipMediaManager.isAudioFile(path)));

                    if (name.startsWith("📌 ")) {
                        if (tvIcon != null) tvIcon.setText("📌");
                        displayName = name.substring(3);
                    } else if (name.startsWith("📦 ")) {
                        if (tvIcon != null) tvIcon.setText("📦");
                        displayName = name.substring(3);
                    } else if (isUp) {
                        if (tvIcon != null) tvIcon.setText("⬆️");
                    } else if (isStorage) {
                        if (tvIcon != null) tvIcon.setText("💾");
                    } else if (isDir) {
                        if (tvIcon != null) tvIcon.setText("📁");
                    } else {
                        if (tvIcon != null) tvIcon.setText("🎵");
                    }

                    if (tvText != null) {
                        tvText.setText(displayName);
                    }

                    boolean isCurrentlyPlaying = (currentPlayingFile != null && !currentPlayingFile.isEmpty() && path != null && path.equals(currentPlayingFile));
                    if (isCurrentlyPlaying) {
                        if (tvStatus != null) {
                            tvStatus.setVisibility(View.VISIBLE);
                            tvStatus.setText("▶ PLAYING");
                        }
                        if (tvText != null) {
                            tvText.setTextColor(Color.parseColor("#80D8FF"));
                            tvText.setTypeface(null, android.graphics.Typeface.BOLD);
                        }
                    } else {
                        if (tvStatus != null) {
                            tvStatus.setVisibility(View.GONE);
                        }
                        if (tvText != null) {
                            tvText.setTextColor(Color.WHITE);
                            tvText.setTypeface(null, android.graphics.Typeface.NORMAL);
                        }
                    }

                    String subTextStr = "";
                    String speechMeta = "";
                    if ((isDir || isZip) && !isUp && !isStorage && path != null && !path.isEmpty()) {
                        FolderMetadataManager.FolderMeta meta = FolderMetadataManager.getCachedMeta(MainActivity.this, path);
                        if (meta != null) {
                            subTextStr = meta.getShortDescription();
                            speechMeta = meta.getSpeechDescription();
                        } else {
                            FolderMetadataManager.getFolderInfoAsync(MainActivity.this, path, m -> {
                                if (m.fileCount > 0) {
                                    fileListAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    } else if (!isDir && !isZip && !isUp && !isStorage && path != null && !path.isEmpty() && ZipMediaManager.isAudioFile(path)) {
                        FolderMetadataManager.FileMeta fMeta = FolderMetadataManager.getCachedFileMeta(MainActivity.this, path);
                        if (fMeta != null) {
                            subTextStr = fMeta.getShortDescription();
                            speechMeta = fMeta.getSpeechDescription();
                        } else {
                            FolderMetadataManager.getFileInfoAsync(MainActivity.this, path, m -> {
                                if (m.durationMs > 0 || m.sizeBytes > 0) {
                                    fileListAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }

                    if (tvSubText != null) {
                        if (!subTextStr.isEmpty()) {
                            tvSubText.setVisibility(View.VISIBLE);
                            tvSubText.setText(subTextStr);
                        } else {
                            tvSubText.setVisibility(View.GONE);
                        }
                    }

                    String contentDesc = displayName;
                    if (isDir && !isUp && !isStorage) {
                        contentDesc += ", Folder";
                    } else if (isZip && !isUp) {
                        contentDesc += ", ZIP archive";
                    } else if (!isUp && !isStorage && ZipMediaManager.isAudioFile(path)) {
                        contentDesc += ", Audio track";
                    }
                    if (!speechMeta.isEmpty()) {
                        contentDesc += ", " + speechMeta;
                    }
                    if (isCurrentlyPlaying) {
                        contentDesc += ", Currently playing";
                    }
                    view.setContentDescription(contentDesc);
                }

                final int pos = position;
                view.setOnClickListener(v -> {
                    int itemPos = listViewFiles.getPositionForView(v);
                    if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                    listViewFiles.performItemClick(v, itemPos, fileListAdapter.getItemId(itemPos));
                });
                view.setLongClickable(true);
                view.setOnLongClickListener(v -> {
                    int itemPos = listViewFiles.getPositionForView(v);
                    if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                    return showFileOptionsForPosition(itemPos);
                });
                ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        int itemPos = listViewFiles.getPositionForView(host);
                        if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                        info.setLongClickable(true);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, "More options"));
                        setupFileAccessibilityActions(info, itemPos);
                    }

                    @Override
                    public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                        int itemPos = listViewFiles.getPositionForView(host);
                        if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                        if (action == AccessibilityNodeInfoCompat.ACTION_CLICK || action == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) {
                            listViewFiles.performItemClick(host, itemPos, fileListAdapter.getItemId(itemPos));
                            return true;
                        }
                        if (action == AccessibilityNodeInfoCompat.ACTION_LONG_CLICK || action == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                            return showFileOptionsForPosition(itemPos);
                        }
                        if (handleFileAccessibilityAction(itemPos, action)) {
                            return true;
                        }
                        return super.performAccessibilityAction(host, action, args);
                    }
                });
                return view;
            }
        };
        listViewFiles.setAdapter(fileListAdapter);

        listViewLibrary = findViewById(R.id.listViewLibrary);
        if (tvLibEmpty != null) {
            listViewLibrary.setEmptyView(tvLibEmpty);
        }
        libraryAdapter = new ArrayAdapter<String>(this, R.layout.item_media_file, R.id.itemText, libraryDisplayNames) {
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tvText = view.findViewById(R.id.itemText);
                TextView tvSubText = view.findViewById(R.id.itemSubText);
                TextView tvIcon = view.findViewById(R.id.itemIcon);
                TextView tvStatus = view.findViewById(R.id.itemStatus);

                if (tvText != null) tvText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvSubText != null) tvSubText.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvIcon != null) tvIcon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                if (tvStatus != null) tvStatus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

                if (position >= 0 && position < libraryDisplayNames.size()) {
                    String name = libraryDisplayNames.get(position);
                    String path = (position < libraryDisplayPaths.size()) ? libraryDisplayPaths.get(position) : "";
                    String displayName = name;

                    boolean isActionItem = name.startsWith("[ +") || "CREATE_NEW_SHELF".equals(path) || "CREATE_NEW_PLAYLIST".equals(path);
                    boolean isShelf = (libraryMode == 4);
                    boolean isPlaylist = (libraryMode == 2);
                    boolean isDir = name.startsWith("📁 ") || (path != null && new File(path).isDirectory());
                    boolean isZip = name.startsWith("📦 ") || (path != null && ZipMediaManager.isZipFile(path));
                    boolean isStream = name.startsWith("☁️ ") || (path != null && (path.startsWith("http://") || path.startsWith("https://")));
                    boolean isAudio = !isActionItem && !isShelf && !isPlaylist && !isDir && !isZip && !isStream && path != null && ZipMediaManager.isAudioFile(path);

                    if (isActionItem) {
                        if (tvIcon != null) tvIcon.setText("➕");
                    } else if (isShelf) {
                        if (tvIcon != null) tvIcon.setText("📚");
                    } else if (isPlaylist) {
                        if (tvIcon != null) tvIcon.setText("📋");
                    } else if (isDir) {
                        if (tvIcon != null) tvIcon.setText("📁");
                        if (displayName.startsWith("📁 ")) displayName = displayName.substring(3);
                    } else if (isZip) {
                        if (tvIcon != null) tvIcon.setText("📦");
                        if (displayName.startsWith("📦 ")) displayName = displayName.substring(3);
                    } else if (isStream) {
                        if (tvIcon != null) tvIcon.setText("☁️");
                        if (displayName.startsWith("☁️ ")) displayName = displayName.substring(3);
                    } else {
                        if (tvIcon != null) tvIcon.setText("🎵");
                        if (displayName.startsWith("🎵 ")) displayName = displayName.substring(3);
                    }

                    if (tvText != null) {
                        tvText.setText(displayName);
                    }

                    boolean isCurrentlyPlaying = (currentPlayingFile != null && !currentPlayingFile.isEmpty() && path != null && path.equals(currentPlayingFile));
                    if (isCurrentlyPlaying) {
                        if (tvStatus != null) {
                            tvStatus.setVisibility(View.VISIBLE);
                            tvStatus.setText("▶ PLAYING");
                        }
                        if (tvText != null) {
                            tvText.setTextColor(Color.parseColor("#80D8FF"));
                            tvText.setTypeface(null, android.graphics.Typeface.BOLD);
                        }
                    } else {
                        if (tvStatus != null) {
                            tvStatus.setVisibility(View.GONE);
                        }
                        if (tvText != null) {
                            tvText.setTextColor(Color.WHITE);
                            tvText.setTypeface(null, android.graphics.Typeface.NORMAL);
                        }
                    }

                    String subTextStr = "";
                    String speechMeta = "";
                    if ((isDir || isZip) && path != null && !path.isEmpty()) {
                        FolderMetadataManager.FolderMeta meta = FolderMetadataManager.getCachedMeta(MainActivity.this, path);
                        if (meta != null) {
                            subTextStr = meta.getShortDescription();
                            speechMeta = meta.getSpeechDescription();
                        } else {
                            FolderMetadataManager.getFolderInfoAsync(MainActivity.this, path, m -> {
                                if (m.fileCount > 0) {
                                    libraryAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    } else if (isAudio && path != null && !path.isEmpty()) {
                        FolderMetadataManager.FileMeta fMeta = FolderMetadataManager.getCachedFileMeta(MainActivity.this, path);
                        if (fMeta != null) {
                            subTextStr = fMeta.getShortDescription();
                            speechMeta = fMeta.getSpeechDescription();
                        } else {
                            FolderMetadataManager.getFileInfoAsync(MainActivity.this, path, m -> {
                                if (m.durationMs > 0 || m.sizeBytes > 0) {
                                    libraryAdapter.notifyDataSetChanged();
                                }
                            });
                        }
                    }

                    if (tvSubText != null) {
                        if (!subTextStr.isEmpty()) {
                            tvSubText.setVisibility(View.VISIBLE);
                            tvSubText.setText(subTextStr);
                        } else {
                            tvSubText.setVisibility(View.GONE);
                        }
                    }

                    String contentDesc = displayName;
                    if (isActionItem) {
                        // keep as is
                    } else if (isShelf) {
                        contentDesc += ", Bookshelf";
                    } else if (isPlaylist) {
                        contentDesc += ", Playlist";
                    } else if (isDir) {
                        contentDesc += ", Folder";
                    } else if (isZip) {
                        contentDesc += ", ZIP archive";
                    } else if (isStream) {
                        contentDesc += ", Online stream";
                    } else if (isAudio) {
                        contentDesc += ", Audio track";
                    }

                    if (!speechMeta.isEmpty()) {
                        contentDesc += ", " + speechMeta;
                    }
                    if (isCurrentlyPlaying) {
                        contentDesc += ", Currently playing";
                    }
                    view.setContentDescription(contentDesc);
                }

                final int pos = position;
                view.setOnClickListener(v -> {
                    int itemPos = listViewLibrary.getPositionForView(v);
                    if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                    listViewLibrary.performItemClick(v, itemPos, libraryAdapter.getItemId(itemPos));
                });
                view.setLongClickable(true);
                view.setOnLongClickListener(v -> {
                    int itemPos = listViewLibrary.getPositionForView(v);
                    if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                    return showLibraryItemOptions(itemPos);
                });
                ViewCompat.setAccessibilityDelegate(view, new AccessibilityDelegateCompat() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        int itemPos = listViewLibrary.getPositionForView(host);
                        if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                        info.setLongClickable(true);
                        info.addAction(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK);
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                AccessibilityNodeInfoCompat.ACTION_LONG_CLICK, "More options"));
                        setupLibraryAccessibilityActions(info, itemPos);
                    }

                    @Override
                    public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                        int itemPos = listViewLibrary.getPositionForView(host);
                        if (itemPos == ListView.INVALID_POSITION) itemPos = pos;
                        if (action == AccessibilityNodeInfoCompat.ACTION_CLICK || action == android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK) {
                            listViewLibrary.performItemClick(host, itemPos, libraryAdapter.getItemId(itemPos));
                            return true;
                        }
                        if (action == AccessibilityNodeInfoCompat.ACTION_LONG_CLICK || action == android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                            return showLibraryItemOptions(itemPos);
                        }
                        if (handleLibraryAccessibilityAction(itemPos, action)) {
                            return true;
                        }
                        return super.performAccessibilityAction(host, action, args);
                    }
                });

                return view;
            }
        };
        listViewLibrary.setAdapter(libraryAdapter);

        if (btnFadeConfig != null) {
            btnFadeConfig.setText("Fading Speed: " + fadeOptions[currentFadeIndex]);
        }
        if (btnGapConfig != null) {
            btnGapConfig.setText("Track Gap: " + gapOptions[currentGapIndex]);
        }
        if (btnAutoPlayNextFolder != null) {
            boolean autoPlay = bkPrefs.getBoolean("auto_play_next_folder", false);
            btnAutoPlayNextFolder.setText("Auto-Play Next Folder: " + (autoPlay ? "ON" : "OFF"));
            btnAutoPlayNextFolder.setContentDescription("Auto-Play Next Folder: " + (autoPlay ? "ON" : "OFF"));
        }
        if (btnSkipSilence != null) {
            btnSkipSilence.setText("Skip Silence: " + (isSkipSilenceEnabled ? "ON" : "OFF"));
        }

        if (btnVirtualizerToggle != null && seekVirtualizer != null) {
            btnVirtualizerToggle.setText("3D Virtualizer: " + (isVirtualizerEnabled ? "ON" : "OFF"));
            seekVirtualizer.setProgress(currentVirtualizerStrength);
            seekVirtualizer.setVisibility(isVirtualizerEnabled ? View.VISIBLE : View.GONE);
        }

        btnClearZipCache = findViewById(R.id.btnClearZipCache);
        btnClearSharedMediaCache = findViewById(R.id.btnClearSharedMediaCache);
        btnClearAllCache = findViewById(R.id.btnClearAllCache);
        updateZipCacheUI();
        updateScreenOffVolToggleUI();
        updateSleepTimerUI();
    }

    private void updateZipCacheUI() {
        if (btnClearZipCache != null) {
            long size = ZipMediaManager.getZipCacheSize(this);
            btnClearZipCache.setText("Clear ZIP Cache (" + ZipMediaManager.formatFileSize(size) + ")");
        }
        if (btnClearSharedMediaCache != null) {
            long sharedSize = getSharedMediaCacheSize();
            btnClearSharedMediaCache.setText("Clear Shared Audio Cache (" + ZipMediaManager.formatFileSize(sharedSize) + ")");
        }
    }

    public long getSharedMediaCacheSize() {
        long size = 0;
        try {
            File extShared = getExternalFilesDir("SharedMedia");
            if (extShared != null && extShared.exists()) {
                size += calculateDirectorySize(extShared);
            }
            File intShared = new File(getFilesDir(), "SharedMedia");
            if (intShared.exists()) {
                size += calculateDirectorySize(intShared);
            }
            File cacheShareZips = new File(getCacheDir(), "shared_zips");
            if (cacheShareZips.exists()) {
                size += calculateDirectorySize(cacheShareZips);
            }
            File extCache = getExternalCacheDir();
            if (extCache != null) {
                File extCacheShareZips = new File(extCache, "shared_zips");
                if (extCacheShareZips.exists()) {
                    size += calculateDirectorySize(extCacheShareZips);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getSharedMediaCacheSize error", e);
        }
        return size;
    }

    public boolean clearSharedMediaCache() {
        boolean success = true;
        try {
            File extShared = getExternalFilesDir("SharedMedia");
            if (extShared != null && extShared.exists()) {
                deleteDirectoryContents(extShared);
            }
            File intShared = new File(getFilesDir(), "SharedMedia");
            if (intShared.exists()) {
                deleteDirectoryContents(intShared);
            }
            File cacheShareZips = new File(getCacheDir(), "shared_zips");
            if (cacheShareZips.exists()) {
                deleteDirectoryContents(cacheShareZips);
            }
            File extCache = getExternalCacheDir();
            if (extCache != null) {
                File extCacheShareZips = new File(extCache, "shared_zips");
                if (extCacheShareZips.exists()) {
                    deleteDirectoryContents(extCacheShareZips);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "clearSharedMediaCache error", e);
            success = false;
        }
        return success;
    }

    private void deleteDirectoryContents(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                ZipMediaManager.deleteRecursive(f);
            }
        }
    }

    private long calculateDirectorySize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) {
                size += calculateDirectorySize(f);
            } else {
                size += f.length();
            }
        }
        return size;
    }

    private void setupShortcutsUI() {
        volUpLongAction = bkPrefs.getInt("vol_up_long", 4);
        volUpDoubleAction = bkPrefs.getInt("vol_up_double", 2);
        volDownLongAction = bkPrefs.getInt("vol_down_long", 5);
        volDownDoubleAction = bkPrefs.getInt("vol_down_double", 3);

        updateShortcutButtonText();

        btnVolUpLong.setOnClickListener(v -> showShortcutSelectionDialog("Vol UP (Long Press)", 1, volUpLongAction));
        btnVolUpDouble.setOnClickListener(v -> showShortcutSelectionDialog("Vol UP (Double Click)", 2, volUpDoubleAction));
        btnVolDownLong.setOnClickListener(v -> showShortcutSelectionDialog("Vol DOWN (Long Press)", 3, volDownLongAction));
        btnVolDownDouble.setOnClickListener(v -> showShortcutSelectionDialog("Vol DOWN (Double Click)", 4, volDownDoubleAction));

        if (btnVolShortcutsMasterToggle != null) {
            updateVolShortcutsMasterToggleUI();
            btnVolShortcutsMasterToggle.setOnClickListener(v -> {
                isVolShortcutsMasterEnabled = !isVolShortcutsMasterEnabled;
                bkPrefs.edit().putBoolean("vol_shortcuts_master_enabled", isVolShortcutsMasterEnabled).apply();
                updateVolShortcutsMasterToggleUI();
                updateMediaSessionVolumeRouting();
                announceAccessibility("Volume key shortcuts turned " + (isVolShortcutsMasterEnabled ? "ON" : "OFF"));
                Toast.makeText(this, "Volume Key Shortcuts: " + (isVolShortcutsMasterEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        }

        if (btnScreenOffVolToggle != null) {
            updateScreenOffVolToggleUI();
            btnScreenOffVolToggle.setOnClickListener(v -> {
                isScreenOffVolEnabled = !isScreenOffVolEnabled;
                bkPrefs.edit().putBoolean("screen_off_vol_enabled", isScreenOffVolEnabled).apply();
                updateScreenOffVolToggleUI();
                updateMediaSessionVolumeRouting();
                announceAccessibility("Screen-Off volume control turned " + (isScreenOffVolEnabled ? "ON" : "OFF"));
                Toast.makeText(this, "Screen-Off Control: " + (isScreenOffVolEnabled ? "ON" : "OFF"), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateVolShortcutsMasterToggleUI() {
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        if (btnVolShortcutsMasterToggle != null) {
            btnVolShortcutsMasterToggle.setText("Volume Key Shortcuts: " + (isVolShortcutsMasterEnabled ? "ON" : "OFF"));
            btnVolShortcutsMasterToggle.setBackgroundColor(isVolShortcutsMasterEnabled ? theme.primaryColor : theme.buttonColor);
        }
        float alpha = isVolShortcutsMasterEnabled ? 1.0f : 0.85f;
        int textColor = isVolShortcutsMasterEnabled ? Color.WHITE : Color.parseColor("#B0BEC5");
        int bgColor = isVolShortcutsMasterEnabled ? theme.buttonColor : Color.parseColor("#1C1C1C");
        Button[] buttons = {btnScreenOffVolToggle, btnVolUpLong, btnVolUpDouble, btnVolDownLong, btnVolDownDouble};
        for (Button b : buttons) {
            if (b != null) {
                b.setEnabled(isVolShortcutsMasterEnabled);
                b.setAlpha(alpha);
                b.setTextColor(textColor);
                b.setBackgroundColor(bgColor);
            }
        }
    }

    private void updateScreenOffVolToggleUI() {
        if (btnScreenOffVolToggle != null) {
            ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
            btnScreenOffVolToggle.setText("Screen-Off / Background Control: " + (isScreenOffVolEnabled ? "ON" : "OFF"));
            btnScreenOffVolToggle.setBackgroundColor(isScreenOffVolEnabled ? theme.primaryColor : theme.buttonColor);
        }
    }

    private void updateShortcutButtonText() {
        btnVolUpLong.setText("Vol UP (Long Press): " + shortcutOptions[volUpLongAction]);
        btnVolUpDouble.setText("Vol UP (Double Click): " + shortcutOptions[volUpDoubleAction]);
        btnVolDownLong.setText("Vol DOWN (Long Press): " + shortcutOptions[volDownLongAction]);
        btnVolDownDouble.setText("Vol DOWN (Double Click): " + shortcutOptions[volDownDoubleAction]);
    }

    private void showShortcutSelectionDialog(String title, int buttonId, int currentAction) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(shortcutOptions, currentAction, (dialog, which) -> {
                    if (buttonId == 1) {
                        volUpLongAction = which;
                        bkPrefs.edit().putInt("vol_up_long", which).apply();
                    } else if (buttonId == 2) {
                        volUpDoubleAction = which;
                        bkPrefs.edit().putInt("vol_up_double", which).apply();
                    } else if (buttonId == 3) {
                        volDownLongAction = which;
                        bkPrefs.edit().putInt("vol_down_long", which).apply();
                    } else if (buttonId == 4) {
                        volDownDoubleAction = which;
                        bkPrefs.edit().putInt("vol_down_double", which).apply();
                    }
                    updateShortcutButtonText();
                    announceAccessibility(title + " set to " + shortcutOptions[which]);
                    dialog.dismiss();
                }).show();
    }

    private void fadeAudio(float startVolume, float endVolume, long baseDurationMs, Runnable onComplete) {
        runOnUiThread(() -> {
            if (mediaPlayer == null) {
                if (onComplete != null) onComplete.run();
                return;
            }

            long actualDuration = (long) (baseDurationMs * currentFadeMultiplier);

            if (actualDuration <= 0 || currentFadeMultiplier <= 0) {
                try {
                    mediaPlayer.setVolume(endVolume);
                } catch (Exception ignored) {}
                if (onComplete != null) onComplete.run();
                return;
            }

            if (volumeAnimator != null && volumeAnimator.isRunning()) {
                volumeAnimator.cancel();
            }

            float effectiveStart = startVolume;
            if (effectiveStart < 0) {
                try {
                    effectiveStart = mediaPlayer.getVolume();
                } catch (Exception ignored) {
                    effectiveStart = (endVolume == 0f) ? 1.0f : 0.0f;
                }
            }

            volumeAnimator = ValueAnimator.ofFloat(effectiveStart, endVolume);
            volumeAnimator.setDuration(actualDuration);
            volumeAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            volumeAnimator.addUpdateListener(animation -> {
                if (mediaPlayer != null) {
                    try {
                        mediaPlayer.setVolume((float) animation.getAnimatedValue());
                    } catch (Exception ignored) {}
                }
            });

            volumeAnimator.addListener(new AnimatorListenerAdapter() {
                private boolean isCancelled = false;

                @Override
                public void onAnimationCancel(Animator animation) {
                    isCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!isCancelled && onComplete != null) {
                        onComplete.run();
                    }
                }
            });

            volumeAnimator.start();
        });
    }

    private void safeSeekWithFade(long deltaMs, String announcement) {
        runOnUiThread(() -> {
            if (mediaPlayer == null || currentPlayingFile.isEmpty()) {
                announceAccessibility("No track playing");
                return;
            }

            long duration = mediaPlayer.getDuration();
            long currentPos = (targetSeekPosition >= 0) ? targetSeekPosition : mediaPlayer.getCurrentPosition();
            long newPos = currentPos + deltaMs;
            if (newPos < 0) newPos = 0;
            if (duration > 0 && newPos > duration) newPos = duration;
            targetSeekPosition = newPos;

            if (announcement != null && !announcement.isEmpty()) {
                announceAccessibility(announcement);
            }

            if (!mediaPlayer.isPlaying() || currentFadeMultiplier <= 0) {
                mediaPlayer.seekTo(newPos);
                targetSeekPosition = -1;
                return;
            }

            if (pendingSeekFadeInRunnable != null) {
                handler.removeCallbacks(pendingSeekFadeInRunnable);
                pendingSeekFadeInRunnable = null;
            }

            long fadeOutTime = (long) (180 * currentFadeMultiplier);
            long fadeInTime = (long) (240 * currentFadeMultiplier);

            fadeAudio(-1f, 0.0f, fadeOutTime, () -> {
                if (mediaPlayer != null && targetSeekPosition >= 0) {
                    mediaPlayer.seekTo(targetSeekPosition);
                    pendingSeekFadeInRunnable = () -> {
                        targetSeekPosition = -1;
                        fadeAudio(0.0f, 1.0f, fadeInTime, null);
                    };
                    handler.postDelayed(pendingSeekFadeInRunnable, 50);
                }
            });
        });
    }

    private final Runnable trackFadeMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
                long duration = mediaPlayer.getDuration();
                long current = mediaPlayer.getCurrentPosition();
                if (duration > 4000) {
                    long remaining = duration - current;
                    long fadeOutDur = (long) (1200 * currentFadeMultiplier);
                    if (remaining <= fadeOutDur && remaining > 100 && !isTrackFadingOutAtEnd) {
                        isTrackFadingOutAtEnd = true;
                        fadeAudio(-1f, 0.0f, remaining, null);
                    } else if (isTrackFadingOutAtEnd && remaining > fadeOutDur + 1500) {
                        isTrackFadingOutAtEnd = false;
                        fadeAudio(-1f, 1.0f, 300, null);
                    }
                }
            }
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                long now = System.currentTimeMillis();
                if (now - lastPeriodicSaveTime > 3000) {
                    lastPeriodicSaveTime = now;
                    saveCurrentPosition();
                }
                handler.postDelayed(this, 150);
            }
        }
    };

    private void indexAllAudioForSearch() {
        new Thread(() -> {
            List<String> tempNames = new ArrayList<>();
            List<String> tempPaths = new ArrayList<>();

            String[] projection = {MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA};
            try (android.database.Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(0);
                        String path = cursor.getString(1);
                        if (name != null && path != null) {
                            tempNames.add(name);
                            tempPaths.add(path);
                        }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Audio Indexing error", e); }

            try (android.database.Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)) {
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(0);
                        String path = cursor.getString(1);
                        if (name != null && path != null) {
                            tempNames.add(name);
                            tempPaths.add(path);
                        }
                    }
                }
            } catch (Exception e) { Log.e(TAG, "Video Indexing error", e); }

            Set<String> tempKnownFolders = new HashSet<>();
            for (String p : tempPaths) {
                if (p != null) {
                    File parent = new File(p).getParentFile();
                    while (parent != null) {
                        String pAbs = parent.getAbsolutePath();
                        if (pAbs.endsWith("/") && pAbs.length() > 1) {
                            pAbs = pAbs.substring(0, pAbs.length() - 1);
                        }
                        tempKnownFolders.add(pAbs);
                        parent = parent.getParentFile();
                    }
                }
            }

            runOnUiThread(() -> {
                globalAudioNames.clear();
                globalAudioPaths.clear();
                globalAudioNames.addAll(tempNames);
                globalAudioPaths.addAll(tempPaths);
                knownMediaFoldersSet.clear();
                knownMediaFoldersSet.addAll(tempKnownFolders);
                mediaFolderCache.clear();
                if (!isBrowsingZip && !isBrowsingDrive && (inputSearch == null || inputSearch.getText().toString().isEmpty())) {
                    loadDirectory(currentDir != null && !currentDir.isEmpty() ? currentDir : Environment.getExternalStorageDirectory().getAbsolutePath());
                }
            });
        }).start();
    }

    private void setupUniversalSearch() {
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase().trim();
                if (query.isEmpty()) {
                    loadDirectory(currentDir);
                } else {
                    currentDisplayNames.clear();
                    currentDisplayPaths.clear();
                    for (int i = 0; i < globalAudioNames.size(); i++) {
                        if (globalAudioNames.get(i).toLowerCase().contains(query)) {
                            currentDisplayNames.add(globalAudioNames.get(i));
                            currentDisplayPaths.add(globalAudioPaths.get(i));
                        }
                    }
                    fileListAdapter.notifyDataSetChanged();
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void saveCurrentPosition() {
        if (!currentPlayingFile.isEmpty() && mediaPlayer != null) {
            long pos = mediaPlayer.getCurrentPosition();
            if (pos < 0) pos = 0;
            SharedPreferences.Editor editor = memPrefs.edit();
            editor.putInt(currentPlayingFile, (int) pos);
            editor.putString("global_last_file", currentPlayingFile);
            editor.putInt("global_last_pos", (int) pos);
            editor.putString("global_last_zip_name", currentZipName);
            editor.putString("global_last_zip_path", currentZipFilePath);
            editor.putString("global_last_drive_name", currentDriveFolderName);
            editor.putString("global_last_drive_url", currentDriveFolderUrl);

            if (!isBrowsingDrive && !isBrowsingZip && !currentPlayingFile.startsWith("http")) {
                File file = new File(currentPlayingFile);
                File parent = file.getParentFile();
                if (parent != null) {
                    String folderPath = parent.getAbsolutePath();
                    editor.putString("folder_last_file_" + folderPath, currentPlayingFile);
                    editor.putInt("folder_last_pos_" + folderPath, (int) pos);
                }
            } else if (isBrowsingZip && !currentZipFilePath.isEmpty()) {
                editor.putString("folder_last_file_zip_" + currentZipFilePath, currentPlayingFile);
                editor.putInt("folder_last_pos_zip_" + currentZipFilePath, (int) pos);
            } else if (isBrowsingDrive && !currentDriveFolderUrl.isEmpty()) {
                editor.putString("folder_last_file_drive_" + currentDriveFolderUrl, currentPlayingFile);
                editor.putInt("folder_last_pos_drive_" + currentDriveFolderUrl, (int) pos);
            }
            editor.apply();
            updateAppShortcuts();
        }
    }

    private void restoreLastPlayedInfo() {
        if (memPrefs == null || tvNowPlaying == null) return;
        String lastFile = memPrefs.getString("global_last_file", "");
        int lastPos = memPrefs.getInt("global_last_pos", 0);
        if (!lastFile.isEmpty()) {
            String dispName = new File(lastFile).getName();
            String lastZip = memPrefs.getString("global_last_zip_name", "");
            String lastDrive = memPrefs.getString("global_last_drive_name", "");
            if (!lastZip.isEmpty()) {
                tvNowPlaying.setText("⏸ Last: [" + lastZip + "] " + dispName + " (" + formatTime(lastPos) + ")");
            } else if (!lastDrive.isEmpty()) {
                tvNowPlaying.setText("⏸ Last: [Drive: " + lastDrive + "] " + dispName + " (" + formatTime(lastPos) + ")");
            } else {
                tvNowPlaying.setText("⏸ Last: " + dispName + " (" + formatTime(lastPos) + ")");
            }
        }
    }

    private void resumeGlobalLastPlayed() {
        if (memPrefs == null) return;
        String lastFile = memPrefs.getString("global_last_file", "");
        if (lastFile == null || lastFile.isEmpty()) {
            announceAccessibility("No track has been played yet");
            Toast.makeText(this, "No track has been played yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String lastZipPath = memPrefs.getString("global_last_zip_path", "");
        String lastDriveUrl = memPrefs.getString("global_last_drive_url", "");

        if (!lastZipPath.isEmpty() && new File(lastZipPath).exists()) {
            tryPlayZipFromLibrary(lastZipPath);
        } else if (!lastDriveUrl.isEmpty()) {
            handleGoogleDriveUrl(lastDriveUrl);
        } else {
            File file = new File(lastFile);
            if (file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && parent.exists()) {
                    loadDirectory(parent.getAbsolutePath());
                }
                playExplorerAudioFile(lastFile);
            } else {
                announceAccessibility("Last played file not found: " + file.getName());
                Toast.makeText(this, "Last played file not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateAppShortcuts() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return;

            String lastFile = (memPrefs != null) ? memPrefs.getString("global_last_file", "") : "";
            String resumeShort = "Resume";
            String resumeLong = "Resume Last Played Track";
            if (!lastFile.isEmpty()) {
                String name = new File(lastFile).getName();
                resumeShort = "Resume";
                resumeLong = "Resume: " + name;
            }

            Intent resumeIntent = new Intent(this, MainActivity.class);
            resumeIntent.setAction("com.example.ktsadvancedmediaplayer.action.RESUME_LAST");
            resumeIntent.putExtra("shortcut_action", "resume_last");
            resumeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfoCompat resumeShortcut = new ShortcutInfoCompat.Builder(this, "shortcut_resume")
                    .setShortLabel(resumeShort)
                    .setLongLabel(resumeLong)
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_resume))
                    .setIntent(resumeIntent)
                    .setRank(1)
                    .build();

            Intent shelfIntent = new Intent(this, MainActivity.class);
            shelfIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_BOOKSHELF");
            shelfIntent.putExtra("shortcut_action", "bookshelf");
            shelfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfoCompat shelfShortcut = new ShortcutInfoCompat.Builder(this, "shortcut_bookshelf")
                    .setShortLabel("Book Shelf")
                    .setLongLabel("Open Book Shelf")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_bookshelf))
                    .setIntent(shelfIntent)
                    .setRank(2)
                    .build();

            Intent favIntent = new Intent(this, MainActivity.class);
            favIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_FAVORITES");
            favIntent.putExtra("shortcut_action", "favorites");
            favIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfoCompat favShortcut = new ShortcutInfoCompat.Builder(this, "shortcut_favorites")
                    .setShortLabel("Favorites")
                    .setLongLabel("Open Favorites")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_favorites))
                    .setIntent(favIntent)
                    .setRank(3)
                    .build();

            Intent histIntent = new Intent(this, MainActivity.class);
            histIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_HISTORY");
            histIntent.putExtra("shortcut_action", "history");
            histIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            ShortcutInfoCompat histShortcut = new ShortcutInfoCompat.Builder(this, "shortcut_history")
                    .setShortLabel("History")
                    .setLongLabel("Open History")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_history))
                    .setIntent(histIntent)
                    .setRank(4)
                    .build();

            List<ShortcutInfoCompat> list = new ArrayList<>();
            list.add(resumeShortcut);
            list.add(shelfShortcut);
            list.add(favShortcut);
            list.add(histShortcut);

            ShortcutManagerCompat.setDynamicShortcuts(this, list);
        } catch (Exception e) {
            Log.e(TAG, "Error updating app shortcuts", e);
        }
    }

    private void pinAppShortcut(String shortcutId) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(this)) {
            Toast.makeText(this, "Direct pinning not supported by your launcher. Long press the app icon on your home screen to use shortcuts.", Toast.LENGTH_LONG).show();
            announceAccessibility("Direct pinning not supported. Long press app icon on home screen to use shortcuts.");
            return;
        }

        ShortcutInfoCompat pinShortcut = null;
        if ("shortcut_resume".equals(shortcutId)) {
            String lastFile = (memPrefs != null) ? memPrefs.getString("global_last_file", "") : "";
            String resumeLong = !lastFile.isEmpty() ? "Resume: " + new File(lastFile).getName() : "Resume Last Played Track";
            Intent resumeIntent = new Intent(this, MainActivity.class);
            resumeIntent.setAction("com.example.ktsadvancedmediaplayer.action.RESUME_LAST");
            resumeIntent.putExtra("shortcut_action", "resume_last");
            resumeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            pinShortcut = new ShortcutInfoCompat.Builder(this, "pinned_resume")
                    .setShortLabel("Resume")
                    .setLongLabel(resumeLong)
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_resume))
                    .setIntent(resumeIntent)
                    .build();
        } else if ("shortcut_bookshelf".equals(shortcutId)) {
            Intent shelfIntent = new Intent(this, MainActivity.class);
            shelfIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_BOOKSHELF");
            shelfIntent.putExtra("shortcut_action", "bookshelf");
            shelfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            pinShortcut = new ShortcutInfoCompat.Builder(this, "pinned_bookshelf")
                    .setShortLabel("Book Shelf")
                    .setLongLabel("Open Book Shelf")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_bookshelf))
                    .setIntent(shelfIntent)
                    .build();
        } else if ("shortcut_favorites".equals(shortcutId)) {
            Intent favIntent = new Intent(this, MainActivity.class);
            favIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_FAVORITES");
            favIntent.putExtra("shortcut_action", "favorites");
            favIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            pinShortcut = new ShortcutInfoCompat.Builder(this, "pinned_favorites")
                    .setShortLabel("Favorites")
                    .setLongLabel("Open Favorites")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_favorites))
                    .setIntent(favIntent)
                    .build();
        } else if ("shortcut_history".equals(shortcutId)) {
            Intent histIntent = new Intent(this, MainActivity.class);
            histIntent.setAction("com.example.ktsadvancedmediaplayer.action.OPEN_HISTORY");
            histIntent.putExtra("shortcut_action", "history");
            histIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

            pinShortcut = new ShortcutInfoCompat.Builder(this, "pinned_history")
                    .setShortLabel("History")
                    .setLongLabel("Open History")
                    .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_history))
                    .setIntent(histIntent)
                    .build();
        }

        if (pinShortcut != null) {
            ShortcutManagerCompat.requestPinShortcut(this, pinShortcut, null);
            Toast.makeText(this, "Shortcut pinned to Home Screen!", Toast.LENGTH_SHORT).show();
            announceAccessibility("Shortcut pinned to Home Screen");
        }
    }

    private void resetAB() {
        abState = 0;
        btnLoop.setText("Loop");
        handler.removeCallbacks(loopRunnable);
    }

    private String formatTimeSpeech(long ms) {
        if (ms <= 0) return "0 seconds";
        int totalSec = (int) (ms / 1000);
        int m = totalSec / 60;
        int s = totalSec % 60;
        if (m > 0 && s > 0) return m + " minutes and " + s + " seconds";
        if (m > 0) return m + " minutes";
        return s + " seconds";
    }

    private String formatTime(int ms) {
        int totalSec = ms / 1000;
        int m = totalSec / 60;
        int s = totalSec % 60;
        return m + "m " + s + "s";
    }

    private String formatTime(long ms) {
        return formatTime((int) ms);
    }

    private void updateSeekButtons() {
        String txt = (seekStep == 60000) ? "1m" : (seekStep / 1000) + "s";
        btnRewind.setText("⏪ " + txt);
        btnForward.setText(txt + " ⏩");
        btnRewind.setContentDescription(txt + " backward");
        btnForward.setContentDescription(txt + " forward");
    }

    private void showTimeDetails() {
        StringBuilder sb = new StringBuilder();
        StringBuilder speechSb = new StringBuilder();

        if (mediaPlayer != null && !currentPlayingFile.isEmpty()) {
            long current = mediaPlayer.getCurrentPosition();
            long total = mediaPlayer.getDuration();
            if (total < 0) total = 0;
            long remaining = total > 0 ? total - current : 0;

            String trackName = new File(currentPlayingFile).getName();
            sb.append("Track: ").append(trackName).append("\n");
            sb.append("• Current: ").append(formatTime(current)).append("\n");
            sb.append("• Total: ").append(formatTime(total)).append("\n");
            sb.append("• Remaining: ").append(formatTime(remaining)).append("\n\n");

            speechSb.append("Track total time ").append(formatTimeSpeech(total))
                    .append(". Current time ").append(formatTimeSpeech(current))
                    .append(". Remaining time ").append(formatTimeSpeech(remaining)).append(". ");
        }

        int folderFileCount = currentAudioList.size();
        if (folderFileCount > 0) {
            String folderName = "Current Folder";
            if (isBrowsingZip && !currentZipName.isEmpty()) {
                folderName = "ZIP: " + currentZipName;
            } else if (isBrowsingDrive && !currentDriveFolderName.isEmpty()) {
                folderName = "Drive: " + currentDriveFolderName;
            } else if (currentDir != null && !currentDir.isEmpty()) {
                folderName = new File(currentDir).getName();
            }

            FolderMetadataManager.FolderMeta meta = FolderMetadataManager.getCachedMeta(this, currentDir);
            long folderTotalDur = 0;
            if (meta != null && meta.totalDurationMs > 0) {
                folderTotalDur = meta.totalDurationMs;
            } else {
                folderTotalDur = FolderMetadataManager.calculateTotalDurationForFiles(this, currentAudioList);
            }

            sb.append(folderName).append(":\n");
            sb.append("• Files: ").append(folderFileCount).append("\n");
            if (folderTotalDur > 0) {
                sb.append("• Total Duration: ").append(FolderMetadataManager.formatDurationShort(folderTotalDur));
                speechSb.append(folderName).append(" has ").append(folderFileCount).append(folderFileCount == 1 ? " file. " : " files. ")
                        .append("Total folder duration is ").append(FolderMetadataManager.formatDurationSpeech(folderTotalDur)).append(".");
            } else {
                speechSb.append(folderName).append(" has ").append(folderFileCount).append(folderFileCount == 1 ? " file. " : " files.");
            }
        }

        if (sb.length() == 0) {
            announceAccessibility("No track is currently playing.");
            return;
        }

        String displayMsg = sb.toString().trim();
        String speechMsg = speechSb.toString().trim();

        Toast.makeText(MainActivity.this, displayMsg, Toast.LENGTH_LONG).show();
        announceAccessibility(speechMsg);
    }

    private void showFolderTimeDetails() {
        if ("STORAGE_ROOT".equals(currentDir)) {
            announceAccessibility("Storage root view. Select a folder to view details.");
            return;
        }
        String folderName = "Current Folder";
        if (isBrowsingZip && !currentZipName.isEmpty()) {
            folderName = "ZIP: " + currentZipName;
        } else if (isBrowsingDrive && !currentDriveFolderName.isEmpty()) {
            folderName = "Drive: " + currentDriveFolderName;
        } else if (currentDir != null && !currentDir.isEmpty()) {
            folderName = new File(currentDir).getName();
        }

        final String finalFolderName = folderName;
        FolderMetadataManager.FolderMeta meta = FolderMetadataManager.getCachedMeta(this, currentDir);
        if (meta != null && meta.totalDurationMs > 0) {
            String msg = finalFolderName + "\n" + meta.fileCount + " tracks • Total Duration: " + FolderMetadataManager.formatDurationShort(meta.totalDurationMs);
            String speech = finalFolderName + " contains " + meta.fileCount + " tracks. Total duration is " + FolderMetadataManager.formatDurationSpeech(meta.totalDurationMs);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            announceAccessibility(speech);
        } else {
            announceAccessibility("Calculating duration for " + finalFolderName + "...");
            FolderMetadataManager.getFolderInfoAsync(this, currentDir, m -> {
                String durStr = m.totalDurationMs > 0 ? " • Total Duration: " + FolderMetadataManager.formatDurationShort(m.totalDurationMs) : "";
                String msg = finalFolderName + "\n" + m.fileCount + " tracks" + durStr;
                String speech = finalFolderName + " contains " + m.fileCount + " tracks." + (m.totalDurationMs > 0 ? " Total duration is " + FolderMetadataManager.formatDurationSpeech(m.totalDurationMs) : "");
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                announceAccessibility(speech);
            });
        }
    }

    private void showSliderStepDialog() {
        final String[] options = {"1%", "2%", "5%", "10%", "15%", "20%", "25%"};
        final int[] values = {1, 2, 5, 10, 15, 20, 25};
        int selectedIndex = 2;
        for (int i = 0; i < values.length; i++) {
            if (values[i] == sliderStepPercent) {
                selectedIndex = i;
                break;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("Configure Slider Step")
                .setSingleChoiceItems(options, selectedIndex, (dialog, which) -> {
                    sliderStepPercent = values[which];
                    bkPrefs.edit().putInt("slider_step_percent", sliderStepPercent).apply();
                    if (btnSliderStepSetting != null) {
                        btnSliderStepSetting.setText("Slider Step: " + sliderStepPercent + "%");
                    }
                    announceAccessibility("Slider step set to " + options[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performSliderStep(int deltaPercent) {
        if (mediaPlayer == null) return;
        long total = mediaPlayer.getDuration();
        if (total <= 0) return;
        long current = mediaPlayer.getCurrentPosition();
        long deltaMs = (total * deltaPercent) / 100;
        long target = current + deltaMs;
        if (target < 0) target = 0;
        if (target > total) target = total;
        final long finalTarget = target;
        fadeAudio(1.0f, 0.0f, 100, () -> {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(finalTarget);
            }
            fadeAudio(0.0f, 1.0f, 200, null);
        });
        int percent = (int) ((finalTarget * 100) / total);
        announceAccessibility("Slider " + (deltaPercent > 0 ? "forward " : "backward ") + Math.abs(deltaPercent) + " percent, position " + percent + " percent, " + formatTimeSpeech(finalTarget));
        updatePlaybackProgressUI();
    }

    private void updatePlaybackProgressUI() {
        if (seekBarPlayback == null || isUserTrackingSeekBar) return;
        if (mediaPlayer == null) {
            seekBarPlayback.setProgress(0);
            if (tvCurrentProgressTime != null) tvCurrentProgressTime.setText("00:00");
            if (tvTotalProgressTime != null) tvTotalProgressTime.setText("00:00");
            return;
        }

        long current = mediaPlayer.getCurrentPosition();
        long total = mediaPlayer.getDuration();
        if (total > 0) {
            int progress = (int) ((current * 1000) / total);
            seekBarPlayback.setProgress(progress);
            if (tvCurrentProgressTime != null) tvCurrentProgressTime.setText(formatTime(current));
            if (tvTotalProgressTime != null) tvTotalProgressTime.setText(formatTime(total));
            int percent = (int) ((current * 100) / total);
            seekBarPlayback.setContentDescription("Playback progress slider, " + percent + " percent, current position " + formatTimeSpeech(current) + " of " + formatTimeSpeech(total));
        } else {
            seekBarPlayback.setProgress(0);
            if (tvCurrentProgressTime != null) tvCurrentProgressTime.setText(formatTime(current > 0 ? current : 0));
            if (tvTotalProgressTime != null) tvTotalProgressTime.setText("00:00");
            seekBarPlayback.setContentDescription("Playback progress slider, position " + formatTimeSpeech(current));
        }
    }

    private void showSeekSettings() {
        final String[] options = {"5 seconds", "10 seconds", "15 seconds", "30 seconds", "1 minute"};
        final int[] values = {5000, 10000, 15000, 30000, 60000};
        new AlertDialog.Builder(this)
                .setTitle("Configure Seek Time")
                .setSingleChoiceItems(options, currentSeekIndex, (dialog, which) -> {
                    currentSeekIndex = which;
                    seekStep = values[which];
                    updateSeekButtons();
                    announceAccessibility("Seek time set to " + options[which]);
                    dialog.dismiss();
                }).show();
    }

    private void updateResumeToggleUI() {
        if (btnResumeToggle == null) return;
        boolean isEnabled = bkPrefs.getBoolean("resume_playback_enabled", true);
        btnResumeToggle.setText(isEnabled ? "Resume Playback: ON" : "Resume Playback: OFF");
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnResumeToggle.setBackgroundColor(isEnabled ? theme.primaryColor : theme.buttonColor);
    }

    private void updateBgPlaybackToggleUI() {
        if (btnBgPlaybackToggle == null) return;
        boolean isEnabled = bkPrefs.getBoolean("bg_playback_enabled", true);
        btnBgPlaybackToggle.setText(isEnabled ? "Background Playback: ON" : "Background Playback: OFF");
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnBgPlaybackToggle.setBackgroundColor(isEnabled ? theme.primaryColor : theme.buttonColor);
    }

    private void updateFilterMediaFoldersToggleUI() {
        if (btnFilterMediaFolders == null) return;
        boolean isEnabled = bkPrefs.getBoolean("filter_only_media_folders", true);
        btnFilterMediaFolders.setText(isEnabled ? "Show Media Folders Only: ON" : "Show Media Folders Only: OFF");
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnFilterMediaFolders.setBackgroundColor(isEnabled ? theme.primaryColor : theme.buttonColor);
    }

    private void updateAutoPlayNextFolderUI() {
        if (btnAutoPlayNextFolder == null) return;
        boolean autoPlay = bkPrefs.getBoolean("auto_play_next_folder", false);
        btnAutoPlayNextFolder.setText("Auto-Play Next Folder: " + (autoPlay ? "ON" : "OFF"));
        btnAutoPlayNextFolder.setContentDescription("Auto-Play Next Folder: " + (autoPlay ? "ON" : "OFF"));
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnAutoPlayNextFolder.setBackgroundColor(autoPlay ? theme.primaryColor : theme.buttonColor);
    }

    private void setupMediaSession() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaSession = new MediaSession(this, "KTS_Advanced_Player");
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { runOnUiThread(() -> handleNotificationPlayPause()); }
            @Override public void onPause() { runOnUiThread(() -> handleNotificationPlayPause()); }
            @Override public void onSkipToNext() { runOnUiThread(() -> handleNotificationNext()); }
            @Override public void onSkipToPrevious() { runOnUiThread(() -> handleNotificationPrev()); }
            @Override public void onFastForward() { runOnUiThread(() -> handleNotificationForward()); }
            @Override public void onRewind() { runOnUiThread(() -> handleNotificationRewind()); }
            @Override public void onStop() { runOnUiThread(() -> handleNotificationStop()); }
            @Override
            public void onSeekTo(long pos) {
                runOnUiThread(() -> {
                    if (mediaPlayer != null) {
                        try {
                            mediaPlayer.seekTo(pos);
                            updatePlaybackProgressUI();
                            updateMediaSessionState(mediaPlayer.isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED);
                        } catch (Exception ignored) {}
                    }
                });
            }
            @Override
            public void onCustomAction(String action, Bundle extras) {
                if (MediaNotificationManager.ACTION_REWIND.equals(action)) {
                    runOnUiThread(() -> handleNotificationRewind());
                } else if (MediaNotificationManager.ACTION_FORWARD.equals(action)) {
                    runOnUiThread(() -> handleNotificationForward());
                } else if (MediaNotificationManager.ACTION_PREV.equals(action)) {
                    runOnUiThread(() -> handleNotificationPrev());
                } else if (MediaNotificationManager.ACTION_NEXT.equals(action)) {
                    runOnUiThread(() -> handleNotificationNext());
                } else if (MediaNotificationManager.ACTION_PLAY_PAUSE.equals(action)) {
                    runOnUiThread(() -> handleNotificationPlayPause());
                } else if (MediaNotificationManager.ACTION_STOP.equals(action)) {
                    runOnUiThread(() -> handleNotificationStop());
                }
            }
        });

        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setClass(this, MediaActionReceiver.class);
        PendingIntent mbrPendingIntent = PendingIntent.getBroadcast(
                this, 0, mediaButtonIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );
        mediaSession.setMediaButtonReceiver(mbrPendingIntent);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mediaSession.setActive(true);
        updateMediaSessionVolumeRouting();
        setupVolumeContentObserver();
        updateMediaSessionState(PlaybackState.STATE_PAUSED);
    }

    public void updateMediaSessionState(int state) {
        if (mediaSession == null) return;
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE |
                        PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_SKIP_TO_NEXT |
                        PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_FAST_FORWARD |
                        PlaybackState.ACTION_REWIND | PlaybackState.ACTION_SEEK_TO | PlaybackState.ACTION_STOP);

        stateBuilder.addCustomAction(new PlaybackState.CustomAction.Builder(
                MediaNotificationManager.ACTION_REWIND, "Rewind", R.drawable.ic_notif_rewind
        ).build());
        stateBuilder.addCustomAction(new PlaybackState.CustomAction.Builder(
                MediaNotificationManager.ACTION_FORWARD, "Fast Forwarding", R.drawable.ic_notif_forward
        ).build());

        long position = (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : PlaybackState.PLAYBACK_POSITION_UNKNOWN;
        stateBuilder.setState(state, position, (state == PlaybackState.STATE_PLAYING) ? currentSpeed : 0.0f);
        mediaSession.setPlaybackState(stateBuilder.build());
        updateMediaSessionVolumeRouting();
    }

    public void updateMediaSessionMetadata(String title, String artist, long durationMs) {
        if (mediaSession == null) return;
        try {
            MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                    .putString(MediaMetadata.METADATA_KEY_ALBUM, "KTS Advanced Player")
                    .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs);
            mediaSession.setMetadata(metaBuilder.build());
        } catch (Exception ignored) {}
    }

    public void updateMediaNotification() {
        if (mediaPlayer == null) return;
        boolean isPlaying = mediaPlayer.isPlaying();

        String rawTitle = (tvNowPlaying != null) ? tvNowPlaying.getText().toString() : "";
        if (rawTitle.startsWith("🎵 ")) {
            rawTitle = rawTitle.substring(3);
        } else if (rawTitle.startsWith("📻 ")) {
            rawTitle = rawTitle.substring(3);
        }
        if (rawTitle.isEmpty() && currentPlayingFile != null && !currentPlayingFile.isEmpty()) {
            rawTitle = new File(currentPlayingFile).getName();
        }
        if (rawTitle.isEmpty()) {
            rawTitle = "KTS Advanced Media Player";
        }

        String subtitle = "";
        if (!currentZipName.isEmpty()) {
            subtitle = "Archive: " + currentZipName;
        } else if (!currentDriveFolderName.isEmpty()) {
            subtitle = "Google Drive: " + currentDriveFolderName;
        } else if (currentDir != null && !currentDir.isEmpty()) {
            subtitle = new File(currentDir).getName();
        } else {
            subtitle = "KTS Player";
        }

        long duration = mediaPlayer.getDuration();
        if (duration < 0) duration = 0;
        updateMediaSessionMetadata(rawTitle, subtitle, duration);

        int sessionState = isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        if (mediaPlayer.getPlaybackState() == Player.STATE_IDLE || mediaPlayer.getPlaybackState() == Player.STATE_ENDED) {
            sessionState = PlaybackState.STATE_STOPPED;
        }
        updateMediaSessionState(sessionState);

        try {
            Intent notifIntent = new Intent(this, MediaNotificationService.class);
            notifIntent.setAction(MediaNotificationService.ACTION_UPDATE);
            notifIntent.putExtra(MediaNotificationService.EXTRA_TITLE, rawTitle);
            notifIntent.putExtra(MediaNotificationService.EXTRA_SUBTITLE, subtitle);
            notifIntent.putExtra(MediaNotificationService.EXTRA_IS_PLAYING, isPlaying);
            if (mediaSession != null) {
                notifIntent.putExtra(MediaNotificationService.EXTRA_TOKEN, mediaSession.getSessionToken());
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(notifIntent);
            } else {
                startService(notifIntent);
            }
        } catch (Exception e) {
            try {
                MediaNotificationManager manager = new MediaNotificationManager(this);
                manager.showNotificationDirectly(manager.buildNotification(
                        mediaSession != null ? mediaSession.getSessionToken() : null,
                        rawTitle,
                        subtitle,
                        isPlaying
                ));
            } catch (Exception ignored) {}
        }
    }

    public void dismissMediaNotification() {
        try {
            Intent stopIntent = new Intent(this, MediaNotificationService.class);
            stopIntent.setAction(MediaNotificationService.ACTION_STOP);
            startService(stopIntent);
        } catch (Exception ignored) {}
        try {
            new MediaNotificationManager(this).cancelNotification();
        } catch (Exception ignored) {}
    }

    public void handleNotificationPlayPause() {
        runOnUiThread(() -> {
            if (btnPlay != null) btnPlay.performClick();
        });
    }

    public void handleNotificationNext() {
        runOnUiThread(() -> {
            if (btnNext != null) btnNext.performClick();
        });
    }

    public void handleNotificationPrev() {
        runOnUiThread(() -> {
            if (btnPrev != null) btnPrev.performClick();
        });
    }

    public void handleNotificationRewind() {
        runOnUiThread(() -> {
            if (btnRewind != null) btnRewind.performClick();
        });
    }

    public void handleNotificationForward() {
        runOnUiThread(() -> {
            if (btnForward != null) btnForward.performClick();
        });
    }

    public void handleNotificationStop() {
        runOnUiThread(() -> {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                saveCurrentPosition();
            }
            updateMediaSessionState(PlaybackState.STATE_STOPPED);
            dismissMediaNotification();
        });
    }

    private void updateMediaSessionVolumeRouting() {
        if (mediaSession == null) return;
        int usage = isEarSpeakerRouted ? android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION : android.media.AudioAttributes.USAGE_MEDIA;
        int contentType = isEarSpeakerRouted ? android.media.AudioAttributes.CONTENT_TYPE_SPEECH : android.media.AudioAttributes.CONTENT_TYPE_MUSIC;
        android.media.AudioAttributes localAttrs = new android.media.AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(contentType)
                .build();
        mediaSession.setPlaybackToLocal(localAttrs);
    }

    private void vibrateFeedback() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(50);
                }
            }
        } catch (Exception ignored) {}
    }

    private void restoreSystemVolume(int targetVolume) {
        if (audioManager == null || targetVolume < 0) return;
        try {
            isRestoringVolume = true;
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0);
            previousStreamVolume = targetVolume;
            volHandler.postDelayed(() -> {
                isRestoringVolume = false;
                if (audioManager != null) {
                    previousStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                }
            }, 300);
        } catch (Exception ignored) {
            isRestoringVolume = false;
        }
    }

    private void setupVolumeContentObserver() {
        try {
            if (audioManager != null) {
                previousStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
            volumeContentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    super.onChange(selfChange);
                    if (!isVolShortcutsMasterEnabled || !isScreenOffVolEnabled || audioManager == null) return;

                    int currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (previousStreamVolume == -1) {
                        previousStreamVolume = currentVol;
                        return;
                    }

                    // When app is in foreground, hardware keys are handled by dispatchKeyEvent / onKeyDown / onKeyUp.
                    // Do NOT process background gestures here to prevent conflict or volume oscillation!
                    if (isAppInForeground) {
                        previousStreamVolume = currentVol;
                        return;
                    }

                    // Ignore self-triggered restorations
                    if (isRestoringVolume) {
                        previousStreamVolume = currentVol;
                        return;
                    }

                    int delta = currentVol - previousStreamVolume;
                    if (delta == 0) return;

                    long currentTime = System.currentTimeMillis();
                    int direction = delta > 0 ? 1 : -1;
                    previousStreamVolume = currentVol;

                    // If a long press was already triggered during this hold, silently keep volume restored
                    if (isLongPressTriggered) {
                        volHandler.removeCallbacks(bgResetGestureRunnable);
                        volHandler.postDelayed(bgResetGestureRunnable, 400);
                        restoreSystemVolume(gestureBaseVolume);
                        return;
                    }

                    // If direction changed (e.g. was Up, now Down), reset gesture state
                    if (bgTapDirection != 0 && bgTapDirection != direction) {
                        volHandler.removeCallbacks(bgResetGestureRunnable);
                        gestureBaseVolume = -1;
                        bgTapCount = 0;
                        bgTapDirection = 0;
                    }

                    if (gestureBaseVolume == -1) {
                        // Original volume before this gesture sequence started
                        gestureBaseVolume = currentVol - delta;
                        gestureStartTime = currentTime;
                        bgTapCount = 0;
                    }

                    long timeSinceStart = currentTime - gestureStartTime;
                    long timeSinceLastTap = currentTime - lastBgTapTime;
                    bgTapCount++;
                    bgTapDirection = direction;
                    lastBgTapTime = currentTime;

                    volHandler.removeCallbacks(bgResetGestureRunnable);

                    // --- LONG PRESS DETECTION ---
                    // Android hardware key repeat when holding volume button occurs after an initial ~350-450ms delay.
                    // Subsequent repeats arrive every ~80-120ms.
                    // If steps >= 2 and timeSinceStart >= 330ms (held past key repeat threshold), OR steps >= 3:
                    boolean isLongPress = (bgTapCount >= 2 && timeSinceStart >= 330 && timeSinceStart <= 1200)
                            || (bgTapCount >= 3)
                            || (bgTapCount >= 2 && timeSinceLastTap < 130 && timeSinceStart > 200);

                    if (isLongPress) {
                        isLongPressTriggered = true;
                        int base = gestureBaseVolume;
                        restoreSystemVolume(base);
                        int action = (direction > 0) ? volUpLongAction : volDownLongAction;
                        if (action != 0) {
                            executeShortcutAction(action);
                            vibrateFeedback();
                            announceAccessibility("Action Triggered");
                        }
                        volHandler.postDelayed(bgResetGestureRunnable, 500);
                        return;
                    }

                    // --- QUICK DOUBLE TAP DETECTION ---
                    // Double tap: exactly 2 taps within BACKGROUND_DOUBLE_TAP_WINDOW_MS (230ms) of start
                    if (bgTapCount == 2 && timeSinceStart <= BACKGROUND_DOUBLE_TAP_WINDOW_MS) {
                        int base = gestureBaseVolume;
                        bgResetGestureRunnable.run();
                        restoreSystemVolume(base);
                        int action = (direction > 0) ? volUpDoubleAction : volDownDoubleAction;
                        if (action != 0) {
                            executeShortcutAction(action);
                            vibrateFeedback();
                            announceAccessibility("Action Triggered");
                        }
                        return;
                    }

                    // --- SINGLE TAP ---
                    // Wait up to BACKGROUND_LONG_PRESS_WINDOW_MS (550ms) to see if key repeat for Long Press arrives.
                    // If no subsequent event arrives, user merely single-tapped to adjust volume.
                    // The volume was already altered natively by Android and stays at currentVol.
                    volHandler.postDelayed(bgResetGestureRunnable, BACKGROUND_LONG_PRESS_WINDOW_MS);
                }
            };
            getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, volumeContentObserver);
        } catch (Exception ignored) {}
    }

    private void setupAudioEffectsUI() {
        seekLoudness.setProgress(currentLoudnessProgress);
        seekLoudness.setContentDescription("Volume Booster at " + (currentLoudnessProgress / 20) + " percent");
        seekLoudness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentLoudnessProgress = progress;
                    bkPrefs.edit().putInt("loudness_gain", progress).apply();
                    applyLoudnessEnhancer();
                    seekBar.setContentDescription("Volume Booster at " + (progress / 20) + " percent");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnEqToggle.setOnClickListener(v -> {
            isEqEnabled = !isEqEnabled;
            setupAudioEffects();
            btnEqToggle.setText(isEqEnabled ? "Equalizer: ON" : "Equalizer: OFF");
            announceAccessibility(isEqEnabled ? "Equalizer turned ON" : "Equalizer turned OFF");

            if (isEqEnabled) {
                buildEqUI();
            } else {
                eqBandsContainer.removeAllViews();
            }
        });

        btnVirtualizerToggle.setOnClickListener(v -> {
            isVirtualizerEnabled = !isVirtualizerEnabled;
            bkPrefs.edit().putBoolean("virtualizer_enabled", isVirtualizerEnabled).apply();
            setupAudioEffects();
            btnVirtualizerToggle.setText("3D Virtualizer: " + (isVirtualizerEnabled ? "ON" : "OFF"));
            seekVirtualizer.setVisibility(isVirtualizerEnabled ? View.VISIBLE : View.GONE);
            announceAccessibility(isVirtualizerEnabled ? "3D Virtualizer ON" : "3D Virtualizer OFF");
        });

        seekVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentVirtualizerStrength = progress;
                    bkPrefs.edit().putInt("virtualizer_strength", currentVirtualizerStrength).apply();
                    setupAudioEffects();
                    int pct = (progress * 100) / 1000;
                    seekBar.setContentDescription("3D Effect at " + pct + " percent");
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupMediaPlayer() {
        androidx.media3.common.AudioAttributes audioAttributes = new androidx.media3.common.AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(OnlineStreamManager.DEFAULT_USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(20000)
                .setReadTimeoutMs(20000)
                .setKeepPostFor302Redirects(true);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpDataSourceFactory);

        DefaultExtractorsFactory extractorsFactory = new DefaultExtractorsFactory()
                .setConstantBitrateSeekingEnabled(true);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory);

        mediaPlayer = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(mediaSourceFactory)
                .setAudioAttributes(audioAttributes, true)
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_LOCAL)
                .build();

        mediaPlayer.setSkipSilenceEnabled(isSkipSilenceEnabled);
        if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(this)) {
            updateMediaPlayerRoutingForSeparation(true);
        } else {
            updateMediaPlayerOutputDevice();
        }

        if (audioManager != null) {
            try {
                audioManager.registerAudioDeviceCallback(new AudioDeviceCallback() {
                    @Override
                    public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                        updateMediaPlayerOutputDevice();
                    }
                    @Override
                    public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                        updateMediaPlayerOutputDevice();
                    }
                }, handler);
            } catch (Exception ignored) {}
        }

        mediaPlayer.addListener(new Player.Listener() {
            @Override
            public void onAudioSessionIdChanged(int audioSessionId) {
                setupAudioEffects();
            }

            @Override
            public void onMediaMetadataChanged(@NonNull androidx.media3.common.MediaMetadata mediaMetadata) {
                if (currentPlayingFile != null && (currentPlayingFile.startsWith("http://") || currentPlayingFile.startsWith("https://"))) {
                    CharSequence title = mediaMetadata.title;
                    CharSequence artist = mediaMetadata.artist;
                    CharSequence station = mediaMetadata.station;
                    StringBuilder sb = new StringBuilder("📻 ");
                    if (title != null && title.length() > 0) {
                        sb.append(title);
                        if (artist != null && artist.length() > 0) {
                            sb.append(" - ").append(artist);
                        }
                    } else if (station != null && station.length() > 0) {
                        sb.append(station);
                    }
                    if (sb.length() > 3) {
                        tvNowPlaying.setText(sb.toString());
                    }
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "ExoPlayer playback error: " + error.getMessage(), error);
                String msg = "Cannot play this file";
                if (currentPlayingFile != null && (currentPlayingFile.startsWith("http://") || currentPlayingFile.startsWith("https://"))) {
                    msg = "Cannot connect to stream. Please check URL or network.";
                }
                announceAccessibility(msg);
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                updateMediaPlayerOutputDevice();
                if (mediaItem != null && mediaItem.localConfiguration != null) {
                    if (isSleepEndOfTrack && reason != Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT) {
                        onSleepTimerTriggered();
                        return;
                    }
                    currentPlayingFile = mediaItem.localConfiguration.uri.getPath();
                    if (currentPlayingFile == null) currentPlayingFile = mediaItem.localConfiguration.uri.toString();

                    String fileName = new File(currentPlayingFile).getName();
                    if (mediaItem.mediaMetadata != null && mediaItem.mediaMetadata.title != null) {
                        fileName = mediaItem.mediaMetadata.title.toString();
                    }

                    int trackIdx = currentAudioList.indexOf(currentPlayingFile);
                    if (trackIdx == -1 && mediaPlayer != null) {
                        trackIdx = mediaPlayer.getCurrentMediaItemIndex();
                    }
                    int totalTracks = currentAudioList.size();

                    if (!currentZipName.isEmpty() && totalTracks > 1) {
                        tvNowPlaying.setText("🎵 [" + currentZipName + "] " + fileName);
                    } else if (!currentDriveFolderName.isEmpty() && totalTracks > 1) {
                        tvNowPlaying.setText("🎵 [Drive: " + currentDriveFolderName + "] " + fileName);
                    } else {
                        tvNowPlaying.setText("🎵 " + fileName);
                    }
                    if (fileListAdapter != null) {
                        fileListAdapter.notifyDataSetChanged();
                    }
                    if (libraryAdapter != null) {
                        libraryAdapter.notifyDataSetChanged();
                    }

                    if (!currentPlayingFile.startsWith("http")) {
                        if (!currentZipFilePath.isEmpty()) {
                            libPrefs.edit().putString("title_" + currentZipFilePath, "📦 " + currentZipName).apply();
                            addToLibrary("history", currentZipFilePath);
                            addToLibrary("history_folders", currentZipFilePath);
                            addToLibrary("history_tracks", currentPlayingFile);
                        } else {
                            addToLibrary("history", currentPlayingFile);
                            addToLibrary("history_tracks", currentPlayingFile);
                            File parent = new File(currentPlayingFile).getParentFile();
                            if (parent != null) {
                                String parentPath = parent.getAbsolutePath();
                                addToLibrary("history_folders", parentPath);
                                libPrefs.edit().putString("title_" + parentPath, "📁 " + parent.getName()).apply();
                            }
                        }
                    } else if (!currentDriveFolderUrl.isEmpty()) {
                        if (!currentDriveFolderName.isEmpty()) {
                            libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                        }
                        addToLibrary("history", currentDriveFolderUrl);
                        addToLibrary("history_folders", currentDriveFolderUrl);
                        addToLibrary("history_tracks", currentPlayingFile);
                    } else {
                        addToLibrary("history", currentPlayingFile);
                        addToLibrary("history_tracks", currentPlayingFile);
                    }
                    if (currentPlaylistName != null && !currentPlaylistName.isEmpty()) {
                        addToLibrary("history_playlists", currentPlaylistName);
                        libPrefs.edit().putString("title_" + currentPlaylistName, "🎵 " + currentPlaylistName).apply();
                    }

                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                        isTrackFadingOutAtEnd = false;
                        long gap = gapValues[currentGapIndex];
                        if (mediaPlayer != null) {
                            if (gap > 0) {
                                mediaPlayer.pause();
                                mediaPlayer.setVolume(0f);
                                handler.postDelayed(() -> {
                                    if (mediaPlayer != null) {
                                        mediaPlayer.play();
                                        fadeAudio(0.0f, 1.0f, 450, null);
                                    }
                                }, gap);
                            } else {
                                mediaPlayer.setVolume(0f);
                                fadeAudio(0.0f, 1.0f, 450, null);
                            }
                        }
                        if (!currentZipName.isEmpty() && trackIdx >= 0) {
                            announceAccessibility("Playing Track " + (trackIdx + 1) + " of " + totalTracks + ": " + fileName);
                        } else if (!currentDriveFolderName.isEmpty() && trackIdx >= 0) {
                            announceAccessibility("Playing Track " + (trackIdx + 1) + " of " + totalTracks + ": " + fileName);
                        } else {
                            announceAccessibility("Playing " + fileName);
                        }
                    } else {
                        isTrackFadingOutAtEnd = false;
                    }
                    updatePlaybackProgressUI();
                    updateMediaNotification();
                    if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(MainActivity.this)) {
                        AudioRoutingManager.getInstance().applyAudioSeparationRouting();
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    setupAudioEffects();
                    updateMediaPlayerOutputDevice();
                    if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(MainActivity.this)) {
                        AudioRoutingManager.getInstance().ensureAudioSeparationStable();
                    }
                    updatePlaybackProgressUI();
                    updateMediaNotification();
                } else if (playbackState == Player.STATE_ENDED) {
                    memPrefs.edit().putInt(currentPlayingFile, 0).apply();
                    if (isSleepEndOfTrack) {
                        onSleepTimerTriggered();
                        return;
                    }

                    if (playNextQueueItem()) {
                        return;
                    }

                    if (bkPrefs.getBoolean("auto_play_next_folder", false) && playNextSiblingFolder()) {
                        return;
                    }

                    updateMediaSessionState(PlaybackState.STATE_STOPPED);
                    if (btnPlay != null) {
                        btnPlay.setText("Play");
                    }
                    updatePlaybackProgressUI();
                    updateMediaNotification();
                    announceAccessibility("Folder playback completed");
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updateMediaSessionVolumeRouting();
                updateMediaNotification();
                if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(MainActivity.this)) {
                    AudioRoutingManager.getInstance().ensureAudioSeparationStable();
                }
                if (btnPlay != null) {
                    btnPlay.setText(isPlaying ? "Pause" : "Play");
                }
                if (isPlaying) {
                    handler.post(trackFadeMonitorRunnable);
                    handler.removeCallbacks(progressUpdateRunnable);
                    handler.post(progressUpdateRunnable);
                } else {
                    handler.removeCallbacks(trackFadeMonitorRunnable);
                    handler.removeCallbacks(progressUpdateRunnable);
                    updatePlaybackProgressUI();
                }
            }
        });

        loopRunnable = new Runnable() {
            @Override
            public void run() {
                if (abState == 2 && mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (mediaPlayer.getCurrentPosition() >= pointB) mediaPlayer.seekTo(pointA);
                }
                if (abState == 2) handler.postDelayed(this, 100);
            }
        };
    }

    private void updateExoPlayerPlayMode() {
        updateExoPlayerPlayMode(false);
    }

    private void updateExoPlayerPlayMode(boolean announce) {
        if (btnPlayMode != null) {
            if (playMode == 0) {
                btnPlayMode.setText("▶️ Mode: Play All");
                btnPlayMode.setContentDescription("Play mode: Play all tracks and stop at end");
                if (announce) announceAccessibility("Play mode: Play all tracks and stop at end");
            } else if (playMode == 1) {
                btnPlayMode.setText("🔂 Mode: Repeat One");
                btnPlayMode.setContentDescription("Play mode: Repeat current track");
                if (announce) announceAccessibility("Play mode: Repeat current track");
            } else if (playMode == 2) {
                btnPlayMode.setText("🔁 Mode: Repeat All");
                btnPlayMode.setContentDescription("Play mode: Repeat all tracks continuously");
                if (announce) announceAccessibility("Play mode: Repeat all tracks continuously");
            } else if (playMode == 3) {
                btnPlayMode.setText("🔀 Mode: Shuffle");
                btnPlayMode.setContentDescription("Play mode: Shuffle tracks and stop at end");
                if (announce) announceAccessibility("Play mode: Shuffle tracks and stop at end");
            }
        }
        if (mediaPlayer == null) return;
        if (playMode == 0) {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            mediaPlayer.setShuffleModeEnabled(false);
        } else if (playMode == 1) {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
            mediaPlayer.setShuffleModeEnabled(false);
        } else if (playMode == 2) {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            mediaPlayer.setShuffleModeEnabled(false);
        } else if (playMode == 3) {
            mediaPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
            mediaPlayer.setShuffleModeEnabled(true);
        }
    }

    private void updateQueueButtonText() {
        if (btnQueue != null) {
            int count = playQueue.size();
            btnQueue.setText("Queue (" + count + ")");
            btnQueue.setContentDescription("Playback queue, " + count + " items. Tap to view or manage queue.");
        }
    }

    private void addToQueue(int type, String path, String title) {
        playQueue.add(new QueueItem(type, path, title));
        updateQueueButtonText();
        String typeName = (type == QueueItem.TYPE_TRACK) ? "Track" : ((type == QueueItem.TYPE_FOLDER) ? "Folder" : "ZIP archive");
        announceAccessibility(typeName + " " + title + " added to queue. Total items in queue: " + playQueue.size());
        Toast.makeText(this, title + " added to queue (" + playQueue.size() + ")", Toast.LENGTH_SHORT).show();
    }

    private void showQueueDialog() {
        if (playQueue.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Playback Queue")
                    .setMessage("The queue is currently empty.\n\nYou can add tracks, folders, or ZIP archives to the queue from the file options menu.")
                    .setPositiveButton("OK", null)
                    .show();
            announceAccessibility("Playback queue is empty");
            return;
        }

        String[] itemTitles = new String[playQueue.size()];
        for (int i = 0; i < playQueue.size(); i++) {
            QueueItem item = playQueue.get(i);
            String tag = (item.type == QueueItem.TYPE_TRACK) ? "🎵 [Track] " : ((item.type == QueueItem.TYPE_FOLDER) ? "📁 [Folder] " : "📦 [ZIP] ");
            itemTitles[i] = (i + 1) + ". " + tag + item.title;
        }

        new AlertDialog.Builder(this)
                .setTitle("Playback Queue (" + playQueue.size() + " items)")
                .setItems(itemTitles, (dialog, which) -> {
                    if (which >= 0 && which < playQueue.size()) {
                        showQueueItemActionDialog(which);
                    }
                })
                .setNegativeButton("Clear Queue", (dialog, which) -> {
                    playQueue.clear();
                    updateQueueButtonText();
                    announceAccessibility("Queue cleared");
                    Toast.makeText(this, "Queue cleared", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Close", null)
                .show();
        announceAccessibility("Playback queue showing " + playQueue.size() + " items");
    }

    private void showQueueItemActionDialog(int index) {
        if (index < 0 || index >= playQueue.size()) return;
        QueueItem item = playQueue.get(index);
        String[] opts = {"▶ Play Now", "❌ Remove from Queue"};
        new AlertDialog.Builder(this)
                .setTitle(item.title)
                .setItems(opts, (d, w) -> {
                    if (w == 0) {
                        playQueue.remove(index);
                        updateQueueButtonText();
                        playQueueItemDirectly(item);
                    } else if (w == 1) {
                        playQueue.remove(index);
                        updateQueueButtonText();
                        announceAccessibility("Removed " + item.title + " from queue");
                        Toast.makeText(this, "Removed from queue", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void playQueueItemDirectly(QueueItem item) {
        if (item == null) return;
        if (item.type == QueueItem.TYPE_TRACK) {
            announceAccessibility("Playing from queue: " + item.title);
            currentAudioList.clear();
            currentAudioList.add(item.path);
            playAudioFile(item.path);
        } else if (item.type == QueueItem.TYPE_FOLDER) {
            announceAccessibility("Playing folder from queue: " + item.title);
            playFolderTracks(new File(item.path), 0);
        } else if (item.type == QueueItem.TYPE_ZIP) {
            announceAccessibility("Playing ZIP from queue: " + item.title);
            playZipFile(new File(item.path), 0);
        }
    }

    private boolean playNextQueueItem() {
        if (playQueue.isEmpty()) return false;
        QueueItem nextItem = playQueue.remove(0);
        updateQueueButtonText();
        playQueueItemDirectly(nextItem);
        return true;
    }

    private List<File> getAudioFilesInFolder(File folder) {
        List<File> audioFiles = new ArrayList<>();
        if (folder == null || !folder.exists() || !folder.isDirectory()) return audioFiles;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (!f.isDirectory() && !f.isHidden() && ZipMediaManager.isAudioFile(f.getName())) {
                    audioFiles.add(f);
                }
            }
            audioFiles.sort((f1, f2) -> ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(f1.getName(), f2.getName()));
        }
        return audioFiles;
    }

    private void playFolderTracks(File folder, int startIndex) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            announceAccessibility("Folder not found");
            return;
        }
        List<File> audioFiles = getAudioFilesInFolder(folder);
        if (audioFiles.isEmpty()) {
            announceAccessibility("No audio tracks found in " + folder.getName());
            return;
        }
        currentAudioList.clear();
        for (File f : audioFiles) {
            currentAudioList.add(f.getAbsolutePath());
        }
        currentDir = folder.getAbsolutePath();
        loadDirectory(currentDir);
        int idx = (startIndex >= 0 && startIndex < audioFiles.size()) ? startIndex : 0;
        executePlayAudioFile(audioFiles.get(idx).getAbsolutePath());
    }

    private boolean playNextSiblingFolder() {
        File currentFolder = null;
        if (currentDir != null && !currentDir.isEmpty() && !"STORAGE_ROOT".equals(currentDir)) {
            currentFolder = new File(currentDir);
        } else if (currentPlayingFile != null && !currentPlayingFile.isEmpty()) {
            currentFolder = new File(currentPlayingFile).getParentFile();
        }
        if (currentFolder == null || !currentFolder.exists() || !currentFolder.isDirectory()) {
            return false;
        }
        File parentFolder = currentFolder.getParentFile();
        if (parentFolder == null || !parentFolder.exists() || !parentFolder.isDirectory()) {
            return false;
        }
        File[] siblings = parentFolder.listFiles();
        if (siblings == null || siblings.length == 0) return false;

        List<File> subfolders = new ArrayList<>();
        for (File f : siblings) {
            if (f.isDirectory() && !f.isHidden()) {
                subfolders.add(f);
            }
        }
        subfolders.sort((f1, f2) -> ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(f1.getName(), f2.getName()));

        int currentIndex = -1;
        for (int i = 0; i < subfolders.size(); i++) {
            if (subfolders.get(i).getAbsolutePath().equals(currentFolder.getAbsolutePath())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1) {
            for (int i = currentIndex + 1; i < subfolders.size(); i++) {
                File candidate = subfolders.get(i);
                List<File> tracks = getAudioFilesInFolder(candidate);
                if (!tracks.isEmpty()) {
                    announceAccessibility("Auto-playing next folder: " + candidate.getName());
                    Toast.makeText(this, "Next folder: " + candidate.getName(), Toast.LENGTH_SHORT).show();
                    playFolderTracks(candidate, 0);
                    return true;
                }
            }
        }
        return false;
    }

    public ExoPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public int getCurrentEarMode() {
        return currentEarMode;
    }

    private void setupAudioRouting() {
        AudioRoutingManager.getInstance().initRouting(this);
        updateAudioSeparationButtonUI();

        if (btnAudioRoute != null) {
            btnAudioRoute.setOnClickListener(v -> {
                AudioRoutingManager.getInstance().toggleAudioSeparation(this);
            });
        }
    }

    public void updateAudioSeparationButtonUI() {
        if (btnAudioRoute == null) return;
        boolean isEnabled = AudioRoutingManager.getInstance().isAudioSeparationEnabled(this);
        btnAudioRoute.setText(isEnabled ? "Audio Separation: ON" : "Audio Separation: OFF");
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnAudioRoute.setBackgroundColor(isEnabled ? theme.primaryColor : theme.buttonColor);
    }

    private void setupEarSpeakerMode() {
        try {
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }
        } catch (Exception e) {
            Log.e(TAG, "Proximity sensor setup error", e);
        }

        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                proximityWakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, getPackageName() + ":ProximityWakeLock");
            }
        } catch (Exception e) {
            Log.e(TAG, "Proximity wake lock init error", e);
        }

        proximitySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_PROXIMITY && proximitySensor != null) {
                    float distance = event.values[0];
                    float maxRange = proximitySensor.getMaximumRange();
                    boolean near = (distance < maxRange) && (distance < 5.0f);
                    onProximityStateChanged(near);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        currentEarMode = bkPrefs.getInt("ear_speaker_mode", EAR_MODE_OFF);
        updateEarSpeakerButtonUI();

        if (btnEarSpeakerMode != null) {
            btnEarSpeakerMode.setOnClickListener(v -> toggleEarSpeakerMode());
        }

        applyCurrentEarMode(false);
    }

    private void toggleEarSpeakerMode() {
        int nextMode;
        if (currentEarMode == EAR_MODE_OFF) {
            nextMode = EAR_MODE_ON;
        } else if (currentEarMode == EAR_MODE_ON) {
            if (proximitySensor != null) {
                nextMode = EAR_MODE_AUTO;
            } else {
                Toast.makeText(this, "Proximity sensor not available on this device", Toast.LENGTH_SHORT).show();
                nextMode = EAR_MODE_OFF;
            }
        } else {
            nextMode = EAR_MODE_OFF;
        }
        setEarSpeakerMode(nextMode, true);
    }

    public void setEarSpeakerMode(int mode, boolean announce) {
        if (mode != EAR_MODE_OFF && AudioRoutingManager.getInstance().isAudioSeparationEnabled(this)) {
            AudioRoutingManager.getInstance().disableAudioSeparation(this, false);
        }
        currentEarMode = mode;
        bkPrefs.edit().putInt("ear_speaker_mode", currentEarMode).apply();
        updateEarSpeakerButtonUI();
        applyCurrentEarMode(announce);
    }

    private void updateEarSpeakerButtonUI() {
        if (btnEarSpeakerMode == null) return;
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        if (currentEarMode == EAR_MODE_ON) {
            btnEarSpeakerMode.setText("Ear Speaker: ON");
            btnEarSpeakerMode.setBackgroundColor(theme.primaryColor);
        } else if (currentEarMode == EAR_MODE_AUTO) {
            btnEarSpeakerMode.setText("Ear Speaker: AUTO");
            btnEarSpeakerMode.setBackgroundColor(theme.secondaryColor);
        } else {
            btnEarSpeakerMode.setText("Ear Speaker: OFF");
            btnEarSpeakerMode.setBackgroundColor(theme.buttonColor);
        }
    }

    private void applyCurrentEarMode(boolean announce) {
        if (currentEarMode == EAR_MODE_ON) {
            registerProximitySensor();
            applyAudioOutputRouting(true);
            if (announce) {
                Toast.makeText(this, "Ear Speaker Mode: Always ON", Toast.LENGTH_SHORT).show();
                announceAccessibility("Ear speaker mode enabled");
            }
        } else if (currentEarMode == EAR_MODE_AUTO) {
            registerProximitySensor();
            if (isNearEar) {
                applyAudioOutputRouting(true);
                acquireProximityWakeLock();
            } else {
                applyAudioOutputRouting(false);
                releaseProximityWakeLock();
            }
            if (announce) {
                Toast.makeText(this, "Auto Ear Speaker: Near ear switches to ear speaker", Toast.LENGTH_SHORT).show();
                announceAccessibility("Auto ear speaker mode enabled");
            }
        } else { // EAR_MODE_OFF
            unregisterProximitySensor();
            releaseProximityWakeLock();
            applyAudioOutputRouting(false);
            if (announce) {
                Toast.makeText(this, "Ear Speaker: OFF (Loudspeaker)", Toast.LENGTH_SHORT).show();
                announceAccessibility("Ear speaker mode disabled");
            }
        }
    }

    private void onProximityStateChanged(boolean near) {
        if (isNearEar == near) return;
        isNearEar = near;

        if (currentEarMode == EAR_MODE_AUTO) {
            if (near) {
                applyAudioOutputRouting(true);
                acquireProximityWakeLock();
            } else {
                applyAudioOutputRouting(false);
                releaseProximityWakeLock();
            }
        } else if (currentEarMode == EAR_MODE_ON) {
            if (near) {
                acquireProximityWakeLock();
            } else {
                releaseProximityWakeLock();
            }
        }
    }

    private void applyAudioOutputRouting(boolean routeToEarSpeaker) {
        if (audioManager == null) return;
        try {
            if (routeToEarSpeaker) {
                if (!isEarSpeakerRouted) {
                    isEarSpeakerRouted = true;
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        List<AudioDeviceInfo> devices = audioManager.getAvailableCommunicationDevices();
                        AudioDeviceInfo earpieceDevice = null;
                        for (AudioDeviceInfo device : devices) {
                            if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE) {
                                earpieceDevice = device;
                                break;
                            }
                        }
                        if (earpieceDevice != null) {
                            audioManager.setCommunicationDevice(earpieceDevice);
                        }
                    } else {
                        audioManager.setSpeakerphoneOn(false);
                    }

                    if (mediaPlayer != null) {
                        androidx.media3.common.AudioAttributes voiceAttrs = new androidx.media3.common.AudioAttributes.Builder()
                                .setUsage(C.USAGE_VOICE_COMMUNICATION)
                                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                                .build();
                        mediaPlayer.setAudioAttributes(voiceAttrs, false);
                    }
                    updateMediaSessionVolumeRouting();
                }
            } else {
                if (isEarSpeakerRouted) {
                    isEarSpeakerRouted = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        audioManager.clearCommunicationDevice();
                    } else {
                        audioManager.setSpeakerphoneOn(false);
                    }
                    audioManager.setMode(AudioManager.MODE_NORMAL);

                    if (mediaPlayer != null) {
                        androidx.media3.common.AudioAttributes mediaAttrs = new androidx.media3.common.AudioAttributes.Builder()
                                .setUsage(C.USAGE_MEDIA)
                                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                                .build();
                        mediaPlayer.setAudioAttributes(mediaAttrs, true);
                    }
                    updateMediaPlayerOutputDevice();
                    updateMediaSessionVolumeRouting();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "applyAudioOutputRouting error", e);
        }
    }

    private void registerProximitySensor() {
        if (sensorManager != null && proximitySensor != null && !isProximitySensorRegistered && isAppInForeground) {
            sensorManager.registerListener(proximitySensorListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            isProximitySensorRegistered = true;
        }
    }

    private void unregisterProximitySensor() {
        if (sensorManager != null && isProximitySensorRegistered) {
            try {
                sensorManager.unregisterListener(proximitySensorListener);
            } catch (Exception ignored) {}
            isProximitySensorRegistered = false;
            isNearEar = false;
        }
    }

    private void acquireProximityWakeLock() {
        try {
            if (proximityWakeLock != null && !proximityWakeLock.isHeld()) {
                proximityWakeLock.acquire(10 * 60 * 1000L /* 10 minutes timeout */);
            }
        } catch (Exception ignored) {}
    }

    private void releaseProximityWakeLock() {
        try {
            if (proximityWakeLock != null && proximityWakeLock.isHeld()) {
                proximityWakeLock.release();
            }
        } catch (Exception ignored) {}
    }

    public void updateMediaPlayerOutputDevice() {
        if (mediaPlayer == null || audioManager == null) return;
        if (isEarSpeakerRouted) return;
        try {
            AudioDeviceInfo preferredDevice = null;
            AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            if (devices != null) {
                // 1. Look for Bluetooth devices first
                for (AudioDeviceInfo dev : devices) {
                    int type = dev.getType();
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                        type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        type == AudioDeviceInfo.TYPE_BLE_SPEAKER) {
                        preferredDevice = dev;
                        break;
                    }
                }
                // 2. If no Bluetooth device, look for wired or USB headsets
                if (preferredDevice == null) {
                    for (AudioDeviceInfo dev : devices) {
                        int type = dev.getType();
                        if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                            type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                            preferredDevice = dev;
                            break;
                        }
                    }
                }
            }
            mediaPlayer.setPreferredAudioDevice(preferredDevice);
        } catch (Exception e) {
            Log.e(TAG, "updateMediaPlayerOutputDevice error", e);
        }
    }

    public void updateMediaPlayerRoutingForSeparation(boolean isSeparationOn) {
        if (mediaPlayer == null) return;
        try {
            androidx.media3.common.AudioAttributes mediaAttrs = new androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build();
            // When separation is ON: handleAudioFocus = false.
            // This prevents ExoPlayer from ducking and causing OEM audio routing to divert the ducked stream to the communication device (phone speaker) when TalkBack speaks!
            mediaPlayer.setAudioAttributes(mediaAttrs, !isSeparationOn);
            updateMediaPlayerOutputDevice();
        } catch (Exception e) {
            Log.e(TAG, "updateMediaPlayerRoutingForSeparation error", e);
        }
    }

    public void ensureMediaPlayerOutputPinned() {
        if (mediaPlayer == null || audioManager == null) return;
        if (isEarSpeakerRouted) return;
        try {
            updateMediaPlayerOutputDevice();
        } catch (Exception ignored) {}
    }

    private void applyLoudnessEnhancer() {
        if (mediaPlayer == null) return;
        int sessionId = mediaPlayer.getAudioSessionId();
        if (sessionId == 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return;

        try {
            if (currentLoudnessProgress > 0) {
                if (loudnessEnhancer == null || currentAudioSessionId != sessionId) {
                    if (loudnessEnhancer != null) {
                        try { loudnessEnhancer.release(); } catch (Exception ignored) {}
                    }
                    loudnessEnhancer = new LoudnessEnhancer(sessionId);
                }
                loudnessEnhancer.setTargetGain(currentLoudnessProgress);
                loudnessEnhancer.setEnabled(true);
            } else {
                if (loudnessEnhancer != null) {
                    try {
                        loudnessEnhancer.setEnabled(false);
                        loudnessEnhancer.release();
                    } catch (Exception ignored) {}
                    loudnessEnhancer = null;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "LoudnessEnhancer Error", e);
            if (loudnessEnhancer != null) {
                try { loudnessEnhancer.release(); } catch (Exception ignored) {}
                loudnessEnhancer = null;
            }
        }
    }

    private void setupAudioEffects() {
        if (mediaPlayer == null) return;

        int sessionId = mediaPlayer.getAudioSessionId();
        if (sessionId == 0 || sessionId == C.AUDIO_SESSION_ID_UNSET) return;

        boolean sessionChanged = (currentAudioSessionId != sessionId);
        currentAudioSessionId = sessionId;

        applyLoudnessEnhancer();

        if (isEqEnabled) {
            try {
                if (equalizer == null || sessionChanged) {
                    if (equalizer != null) { equalizer.release(); }
                    equalizer = new Equalizer(0, sessionId);
                }
                equalizer.setEnabled(true);

                if (savedBandLevels != null && savedBandLevels.length == equalizer.getNumberOfBands()) {
                    for (short i = 0; i < savedBandLevels.length; i++) {
                        try { equalizer.setBandLevel(i, savedBandLevels[i]); } catch (Exception ignored) {}
                    }
                }

                runOnUiThread(this::buildEqUI);
            } catch (Exception e) {
                Log.e(TAG, "Equalizer Error", e);
                equalizer = null;
            }
        } else {
            if (equalizer != null) {
                try {
                    equalizer.setEnabled(false);
                    equalizer.release();
                } catch (Exception ignored) {}
                equalizer = null;
            }
        }

        boolean canEnableVirtualizer = isVirtualizerEnabled && currentVirtualizerStrength > 0;
        if (canEnableVirtualizer) {
            try {
                if (virtualizer == null || sessionChanged) {
                    if (virtualizer != null) { virtualizer.release(); }
                    virtualizer = new Virtualizer(0, sessionId);
                }
                virtualizer.setEnabled(true);
                if (virtualizer.getStrengthSupported()) {
                    virtualizer.setStrength((short) currentVirtualizerStrength);
                }
            } catch (Exception e) {
                Log.e(TAG, "Virtualizer Error", e);
                virtualizer = null;
            }
        } else {
            if (virtualizer != null) {
                try {
                    virtualizer.setEnabled(false);
                    virtualizer.release();
                } catch (Exception ignored) {}
                virtualizer = null;
            }
        }
    }

    private void buildEqUI() {
        eqBandsContainer.removeAllViews();
        if (equalizer == null) return;

        try {
            short numBands = equalizer.getNumberOfBands();
            final short minEQLevel = equalizer.getBandLevelRange()[0];
            final short maxEQLevel = equalizer.getBandLevelRange()[1];

            if (savedBandLevels == null || savedBandLevels.length != numBands) {
                savedBandLevels = new short[numBands];
                for (short i = 0; i < numBands; i++) {
                    savedBandLevels[i] = equalizer.getBandLevel(i);
                }
            }

            for (short i = 0; i < numBands; i++) {
                final short bandIndex = i;
                int freq = equalizer.getCenterFreq(bandIndex) / 1000;
                TextView tv = new TextView(this);
                tv.setText("Band " + (i+1) + " (" + freq + "Hz)");
                tv.setTextColor(Color.parseColor("#CCCCCC"));
                tv.setTextSize(16);
                tv.setPadding(0, 15, 0, 5);
                eqBandsContainer.addView(tv);

                SeekBar bar = new SeekBar(this);
                bar.setMax(maxEQLevel - minEQLevel);
                bar.setProgress(savedBandLevels[bandIndex] - minEQLevel);
                int currentPercentage = (bar.getProgress() * 100) / bar.getMax();
                bar.setContentDescription("Equalizer Band " + (i+1) + " at " + currentPercentage + " percent");

                bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser) {
                            short newLevel = (short) (progress + minEQLevel);
                            savedBandLevels[bandIndex] = newLevel;
                            if (equalizer != null) {
                                try { equalizer.setBandLevel(bandIndex, newLevel); } catch (Exception ignored) {}
                            }
                            int pct = (progress * 100) / seekBar.getMax();
                            seekBar.setContentDescription("Equalizer Band " + (bandIndex+1) + " at " + pct + " percent");
                        }
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
                eqBandsContainer.addView(bar);
            }
        } catch (Exception e) {
            Log.e(TAG, "Eq UI Error", e);
        }
    }

    private void updateLibraryTabColors() {
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        int active = theme.primaryColor;
        int inactive = theme.buttonColor;
        btnHist.setBackgroundColor(libraryMode == 0 ? active : inactive);
        btnFav.setBackgroundColor(libraryMode == 1 ? active : inactive);
        btnPlaylists.setBackgroundColor((libraryMode == 2 || libraryMode == 3) ? active : inactive);
        btnBookShelf.setBackgroundColor((libraryMode == 4 || libraryMode == 5) ? active : inactive);
    }

    private void updateHistoryFilterUI() {
        if (btnHistFilterTracks == null || btnHistFilterFolders == null || btnHistFilterPlaylists == null) return;
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        int active = theme.primaryColor;
        int inactive = theme.buttonColor;

        btnHistFilterTracks.setBackgroundColor(historyFilterMode == 0 ? active : inactive);
        btnHistFilterFolders.setBackgroundColor(historyFilterMode == 1 ? active : inactive);
        btnHistFilterPlaylists.setBackgroundColor(historyFilterMode == 2 ? active : inactive);

        btnHistFilterTracks.setContentDescription("Filter: Tracks" + (historyFilterMode == 0 ? ", selected" : ""));
        btnHistFilterFolders.setContentDescription("Filter: Folders" + (historyFilterMode == 1 ? ", selected" : ""));
        btnHistFilterPlaylists.setContentDescription("Filter: Playlists" + (historyFilterMode == 2 ? ", selected" : ""));
    }

    private void loadHistoryData() {
        libraryMode = 0;
        updateHistoryFilterUI();
        String key;
        if (historyFilterMode == 0) {
            key = "history_tracks";
            String existingTracks = libPrefs.getString("history_tracks", "");
            if (existingTracks.isEmpty()) {
                String oldHist = libPrefs.getString("history", "");
                if (!oldHist.isEmpty()) {
                    libPrefs.edit().putString("history_tracks", oldHist).apply();
                }
            }
        } else if (historyFilterMode == 1) {
            key = "history_folders";
            String existingFolders = libPrefs.getString("history_folders", "");
            if (existingFolders.isEmpty()) {
                populateHistoryFoldersFromTracks();
            }
        } else {
            key = "history_playlists";
        }
        loadLibraryData(key);
    }

    private void populateHistoryFoldersFromTracks() {
        String hist = libPrefs.getString("history", "");
        if (hist.isEmpty()) hist = libPrefs.getString("history_tracks", "");
        if (hist.isEmpty()) return;
        String[] items = hist.split("\\|");
        Set<String> seen = new LinkedHashSet<>();
        for (String p : items) {
            if (p == null || p.isEmpty()) continue;
            if (p.startsWith("http")) {
                if (GoogleDriveManager.isGoogleDriveUrl(p)) seen.add(p);
            } else if (ZipMediaManager.isZipFile(p)) {
                seen.add(p);
            } else {
                File parent = new File(p).getParentFile();
                if (parent != null && parent.exists() && parent.isDirectory()) {
                    seen.add(parent.getAbsolutePath());
                }
            }
        }
        if (!seen.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String f : seen) {
                if (sb.length() > 0) sb.append("|");
                sb.append(f);
                if (!libPrefs.contains("title_" + f)) {
                    libPrefs.edit().putString("title_" + f, "📁 " + new File(f).getName()).apply();
                }
            }
            libPrefs.edit().putString("history_folders", sb.toString()).apply();
        }
    }

    private void updateLibraryHeader() {
        updateLibraryTabColors();
        int count = libraryDisplayPaths.size();
        if (libraryMode == 2 || libraryMode == 4) {
            if (count > 0 && (libraryDisplayPaths.get(0).equals("CREATE_NEW_SHELF") || libraryDisplayPaths.get(0).equals("CREATE_NEW_PLAYLIST"))) {
                count--;
            }
        }
        if (count < 0) count = 0;

        if (layoutHistoryFilter != null) {
            layoutHistoryFilter.setVisibility(libraryMode == 0 ? View.VISIBLE : View.GONE);
        }

        String title;
        boolean showBack = false;
        boolean showNew = false;
        String emptyText = "No items found";

        if (libraryMode == 0) {
            updateHistoryFilterUI();
            if (historyFilterMode == 0) {
                title = "🕒 History: Tracks (" + count + ")";
                emptyText = "No played tracks yet.\nPlayed audio tracks will appear here.";
            } else if (historyFilterMode == 1) {
                title = "🕒 History: Folders (" + count + ")";
                emptyText = "No played folders yet.\nFolders you play music from will appear here.";
            } else {
                title = "🕒 History: Playlists (" + count + ")";
                emptyText = "No played playlists yet.\nPlaylists you play will appear here.";
            }
        } else if (libraryMode == 1) {
            title = "⭐ Favorites (" + count + ")";
            emptyText = "No favorites added yet.\nLong press any file or folder to add to Favorites.";
        } else if (libraryMode == 2) {
            title = "🎵 Playlists (" + count + ")";
            showNew = true;
            emptyText = "No playlists created yet.\nTap '+ New' to create a playlist.";
        } else if (libraryMode == 3) {
            title = "🎵 " + currentPlaylistName + " (" + count + ")";
            showBack = true;
            emptyText = "This playlist is empty.\nLong press any audio file to add to this playlist.";
        } else if (libraryMode == 4) {
            title = "📚 Bookshelves (" + count + ")";
            showNew = true;
            emptyText = "No bookshelves created yet.\nTap '+ New' to create a bookshelf.";
        } else {
            title = "📚 " + currentShelfName + " (" + count + ")";
            showBack = true;
            emptyText = "This bookshelf is empty.\nLong press any folder or audio file to add to this shelf.";
        }

        if (tvLibTitle != null) {
            tvLibTitle.setText(title);
        }
        if (btnLibBack != null) {
            btnLibBack.setVisibility(showBack ? View.VISIBLE : View.GONE);
        }
        if (btnLibNew != null) {
            btnLibNew.setVisibility(showNew ? View.VISIBLE : View.GONE);
        }
        if (tvLibEmpty != null) {
            tvLibEmpty.setText(emptyText);
        }
    }

    private void promptCreateNewShelf() {
        EditText input = new EditText(this);
        input.setHint("Shelf Name (e.g. Audiobooks)");
        new AlertDialog.Builder(this)
                .setTitle("New Bookshelf")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.contains("|")) {
                        String allStr = libPrefs.getString("all_bookshelves", "");
                        libPrefs.edit().putString("all_bookshelves", allStr.isEmpty() ? newName : allStr + "|" + newName).apply();
                        loadBookShelfList();
                        announceAccessibility("Bookshelf created");
                        Toast.makeText(this, "Bookshelf '" + newName + "' created", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptCreateNewPlaylist() {
        EditText input = new EditText(this);
        input.setHint("Playlist Name");
        new AlertDialog.Builder(this)
                .setTitle("New Playlist")
                .setView(input)
                .setPositiveButton("Create", (d, w) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.contains("|")) {
                        String allStr = libPrefs.getString("all_playlists", "");
                        libPrefs.edit().putString("all_playlists", allStr.isEmpty() ? newName : allStr + "|" + newName).apply();
                        loadPlaylistsList();
                        announceAccessibility("Playlist created");
                        Toast.makeText(this, "Playlist '" + newName + "' created", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupLibraryFeatures() {
        btnHist.setOnClickListener(v -> {
            libraryMode = 0;
            loadHistoryData();
            announceAccessibility("Showing History");
        });

        btnFav.setOnClickListener(v -> {
            libraryMode = 1;
            loadLibraryData("favorites");
            announceAccessibility("Showing Favorites");
        });

        btnPlaylists.setOnClickListener(v -> {
            libraryMode = 2;
            loadPlaylistsList();
            announceAccessibility("Showing Playlists");
        });

        btnBookShelf.setOnClickListener(v -> {
            libraryMode = 4;
            loadBookShelfList();
            announceAccessibility("Showing Book Shelves");
        });

        if (btnLibBack != null) {
            btnLibBack.setOnClickListener(v -> {
                if (libraryMode == 3) {
                    libraryMode = 2;
                    loadPlaylistsList();
                    announceAccessibility("Back to playlists");
                } else if (libraryMode == 5) {
                    libraryMode = 4;
                    loadBookShelfList();
                    announceAccessibility("Back to book shelves");
                }
            });
        }

        if (btnLibNew != null) {
            btnLibNew.setOnClickListener(v -> {
                if (libraryMode == 2) {
                    promptCreateNewPlaylist();
                } else if (libraryMode == 4) {
                    promptCreateNewShelf();
                }
            });
        }

        btnClear.setOnClickListener(v -> {
            final String targetName;
            if (libraryMode == 0) {
                if (historyFilterMode == 0) targetName = "track history";
                else if (historyFilterMode == 1) targetName = "folder history";
                else targetName = "playlist history";
            }
            else if (libraryMode == 1) targetName = "favorites";
            else if (libraryMode == 2) targetName = "all playlists";
            else if (libraryMode == 3) targetName = "playlist " + currentPlaylistName;
            else if (libraryMode == 4) targetName = "all bookshelves";
            else targetName = "bookshelf " + currentShelfName;

            new AlertDialog.Builder(this)
                    .setTitle("Clear All")
                    .setMessage("Are you sure you want to clear " + targetName + "?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        if (libraryMode == 0) {
                            if (historyFilterMode == 0) {
                                libPrefs.edit().putString("history_tracks", "").putString("history", "").apply();
                            } else if (historyFilterMode == 1) {
                                libPrefs.edit().putString("history_folders", "").apply();
                            } else {
                                libPrefs.edit().putString("history_playlists", "").apply();
                            }
                            loadHistoryData();
                        } else if (libraryMode == 1) {
                            libPrefs.edit().putString("favorites", "").apply();
                            loadLibraryData("favorites");
                        } else if (libraryMode == 2) {
                            String all = libPrefs.getString("all_playlists", "");
                            for (String p : all.split("\\|")) {
                                libPrefs.edit().remove("playlist_" + p).apply();
                            }
                            libPrefs.edit().putString("all_playlists", "").apply();
                            loadPlaylistsList();
                        } else if (libraryMode == 3) {
                            libPrefs.edit().putString("playlist_" + currentPlaylistName, "").apply();
                            loadLibraryData("playlist_" + currentPlaylistName);
                        } else if (libraryMode == 4) {
                            String all = libPrefs.getString("all_bookshelves", "");
                            for (String s : all.split("\\|")) {
                                libPrefs.edit().remove("bookshelf_" + s).apply();
                            }
                            libPrefs.edit().putString("all_bookshelves", "").apply();
                            loadBookShelfList();
                        } else if (libraryMode == 5) {
                            libPrefs.edit().putString("bookshelf_" + currentShelfName, "").apply();
                            loadLibraryData("bookshelf_" + currentShelfName);
                        }
                        announceAccessibility("Cleared " + targetName);
                        Toast.makeText(this, "Cleared " + targetName, Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        listViewLibrary.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= libraryDisplayPaths.size()) return;
            if (libraryMode == 2) {
                String path = libraryDisplayPaths.get(position);
                if (path.equals("CREATE_NEW_PLAYLIST")) {
                    promptCreateNewPlaylist();
                    return;
                }
                currentPlaylistName = libraryDisplayNames.get(position);
                libraryMode = 3;
                loadLibraryData("playlist_" + currentPlaylistName);
                announceAccessibility("Opened playlist " + currentPlaylistName);
            } else if (libraryMode == 4) {
                String path = libraryDisplayPaths.get(position);
                if (path.equals("CREATE_NEW_SHELF")) {
                    promptCreateNewShelf();
                    return;
                }
                currentShelfName = libraryDisplayNames.get(position);
                libraryMode = 5;
                loadLibraryData("bookshelf_" + currentShelfName);
                announceAccessibility("Opened bookshelf " + currentShelfName);
            } else if (libraryMode == 0 && historyFilterMode == 2) {
                String path = libraryDisplayPaths.get(position);
                currentPlaylistName = path;
                libraryMode = 3;
                loadLibraryData("playlist_" + currentPlaylistName);
                announceAccessibility("Opened playlist " + currentPlaylistName);
            } else {
                playItemFromLibrary(position);
            }
        });

        listViewLibrary.setOnItemLongClickListener((parent, view, position, id) -> showLibraryItemOptions(position));
        listViewFiles.setOnItemLongClickListener((parent, view, position, id) -> showFileOptionsForPosition(position));

        // Default to Favorites on initialization
        libraryMode = 1;
        loadLibraryData("favorites");
    }

    private void playItemFromLibrary(int position) {
        if (position < 0 || position >= libraryDisplayPaths.size()) return;
        String path = libraryDisplayPaths.get(position);
        if (path.startsWith("http://") || path.startsWith("https://")) {
            switchTab(1);
            if (GoogleDriveManager.isGoogleDriveUrl(path)) {
                handleGoogleDriveUrl(path);
            } else {
                playNetworkStream(path);
            }
            return;
        }

        if (path.startsWith("content://")) {
            switchTab(1);
            handleIncomingAudioUris(Collections.singletonList(Uri.parse(path)));
            return;
        }

        if (tryPlayZipFromLibrary(path)) {
            return;
        }

        File f = new File(path);
        boolean isZip = ZipMediaManager.isZipFile(f.getName());
        boolean isAudio = ZipMediaManager.isAudioFile(f.getName());
        boolean isDir = (!isZip && !isAudio) && (f.isDirectory() || !isAudio);

        if (isDir && (f.exists() || hasMediaUnder(path))) {
            folderBackStack.clear();
            if (libraryMode == 5) {
                folderBackStack.push("BOOKSHELF:" + currentShelfName);
            } else if (libraryMode == 3) {
                folderBackStack.push("PLAYLIST:" + currentPlaylistName);
            } else if (libraryMode == 1) {
                folderBackStack.push("FAVORITES");
            } else if (libraryMode == 0) {
                folderBackStack.push("HISTORY:FOLDERS");
            }
            switchTab(1);
            inputSearch.setText("");
            loadDirectory(path);
            announceAccessibility("Opened folder " + f.getName());
        } else if (isAudio || f.exists() || getContentUriForFilePath(path) != null) {
            if (libraryMode == 3 && !currentPlaylistName.isEmpty()) {
                addToLibrary("history_playlists", currentPlaylistName);
                libPrefs.edit().putString("title_" + currentPlaylistName, "🎵 " + currentPlaylistName).apply();
            }
            switchTab(1);
            currentAudioList.clear();
            for (String p : libraryDisplayPaths) {
                if (ZipMediaManager.isAudioFile(new File(p).getName())) currentAudioList.add(p);
            }
            if (!currentAudioList.contains(path)) {
                currentAudioList.add(path);
            }
            playAudioFile(path);
        } else {
            announceAccessibility("File not found on device");
            Toast.makeText(this, "Item not found on device", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupLibraryAccessibilityActions(AccessibilityNodeInfoCompat info, int position) {
        if (position < 0 || position >= libraryDisplayPaths.size()) return;
        String p = libraryDisplayPaths.get(position);
        if ("CREATE_NEW_SHELF".equals(p) || "CREATE_NEW_PLAYLIST".equals(p)) return;

        if (libraryMode == 2) { // Playlists list
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Open Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_delete, "Delete Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else if (libraryMode == 4) { // Bookshelves list
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Open Bookshelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_delete, "Delete Bookshelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else if (libraryMode == 0) { // History
            if (historyFilterMode == 1) { // Folders
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_play, "Open Folder"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_add_to_queue, "Add Folder to Queue"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_favorite, "Add Folder to Favorites"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_bookshelf, "Add Folder to Book Shelf"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_remove_from_lib, "Remove from History"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "More options"));
            } else if (historyFilterMode == 2) { // Playlists
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_play, "Open Playlist"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_remove_from_lib, "Remove from History"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "More options"));
            } else { // Tracks
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_play, "Play Now"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_add_to_queue, "Add to Queue"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_favorite, "Add to Favorites"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_bookshelf, "Add to Book Shelf"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_playlist, "Add to Playlist"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_share, "Share"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_remove_from_lib, "Remove from History"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "More options"));
            }
        } else if (libraryMode == 1) { // Favorites
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Play Now"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_bookshelf, "Add to Book Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_playlist, "Add to Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_remove_from_lib, "Remove from Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else if (libraryMode == 5) { // Book Shelf
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Play Now"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_favorite, "Add to Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_playlist, "Add to Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_remove_from_lib, "Remove from Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else { // Playlist
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Play Now"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_favorite, "Add to Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_bookshelf, "Add to Book Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_remove_from_lib, "Remove from Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        }
    }

    private boolean handleLibraryAccessibilityAction(int position, int action) {
        if (position < 0 || position >= libraryDisplayPaths.size()) return false;
        if (action == R.id.action_show_file_options) {
            return showLibraryItemOptions(position);
        }

        final String name = libraryDisplayNames.get(position);
        final String path = libraryDisplayPaths.get(position);

        if (libraryMode == 2) { // Playlists list
            if (action == R.id.action_file_play) {
                currentPlaylistName = name;
                libraryMode = 3;
                loadLibraryData("playlist_" + currentPlaylistName);
                announceAccessibility("Opened playlist " + currentPlaylistName);
                return true;
            } else if (action == R.id.action_file_delete) {
                return showLibraryItemOptions(position);
            }
            return false;
        } else if (libraryMode == 4) { // Bookshelves list
            if (action == R.id.action_file_play) {
                currentShelfName = name;
                libraryMode = 5;
                loadLibraryData("bookshelf_" + currentShelfName);
                announceAccessibility("Opened bookshelf " + currentShelfName);
                return true;
            } else if (action == R.id.action_file_delete) {
                return showLibraryItemOptions(position);
            }
            return false;
        }

        if (action == R.id.action_file_play) {
            if (libraryMode == 0 && historyFilterMode == 2) {
                currentPlaylistName = path;
                libraryMode = 3;
                loadLibraryData("playlist_" + currentPlaylistName);
                announceAccessibility("Opened playlist " + currentPlaylistName);
                return true;
            }
            playItemFromLibrary(position);
            return true;
        } else if (action == R.id.action_file_favorite) {
            libPrefs.edit().putString("title_" + path, name).apply();
            addToLibrary("favorites", path);
            announceAccessibility("Added to favorites");
            Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
            return true;
        } else if (action == R.id.action_file_bookshelf) {
            libPrefs.edit().putString("title_" + path, name).apply();
            showBookShelfSelectionDialog(path);
            return true;
        } else if (action == R.id.action_file_playlist) {
            libPrefs.edit().putString("title_" + path, name).apply();
            showPlaylistSelectionDialog(path);
            return true;
        } else if (action == R.id.action_file_share) {
            File f = new File(path);
            if (f.exists()) {
                if (f.isDirectory()) shareFolderAsZip(f);
                else shareFileDirectly(f);
            } else {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, path);
                startActivity(Intent.createChooser(shareIntent, "Share"));
            }
            return true;
        } else if (action == R.id.action_file_remove_from_lib) {
            final String fKey;
            if (libraryMode == 0) {
                fKey = (historyFilterMode == 0) ? "history_tracks" : (historyFilterMode == 1 ? "history_folders" : "history_playlists");
                removeFromLibrary(fKey, path);
                if (historyFilterMode == 0) {
                    removeFromLibrary("history", path);
                }
                loadHistoryData();
                announceAccessibility("Removed from History");
                Toast.makeText(this, "Removed from History", Toast.LENGTH_SHORT).show();
                return true;
            } else if (libraryMode == 1) fKey = "favorites";
            else if (libraryMode == 3) fKey = "playlist_" + currentPlaylistName;
            else fKey = "bookshelf_" + currentShelfName;

            removeFromLibrary(fKey, path);
            loadLibraryData(fKey);
            announceAccessibility("Removed from list");
            Toast.makeText(this, "Removed from list", Toast.LENGTH_SHORT).show();
            return true;
        } else if (action == R.id.action_add_to_queue) {
            File f = new File(path);
            if (f.isDirectory()) {
                addToQueue(QueueItem.TYPE_FOLDER, path, name);
            } else if (ZipMediaManager.isZipFile(f.getName())) {
                addToQueue(QueueItem.TYPE_ZIP, path, name);
            } else {
                addToQueue(QueueItem.TYPE_TRACK, path, name);
            }
            return true;
        }
        return false;
    }

    private boolean showLibraryItemOptions(int position) {
        if (position < 0 || position >= libraryDisplayPaths.size()) return false;
        final String name = libraryDisplayNames.get(position);
        final String path = libraryDisplayPaths.get(position);

        if (libraryMode == 2) {
            if ("CREATE_NEW_PLAYLIST".equals(path)) return true;
            new AlertDialog.Builder(this)
                    .setTitle("Delete Playlist")
                    .setMessage("Delete playlist '" + name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String allPlaylists = libPrefs.getString("all_playlists", "");
                        StringBuilder sb = new StringBuilder();
                        for (String p : allPlaylists.split("\\|")) {
                            if (!p.equals(name) && !p.isEmpty()) sb.append(p).append("|");
                        }
                        String res = sb.toString();
                        if (res.endsWith("|")) res = res.substring(0, res.length() - 1);
                        libPrefs.edit().putString("all_playlists", res).apply();
                        libPrefs.edit().remove("playlist_" + name).apply();
                        loadPlaylistsList();
                        announceAccessibility("Playlist deleted");
                        Toast.makeText(this, "Playlist '" + name + "' deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else if (libraryMode == 4) {
            if ("CREATE_NEW_SHELF".equals(path)) return true;
            new AlertDialog.Builder(this)
                    .setTitle("Delete Bookshelf")
                    .setMessage("Delete shelf '" + name + "'?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        String allShelves = libPrefs.getString("all_bookshelves", "");
                        StringBuilder sb = new StringBuilder();
                        for (String s : allShelves.split("\\|")) {
                            if (!s.equals(name) && !s.isEmpty()) sb.append(s).append("|");
                        }
                        String res = sb.toString();
                        if (res.endsWith("|")) res = res.substring(0, res.length() - 1);
                        libPrefs.edit().putString("all_bookshelves", res).apply();
                        libPrefs.edit().remove("bookshelf_" + name).apply();
                        loadBookShelfList();
                        announceAccessibility("Bookshelf deleted");
                        Toast.makeText(this, "Bookshelf '" + name + "' deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        } else {
            List<String> optionsList = new ArrayList<>();
            if (libraryMode == 0 && historyFilterMode == 1) { // History Folders
                optionsList.add("📁 Open Folder");
                optionsList.add("⏳ Add Folder to Queue");
                optionsList.add("⭐ Add to Favorites");
                optionsList.add("📚 Add to Book Shelf");
                optionsList.add("🗑️ Remove from History");
            } else if (libraryMode == 0 && historyFilterMode == 2) { // History Playlists
                optionsList.add("🎵 Open Playlist");
                optionsList.add("🗑️ Remove from History");
            } else {
                optionsList.add("▶ Play Now");
                optionsList.add("⏳ Add to Queue");
                if (libraryMode == 0) { // History Tracks
                    optionsList.add("⭐ Add to Favorites");
                    optionsList.add("📚 Add to Book Shelf");
                    optionsList.add("🎵 Add to Playlist");
                    optionsList.add("↗ Share");
                    optionsList.add("🗑️ Remove from History");
                } else if (libraryMode == 1) { // Favorites
                    optionsList.add("📚 Add to Book Shelf");
                    optionsList.add("🎵 Add to Playlist");
                    optionsList.add("↗ Share");
                    optionsList.add("🗑️ Remove from Favorites");
                } else if (libraryMode == 5) { // Book Shelf
                    optionsList.add("⭐ Add to Favorites");
                    optionsList.add("🎵 Add to Playlist");
                    optionsList.add("↗ Share");
                    optionsList.add("🗑️ Remove from Shelf");
                } else { // Playlist
                    optionsList.add("⭐ Add to Favorites");
                    optionsList.add("📚 Add to Book Shelf");
                    optionsList.add("↗ Share");
                    optionsList.add("🗑️ Remove from Playlist");
                }
            }

            final String[] options = optionsList.toArray(new String[0]);
            new AlertDialog.Builder(this)
                    .setTitle(name)
                    .setItems(options, (dialog, which) -> {
                        String chosen = options[which];
                        if (chosen.contains("Play Now") || chosen.contains("Open Folder") || chosen.contains("Open Playlist")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_play);
                        } else if (chosen.contains("Queue")) {
                            handleLibraryAccessibilityAction(position, R.id.action_add_to_queue);
                        } else if (chosen.contains("Favorites")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_favorite);
                        } else if (chosen.contains("Book Shelf")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_bookshelf);
                        } else if (chosen.contains("Playlist")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_playlist);
                        } else if (chosen.contains("Share")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_share);
                        } else if (chosen.contains("Remove")) {
                            handleLibraryAccessibilityAction(position, R.id.action_file_remove_from_lib);
                        }
                    }).show();
            return true;
        }
    }

    private void playExplorerAudioFile(String path) {
        currentAudioList.clear();
        for (String p : currentDisplayPaths) {
            if (ZipMediaManager.isAudioFile(new File(p).getName())) {
                currentAudioList.add(p);
            }
        }
        playAudioFile(path);
    }

    private void setupFileAccessibilityActions(AccessibilityNodeInfoCompat info, int position) {
        if (position < 0 || position >= currentDisplayPaths.size()) return;
        String name = (position < currentDisplayNames.size()) ? currentDisplayNames.get(position) : "";
        if ("Up".equals(name) || name.startsWith("Up (")) return;

        if (isBrowsingDrive) {
            if (position == 0) return;
            String p = currentDisplayPaths.get(position);
            if ("DRIVE_FOLDER_OPTIONS".equals(p)) {
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "Drive Folder Options"));
                return;
            }
            int trackIndex = position - 2;
            if (trackIndex >= 0 && trackIndex < currentDriveTracks.size()) {
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_play, "Play Track"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_favorite, "Add Track to Favorites"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_playlist, "Add Track to Playlist"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_bookshelf, "Add Folder to Book Shelf"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "More options"));
            }
            return;
        }

        if ("STORAGE_ROOT".equals(currentDir)) return;

        final String path = currentDisplayPaths.get(position);
        if (path == null || "STORAGE_ROOT".equals(path)) return;
        if (path.equals(new File(currentDir).getParent())) return;

        List<StorageInfo> allStorages = getAvailableStorageVolumes();
        for (StorageInfo s : allStorages) {
            if (path.equals(s.path) || path.equals(s.path + "/")) {
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_favorite, "Add to Favorites"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_bookshelf, "Add to Book Shelf"));
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_show_file_options, "More options"));
                return;
            }
        }

        File f = new File(path);
        if (f.isDirectory()) {
            String pinnedStr = libPrefs.getString("pinned_folders", "");
            boolean isPinned = false;
            for (String p : pinnedStr.split("\\|")) {
                if (p.equals(path)) { isPinned = true; break; }
            }
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_pin, isPinned ? "Unpin Folder" : "Pin Folder to Top"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add Folder to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_folder_time_details, "Folder Duration Details"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_favorite, "Add to Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_bookshelf, "Add to Book Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_playlist, "Add to Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share Folder (ZIP)"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_rename, "Rename"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_delete, "Delete"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else if (ZipMediaManager.isZipFile(f.getName())) {
            String lastZipFile = memPrefs.getString("folder_last_file_zip_" + path, "");
            if (!lastZipFile.isEmpty() && new File(lastZipFile).exists()) {
                info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        R.id.action_file_resume_zip, "Resume ZIP Playback"));
            }
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Play All Tracks"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_browse_zip, "Browse Tracks inside ZIP"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add ZIP to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_folder_time_details, "ZIP Duration Details"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_favorite, "Add to Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_bookshelf, "Add to Book Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_playlist, "Add to Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_rename, "Rename"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_delete, "Delete"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        } else {
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_play, "Play Now"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_add_to_queue, "Add to Queue"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_favorite, "Add to Favorites"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_bookshelf, "Add to Book Shelf"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_playlist, "Add to Playlist"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_share, "Share"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_rename, "Rename"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_file_delete, "Delete"));
            info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                    R.id.action_show_file_options, "More options"));
        }
    }

    private boolean handleFileAccessibilityAction(int position, int action) {
        if (position < 0 || position >= currentDisplayPaths.size()) return false;
        if (action == R.id.action_show_file_options) {
            return showFileOptionsForPosition(position);
        }

        if (isBrowsingDrive) {
            int trackIndex = position - 2;
            if (trackIndex >= 0 && trackIndex < currentDriveTracks.size()) {
                GoogleDriveManager.DriveTrack dt = currentDriveTracks.get(trackIndex);
                if (action == R.id.action_file_play) {
                    if (mediaPlayer != null && mediaPlayer.getMediaItemCount() == currentDriveTracks.size()) {
                        saveCurrentPosition();
                        mediaPlayer.seekTo(trackIndex, 0);
                        mediaPlayer.play();
                        fadeAudio(0.0f, 1.0f, 400, null);
                        resetAB();
                        updateMediaSessionState(PlaybackState.STATE_PLAYING);
                        String trackTitle = dt.title;
                        tvNowPlaying.setText("🎵 [Drive: " + currentDriveFolderName + "] " + trackTitle);
                        announceAccessibility("Playing Track " + (trackIndex + 1) + " of " + currentDriveTracks.size() + ": " + trackTitle);
                    } else {
                        playDriveTracks(currentDriveFolderName, currentDriveFolderUrl, new ArrayList<>(currentDriveTracks), trackIndex);
                    }
                    return true;
                } else if (action == R.id.action_file_favorite) {
                    addToLibrary("favorites", dt.streamUrl);
                    libPrefs.edit().putString("title_" + dt.streamUrl, "🎵 " + dt.title).apply();
                    announceAccessibility("Track added to favorites");
                    Toast.makeText(this, "Track added to Favorites", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (action == R.id.action_file_playlist) {
                    libPrefs.edit().putString("title_" + dt.streamUrl, "🎵 " + dt.title).apply();
                    showPlaylistSelectionDialog(dt.streamUrl);
                    return true;
                } else if (action == R.id.action_file_bookshelf) {
                    libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                    showBookShelfSelectionDialog(currentDriveFolderUrl);
                    return true;
                }
            }
            return false;
        }

        final String path = currentDisplayPaths.get(position);
        if (path == null || "STORAGE_ROOT".equals(path)) return false;

        List<StorageInfo> allStorages = getAvailableStorageVolumes();
        for (StorageInfo s : allStorages) {
            if (path.equals(s.path) || path.equals(s.path + "/")) {
                if (action == R.id.action_file_favorite) {
                    addToLibrary("favorites", path);
                    announceAccessibility("Added to favorites");
                    Toast.makeText(this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (action == R.id.action_file_bookshelf) {
                    showBookShelfSelectionDialog(path);
                    return true;
                }
            }
        }

        File f = new File(path);
        if (action == R.id.action_file_pin) {
            if (f.isDirectory()) {
                togglePinFolder(path);
                loadDirectory(currentDir);
                return true;
            }
        } else if (action == R.id.action_file_favorite) {
            String prefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isZipFile(f.getName()) ? "📦 " : "🎵 ");
            libPrefs.edit().putString("title_" + path, prefix + f.getName()).apply();
            addToLibrary("favorites", path);
            announceAccessibility(f.isDirectory() ? "Folder added to favorites" : "Added to favorites");
            Toast.makeText(this, f.isDirectory() ? "Folder added to Favorites" : "Added to Favorites", Toast.LENGTH_SHORT).show();
            return true;
        } else if (action == R.id.action_file_bookshelf) {
            String prefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isZipFile(f.getName()) ? "📦 " : "🎵 ");
            libPrefs.edit().putString("title_" + path, prefix + f.getName()).apply();
            showBookShelfSelectionDialog(path);
            return true;
        } else if (action == R.id.action_file_playlist) {
            String prefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isZipFile(f.getName()) ? "📦 " : "🎵 ");
            libPrefs.edit().putString("title_" + path, prefix + f.getName()).apply();
            showPlaylistSelectionDialog(path);
            return true;
        } else if (action == R.id.action_file_share) {
            if (f.isDirectory()) {
                shareFolderAsZip(f);
            } else {
                shareFileDirectly(f);
            }
            return true;
        } else if (action == R.id.action_file_rename) {
            showRenameDialog(f);
            return true;
        } else if (action == R.id.action_file_delete) {
            confirmDeleteFileOrFolder(f);
            return true;
        } else if (action == R.id.action_file_play) {
            if (ZipMediaManager.isZipFile(f.getName())) {
                playZipFile(f, 0);
            } else if (f.isDirectory()) {
                loadDirectory(path);
                announceAccessibility("Opened folder " + f.getName());
            } else {
                playExplorerAudioFile(path);
            }
            return true;
        } else if (action == R.id.action_file_browse_zip) {
            if (ZipMediaManager.isZipFile(f.getName())) {
                browseZipFile(f);
                return true;
            }
        } else if (action == R.id.action_folder_time_details) {
            FolderMetadataManager.FolderMeta meta = FolderMetadataManager.getCachedMeta(this, path);
            if (meta != null && meta.totalDurationMs > 0) {
                String msg = f.getName() + ": " + meta.fileCount + " tracks, total duration " + FolderMetadataManager.formatDurationSpeech(meta.totalDurationMs);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                announceAccessibility(msg);
            } else {
                FolderMetadataManager.getFolderInfoAsync(this, path, m -> {
                    String msg = f.getName() + ": " + m.fileCount + " tracks" + (m.totalDurationMs > 0 ? ", total duration " + FolderMetadataManager.formatDurationSpeech(m.totalDurationMs) : "");
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    announceAccessibility(msg);
                });
            }
            return true;
        } else if (action == R.id.action_file_resume_zip) {
            if (ZipMediaManager.isZipFile(f.getName())) {
                resumeZipPlayback(f);
                return true;
            }
        } else if (action == R.id.action_add_to_queue) {
            if (f.isDirectory()) {
                addToQueue(QueueItem.TYPE_FOLDER, path, f.getName());
            } else if (ZipMediaManager.isZipFile(f.getName())) {
                addToQueue(QueueItem.TYPE_ZIP, path, f.getName());
            } else {
                addToQueue(QueueItem.TYPE_TRACK, path, f.getName());
            }
            return true;
        }

        return false;
    }

    private boolean showFileOptionsForPosition(int position) {
        if (position < 0 || position >= currentDisplayPaths.size()) return false;

        if (isBrowsingDrive) {
            if (position == 0) return false;
            String p = currentDisplayPaths.get(position);
            if ("DRIVE_FOLDER_OPTIONS".equals(p)) {
                showDriveFolderOptionsDialog();
                return true;
            }
            int trackIndex = position - 2;
            if (trackIndex >= 0 && trackIndex < currentDriveTracks.size()) {
                GoogleDriveManager.DriveTrack dt = currentDriveTracks.get(trackIndex);
                String[] options = {
                        "⭐ Add Folder to Favorites",
                        "📚 Add Folder to Book Shelf",
                        "⭐ Add Track to Favorites",
                        "🎵 Add Track to Playlist"
                };
                new AlertDialog.Builder(this)
                        .setTitle("Google Drive: " + dt.title)
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                addToLibrary("favorites", currentDriveFolderUrl);
                                libPrefs.edit().putString("title_" + currentDriveFolderUrl, "📁 " + currentDriveFolderName).apply();
                                announceAccessibility("Folder added to favorites");
                                Toast.makeText(this, "Folder added to Favorites", Toast.LENGTH_SHORT).show();
                            } else if (which == 1) {
                                handleFileAccessibilityAction(position, R.id.action_file_bookshelf);
                            } else if (which == 2) {
                                handleFileAccessibilityAction(position, R.id.action_file_favorite);
                            } else if (which == 3) {
                                handleFileAccessibilityAction(position, R.id.action_file_playlist);
                            }
                        }).show();
            }
            return true;
        }

        if ("STORAGE_ROOT".equals(currentDir)) return false;

        final String path = currentDisplayPaths.get(position);
        if (path == null || "STORAGE_ROOT".equals(path)) return false;
        if (path.equals(new File(currentDir).getParent())) return false;

        List<StorageInfo> allStorages = getAvailableStorageVolumes();
        for (StorageInfo s : allStorages) {
            if (path.equals(s.path) || path.equals(s.path + "/")) {
                String[] rootOpts = {"Add to Favorites", "Add to Book Shelf"};
                new AlertDialog.Builder(this)
                        .setTitle(s.name)
                        .setItems(rootOpts, (d, w) -> {
                            if (w == 0) {
                                handleFileAccessibilityAction(position, R.id.action_file_favorite);
                            } else if (w == 1) {
                                handleFileAccessibilityAction(position, R.id.action_file_bookshelf);
                            }
                        }).show();
                return true;
            }
        }

        File f = new File(path);
        if (f.isDirectory()) {
            String pinnedStr = libPrefs.getString("pinned_folders", "");
            boolean isPinned = false;
            for (String p : pinnedStr.split("\\|")) {
                if (p.equals(path)) { isPinned = true; break; }
            }
            final boolean currentlyPinned = isPinned;

            String[] options = {
                    currentlyPinned ? "Unpin Folder" : "Pin Folder to Top",
                    "View Bookmarks",
                    "Add to Queue",
                    "Add to Favorites",
                    "Add to Playlist",
                    "Add to Book Shelf",
                    "Share Folder (ZIP)",
                    "Rename",
                    "Delete"
            };
            new AlertDialog.Builder(this)
                    .setTitle("Folder: " + f.getName())
                    .setItems(options, (dialog, which) -> {
                        String opt = options[which];
                        if ("View Bookmarks".equals(opt)) {
                            showBookmarksDialogForScope(path, false, f.getName());
                        } else if (opt.contains("Pin Folder")) {
                            handleFileAccessibilityAction(position, R.id.action_file_pin);
                        } else if ("Add to Queue".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_add_to_queue);
                        } else if ("Add to Favorites".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_favorite);
                        } else if ("Add to Playlist".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_playlist);
                        } else if ("Add to Book Shelf".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_bookshelf);
                        } else if ("Share Folder (ZIP)".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_share);
                        } else if ("Rename".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_rename);
                        } else if ("Delete".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_delete);
                        }
                    }).show();
        } else if (ZipMediaManager.isZipFile(f.getName())) {
            String[] options = {
                    "Play All Tracks",
                    "Browse Tracks inside ZIP",
                    "View Bookmarks",
                    "Add to Queue",
                    "Add to Favorites",
                    "Add to Book Shelf",
                    "Add to Playlist",
                    "Share",
                    "Rename",
                    "Delete"
            };
            new AlertDialog.Builder(this)
                    .setTitle("ZIP: " + f.getName())
                    .setItems(options, (dialog, which) -> {
                        String opt = options[which];
                        if ("Play All Tracks".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_play);
                        } else if ("Browse Tracks inside ZIP".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_browse_zip);
                        } else if ("View Bookmarks".equals(opt)) {
                            showBookmarksDialogForScope(f.getAbsolutePath(), true, f.getName());
                        } else if ("Add to Queue".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_add_to_queue);
                        } else if ("Add to Favorites".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_favorite);
                        } else if ("Add to Book Shelf".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_bookshelf);
                        } else if ("Add to Playlist".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_playlist);
                        } else if ("Share".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_share);
                        } else if ("Rename".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_rename);
                        } else if ("Delete".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_delete);
                        }
                    }).show();
        } else {
            String[] options = {
                    "Play Now",
                    "View Bookmarks",
                    "Add to Queue",
                    "Add to Favorites",
                    "Add to Book Shelf",
                    "Add to Playlist",
                    "Share",
                    "Rename",
                    "Delete"
            };
            new AlertDialog.Builder(this)
                    .setTitle("File: " + f.getName())
                    .setItems(options, (dialog, which) -> {
                        String opt = options[which];
                        if ("Play Now".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_play);
                        } else if ("View Bookmarks".equals(opt)) {
                            String fPath = isBrowsingZip ? currentZipFilePath : (f.getParent() != null ? f.getParent() : currentDir);
                            boolean isZ = isBrowsingZip || ZipMediaManager.isZipFile(fPath);
                            String title = isBrowsingZip ? currentZipName : (f.getParentFile() != null ? f.getParentFile().getName() : f.getName());
                            showBookmarksDialogForScope(fPath, isZ, title);
                        } else if ("Add to Queue".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_add_to_queue);
                        } else if ("Add to Favorites".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_favorite);
                        } else if ("Add to Book Shelf".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_bookshelf);
                        } else if ("Add to Playlist".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_playlist);
                        } else if ("Share".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_share);
                        } else if ("Rename".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_rename);
                        } else if ("Delete".equals(opt)) {
                            handleFileAccessibilityAction(position, R.id.action_file_delete);
                        }
                    }).show();
        }
        return true;
    }

    private void shareFolderAsZip(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) {
            Toast.makeText(this, "Folder not found", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Compressing Folder");
        progressDialog.setMessage("Compressing \"" + folder.getName() + "\" into ZIP for sharing...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                File shareDir = new File(getCacheDir(), "shared_zips");
                if (!shareDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    shareDir.mkdirs();
                }
                File destZip = new File(shareDir, folder.getName() + ".zip");
                ZipMediaManager.zipDirectory(folder, destZip);

                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.dismiss();
                        shareFileUri(destZip, "application/zip");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error compressing folder to ZIP", e);
                runOnUiThread(() -> {
                    if (!isFinishing() && !isDestroyed()) {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Compression failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    private void shareFileDirectly(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            return;
        }
        String mime = getMimeTypeForFile(file);
        shareFileUri(file, mime);
    }

    private void shareFileUri(File file, String mimeType) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType != null ? mimeType : "*/*");
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share " + file.getName()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to share file: " + file.getAbsolutePath(), e);
            Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getMimeTypeForFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot != -1) {
            String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (mime != null) return mime;
        }
        if (ZipMediaManager.isAudioFile(name)) {
            return "audio/*";
        }
        if (ZipMediaManager.isZipFile(name)) {
            return "application/zip";
        }
        return "*/*";
    }

    private void confirmDeleteFileOrFolder(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Item does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isDir = file.isDirectory();
        new AlertDialog.Builder(this)
                .setTitle("Delete " + (isDir ? "Folder" : "File"))
                .setMessage("Are you sure you want to delete \"" + file.getName() + "\"?" +
                        (isDir ? "\n\nAll files and subfolders inside will be permanently deleted." : ""))
                .setPositiveButton("Delete", (dialog, which) -> performDelete(file))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performDelete(File file) {
        String path = file.getAbsolutePath();
        boolean isDir = file.isDirectory();

        if (!currentPlayingFile.isEmpty()) {
            if (currentPlayingFile.equals(path) || (isDir && currentPlayingFile.startsWith(path + "/"))) {
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                }
                currentPlayingFile = "";
                tvNowPlaying.setText("Not Playing");
                btnPlay.setText("Play / Pause");
            }
        }

        boolean success = isDir ? ZipMediaManager.deleteRecursive(file) : file.delete();
        if (success) {
            if (isDir) {
                String pinned = libPrefs.getString("pinned_folders", "");
                if (pinned.contains(path)) {
                    libPrefs.edit().putString("pinned_folders", pinned.replace(path + "|", "").replace(path, "")).apply();
                }
            }
            bkPrefs.edit().remove(path).apply();
            memPrefs.edit().remove(path).apply();

            MediaScannerConnection.scanFile(this, new String[]{path}, null, null);

            announceAccessibility((isDir ? "Folder " : "File ") + file.getName() + " deleted");
            Toast.makeText(this, file.getName() + " deleted", Toast.LENGTH_SHORT).show();
            loadDirectory(currentDir);
        } else {
            Toast.makeText(this, "Failed to delete " + file.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showRenameDialog(File file) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "Item does not exist", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isDir = file.isDirectory();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        EditText input = new EditText(this);
        input.setText(file.getName());
        if (!isDir) {
            int dot = file.getName().lastIndexOf('.');
            if (dot > 0) {
                input.setSelection(0, dot);
            } else {
                input.selectAll();
            }
        } else {
            input.selectAll();
        }
        layout.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Rename " + (isDir ? "Folder" : "File"))
                .setView(layout)
                .setPositiveButton("Rename", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    performRename(file, newName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performRename(File file, String newName) {
        if (newName.isEmpty() || newName.equals(file.getName())) {
            return;
        }
        if (newName.contains("/") || newName.contains("\\") || newName.contains(":") ||
                newName.contains("*") || newName.contains("?") || newName.contains("\"") ||
                newName.contains("<") || newName.contains(">") || newName.contains("|")) {
            Toast.makeText(this, "Invalid characters in name", Toast.LENGTH_SHORT).show();
            return;
        }

        File parent = file.getParentFile();
        File target = new File(parent, newName);
        if (target.exists()) {
            Toast.makeText(this, "An item with this name already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        String oldPath = file.getAbsolutePath();
        boolean renamed = file.renameTo(target);
        if (renamed) {
            String newPath = target.getAbsolutePath();

            if (currentPlayingFile.equals(oldPath)) {
                currentPlayingFile = newPath;
                tvNowPlaying.setText("Playing: " + target.getName());
            } else if (file.isDirectory() && currentPlayingFile.startsWith(oldPath + "/")) {
                currentPlayingFile = newPath + currentPlayingFile.substring(oldPath.length());
            }

            String pinned = libPrefs.getString("pinned_folders", "");
            if (pinned.contains(oldPath)) {
                libPrefs.edit().putString("pinned_folders", pinned.replace(oldPath, newPath)).apply();
            }

            String oldBookmarks = bkPrefs.getString(oldPath, null);
            if (oldBookmarks != null) {
                bkPrefs.edit().remove(oldPath).putString(newPath, oldBookmarks).apply();
            }

            int lastPos = memPrefs.getInt(oldPath, -1);
            if (lastPos != -1) {
                memPrefs.edit().remove(oldPath).putInt(newPath, lastPos).apply();
            }

            MediaScannerConnection.scanFile(this, new String[]{oldPath, newPath}, null, null);

            announceAccessibility("Renamed to " + newName);
            Toast.makeText(this, "Renamed to " + newName, Toast.LENGTH_SHORT).show();
            loadDirectory(currentDir);
        } else {
            Toast.makeText(this, "Failed to rename", Toast.LENGTH_SHORT).show();
        }
    }

    private void togglePinFolder(String path) {
        String pinnedStr = libPrefs.getString("pinned_folders", "");
        String[] items = pinnedStr.split("\\|");
        boolean found = false;
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (item.equals(path)) {
                found = true;
            } else if (!item.isEmpty()) {
                sb.append(item).append("|");
            }
        }
        if (!found) {
            sb.append(path).append("|");
            announceAccessibility("Folder pinned to top");
        } else {
            announceAccessibility("Folder unpinned");
        }
        libPrefs.edit().putString("pinned_folders", sb.toString()).apply();
    }

    private void showBookShelfSelectionDialog(final String folderPath) {
        String allShelvesStr = libPrefs.getString("all_bookshelves", "");
        List<String> shelves = new ArrayList<>();
        if (!allShelvesStr.isEmpty()) {
            for (String s : allShelvesStr.split("\\|")) {
                if (!s.isEmpty()) shelves.add(s);
            }
        }

        List<String> optionsList = new ArrayList<>(shelves);
        optionsList.add("[ Create New Shelf ]");
        final String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select Book Shelf")
                .setItems(options, (dialog, which) -> {
                    if (which == options.length - 1) {
                        EditText input = new EditText(this);
                        input.setHint("Shelf Name (e.g. History)");
                        new AlertDialog.Builder(this)
                                .setTitle("New Book Shelf")
                                .setView(input)
                                .setPositiveButton("Create & Add", (d, w) -> {
                                    String newName = input.getText().toString().trim();
                                    if (!newName.isEmpty() && !newName.contains("|")) {
                                        String updatedShelves = allShelvesStr.isEmpty() ? newName : allShelvesStr + "|" + newName;
                                        libPrefs.edit().putString("all_bookshelves", updatedShelves).apply();
                                        addToBookShelf(newName, folderPath);
                                    }
                                }).show();
                    } else {
                        addToBookShelf(shelves.get(which), folderPath);
                    }
                }).show();
    }

    private void addToBookShelf(String shelfName, String folderPath) {
        String key = "bookshelf_" + shelfName;
        String existing = libPrefs.getString(key, "");
        if (!existing.contains(folderPath)) {
            if (existing.isEmpty()) libPrefs.edit().putString(key, folderPath).apply();
            else libPrefs.edit().putString(key, existing + "|" + folderPath).apply();
            File f = new File(folderPath);
            String prefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isZipFile(f.getName()) ? "📦 " : "🎵 ");
            libPrefs.edit().putString("title_" + folderPath, prefix + f.getName()).apply();
            announceAccessibility("Added to shelf " + shelfName);
            Toast.makeText(this, "Added to " + shelfName, Toast.LENGTH_SHORT).show();
        } else {
            announceAccessibility("Already in shelf " + shelfName);
            Toast.makeText(this, "Already in " + shelfName, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadBookShelfList() {
        libraryDisplayNames.clear();
        libraryDisplayPaths.clear();

        libraryDisplayNames.add("[ + Create New Book Shelf ]");
        libraryDisplayPaths.add("CREATE_NEW_SHELF");

        String allShelvesStr = libPrefs.getString("all_bookshelves", "");
        if (!allShelvesStr.isEmpty()) {
            for (String s : allShelvesStr.split("\\|")) {
                if (!s.isEmpty()) {
                    libraryDisplayNames.add(s);
                    libraryDisplayPaths.add(s);
                }
            }
        }
        libraryAdapter.notifyDataSetChanged();
        updateLibraryHeader();
    }

    private void showPlaylistSelectionDialog(final String filePath) {
        String allPlaylistsStr = libPrefs.getString("all_playlists", "");
        List<String> playlists = new ArrayList<>();
        if (!allPlaylistsStr.isEmpty()) {
            for (String p : allPlaylistsStr.split("\\|")) {
                if (!p.isEmpty()) playlists.add(p);
            }
        }

        List<String> optionsList = new ArrayList<>(playlists);
        optionsList.add("[ Create New Playlist ]");
        final String[] options = optionsList.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Select Playlist")
                .setItems(options, (dialog, which) -> {
                    if (which == options.length - 1) {
                        EditText input = new EditText(this);
                        input.setHint("Playlist Name");
                        new AlertDialog.Builder(this)
                                .setTitle("New Playlist")
                                .setView(input)
                                .setPositiveButton("Create & Add", (d, w) -> {
                                    String newName = input.getText().toString().trim();
                                    if (!newName.isEmpty() && !newName.contains("|")) {
                                        String updatedPlaylists = allPlaylistsStr.isEmpty() ? newName : allPlaylistsStr + "|" + newName;
                                        libPrefs.edit().putString("all_playlists", updatedPlaylists).apply();
                                        addToPlaylist(newName, filePath);
                                    }
                                }).show();
                    } else {
                        addToPlaylist(playlists.get(which), filePath);
                    }
                }).show();
    }

    private void addToPlaylist(String playlistName, String filePath) {
        String key = "playlist_" + playlistName;
        String existing = libPrefs.getString(key, "");
        if (!existing.contains(filePath)) {
            if (existing.isEmpty()) libPrefs.edit().putString(key, filePath).apply();
            else libPrefs.edit().putString(key, existing + "|" + filePath).apply();
            File f = new File(filePath);
            String prefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isZipFile(f.getName()) ? "📦 " : "🎵 ");
            libPrefs.edit().putString("title_" + filePath, prefix + f.getName()).apply();
            announceAccessibility("Added to playlist " + playlistName);
            Toast.makeText(this, "Added to " + playlistName, Toast.LENGTH_SHORT).show();
        } else {
            announceAccessibility("Already in playlist " + playlistName);
            Toast.makeText(this, "Already in " + playlistName, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlaylistsList() {
        libraryDisplayNames.clear();
        libraryDisplayPaths.clear();

        libraryDisplayNames.add("[ + Create New Playlist ]");
        libraryDisplayPaths.add("CREATE_NEW_PLAYLIST");

        String allPlaylistsStr = libPrefs.getString("all_playlists", "");
        if (!allPlaylistsStr.isEmpty()) {
            for (String p : allPlaylistsStr.split("\\|")) {
                if (!p.isEmpty()) {
                    libraryDisplayNames.add(p);
                    libraryDisplayPaths.add(p);
                }
            }
        }
        libraryAdapter.notifyDataSetChanged();
        updateLibraryHeader();
    }

    private void addToLibrary(String key, String path) {
        String existing = libPrefs.getString(key, "");
        if (!existing.contains(path)) {
            if (existing.isEmpty()) libPrefs.edit().putString(key, path).apply();
            else libPrefs.edit().putString(key, path + "|" + existing).apply();
        } else {
            removeFromLibrary(key, path);
            String fresh = libPrefs.getString(key, "");
            if (fresh.isEmpty()) libPrefs.edit().putString(key, path).apply();
            else libPrefs.edit().putString(key, path + "|" + fresh).apply();
        }
    }

    private void removeFromLibrary(String key, String path) {
        String existing = libPrefs.getString(key, "");
        if (existing.isEmpty()) return;
        String[] items = existing.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (!item.equals(path) && !item.isEmpty()) {
                sb.append(item).append("|");
            }
        }
        String result = sb.toString();
        if (result.endsWith("|")) result = result.substring(0, result.length() - 1);
        libPrefs.edit().putString(key, result).apply();
    }

    private void loadLibraryData(String key) {
        libraryDisplayNames.clear();
        libraryDisplayPaths.clear();
        String existing = libPrefs.getString(key, "");
        if (!existing.isEmpty()) {
            String[] items = existing.split("\\|");
            for (String path : items) {
                if (!path.isEmpty()) {
                    libraryDisplayPaths.add(path);
                    String customTitle = libPrefs.getString("title_" + path, "");
                    if (!customTitle.isEmpty()) {
                        libraryDisplayNames.add(customTitle);
                    } else if (GoogleDriveManager.isGoogleDriveUrl(path)) {
                        String id = GoogleDriveManager.extractDriveId(path);
                        libraryDisplayNames.add("☁️ Google Drive (" + (id != null ? id : "Folder") + ")");
                    } else if (ZipMediaManager.isZipFile(path) || path.toLowerCase(Locale.ROOT).endsWith(".zip") || libPrefs.contains("zip_cache_" + path) || libPrefs.contains("zip_name_" + path)) {
                        String zName = libPrefs.getString("zip_name_" + path, "");
                        if (zName.isEmpty()) {
                            File f = new File(path);
                            zName = f.getName();
                        }
                        libraryDisplayNames.add("📦 " + zName);
                    } else if ("history_playlists".equals(key) || (libraryMode == 0 && historyFilterMode == 2)) {
                        libraryDisplayNames.add("🎶 " + path);
                    } else {
                        File f = new File(path);
                        String iconPrefix = f.isDirectory() ? "📁 " : (ZipMediaManager.isAudioFile(f.getName()) ? "🎵 " : "");
                        libraryDisplayNames.add(iconPrefix + f.getName() + (f.isDirectory() ? " (Folder)" : ""));
                    }
                }
            }
        }
        libraryAdapter.notifyDataSetChanged();
        updateLibraryHeader();
    }

    private void setupAdvancedFeatures() {
        btnSort.setOnClickListener(v -> {
            final String[] options = {
                    "Name (A to Z)", "Name (Z to A)",
                    "Modified Date (Newest first)", "Modified Date (Oldest first)",
                    "Size (Largest first)", "Size (Smallest first)",
                    "File Type (A to Z)", "File Type (Z to A)"
            };
            new AlertDialog.Builder(this)
                    .setTitle("Sort By")
                    .setSingleChoiceItems(options, currentSortMode, (dialog, which) -> {
                        currentSortMode = which;
                        loadDirectory(currentDir);
                        announceAccessibility("Sorted by " + options[which]);
                        dialog.dismiss();
                    }).show();
        });

        btnStream.setOnClickListener(v -> {
            EditText inputUrl = new EditText(this);
            inputUrl.setHint("Paste Google Drive Link or Audio Stream URL");
            inputUrl.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
            inputUrl.setPadding(40, 40, 40, 40);

            try {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                    CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).getText();
                    if (clipText != null) {
                        String text = clipText.toString().trim();
                        String norm = OnlineStreamManager.normalizeUrl(text);
                        if (GoogleDriveManager.isGoogleDriveUrl(text) || GoogleDriveManager.isGoogleDriveUrl(norm)) {
                            inputUrl.setText(GoogleDriveManager.isGoogleDriveUrl(text) ? text : norm);
                            inputUrl.setSelection(inputUrl.getText().length());
                        } else if (norm.startsWith("http://") || norm.startsWith("https://")) {
                            inputUrl.setText(norm);
                            inputUrl.setSelection(norm.length());
                        }
                    }
                }
            } catch (Exception ignored) {}

            new AlertDialog.Builder(this)
                    .setTitle("Play Stream / Google Drive")
                    .setView(inputUrl)
                    .setPositiveButton("Play", (dialog, which) -> {
                        String raw = inputUrl.getText().toString().trim();
                        if (!raw.isEmpty()) {
                            String url = OnlineStreamManager.normalizeUrl(raw);
                            if (GoogleDriveManager.isGoogleDriveUrl(raw) || GoogleDriveManager.isGoogleDriveUrl(url)) {
                                handleGoogleDriveUrl(GoogleDriveManager.isGoogleDriveUrl(raw) ? raw : url);
                            } else {
                                playNetworkStream(url);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnResumeToggle.setOnClickListener(v -> {
            boolean currentState = bkPrefs.getBoolean("resume_playback_enabled", true);
            bkPrefs.edit().putBoolean("resume_playback_enabled", !currentState).apply();
            updateResumeToggleUI();
            announceAccessibility(!currentState ? "Resume Playback turned ON" : "Resume Playback turned OFF");
        });

        btnBgPlaybackToggle.setOnClickListener(v -> {
            boolean currentState = bkPrefs.getBoolean("bg_playback_enabled", true);
            bkPrefs.edit().putBoolean("bg_playback_enabled", !currentState).apply();
            updateBgPlaybackToggleUI();
            announceAccessibility(!currentState ? "Background Playback turned ON" : "Background Playback turned OFF");
        });

        if (btnFilterMediaFolders != null) {
            btnFilterMediaFolders.setOnClickListener(v -> {
                boolean currentState = bkPrefs.getBoolean("filter_only_media_folders", true);
                bkPrefs.edit().putBoolean("filter_only_media_folders", !currentState).apply();
                updateFilterMediaFoldersToggleUI();
                mediaFolderCache.clear();
                if (!isBrowsingZip && !isBrowsingDrive) {
                    loadDirectory(currentDir);
                }
                announceAccessibility(!currentState ? "Showing media folders only" : "Showing all folders");
                Toast.makeText(this, !currentState ? "Media folders only: ON" : "Show all folders: ON", Toast.LENGTH_SHORT).show();
            });
        }

        if (btnSkipSilence != null) {
            btnSkipSilence.setOnClickListener(v -> {
                isSkipSilenceEnabled = !isSkipSilenceEnabled;
                bkPrefs.edit().putBoolean("skip_silence", isSkipSilenceEnabled).apply();
                btnSkipSilence.setText("Skip Silence: " + (isSkipSilenceEnabled ? "ON" : "OFF"));
                if (mediaPlayer != null) {
                    mediaPlayer.setSkipSilenceEnabled(isSkipSilenceEnabled);
                }
                announceAccessibility("Skip Silence " + (isSkipSilenceEnabled ? "ON" : "OFF"));
            });
        }

        if (btnFadeConfig != null) {
            btnFadeConfig.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Select Fading Speed")
                    .setSingleChoiceItems(fadeOptions, currentFadeIndex, (dialog, which) -> {
                        currentFadeIndex = which;
                        currentFadeMultiplier = fadeValues[which];
                        bkPrefs.edit().putFloat("fade_multiplier", currentFadeMultiplier)
                                .putInt("fade_index", currentFadeIndex).apply();
                        btnFadeConfig.setText("Fading Speed: " + fadeOptions[which]);
                        announceAccessibility("Fading speed set to " + fadeOptions[which]);
                        dialog.dismiss();
                    }).show());
        }

        if (btnGapConfig != null) {
            btnGapConfig.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Silence Between Tracks")
                    .setSingleChoiceItems(gapOptions, currentGapIndex, (dialog, which) -> {
                        currentGapIndex = which;
                        bkPrefs.edit().putInt("gap_index", currentGapIndex).apply();
                        btnGapConfig.setText("Track Gap: " + gapOptions[which]);
                        announceAccessibility("Gap set to " + gapOptions[which]);
                        dialog.dismiss();
                    }).show());
        }

        if (btnAutoPlayNextFolder != null) {
            btnAutoPlayNextFolder.setOnClickListener(v -> {
                boolean current = bkPrefs.getBoolean("auto_play_next_folder", false);
                boolean updated = !current;
                bkPrefs.edit().putBoolean("auto_play_next_folder", updated).apply();
                btnAutoPlayNextFolder.setText("Auto-Play Next Folder: " + (updated ? "ON" : "OFF"));
                btnAutoPlayNextFolder.setContentDescription("Auto-Play Next Folder: " + (updated ? "ON" : "OFF"));
                String msg = updated ? "Auto-play next folder enabled" : "Auto-play next folder disabled";
                announceAccessibility(msg);
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            });
        }

        if (btnClearZipCache != null) {
            btnClearZipCache.setOnClickListener(v -> {
                long size = ZipMediaManager.getZipCacheSize(this);
                if (size <= 0) {
                    Toast.makeText(this, "ZIP cache is empty.", Toast.LENGTH_SHORT).show();
                    announceAccessibility("ZIP cache is empty.");
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Clear ZIP Cache")
                        .setMessage("Clear all extracted ZIP audio cache (" + ZipMediaManager.formatFileSize(size) + ")?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            ZipMediaManager.clearZipCache(this);
                            updateZipCacheUI();
                            Toast.makeText(this, "ZIP cache cleared.", Toast.LENGTH_SHORT).show();
                            announceAccessibility("ZIP cache cleared.");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnClearSharedMediaCache != null) {
            btnClearSharedMediaCache.setOnClickListener(v -> {
                long size = getSharedMediaCacheSize();
                if (size <= 0) {
                    Toast.makeText(this, "Shared audio cache is empty.", Toast.LENGTH_SHORT).show();
                    announceAccessibility("Shared audio cache is empty.");
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Clear Shared Audio Cache")
                        .setMessage("Clear all cached audio files received via share or 'Open with' (" + ZipMediaManager.formatFileSize(size) + ")?")
                        .setPositiveButton("Clear", (dialog, which) -> {
                            clearSharedMediaCache();
                            updateZipCacheUI();
                            Toast.makeText(this, "Shared audio cache cleared.", Toast.LENGTH_SHORT).show();
                            announceAccessibility("Shared audio cache cleared.");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnClearAllCache != null) {
            btnClearAllCache.setOnClickListener(v -> {
                long zipSize = ZipMediaManager.getZipCacheSize(this);
                long sharedSize = getSharedMediaCacheSize();
                long totalSize = zipSize + sharedSize;
                if (totalSize <= 0) {
                    Toast.makeText(this, "All temporary caches are empty.", Toast.LENGTH_SHORT).show();
                    announceAccessibility("All temporary caches are empty.");
                    return;
                }
                new AlertDialog.Builder(this)
                        .setTitle("Clear All Temporary Cache")
                        .setMessage("Clear all ZIP and shared audio cache files (" + ZipMediaManager.formatFileSize(totalSize) + ")?")
                        .setPositiveButton("Clear All", (dialog, which) -> {
                            ZipMediaManager.clearZipCache(this);
                            clearSharedMediaCache();
                            updateZipCacheUI();
                            Toast.makeText(this, "All temporary caches cleared.", Toast.LENGTH_SHORT).show();
                            announceAccessibility("All temporary caches cleared.");
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        Button btnDefaultAppLinks = findViewById(R.id.btnDefaultAppLinks);
        if (btnDefaultAppLinks != null) {
            btnDefaultAppLinks.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Drive Link Settings")
                        .setMessage("To open Google Drive links directly in KTS Player when clicking them in WhatsApp or other apps:\n\n" +
                                "1. Tap 'Open Settings' below.\n" +
                                "2. Open 'Open by default' and use 'Add link' to add drive.google.com.\n\n" +
                                "Alternatively, open your Google Drive app info -> 'Open by default' and turn off 'Open supported links'.\n\n" +
                                "💡 Tip: You can also tap 'Share' on any WhatsApp link or audio and select 'KTS Advanced Media Player' to play immediately!")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                } else {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                }
                            } catch (Exception e) {
                                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        // App Updates & GitHub Integration
        headerUpdates = findViewById(R.id.headerUpdates);
        tvCurrentVersionInfo = findViewById(R.id.tvCurrentVersionInfo);
        if (tvCurrentVersionInfo != null) {
            tvCurrentVersionInfo.setText("Current Version: v" + AppUpdateManager.getCurrentVersionName(this) + " (Build " + AppUpdateManager.getCurrentVersionCode(this) + ")");
        }

        btnCheckUpdate = findViewById(R.id.btnCheckUpdate);
        if (btnCheckUpdate != null) {
            btnCheckUpdate.setOnClickListener(v -> AppUpdateManager.getInstance().checkForUpdates(MainActivity.this, true));
        }

        btnAutoCheckUpdateToggle = findViewById(R.id.btnAutoCheckUpdateToggle);
        if (btnAutoCheckUpdateToggle != null) {
            updateAutoCheckUpdateUI();
            btnAutoCheckUpdateToggle.setOnClickListener(v -> {
                boolean newState = !AppUpdateManager.isAutoCheckEnabled(MainActivity.this);
                AppUpdateManager.setAutoCheckEnabled(MainActivity.this, newState);
                updateAutoCheckUpdateUI();
                announceAccessibility(newState ? "Auto check for updates enabled" : "Auto check for updates disabled");
            });
        }

        if (btnSleepTimer != null) {
            btnSleepTimer.setOnClickListener(v -> showSleepTimerDialog());
            updateSleepTimerUI();
        }

        setupButtonAccessibilityActions();

        if (seekBarPlayback != null) {
            seekBarPlayback.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer != null) {
                        long total = mediaPlayer.getDuration();
                        if (total > 0) {
                            long target = (progress * total) / 1000;
                            if (tvCurrentProgressTime != null) {
                                tvCurrentProgressTime.setText(formatTime(target));
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    isUserTrackingSeekBar = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    isUserTrackingSeekBar = false;
                    if (mediaPlayer != null) {
                        long total = mediaPlayer.getDuration();
                        if (total > 0) {
                            long target = (seekBar.getProgress() * total) / 1000;
                            fadeAudio(-1.0f, 0.0f, 100, () -> {
                                if (mediaPlayer != null) mediaPlayer.seekTo(target);
                                fadeAudio(0.0f, 1.0f, 200, null);
                            });
                            int percent = (int) ((target * 100) / total);
                            announceAccessibility("Jumped to " + percent + " percent, " + formatTimeSpeech(target));
                        }
                    }
                }
            });

            seekBarPlayback.setOnLongClickListener(v -> {
                showSliderStepDialog();
                return true;
            });
        }

        btnJump.setOnClickListener(v -> {
            if (currentPlayingFile.isEmpty() || mediaPlayer == null) return;
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 40, 40, 40);

            EditText inputMin = new EditText(this);
            inputMin.setHint("Minutes");
            inputMin.setInputType(InputType.TYPE_CLASS_NUMBER);
            layout.addView(inputMin);

            EditText inputSec = new EditText(this);
            inputSec.setHint("Seconds");
            inputSec.setInputType(InputType.TYPE_CLASS_NUMBER);
            layout.addView(inputSec);

            new AlertDialog.Builder(this)
                    .setTitle("Jump to Time")
                    .setView(layout)
                    .setPositiveButton("Go", (dialog, which) -> {
                        int tempM = 0, tempS = 0;
                        try { if (!inputMin.getText().toString().isEmpty()) tempM = Integer.parseInt(inputMin.getText().toString()); } catch (Exception ignored) {}
                        try { if (!inputSec.getText().toString().isEmpty()) tempS = Integer.parseInt(inputSec.getText().toString()); } catch (Exception ignored) {}

                        final int finalM = tempM;
                        final int finalS = tempS;

                        int targetTime = (finalM * 60 + finalS) * 1000;
                        long dur = mediaPlayer.getDuration();
                        if (dur > 0 && targetTime > dur) targetTime = (int) dur;

                        final long finalTarget = targetTime;
                        fadeAudio(-1f, 0.0f, 200, () -> {
                            if (mediaPlayer != null) {
                                mediaPlayer.seekTo(finalTarget);
                            }
                            fadeAudio(0.0f, 1.0f, 250, null);
                            announceAccessibility("Jumped to " + finalM + " minutes " + finalS + " seconds");
                        });
                    }).show();
        });

        btnJump.setOnLongClickListener(v -> {
            showTimeDetails();
            return true;
        });

        btnForward.setOnClickListener(v -> {
            String txt = (seekStep == 60000) ? "1 minute" : (seekStep / 1000) + " seconds";
            safeSeekWithFade(seekStep, "Forwarded " + txt);
        });

        btnRewind.setOnClickListener(v -> {
            String txt = (seekStep == 60000) ? "1 minute" : (seekStep / 1000) + " seconds";
            safeSeekWithFade(-seekStep, "Rewound " + txt);
        });

        btnForward.setOnLongClickListener(v -> { showSeekSettings(); return true; });
        btnRewind.setOnLongClickListener(v -> { showSeekSettings(); return true; });

        btnSpeed.setOnClickListener(v -> {
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(40, 40, 40, 40);
            layout.setGravity(Gravity.CENTER);

            Button btnMinus = new Button(this);
            btnMinus.setText("- 0.1x");

            TextView tvSpeed = new TextView(this);
            tvSpeed.setText(String.format("%.1fx", currentSpeed));
            tvSpeed.setTextSize(20);
            tvSpeed.setPadding(40, 0, 40, 0);
            tvSpeed.setTextColor(Color.WHITE);

            Button btnPlus = new Button(this);
            btnPlus.setText("+ 0.1x");

            btnMinus.setOnClickListener(v1 -> {
                if (currentSpeed > 0.3f) {
                    currentSpeed -= 0.1f;
                    tvSpeed.setText(String.format("%.1fx", currentSpeed));
                    if (mediaPlayer != null) mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
                    announceAccessibility("Speed " + String.format("%.1fx", currentSpeed));
                }
            });

            btnPlus.setOnClickListener(v1 -> {
                if (currentSpeed < 3.0f) {
                    currentSpeed += 0.1f;
                    tvSpeed.setText(String.format("%.1fx", currentSpeed));
                    if (mediaPlayer != null) mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
                    announceAccessibility("Speed " + String.format("%.1fx", currentSpeed));
                }
            });

            layout.addView(btnMinus);
            layout.addView(tvSpeed);
            layout.addView(btnPlus);

            new AlertDialog.Builder(this)
                    .setTitle("Adjust Audio Speed")
                    .setView(layout)
                    .setPositiveButton("Close", null)
                    .show();
        });

        btnLoop.setOnClickListener(v -> {
            if (currentPlayingFile.isEmpty()) return;
            if (abState == 0) {
                pointA = (int) mediaPlayer.getCurrentPosition();
                abState = 1;
                btnLoop.setText("Loop (End)");
                announceAccessibility("Point A set");
            } else if (abState == 1) {
                pointB = (int) mediaPlayer.getCurrentPosition();
                if (pointB <= pointA) {
                    announceAccessibility("Invalid time. Try again");
                    resetAB();
                    return;
                }
                abState = 2;
                btnLoop.setText("Loop (Cancel)");
                announceAccessibility("Point B set. Repeating.");
                handler.post(loopRunnable);
            } else if (abState == 2) {
                resetAB();
                announceAccessibility("Repeat turned off");
            }
        });

        btnBookmark.setContentDescription("Bookmark");
        btnBookmark.setOnClickListener(v -> showBookmarksDialog());
        btnBookmark.setOnLongClickListener(v -> {
            quickSaveBookmarkAtCurrentPosition();
            return true;
        });
    }

    private void setupButtonAccessibilityActions() {
        // 1. Fast Forward Button
        if (btnForward != null) {
            ViewCompat.setAccessibilityDelegate(btnForward, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_fwd_5, "Forward 5 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_fwd_10, "Forward 10 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_fwd_30, "Forward 30 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_fwd_60, "Forward 1 minute"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_fwd_300, "Forward 5 minutes"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_configure, "Configure seek interval"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_seek_fwd_5) {
                        performSeekDelta(5000);
                        return true;
                    } else if (action == R.id.action_seek_fwd_10) {
                        performSeekDelta(10000);
                        return true;
                    } else if (action == R.id.action_seek_fwd_30) {
                        performSeekDelta(30000);
                        return true;
                    } else if (action == R.id.action_seek_fwd_60) {
                        performSeekDelta(60000);
                        return true;
                    } else if (action == R.id.action_seek_fwd_300) {
                        performSeekDelta(300000);
                        return true;
                    } else if (action == R.id.action_seek_configure) {
                        showSeekSettings();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 2. Rewind Button
        if (btnRewind != null) {
            ViewCompat.setAccessibilityDelegate(btnRewind, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_rwd_5, "Rewind 5 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_rwd_10, "Rewind 10 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_rwd_30, "Rewind 30 seconds"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_rwd_60, "Rewind 1 minute"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_rwd_300, "Rewind 5 minutes"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_seek_configure, "Configure seek interval"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_seek_rwd_5) {
                        performSeekDelta(-5000);
                        return true;
                    } else if (action == R.id.action_seek_rwd_10) {
                        performSeekDelta(-10000);
                        return true;
                    } else if (action == R.id.action_seek_rwd_30) {
                        performSeekDelta(-30000);
                        return true;
                    } else if (action == R.id.action_seek_rwd_60) {
                        performSeekDelta(-60000);
                        return true;
                    } else if (action == R.id.action_seek_rwd_300) {
                        performSeekDelta(-300000);
                        return true;
                    } else if (action == R.id.action_seek_configure) {
                        showSeekSettings();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 3. Bookmark Button
        if (btnBookmark != null) {
            ViewCompat.setAccessibilityDelegate(btnBookmark, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_bookmark_view, "View bookmarks in folder / ZIP"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_bookmark_quick_save, "Quick save bookmark"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_bookmark_prompt_save, "Save bookmark with custom title"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_bookmark_clear_track, "Clear bookmarks for this track"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_bookmark_view) {
                        showBookmarksDialog();
                        return true;
                    } else if (action == R.id.action_bookmark_quick_save) {
                        quickSaveBookmarkAtCurrentPosition();
                        return true;
                    } else if (action == R.id.action_bookmark_prompt_save) {
                        promptSaveBookmark();
                        return true;
                    } else if (action == R.id.action_bookmark_clear_track) {
                        if (!currentPlayingFile.isEmpty()) {
                            bkPrefs.edit().remove(currentPlayingFile).apply();
                            if (!currentZipFilePath.isEmpty()) {
                                bkPrefs.edit().remove("ZIP_BM:" + currentZipFilePath + "::" + new File(currentPlayingFile).getName()).apply();
                            } else {
                                File f = new File(currentPlayingFile);
                                if (f.getParent() != null) {
                                    bkPrefs.edit().remove("DIR_BM:" + f.getParent() + "::" + f.getName()).apply();
                                }
                            }
                            announceAccessibility("Bookmarks cleared for current track");
                            Toast.makeText(MainActivity.this, "Bookmarks cleared", Toast.LENGTH_SHORT).show();
                        } else {
                            announceAccessibility("No track playing");
                        }
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 4. Sleep Timer Button
        if (btnSleepTimer != null) {
            ViewCompat.setAccessibilityDelegate(btnSleepTimer, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_dialog, "Sleep timer options dialog"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_15, "Set 15 minutes timer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_30, "Set 30 minutes timer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_45, "Set 45 minutes timer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_60, "Set 1 hour timer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_timer_end_of_track, "Stop at end of track"));
                    if (isSleepTimerActive || isSleepEndOfTrack) {
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                R.id.action_timer_cancel, "Turn off sleep timer"));
                    }
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_timer_dialog) {
                        showSleepTimerDialog();
                        return true;
                    } else if (action == R.id.action_timer_15) {
                        startSleepTimer(15 * 60 * 1000, "15 minutes");
                        return true;
                    } else if (action == R.id.action_timer_30) {
                        startSleepTimer(30 * 60 * 1000, "30 minutes");
                        return true;
                    } else if (action == R.id.action_timer_45) {
                        startSleepTimer(45 * 60 * 1000, "45 minutes");
                        return true;
                    } else if (action == R.id.action_timer_60) {
                        startSleepTimer(60 * 60 * 1000, "1 hour");
                        return true;
                    } else if (action == R.id.action_timer_end_of_track) {
                        startSleepTimerEndOfTrack();
                        return true;
                    } else if (action == R.id.action_timer_cancel) {
                        cancelSleepTimer();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 5. Jump Button
        if (btnJump != null) {
            ViewCompat.setAccessibilityDelegate(btnJump, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_details, "Show time details"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_folder_time_details, "Folder total duration and track count"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_start, "Jump to beginning"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_25, "Jump to 25%"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_50, "Jump to 50% (Middle)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_75, "Jump to 75%"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_jump_dialog, "Jump to specific time dialog"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_jump_details) {
                        showTimeDetails();
                        return true;
                    } else if (action == R.id.action_folder_time_details) {
                        showFolderTimeDetails();
                        return true;
                    } else if (action == R.id.action_jump_start) {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(0);
                            announceAccessibility("Jumped to beginning");
                        }
                        return true;
                    } else if (action == R.id.action_jump_25) {
                        jumpToFraction(0.25f, "25 percent");
                        return true;
                    } else if (action == R.id.action_jump_50) {
                        jumpToFraction(0.50f, "50 percent");
                        return true;
                    } else if (action == R.id.action_jump_75) {
                        jumpToFraction(0.75f, "75 percent");
                        return true;
                    } else if (action == R.id.action_jump_dialog) {
                        btnJump.performClick();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 5b. Playback Progress Slider (SeekBar)
        if (seekBarPlayback != null) {
            ViewCompat.setAccessibilityDelegate(seekBarPlayback, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_slider_step_forward, "Step forward " + sliderStepPercent + "%"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_slider_step_backward, "Step backward " + sliderStepPercent + "%"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_slider_config_step, "Configure slider step size"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_slider_step_forward || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) {
                        performSliderStep(sliderStepPercent);
                        return true;
                    } else if (action == R.id.action_slider_step_backward || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                        performSliderStep(-sliderStepPercent);
                        return true;
                    } else if (action == R.id.action_slider_config_step) {
                        showSliderStepDialog();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 6. Play / Pause Button
        if (btnPlay != null) {
            ViewCompat.setAccessibilityDelegate(btnPlay, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_play_stop, "Stop playback"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_play_restart, "Restart track from beginning"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_play_rewind_3, "Rewind 3 seconds and play"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_play_stop) {
                        if (mediaPlayer != null) {
                            mediaPlayer.pause();
                            mediaPlayer.seekTo(0);
                            saveCurrentPosition();
                            updateMediaSessionState(PlaybackState.STATE_STOPPED);
                            btnPlay.setText("Play");
                            announceAccessibility("Stopped playback");
                        }
                        return true;
                    } else if (action == R.id.action_play_restart) {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(0);
                            mediaPlayer.play();
                            btnPlay.setText("Pause");
                            announceAccessibility("Restarted from beginning");
                        }
                        return true;
                    } else if (action == R.id.action_play_rewind_3) {
                        if (mediaPlayer != null) {
                            long pos = Math.max(0, mediaPlayer.getCurrentPosition() - 3000);
                            mediaPlayer.seekTo(pos);
                            mediaPlayer.play();
                            btnPlay.setText("Pause");
                            announceAccessibility("Playing from " + formatTime((int) pos));
                        }
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 7. Next Track Button
        if (btnNext != null) {
            ViewCompat.setAccessibilityDelegate(btnNext, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_nav_next_track, "Next track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_nav_fwd_30, "Forward 30 seconds"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_nav_next_track) {
                        btnNext.performClick();
                        return true;
                    } else if (action == R.id.action_nav_fwd_30) {
                        performSeekDelta(30000);
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 8. Previous Track Button
        if (btnPrev != null) {
            ViewCompat.setAccessibilityDelegate(btnPrev, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_nav_prev_track, "Previous track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_nav_restart_track, "Restart current track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_nav_rwd_30, "Rewind 30 seconds"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_nav_prev_track) {
                        btnPrev.performClick();
                        return true;
                    } else if (action == R.id.action_nav_restart_track) {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(0);
                            mediaPlayer.play();
                            announceAccessibility("Playing from beginning");
                        }
                        return true;
                    } else if (action == R.id.action_nav_rwd_30) {
                        performSeekDelta(-30000);
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 9. Play Mode Button
        if (btnPlayMode != null) {
            ViewCompat.setAccessibilityDelegate(btnPlayMode, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_mode_set_all, "Play all tracks and stop at end"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_mode_set_one, "Repeat current track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_mode_set_repeat_all, "Repeat all tracks continuously"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_mode_set_shuffle, "Shuffle tracks and stop at end"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_mode_set_all) {
                        playMode = 0;
                        bkPrefs.edit().putInt("persistent_play_mode", playMode).apply();
                        updateExoPlayerPlayMode(true);
                        return true;
                    } else if (action == R.id.action_mode_set_one) {
                        playMode = 1;
                        bkPrefs.edit().putInt("persistent_play_mode", playMode).apply();
                        updateExoPlayerPlayMode(true);
                        return true;
                    } else if (action == R.id.action_mode_set_repeat_all) {
                        playMode = 2;
                        bkPrefs.edit().putInt("persistent_play_mode", playMode).apply();
                        updateExoPlayerPlayMode(true);
                        return true;
                    } else if (action == R.id.action_mode_set_shuffle) {
                        playMode = 3;
                        bkPrefs.edit().putInt("persistent_play_mode", playMode).apply();
                        updateExoPlayerPlayMode(true);
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 10. Playback Speed Button
        if (btnSpeed != null) {
            ViewCompat.setAccessibilityDelegate(btnSpeed, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_set_10, "Normal speed (1.0x)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_set_125, "Speed 1.25x"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_set_15, "Speed 1.5x"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_set_20, "Speed 2.0x"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_set_075, "Speed 0.75x"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_increase, "Increase speed (+0.1x)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_decrease, "Decrease speed (-0.1x)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_speed_dialog, "Speed adjust dialog"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_speed_set_10) {
                        applyPlaybackSpeed(1.0f);
                        return true;
                    } else if (action == R.id.action_speed_set_125) {
                        applyPlaybackSpeed(1.25f);
                        return true;
                    } else if (action == R.id.action_speed_set_15) {
                        applyPlaybackSpeed(1.5f);
                        return true;
                    } else if (action == R.id.action_speed_set_20) {
                        applyPlaybackSpeed(2.0f);
                        return true;
                    } else if (action == R.id.action_speed_set_075) {
                        applyPlaybackSpeed(0.75f);
                        return true;
                    } else if (action == R.id.action_speed_increase) {
                        if (currentSpeed < 3.0f) {
                            applyPlaybackSpeed(Math.round((currentSpeed + 0.1f) * 10.0f) / 10.0f);
                        }
                        return true;
                    } else if (action == R.id.action_speed_decrease) {
                        if (currentSpeed > 0.3f) {
                            applyPlaybackSpeed(Math.round((currentSpeed - 0.1f) * 10.0f) / 10.0f);
                        }
                        return true;
                    } else if (action == R.id.action_speed_dialog) {
                        btnSpeed.performClick();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 11. Loop Button
        if (btnLoop != null) {
            ViewCompat.setAccessibilityDelegate(btnLoop, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_loop_set_a, "Set point A (Loop start)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_loop_set_b, "Set point B (Loop end & repeat)"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_loop_cancel, "Clear AB loop"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_loop_set_a) {
                        if (mediaPlayer != null && !currentPlayingFile.isEmpty()) {
                            pointA = (int) mediaPlayer.getCurrentPosition();
                            abState = 1;
                            btnLoop.setText("Loop (End)");
                            announceAccessibility("Point A set at " + formatTime(pointA));
                        }
                        return true;
                    } else if (action == R.id.action_loop_set_b) {
                        if (mediaPlayer != null && !currentPlayingFile.isEmpty() && abState == 1) {
                            pointB = (int) mediaPlayer.getCurrentPosition();
                            if (pointB <= pointA) {
                                announceAccessibility("Invalid end time. Try again");
                                resetAB();
                            } else {
                                abState = 2;
                                btnLoop.setText("Loop (Cancel)");
                                announceAccessibility("Point B set at " + formatTime(pointB) + ". Repeating loop.");
                                handler.post(loopRunnable);
                            }
                        } else {
                            announceAccessibility("Please set Point A first");
                        }
                        return true;
                    } else if (action == R.id.action_loop_cancel) {
                        resetAB();
                        announceAccessibility("Repeat loop cleared");
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 12. Audio Route Button
        if (btnAudioRoute != null) {
            ViewCompat.setAccessibilityDelegate(btnAudioRoute, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_route_toggle_on, "Turn ON audio separation"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_route_toggle_off, "Turn OFF audio separation"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_route_toggle_on) {
                        AudioRoutingManager.getInstance().enableAudioSeparation(MainActivity.this, true);
                        return true;
                    } else if (action == R.id.action_route_toggle_off) {
                        AudioRoutingManager.getInstance().disableAudioSeparation(MainActivity.this, true);
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 12b. Ear Speaker Mode Button
        if (btnEarSpeakerMode != null) {
            ViewCompat.setAccessibilityDelegate(btnEarSpeakerMode, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_ear_mode_on, "Turn ON ear speaker mode"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_ear_mode_auto, "Turn ON auto ear speaker mode"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_ear_mode_off, "Turn OFF ear speaker mode"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_ear_mode_on) {
                        setEarSpeakerMode(EAR_MODE_ON, true);
                        return true;
                    } else if (action == R.id.action_ear_mode_auto) {
                        setEarSpeakerMode(EAR_MODE_AUTO, true);
                        return true;
                    } else if (action == R.id.action_ear_mode_off) {
                        setEarSpeakerMode(EAR_MODE_OFF, true);
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 13. Sort Button
        if (btnSort != null) {
            ViewCompat.setAccessibilityDelegate(btnSort, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_sort_order_az, "Sort by name A to Z"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_sort_order_za, "Sort by name Z to A"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_sort_order_date, "Sort by date modified"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_sort_order_size, "Sort by file size"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_sort_dialog, "Sort options dialog"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_sort_order_az) {
                        currentSortMode = 0;
                        loadDirectory(currentDir);
                        announceAccessibility("Sorted by name A to Z");
                        return true;
                    } else if (action == R.id.action_sort_order_za) {
                        currentSortMode = 1;
                        loadDirectory(currentDir);
                        announceAccessibility("Sorted by name Z to A");
                        return true;
                    } else if (action == R.id.action_sort_order_date) {
                        currentSortMode = 2;
                        loadDirectory(currentDir);
                        announceAccessibility("Sorted by date modified");
                        return true;
                    } else if (action == R.id.action_sort_order_size) {
                        currentSortMode = 4;
                        loadDirectory(currentDir);
                        announceAccessibility("Sorted by file size");
                        return true;
                    } else if (action == R.id.action_sort_dialog) {
                        btnSort.performClick();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 14. Stream Button
        if (btnStream != null) {
            ViewCompat.setAccessibilityDelegate(btnStream, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_stream_play_clipboard, "Play URL from clipboard"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_stream_open_dialog, "Stream or Drive link dialog"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_stream_play_clipboard) {
                        playFromClipboardUrl();
                        return true;
                    } else if (action == R.id.action_stream_open_dialog) {
                        btnStream.performClick();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        if (btnQueue != null) {
            ViewCompat.setAccessibilityDelegate(btnQueue, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_view_queue, "View Playback Queue"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_view_queue) {
                        showQueueDialog();
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 15. Equalizer Toggle Button
        if (btnEqToggle != null) {
            ViewCompat.setAccessibilityDelegate(btnEqToggle, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_eq_turn_on, "Turn ON equalizer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_eq_turn_off, "Turn OFF equalizer"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_eq_reset_flat, "Reset equalizer bands to flat (0 dB)"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_eq_turn_on) {
                        setupAudioEffects();
                        isEqEnabled = true;
                        if (equalizer != null) {
                            try { equalizer.setEnabled(true); } catch(Exception ignored) {}
                            btnEqToggle.setText("Equalizer: ON");
                            announceAccessibility("Equalizer turned ON");
                            buildEqUI();
                        }
                        return true;
                    } else if (action == R.id.action_eq_turn_off) {
                        setupAudioEffects();
                        isEqEnabled = false;
                        if (equalizer != null) {
                            try { equalizer.setEnabled(false); } catch(Exception ignored) {}
                            btnEqToggle.setText("Equalizer: OFF");
                            announceAccessibility("Equalizer turned OFF");
                            eqBandsContainer.removeAllViews();
                        }
                        return true;
                    } else if (action == R.id.action_eq_reset_flat) {
                        if (equalizer != null) {
                            short numBands = equalizer.getNumberOfBands();
                            for (short i = 0; i < numBands; i++) {
                                equalizer.setBandLevel(i, (short) 0);
                            }
                            buildEqUI();
                            announceAccessibility("Equalizer bands reset to flat");
                            Toast.makeText(MainActivity.this, "Equalizer reset to flat", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 16. Now Playing Display
        if (tvNowPlaying != null) {
            ViewCompat.setAccessibilityDelegate(tvNowPlaying, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_resume_last, "Resume last played track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_bookshelf, "Open book shelf"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_favorites, "Open favorites"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_history, "Open history"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_now_details, "Show track time and playback details"));
                    if (!currentPlayingFile.isEmpty()) {
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                R.id.action_now_fav, "Add current track to favorites"));
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                R.id.action_now_bookshelf, "Add current track to book shelf"));
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                R.id.action_now_playlist, "Add current track to playlist"));
                        info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                                R.id.action_now_share, "Share current track"));
                    }
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_resume_last) {
                        resumeGlobalLastPlayed();
                        return true;
                    } else if (action == R.id.action_open_bookshelf) {
                        switchTab(2);
                        libraryMode = 4;
                        loadBookShelfList();
                        announceAccessibility("Showing Book Shelves");
                        return true;
                    } else if (action == R.id.action_open_favorites) {
                        switchTab(2);
                        libraryMode = 1;
                        loadLibraryData("favorites");
                        announceAccessibility("Showing Favorites");
                        return true;
                    } else if (action == R.id.action_open_history) {
                        switchTab(2);
                        libraryMode = 0;
                        historyFilterMode = 0;
                        loadHistoryData();
                        announceAccessibility("Showing History Tracks");
                        return true;
                    } else if (action == R.id.action_now_details) {
                        showTimeDetails();
                        return true;
                    } else if (action == R.id.action_now_fav) {
                        if (!currentPlayingFile.isEmpty()) {
                            libPrefs.edit().putString("title_" + currentPlayingFile, "🎵 " + new File(currentPlayingFile).getName()).apply();
                            addToLibrary("favorites", currentPlayingFile);
                            announceAccessibility("Track added to favorites");
                            Toast.makeText(MainActivity.this, "Track added to Favorites", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    } else if (action == R.id.action_now_bookshelf) {
                        if (!currentPlayingFile.isEmpty()) {
                            libPrefs.edit().putString("title_" + currentPlayingFile, "🎵 " + new File(currentPlayingFile).getName()).apply();
                            showBookShelfSelectionDialog(currentPlayingFile);
                        }
                        return true;
                    } else if (action == R.id.action_now_playlist) {
                        if (!currentPlayingFile.isEmpty()) {
                            libPrefs.edit().putString("title_" + currentPlayingFile, "🎵 " + new File(currentPlayingFile).getName()).apply();
                            showPlaylistSelectionDialog(currentPlayingFile);
                        }
                        return true;
                    } else if (action == R.id.action_now_share) {
                        if (!currentPlayingFile.isEmpty()) {
                            File f = new File(currentPlayingFile);
                            if (f.exists()) shareFileDirectly(f);
                        }
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }

        // 17. Root Layout Shortcuts for Accessibility (TalkBack custom actions)
        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            ViewCompat.setAccessibilityDelegate(rootLayout, new AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_resume_last, "Resume last played track"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_bookshelf, "Open book shelf"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_favorites, "Open favorites"));
                    info.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                            R.id.action_open_history, "Open history"));
                }

                @Override
                public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
                    if (action == R.id.action_resume_last) {
                        resumeGlobalLastPlayed();
                        return true;
                    } else if (action == R.id.action_open_bookshelf) {
                        switchTab(2);
                        libraryMode = 4;
                        loadBookShelfList();
                        announceAccessibility("Showing Book Shelves");
                        return true;
                    } else if (action == R.id.action_open_favorites) {
                        switchTab(2);
                        libraryMode = 1;
                        loadLibraryData("favorites");
                        announceAccessibility("Showing Favorites");
                        return true;
                    } else if (action == R.id.action_open_history) {
                        switchTab(2);
                        libraryMode = 0;
                        historyFilterMode = 0;
                        loadHistoryData();
                        announceAccessibility("Showing History Tracks");
                        return true;
                    }
                    return super.performAccessibilityAction(host, action, args);
                }
            });
        }
    }

    private void performSeekDelta(long deltaMs) {
        String dir = deltaMs >= 0 ? "Forwarded " : "Rewound ";
        long abs = Math.abs(deltaMs);
        String txt = (abs >= 60000) ? (abs / 60000) + " minute" + (abs >= 120000 ? "s" : "") : (abs / 1000) + " seconds";
        safeSeekWithFade(deltaMs, dir + txt);
    }

    private void quickSaveBookmarkAtCurrentPosition() {
        if (currentPlayingFile.isEmpty() || mediaPlayer == null) {
            announceAccessibility("No track is currently playing.");
            Toast.makeText(this, "No track is playing", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentPos = (int) mediaPlayer.getCurrentPosition();
        String timeStr = formatTime(currentPos);
        String name = "Bookmark at " + timeStr;
        String trackName = new File(currentPlayingFile).getName();
        saveBookmark(currentPlayingFile, trackName, name, currentPos);
        announceAccessibility("Bookmark saved at " + timeStr);
        Toast.makeText(this, "Bookmark saved at " + timeStr, Toast.LENGTH_SHORT).show();
    }

    private void jumpToFraction(float fraction, String label) {
        if (mediaPlayer != null && !currentPlayingFile.isEmpty()) {
            long dur = mediaPlayer.getDuration();
            if (dur > 0) {
                long target = (long) (dur * fraction);
                if (mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
                    fadeAudio(-1f, 0.0f, 180, () -> {
                        if (mediaPlayer != null) {
                            mediaPlayer.seekTo(target);
                        }
                        fadeAudio(0.0f, 1.0f, 240, null);
                        announceAccessibility("Jumped to " + label + " (" + formatTime((int) target) + ")");
                    });
                } else {
                    mediaPlayer.seekTo(target);
                    announceAccessibility("Jumped to " + label + " (" + formatTime((int) target) + ")");
                }
            }
        }
    }

    private void applyPlaybackSpeed(float speed) {
        currentSpeed = speed;
        if (mediaPlayer != null) {
            mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
        }
        btnSpeed.setText(String.format("%.1fx ⚡", currentSpeed));
        btnSpeed.setContentDescription(String.format("Speed %.1fx", currentSpeed));
        announceAccessibility("Speed " + String.format("%.1fx", currentSpeed));
        Toast.makeText(this, "Speed: " + String.format("%.1fx", currentSpeed), Toast.LENGTH_SHORT).show();
    }

    private void playFromClipboardUrl() {
        try {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence clipText = clipboard.getPrimaryClip().getItemAt(0).getText();
                if (clipText != null) {
                    String text = clipText.toString().trim();
                    if (!text.isEmpty()) {
                        String norm = OnlineStreamManager.normalizeUrl(text);
                        if (GoogleDriveManager.isGoogleDriveUrl(text) || GoogleDriveManager.isGoogleDriveUrl(norm)) {
                            handleGoogleDriveUrl(GoogleDriveManager.isGoogleDriveUrl(text) ? text : norm);
                            announceAccessibility("Loading Google Drive link from clipboard");
                            return;
                        } else if (norm.startsWith("http://") || norm.startsWith("https://")) {
                            playNetworkStream(norm);
                            announceAccessibility("Playing stream from clipboard");
                            return;
                        }
                    }
                }
            }
            announceAccessibility("No valid audio URL in clipboard");
            Toast.makeText(this, "No valid audio link in clipboard", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            announceAccessibility("Failed to read clipboard");
        }
    }

    public static class MediaBookmarkItem {
        public String trackPath;
        public String trackName;
        public String bookmarkTitle;
        public long positionMs;
        public boolean isZip;
        public String zipPath;
        public String folderPath;
    }

    private void saveBookmark(String trackPath, String trackName, String title, int positionMs) {
        if (trackName == null || trackName.isEmpty()) {
            if (trackPath != null && !trackPath.isEmpty()) {
                trackName = new File(trackPath).getName();
            } else {
                return;
            }
        }
        String newEntry = title + "<kts>" + positionMs;

        boolean isZip = !currentZipFilePath.isEmpty();
        if (isZip) {
            String permKey = "ZIP_BM:" + currentZipFilePath + "::" + trackName;
            String curVal = bkPrefs.getString(permKey, "");
            String updated = curVal.isEmpty() ? newEntry : curVal + "|" + newEntry;
            bkPrefs.edit().putString(permKey, updated).apply();

            if (trackPath != null && !trackPath.isEmpty()) {
                String pathVal = bkPrefs.getString(trackPath, "");
                String pathUpdated = pathVal.isEmpty() ? newEntry : pathVal + "|" + newEntry;
                bkPrefs.edit().putString(trackPath, pathUpdated).apply();
            }
        } else {
            String folderPath = "";
            if (trackPath != null && !trackPath.isEmpty()) {
                folderPath = new File(trackPath).getParent();
            }
            if (folderPath == null || folderPath.isEmpty()) {
                folderPath = currentDir;
            }

            if (folderPath != null && !folderPath.isEmpty()) {
                String permKey = "DIR_BM:" + folderPath + "::" + trackName;
                String curVal = bkPrefs.getString(permKey, "");
                String updated = curVal.isEmpty() ? newEntry : curVal + "|" + newEntry;
                bkPrefs.edit().putString(permKey, updated).apply();
            }

            if (trackPath != null && !trackPath.isEmpty()) {
                String pathVal = bkPrefs.getString(trackPath, "");
                String pathUpdated = pathVal.isEmpty() ? newEntry : pathVal + "|" + newEntry;
                bkPrefs.edit().putString(trackPath, pathUpdated).apply();
            }
        }
    }

    private void promptSaveBookmark() {
        if (currentPlayingFile.isEmpty() || mediaPlayer == null) {
            announceAccessibility("No track is currently playing.");
            Toast.makeText(this, "No track is playing", Toast.LENGTH_SHORT).show();
            return;
        }
        int currentPos = (int) mediaPlayer.getCurrentPosition();
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        EditText input = new EditText(this);
        input.setText("Bookmark at " + formatTime(currentPos));
        input.setSelectAllOnFocus(true);
        layout.addView(input);
        new AlertDialog.Builder(this)
                .setTitle("Save Bookmark")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = "Bookmark at " + formatTime(currentPos);
                    String trackName = new File(currentPlayingFile).getName();
                    saveBookmark(currentPlayingFile, trackName, name, currentPos);
                    announceAccessibility("Bookmark saved");
                    Toast.makeText(this, "Bookmark saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSavedBookmarksDialog() {
        showBookmarksDialog();
    }

    private void showBookmarksDialog() {
        showBookmarksDialogForScope(null, null, null);
    }

    private void showBookmarksDialogForScope(String specificPath, Boolean isZipOverride, String titleOverride) {
        String scopePath = specificPath;
        boolean isZip = isZipOverride != null ? isZipOverride : false;
        String scopeTitle = titleOverride;

        if (scopePath == null || scopePath.isEmpty()) {
            if (!currentZipFilePath.isEmpty()) {
                scopePath = currentZipFilePath;
                isZip = true;
                scopeTitle = !currentZipName.isEmpty() ? currentZipName : new File(currentZipFilePath).getName();
            } else if (isBrowsingZip && !currentZipFilePath.isEmpty()) {
                scopePath = currentZipFilePath;
                isZip = true;
                scopeTitle = !currentZipName.isEmpty() ? currentZipName : new File(currentZipFilePath).getName();
            } else if (!currentPlayingFile.isEmpty()) {
                File pf = new File(currentPlayingFile);
                scopePath = pf.getParent() != null ? pf.getParent() : "";
                isZip = false;
                scopeTitle = pf.getParentFile() != null ? pf.getParentFile().getName() : "Folder";
            } else if (currentDir != null && !currentDir.isEmpty() && !"STORAGE_ROOT".equals(currentDir)) {
                scopePath = currentDir;
                isZip = false;
                scopeTitle = new File(currentDir).getName();
            }
        }

        if (scopePath == null || scopePath.isEmpty()) {
            announceAccessibility("No active folder or track selected");
            Toast.makeText(this, "No active folder or track", Toast.LENGTH_SHORT).show();
            return;
        }

        if (scopeTitle == null || scopeTitle.isEmpty()) {
            scopeTitle = new File(scopePath).getName();
        }

        final String finalScopePath = scopePath;
        final boolean finalIsZip = isZip;
        final String finalScopeTitle = scopeTitle;

        List<MediaBookmarkItem> bookmarks = getBookmarksForScope(finalScopePath, finalIsZip);

        List<String> displayItems = new ArrayList<>();
        List<Runnable> actionItems = new ArrayList<>();

        boolean canAdd = mediaPlayer != null && !currentPlayingFile.isEmpty();
        int currentPos = canAdd ? (int) mediaPlayer.getCurrentPosition() : 0;
        String curTimeStr = formatTime(currentPos);

        if (canAdd) {
            displayItems.add("➕ Save Bookmark at " + curTimeStr);
            actionItems.add(() -> promptSaveBookmark());

            displayItems.add("⚡ Quick Save at " + curTimeStr);
            actionItems.add(() -> quickSaveBookmarkAtCurrentPosition());
        }

        if (!bookmarks.isEmpty()) {
            for (MediaBookmarkItem bm : bookmarks) {
                String label = "🔖 " + bm.trackName + " • " + bm.bookmarkTitle + " (" + formatTime((int) bm.positionMs) + ")";
                displayItems.add(label);
                actionItems.add(() -> jumpToBookmark(bm));
            }

            displayItems.add("🗑️ Delete a Bookmark...");
            actionItems.add(() -> showDeleteBookmarkDialog(finalScopePath, finalIsZip, bookmarks));

            displayItems.add("🧹 Clear All Bookmarks in this " + (finalIsZip ? "ZIP" : "Folder"));
            actionItems.add(() -> confirmClearAllBookmarks(finalScopePath, finalIsZip, finalScopeTitle));
        } else {
            if (!canAdd) {
                displayItems.add("ℹ️ No bookmarks in " + (finalIsZip ? "ZIP: " : "Folder: ") + finalScopeTitle);
                actionItems.add(null);
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("📑 Bookmarks: " + finalScopeTitle)
                .setItems(displayItems.toArray(new String[0]), (d, which) -> {
                    if (which >= 0 && which < actionItems.size()) {
                        Runnable r = actionItems.get(which);
                        if (r != null) {
                            r.run();
                        }
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private List<MediaBookmarkItem> getBookmarksForScope(String scopePath, boolean isZip) {
        List<MediaBookmarkItem> result = new ArrayList<>();
        if (scopePath == null || scopePath.isEmpty()) return result;

        Map<String, ?> allEntries = bkPrefs.getAll();
        Set<String> seen = new HashSet<>();

        if (isZip) {
            String zipPrefix = "ZIP_BM:" + scopePath + "::";
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(zipPrefix) && entry.getValue() instanceof String) {
                    String trackName = key.substring(zipPrefix.length());
                    parseBookmarksIntoList(result, trackName, null, scopePath, true, (String) entry.getValue(), seen);
                }
            }

            File zipFile = new File(scopePath);
            File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
            if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
                File alt = ZipMediaManager.findExtractedCacheDir(this, scopePath);
                if (alt != null) cacheDir = alt;
            }
            if (cacheDir != null && cacheDir.exists()) {
                List<File> extracted = ZipMediaManager.getExtractedAudioTracks(cacheDir);
                for (File t : extracted) {
                    String legacyVal = bkPrefs.getString(t.getAbsolutePath(), null);
                    if (legacyVal != null && !legacyVal.isEmpty()) {
                        parseBookmarksIntoList(result, t.getName(), t.getAbsolutePath(), scopePath, true, legacyVal, seen);
                        bkPrefs.edit().putString(zipPrefix + t.getName(), legacyVal).apply();
                    }
                }
            }
        } else {
            String dirPrefix = "DIR_BM:" + scopePath + "::";
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(dirPrefix) && entry.getValue() instanceof String) {
                    String trackName = key.substring(dirPrefix.length());
                    String trackPath = new File(scopePath, trackName).getAbsolutePath();
                    parseBookmarksIntoList(result, trackName, trackPath, scopePath, false, (String) entry.getValue(), seen);
                }
            }

            File folder = new File(scopePath);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (ZipMediaManager.isAudioFile(f.getName())) {
                            String legacyVal = bkPrefs.getString(f.getAbsolutePath(), null);
                            if (legacyVal != null && !legacyVal.isEmpty()) {
                                parseBookmarksIntoList(result, f.getName(), f.getAbsolutePath(), scopePath, false, legacyVal, seen);
                                bkPrefs.edit().putString(dirPrefix + f.getName(), legacyVal).apply();
                            }
                        }
                    }
                }
            }

            String prefixPath = scopePath.endsWith("/") ? scopePath : scopePath + "/";
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith(prefixPath) && entry.getValue() instanceof String) {
                    String remainder = key.substring(prefixPath.length());
                    if (!remainder.contains("/")) {
                        parseBookmarksIntoList(result, remainder, key, scopePath, false, (String) entry.getValue(), seen);
                        bkPrefs.edit().putString(dirPrefix + remainder, (String) entry.getValue()).apply();
                    }
                }
            }
        }

        Collections.sort(result, (a, b) -> {
            int c = ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(a.trackName, b.trackName);
            if (c != 0) return c;
            return Long.compare(a.positionMs, b.positionMs);
        });

        return result;
    }

    private void parseBookmarksIntoList(List<MediaBookmarkItem> list, String trackName, String trackPath,
                                        String scopePath, boolean isZip, String rawValue, Set<String> seen) {
        if (rawValue == null || rawValue.trim().isEmpty()) return;
        String[] entries = rawValue.split("\\|");
        for (String entry : entries) {
            String[] parts = entry.split("<kts>");
            if (parts.length == 2) {
                try {
                    String title = parts[0].trim();
                    long pos = Long.parseLong(parts[1].trim());
                    String uniqueId = trackName + "@@@" + pos;
                    if (!seen.contains(uniqueId)) {
                        seen.add(uniqueId);
                        MediaBookmarkItem item = new MediaBookmarkItem();
                        item.trackName = trackName;
                        item.trackPath = trackPath;
                        item.bookmarkTitle = title;
                        item.positionMs = pos;
                        item.isZip = isZip;
                        if (isZip) {
                            item.zipPath = scopePath;
                        } else {
                            item.folderPath = scopePath;
                        }
                        list.add(item);
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    private void showDeleteBookmarkDialog(String scopePath, boolean isZip, List<MediaBookmarkItem> bookmarks) {
        if (bookmarks.isEmpty()) {
            Toast.makeText(this, "No bookmarks to delete", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[bookmarks.size()];
        for (int i = 0; i < bookmarks.size(); i++) {
            MediaBookmarkItem b = bookmarks.get(i);
            titles[i] = b.trackName + " • " + b.bookmarkTitle + " (" + formatTime((int) b.positionMs) + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Bookmark to Delete")
                .setItems(titles, (dialog, which) -> {
                    MediaBookmarkItem toDelete = bookmarks.get(which);
                    new AlertDialog.Builder(this)
                            .setTitle("Delete Bookmark?")
                            .setMessage("Delete \"" + toDelete.bookmarkTitle + "\" in " + toDelete.trackName + "?")
                            .setPositiveButton("Delete", (d, w) -> deleteBookmark(toDelete))
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmClearAllBookmarks(String scopePath, boolean isZip, String scopeTitle) {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Bookmarks?")
                .setMessage("Are you sure you want to delete all bookmarks in " + (isZip ? "ZIP: " : "Folder: ") + scopeTitle + "?")
                .setPositiveButton("Clear All", (d, w) -> clearAllBookmarksForScope(scopePath, isZip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteBookmark(MediaBookmarkItem item) {
        if (item == null) return;
        if (item.isZip) {
            String permKey = "ZIP_BM:" + item.zipPath + "::" + item.trackName;
            removeEntryFromKey(permKey, item.positionMs);
            if (item.trackPath != null && !item.trackPath.isEmpty()) {
                removeEntryFromKey(item.trackPath, item.positionMs);
            }
        } else {
            if (item.folderPath != null && !item.folderPath.isEmpty()) {
                String permKey = "DIR_BM:" + item.folderPath + "::" + item.trackName;
                removeEntryFromKey(permKey, item.positionMs);
            }
            if (item.trackPath != null && !item.trackPath.isEmpty()) {
                removeEntryFromKey(item.trackPath, item.positionMs);
            } else if (item.folderPath != null && item.trackName != null) {
                String fullPath = new File(item.folderPath, item.trackName).getAbsolutePath();
                removeEntryFromKey(fullPath, item.positionMs);
            }
        }
        announceAccessibility("Bookmark deleted: " + item.bookmarkTitle);
        Toast.makeText(this, "Bookmark deleted", Toast.LENGTH_SHORT).show();
    }

    private void removeEntryFromKey(String key, long positionMs) {
        String currentVal = bkPrefs.getString(key, null);
        if (currentVal == null || currentVal.isEmpty()) return;
        String[] entries = currentVal.split("\\|");
        StringBuilder sb = new StringBuilder();
        for (String e : entries) {
            String[] parts = e.split("<kts>");
            if (parts.length == 2) {
                try {
                    long pos = Long.parseLong(parts[1].trim());
                    if (pos == positionMs) {
                        continue;
                    }
                } catch (Exception ignored) {}
            }
            if (sb.length() > 0) sb.append("|");
            sb.append(e);
        }
        if (sb.length() == 0) {
            bkPrefs.edit().remove(key).apply();
        } else {
            bkPrefs.edit().putString(key, sb.toString()).apply();
        }
    }

    private void clearAllBookmarksForScope(String scopePath, boolean isZip) {
        if (isZip) {
            String zipPrefix = "ZIP_BM:" + scopePath + "::";
            Map<String, ?> all = bkPrefs.getAll();
            SharedPreferences.Editor ed = bkPrefs.edit();
            for (String k : all.keySet()) {
                if (k.startsWith(zipPrefix)) {
                    ed.remove(k);
                }
            }
            File zipFile = new File(scopePath);
            File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
            if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
                File alt = ZipMediaManager.findExtractedCacheDir(this, scopePath);
                if (alt != null) cacheDir = alt;
            }
            if (cacheDir != null && cacheDir.exists()) {
                List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
                for (File t : tracks) {
                    ed.remove(t.getAbsolutePath());
                }
            }
            ed.apply();
        } else {
            String dirPrefix = "DIR_BM:" + scopePath + "::";
            Map<String, ?> all = bkPrefs.getAll();
            SharedPreferences.Editor ed = bkPrefs.edit();
            for (String k : all.keySet()) {
                if (k.startsWith(dirPrefix)) {
                    ed.remove(k);
                }
            }
            File folder = new File(scopePath);
            if (folder.exists() && folder.isDirectory()) {
                File[] files = folder.listFiles();
                if (files != null) {
                    for (File f : files) {
                        ed.remove(f.getAbsolutePath());
                    }
                }
            }
            ed.apply();
        }
        announceAccessibility("All bookmarks cleared for this " + (isZip ? "ZIP" : "folder"));
        Toast.makeText(this, "All bookmarks cleared", Toast.LENGTH_SHORT).show();
    }

    private void jumpToBookmark(MediaBookmarkItem item) {
        if (item == null) return;
        boolean isSameTrack = false;
        if (mediaPlayer != null && !currentPlayingFile.isEmpty()) {
            if (item.isZip) {
                String curName = new File(currentPlayingFile).getName();
                if (curName.equalsIgnoreCase(item.trackName)) {
                    isSameTrack = true;
                }
            } else {
                if (item.trackPath != null && item.trackPath.equals(currentPlayingFile)) {
                    isSameTrack = true;
                } else {
                    String curName = new File(currentPlayingFile).getName();
                    String curParent = new File(currentPlayingFile).getParent();
                    if (curName.equalsIgnoreCase(item.trackName) && (item.folderPath == null || item.folderPath.equals(curParent))) {
                        isSameTrack = true;
                    }
                }
            }
        }

        if (isSameTrack) {
            fadeAudio(1.0f, 0.0f, 180, () -> {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(item.positionMs);
                }
                fadeAudio(0.0f, 1.0f, 220, null);
                announceAccessibility("Playing " + item.bookmarkTitle + " in " + item.trackName + " at " + formatTime((int) item.positionMs));
                Toast.makeText(MainActivity.this, item.trackName + " (" + formatTime((int) item.positionMs) + ")", Toast.LENGTH_SHORT).show();
            });
            return;
        }

        if (item.isZip) {
            int targetIdx = -1;
            if (item.zipPath.equals(currentZipFilePath) && !currentAudioList.isEmpty()) {
                for (int i = 0; i < currentAudioList.size(); i++) {
                    if (new File(currentAudioList.get(i)).getName().equalsIgnoreCase(item.trackName)) {
                        targetIdx = i;
                        break;
                    }
                }
            }

            if (targetIdx != -1 && mediaPlayer != null && mediaPlayer.getMediaItemCount() > targetIdx) {
                final int finalIdx = targetIdx;
                fadeAudio(1.0f, 0.0f, 180, () -> {
                    if (mediaPlayer != null) {
                        saveCurrentPosition();
                        mediaPlayer.seekTo(finalIdx, item.positionMs);
                        mediaPlayer.play();
                    }
                    fadeAudio(0.0f, 1.0f, 220, null);
                    announceAccessibility("Playing " + item.bookmarkTitle + " in " + item.trackName + " at " + formatTime((int) item.positionMs));
                    Toast.makeText(MainActivity.this, item.trackName + " (" + formatTime((int) item.positionMs) + ")", Toast.LENGTH_SHORT).show();
                });
            } else {
                File zipFile = new File(item.zipPath);
                playZipTrackByName(zipFile, item.trackName, item.positionMs, item.bookmarkTitle);
            }
        } else {
            String targetPath = (item.trackPath != null && new File(item.trackPath).exists())
                    ? item.trackPath
                    : (item.folderPath != null ? new File(item.folderPath, item.trackName).getAbsolutePath() : null);

            if (targetPath != null) {
                int targetIdx = -1;
                if (!currentAudioList.isEmpty() && currentAudioList.contains(targetPath)) {
                    targetIdx = currentAudioList.indexOf(targetPath);
                }

                if (targetIdx != -1 && mediaPlayer != null && mediaPlayer.getMediaItemCount() > targetIdx) {
                    final int finalIdx = targetIdx;
                    fadeAudio(1.0f, 0.0f, 180, () -> {
                        if (mediaPlayer != null) {
                            saveCurrentPosition();
                            mediaPlayer.seekTo(finalIdx, item.positionMs);
                            mediaPlayer.play();
                        }
                        fadeAudio(0.0f, 1.0f, 220, null);
                        announceAccessibility("Playing " + item.bookmarkTitle + " in " + item.trackName + " at " + formatTime((int) item.positionMs));
                        Toast.makeText(MainActivity.this, item.trackName + " (" + formatTime((int) item.positionMs) + ")", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    if (item.folderPath != null) {
                        File folder = new File(item.folderPath);
                        File[] afs = folder.listFiles(f -> !f.isDirectory() && ZipMediaManager.isAudioFile(f.getName()));
                        if (afs != null && afs.length > 0) {
                            Arrays.sort(afs, ZipMediaManager.FILE_NATURAL_ORDER_COMPARATOR);
                            currentAudioList.clear();
                            for (File af : afs) currentAudioList.add(af.getAbsolutePath());
                        }
                    }
                    executePlayAudioFile(targetPath, item.positionMs);
                    announceAccessibility("Playing " + item.bookmarkTitle + " in " + item.trackName + " at " + formatTime((int) item.positionMs));
                    Toast.makeText(MainActivity.this, item.trackName + " (" + formatTime((int) item.positionMs) + ")", Toast.LENGTH_SHORT).show();
                }
            } else {
                announceAccessibility("Track file not found");
                Toast.makeText(MainActivity.this, "Track file not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void playZipTrackByName(File zipFile, String trackName, long positionMs, String bookmarkTitle) {
        if (zipFile == null || !zipFile.exists()) {
            announceAccessibility("ZIP file not found");
            Toast.makeText(this, "ZIP file not found", Toast.LENGTH_SHORT).show();
            return;
        }

        File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
        if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            File alt = ZipMediaManager.findExtractedCacheDir(this, zipFile.getAbsolutePath());
            if (alt != null) cacheDir = alt;
        }

        if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                int foundIdx = 0;
                for (int i = 0; i < tracks.size(); i++) {
                    if (tracks.get(i).getName().equalsIgnoreCase(trackName)) {
                        foundIdx = i;
                        break;
                    }
                }
                playZipTracks(zipFile.getName(), zipFile.getAbsolutePath(), tracks, foundIdx, positionMs);
                announceAccessibility("Playing " + bookmarkTitle + " in " + trackName + " at " + formatTime((int) positionMs));
                return;
            }
        }

        AlertDialog progressDialog = createExtractionProgressDialog(zipFile.getName());
        progressDialog.show();
        announceAccessibility("Extracting ZIP archive " + zipFile.getName() + ". Please wait.");

        ZipMediaManager.extractZipAsync(this, zipFile, new ZipMediaManager.ExtractionCallback() {
            @Override
            public void onProgress(int currentTrack, int totalTracks, String tName) {
                if (progressDialog.isShowing()) {
                    TextView tvMsg = progressDialog.findViewById(android.R.id.message);
                    if (tvMsg != null) {
                        tvMsg.setText(totalTracks > 0 ?
                                "Extracting (" + currentTrack + "/" + totalTracks + "): " + tName :
                                "Extracting track " + currentTrack + ": " + tName);
                    }
                }
            }

            @Override
            public void onSuccess(File cDir, List<File> audioTracks) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                updateZipCacheUI();
                int fIdx = 0;
                for (int i = 0; i < audioTracks.size(); i++) {
                    if (audioTracks.get(i).getName().equalsIgnoreCase(trackName)) {
                        fIdx = i;
                        break;
                    }
                }
                playZipTracks(zipFile.getName(), zipFile.getAbsolutePath(), audioTracks, fIdx, positionMs);
                announceAccessibility("Playing " + bookmarkTitle + " in " + trackName + " at " + formatTime((int) positionMs));
            }

            @Override
            public void onError(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility(errorMessage);
            }
        });
    }

    private void setupListeners() {
        tabPlayer.setOnClickListener(v -> switchTab(1));
        tabLibrary.setOnClickListener(v -> {
            switchTab(2);
            libraryMode = 0;
            updateLibraryTabColors();
            loadLibraryData("history");
            announceAccessibility("Showing History");
        });
        tabSettings.setOnClickListener(v -> switchTab(3));

        listViewFiles.setOnItemClickListener((parent, view, position, id) -> {
            String path = currentDisplayPaths.get(position);
            if ("STORAGE_ROOT".equals(path)) {
                loadDirectory("STORAGE_ROOT");
                return;
            }
            File f = new File(path);

            if (isBrowsingZip && position == 0 && currentDisplayNames.get(0).equals("Up")) {
                isBrowsingZip = false;
                loadDirectory(zipSourceFolderPath);
                announceAccessibility("Back to folder " + new File(zipSourceFolderPath).getName());
                return;
            }

            if (isBrowsingZip) {
                int trackIndex = position - 1;
                File zipFile = new File(currentZipFilePath);
                File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
                if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
                    File alt = ZipMediaManager.findExtractedCacheDir(this, currentZipFilePath);
                    if (alt != null) cacheDir = alt;
                }
                List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
                if (trackIndex >= 0 && trackIndex < tracks.size()) {
                    playZipTracks(currentZipName, currentZipFilePath, tracks, trackIndex);
                }
                return;
            }

            if (isBrowsingDrive && position == 0 && currentDisplayNames.get(0).equals("Up")) {
                isBrowsingDrive = false;
                String target = (driveSourceFolderPath != null && !driveSourceFolderPath.isEmpty())
                        ? driveSourceFolderPath
                        : Environment.getExternalStorageDirectory().getAbsolutePath();
                loadDirectory(target);
                announceAccessibility("Back to folder " + new File(target).getName());
                return;
            }

            if (isBrowsingDrive && "DRIVE_FOLDER_OPTIONS".equals(currentDisplayPaths.get(position))) {
                showDriveFolderOptionsDialog();
                return;
            }

            if (isBrowsingDrive) {
                int trackIndex = position - 2;
                if (trackIndex >= 0 && trackIndex < currentDriveTracks.size()) {
                    if (mediaPlayer != null && mediaPlayer.getMediaItemCount() == currentDriveTracks.size()) {
                        saveCurrentPosition();
                        mediaPlayer.seekTo(trackIndex, 0);
                        mediaPlayer.play();
                        fadeAudio(0.0f, 1.0f, 400, null);
                        resetAB();
                        updateMediaSessionState(PlaybackState.STATE_PLAYING);
                        String trackTitle = currentDriveTracks.get(trackIndex).title;
                        tvNowPlaying.setText("🎵 [Drive: " + currentDriveFolderName + "] " + trackTitle);
                        announceAccessibility("Playing Track " + (trackIndex + 1) + " of " + currentDriveTracks.size() + ": " + trackTitle);
                    } else {
                        playDriveTracks(currentDriveFolderName, currentDriveFolderUrl, new ArrayList<>(currentDriveTracks), trackIndex);
                    }
                }
                return;
            }

            if (currentDisplayNames.get(position).startsWith("Up")) {
                if (!folderBackStack.isEmpty()) {
                    String prev = folderBackStack.pop();
                    handleFolderBackStep(prev);
                    return;
                }
            }

            boolean isZip = ZipMediaManager.isZipFile(f.getName());
            boolean isAudio = ZipMediaManager.isAudioFile(f.getName());
            boolean isDir = (!isZip && !isAudio) || f.isDirectory();

            if (isDir) {
                if (!currentDisplayNames.get(position).startsWith("Up")) {
                    if (currentDir != null && !currentDir.isEmpty()) {
                        folderBackStack.push(currentDir);
                    }
                }
                loadDirectory(path);
                announceAccessibility("Opened folder " + f.getName());
            } else if (isZip) {
                showZipOptionsDialog(f);
            } else {
                currentAudioList.clear();
                for (String p : currentDisplayPaths) {
                    if (ZipMediaManager.isAudioFile(new File(p).getName())) {
                        currentAudioList.add(p);
                    }
                }
                playAudioFile(path);
            }
        });

        btnPlayMode.setOnClickListener(v -> {
            playMode = (playMode + 1) % 4;
            bkPrefs.edit().putInt("persistent_play_mode", playMode).apply();
            updateExoPlayerPlayMode(true);
        });

        btnPlay.setOnClickListener(v -> {
            if (mediaPlayer == null) return;
            if (mediaPlayer.isPlaying()) {
                fadeAudio(-1f, 0.0f, 300, () -> {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        saveCurrentPosition();
                        updateMediaSessionState(PlaybackState.STATE_PAUSED);
                        announceAccessibility("Paused");
                    }
                });
            } else {
                if (mediaPlayer.getMediaItemCount() > 0) {
                    if (mediaPlayer.getPlaybackState() == Player.STATE_IDLE) {
                        mediaPlayer.prepare();
                    } else if (mediaPlayer.getPlaybackState() == Player.STATE_ENDED) {
                        mediaPlayer.seekTo(0, 0);
                    }
                    int currentPos = (int) mediaPlayer.getCurrentPosition();
                    if (currentPos > 3000) mediaPlayer.seekTo(currentPos - 3000);
                    else mediaPlayer.seekTo(0);

                    mediaPlayer.setVolume(0f);
                    mediaPlayer.play();
                    fadeAudio(0.0f, 1.0f, 400, null);

                    updateMediaSessionState(PlaybackState.STATE_PLAYING);
                    announceAccessibility("Playing");
                } else {
                    resumeGlobalLastPlayed();
                }
            }
        });

        btnPlay.setOnLongClickListener(v -> {
            if (!currentPlayingFile.isEmpty() && mediaPlayer != null) {
                fadeAudio(-1f, 0.0f, 200, () -> {
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(0);
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.play();
                            updateMediaSessionState(PlaybackState.STATE_PLAYING);
                        }
                    }
                    fadeAudio(0.0f, 1.0f, 250, null);
                    announceAccessibility("Playing from the beginning");
                });
            }
            return true;
        });

        btnNext.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.hasNextMediaItem()) {
                if (mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
                    fadeAudio(-1f, 0.0f, 250, () -> {
                        long delay = gapValues[currentGapIndex];
                        Runnable switchTrack = () -> {
                            if (mediaPlayer != null) {
                                mediaPlayer.setVolume(0f);
                                mediaPlayer.seekToNextMediaItem();
                                handler.postDelayed(() -> fadeAudio(0.0f, 1.0f, 400, null), 60);
                            }
                        };
                        if (delay > 0) {
                            handler.postDelayed(switchTrack, delay);
                        } else {
                            switchTrack.run();
                        }
                    });
                } else {
                    mediaPlayer.seekToNextMediaItem();
                    mediaPlayer.setVolume(1.0f);
                }
            } else {
                if (playNextQueueItem()) {
                    // Next queue item played
                } else if (bkPrefs.getBoolean("auto_play_next_folder", false) && playNextSiblingFolder()) {
                    // Auto-played next folder
                } else {
                    announceAccessibility("No next track available");
                }
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (mediaPlayer != null && mediaPlayer.hasPreviousMediaItem()) {
                if (mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
                    fadeAudio(-1f, 0.0f, 250, () -> {
                        long delay = gapValues[currentGapIndex];
                        Runnable switchTrack = () -> {
                            if (mediaPlayer != null) {
                                mediaPlayer.setVolume(0f);
                                mediaPlayer.seekToPreviousMediaItem();
                                handler.postDelayed(() -> fadeAudio(0.0f, 1.0f, 400, null), 60);
                            }
                        };
                        if (delay > 0) {
                            handler.postDelayed(switchTrack, delay);
                        } else {
                            switchTrack.run();
                        }
                    });
                } else {
                    mediaPlayer.seekToPreviousMediaItem();
                    mediaPlayer.setVolume(1.0f);
                }
            } else {
                announceAccessibility("No previous track available");
            }
        });
    }

    private void loadZipDirectory(File cacheDir) {
        currentDir = cacheDir.getAbsolutePath();
        tvCurrentFolder.setText("ZIP: " + currentZipName);
        currentDisplayNames.clear();
        currentDisplayPaths.clear();

        currentDisplayNames.add("Up");
        currentDisplayPaths.add(zipSourceFolderPath);

        List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
        currentAudioList.clear();
        for (int i = 0; i < tracks.size(); i++) {
            File t = tracks.get(i);
            currentDisplayNames.add((i + 1) + ". " + t.getName());
            currentDisplayPaths.add(t.getAbsolutePath());
            currentAudioList.add(t.getAbsolutePath());
        }
        fileListAdapter.notifyDataSetChanged();
        updateZipResumeButton(currentZipFilePath, cacheDir);
    }

    public static class StorageInfo {
        public final String name;
        public final String path;
        public final boolean isPrimary;

        public StorageInfo(String name, String path, boolean isPrimary) {
            this.name = name;
            this.path = path;
            this.isPrimary = isPrimary;
        }
    }

    private List<StorageInfo> getAvailableStorageVolumes() {
        List<StorageInfo> list = new ArrayList<>();
        Set<String> seenPaths = new HashSet<>();

        File primary = Environment.getExternalStorageDirectory();
        if (primary != null && primary.exists()) {
            list.add(new StorageInfo("📱 Internal Storage", primary.getAbsolutePath(), true));
            seenPaths.add(primary.getAbsolutePath());
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                StorageManager sm = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
                if (sm != null) {
                    List<StorageVolume> volumes = sm.getStorageVolumes();
                    for (StorageVolume vol : volumes) {
                        File dir = null;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            dir = vol.getDirectory();
                        } else {
                            try {
                                java.lang.reflect.Method getPath = vol.getClass().getMethod("getPath");
                                String p = (String) getPath.invoke(vol);
                                if (p != null) dir = new File(p);
                            } catch (Exception ignored) {}
                        }
                        if (dir != null && dir.exists() && !seenPaths.contains(dir.getAbsolutePath())) {
                            String desc = vol.getDescription(this);
                            String icon = vol.isRemovable() ? "💾 " : "📁 ";
                            list.add(new StorageInfo(icon + (desc != null ? desc : dir.getName()), dir.getAbsolutePath(), vol.isPrimary()));
                            seenPaths.add(dir.getAbsolutePath());
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying StorageManager", e);
            }
        }

        File[] extDirs = ContextCompat.getExternalFilesDirs(this, null);
        if (extDirs != null) {
            for (File f : extDirs) {
                if (f != null) {
                    String p = f.getAbsolutePath();
                    int idx = p.indexOf("/Android/");
                    if (idx > 0) {
                        String root = p.substring(0, idx);
                        if (!seenPaths.contains(root) && new File(root).exists()) {
                            list.add(new StorageInfo("💾 SD Card (" + new File(root).getName() + ")", root, false));
                            seenPaths.add(root);
                        }
                    }
                }
            }
        }

        File storageDir = new File("/storage");
        if (storageDir.exists() && storageDir.isDirectory()) {
            File[] subs = storageDir.listFiles();
            if (subs != null) {
                for (File sub : subs) {
                    if (sub.isDirectory() && !sub.getName().equals("self") && !sub.getName().equals("knox-emulated")) {
                        String abs = sub.getAbsolutePath();
                        if (!seenPaths.contains(abs) && !abs.equals("/storage/emulated")) {
                            File emulated0 = new File(sub, "0");
                            if (sub.getName().equals("emulated") && emulated0.exists()) {
                                // already covered
                            } else {
                                list.add(new StorageInfo("💾 " + sub.getName(), abs, false));
                                seenPaths.add(abs);
                            }
                        }
                    }
                }
            }
        }

        return list;
    }

    private void showStorageSelectionDialog() {
        List<StorageInfo> storages = getAvailableStorageVolumes();
        if (storages.isEmpty()) {
            Toast.makeText(this, "No storage devices found", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] titles = new String[storages.size()];
        for (int i = 0; i < storages.size(); i++) {
            titles[i] = storages.get(i).name;
        }
        new AlertDialog.Builder(this)
                .setTitle("Select Storage Device")
                .setItems(titles, (dialog, which) -> {
                    StorageInfo selected = storages.get(which);
                    folderBackStack.clear();
                    loadDirectory(selected.path);
                    announceAccessibility("Switched to " + selected.name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadDirectory(String path) {
        isBrowsingZip = false;
        isBrowsingDrive = false;
        currentDir = path;
        currentDisplayNames.clear();
        currentDisplayPaths.clear();

        if ("STORAGE_ROOT".equals(path)) {
            tvCurrentFolder.setText("Storage Devices");
            if (btnResumeFolder != null) btnResumeFolder.setVisibility(View.GONE);
            List<StorageInfo> storages = getAvailableStorageVolumes();
            for (StorageInfo s : storages) {
                currentDisplayNames.add(s.name);
                currentDisplayPaths.add(s.path);
            }
            fileListAdapter.notifyDataSetChanged();
            announceAccessibility("Showing storage devices");
            return;
        }

        List<StorageInfo> storages = getAvailableStorageVolumes();
        boolean isStorageRoot = false;
        String storageDesc = "";
        for (StorageInfo s : storages) {
            if (path.equals(s.path) || path.equals(s.path + "/")) {
                isStorageRoot = true;
                storageDesc = s.name;
                break;
            }
        }

        File currentFolderFile = new File(path);
        if (isStorageRoot && !storageDesc.isEmpty()) {
            tvCurrentFolder.setText("Folder: " + storageDesc);
        } else {
            tvCurrentFolder.setText("Folder: " + currentFolderFile.getName());
        }

        if (!isStorageRoot) {
            currentDisplayNames.add("Up");
            currentDisplayPaths.add(currentFolderFile.getParent());
        } else if (storages.size() > 1) {
            currentDisplayNames.add("Up (Storage Devices)");
            currentDisplayPaths.add("STORAGE_ROOT");
        }

        File dir = new File(path);
        File[] files = (dir.exists() && dir.isDirectory()) ? dir.listFiles() : null;

        boolean populated = false;
        if (files != null && files.length > 0) {
            populated = populateFromFileSystem(files);
        }

        if (!populated) {
            populateFromMediaStoreFallback(path, dir);
        }
        fileListAdapter.notifyDataSetChanged();
        updateFolderResumeButton(path);
    }

    private void updateFolderResumeButton(String path) {
        if (btnResumeFolder == null) return;
        if ("STORAGE_ROOT".equals(path) || isBrowsingDrive) {
            btnResumeFolder.setVisibility(View.GONE);
            return;
        }
        if (isBrowsingZip) {
            updateZipResumeButton(currentZipFilePath, new File(currentDir));
            return;
        }
        String lastFileInFolder = memPrefs.getString("folder_last_file_" + path, "");
        int lastPosInFolder = memPrefs.getInt("folder_last_pos_" + path, 0);
        if (!lastFileInFolder.isEmpty() && new File(lastFileInFolder).exists()) {
            String name = new File(lastFileInFolder).getName();
            btnResumeFolder.setVisibility(View.VISIBLE);
            btnResumeFolder.setText("▶ Resume: " + name + " (" + formatTime(lastPosInFolder) + ")");
            btnResumeFolder.setContentDescription("Resume " + name + " at " + formatTimeSpeech(lastPosInFolder));
            btnResumeFolder.setOnClickListener(v -> playExplorerAudioFile(lastFileInFolder));
        } else {
            btnResumeFolder.setVisibility(View.GONE);
        }
    }

    private void updateZipResumeButton(String zipPath, File cacheDir) {
        if (btnResumeFolder == null) return;
        if (zipPath == null || zipPath.isEmpty()) {
            btnResumeFolder.setVisibility(View.GONE);
            return;
        }
        String lastFileInZip = memPrefs.getString("folder_last_file_zip_" + zipPath, "");
        int lastPosInZip = memPrefs.getInt("folder_last_pos_zip_" + zipPath, 0);
        if (!lastFileInZip.isEmpty() && new File(lastFileInZip).exists()) {
            String name = new File(lastFileInZip).getName();
            btnResumeFolder.setVisibility(View.VISIBLE);
            btnResumeFolder.setText("▶ Resume ZIP: " + name + " (" + formatTime(lastPosInZip) + ")");
            btnResumeFolder.setContentDescription("Resume ZIP archive " + currentZipName + ", track " + name + " at " + formatTimeSpeech(lastPosInZip));
            btnResumeFolder.setOnClickListener(v -> playExplorerAudioFile(lastFileInZip));
        } else {
            btnResumeFolder.setVisibility(View.GONE);
        }
    }

    private void handleFolderBackStep(String prev) {
        if (prev == null || prev.isEmpty()) return;
        if (prev.startsWith("BOOKSHELF:")) {
            String shelf = prev.substring("BOOKSHELF:".length());
            switchTab(2);
            libraryMode = 5;
            currentShelfName = shelf;
            loadLibraryData("bookshelf_" + currentShelfName);
            announceAccessibility("Back to bookshelf " + currentShelfName);
        } else if (prev.startsWith("PLAYLIST:")) {
            String pl = prev.substring("PLAYLIST:".length());
            switchTab(2);
            libraryMode = 3;
            currentPlaylistName = pl;
            loadLibraryData("playlist_" + currentPlaylistName);
            announceAccessibility("Back to playlist " + currentPlaylistName);
        } else if ("FAVORITES".equals(prev)) {
            switchTab(2);
            libraryMode = 1;
            loadLibraryData("favorites");
            announceAccessibility("Back to Favorites");
        } else if ("HISTORY:FOLDERS".equals(prev)) {
            switchTab(2);
            libraryMode = 0;
            historyFilterMode = 1;
            loadHistoryData();
            announceAccessibility("Back to History Folders");
        } else if ("HISTORY:PLAYLISTS".equals(prev)) {
            switchTab(2);
            libraryMode = 0;
            historyFilterMode = 2;
            loadHistoryData();
            announceAccessibility("Back to History Playlists");
        } else {
            File f = new File(prev);
            if (f.exists() || "STORAGE_ROOT".equals(prev)) {
                loadDirectory(prev);
                announceAccessibility("Moved back to folder " + f.getName());
            } else if (!folderBackStack.isEmpty()) {
                handleFolderBackStep(folderBackStack.pop());
            } else {
                loadDirectory(Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        }
    }

    private boolean populateFromFileSystem(File[] files) {
        List<File> pinnedFolders = new ArrayList<>();
        List<File> regularFolders = new ArrayList<>();
        List<File> zipList = new ArrayList<>();
        List<File> fileList = new ArrayList<>();

        String pinnedStr = libPrefs.getString("pinned_folders", "");
        List<String> pinnedList = new ArrayList<>();
        for (String s : pinnedStr.split("\\|")) {
            if (!s.isEmpty()) pinnedList.add(s);
        }

        boolean filterMediaOnly = bkPrefs.getBoolean("filter_only_media_folders", true);

        for (File f : files) {
            if (!f.isHidden()) {
                if (f.isDirectory()) {
                    boolean isPinned = pinnedList.contains(f.getAbsolutePath());
                    if (!filterMediaOnly || isPinned || folderContainsSupportedMedia(f)) {
                        if (isPinned) {
                            pinnedFolders.add(f);
                        } else {
                            regularFolders.add(f);
                        }
                    }
                } else if (ZipMediaManager.isZipFile(f.getName())) {
                    zipList.add(f);
                } else if (ZipMediaManager.isAudioFile(f.getName())) {
                    fileList.add(f);
                }
            }
        }

        if (pinnedFolders.isEmpty() && regularFolders.isEmpty() && zipList.isEmpty() && fileList.isEmpty()) {
            return false;
        }

        Comparator<File> fileComparator = (f1, f2) -> {
            if (currentSortMode == 0) return ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(f1.getName(), f2.getName());
            if (currentSortMode == 1) return ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(f2.getName(), f1.getName());
            if (currentSortMode == 2) return Long.compare(f2.lastModified(), f1.lastModified());
            if (currentSortMode == 3) return Long.compare(f1.lastModified(), f2.lastModified());
            if (currentSortMode == 4) return Long.compare(f2.length(), f1.length());
            if (currentSortMode == 5) return Long.compare(f1.length(), f2.length());
            if (currentSortMode == 6) return getFileExtension(f1).compareToIgnoreCase(getFileExtension(f2));
            if (currentSortMode == 7) return getFileExtension(f2).compareToIgnoreCase(getFileExtension(f1));
            return 0;
        };

        pinnedFolders.sort(fileComparator);
        regularFolders.sort(fileComparator);
        zipList.sort(fileComparator);
        fileList.sort(fileComparator);

        for (File f : pinnedFolders) {
            currentDisplayNames.add("📌 " + f.getName());
            currentDisplayPaths.add(f.getAbsolutePath());
        }
        for (File f : regularFolders) {
            currentDisplayNames.add(f.getName());
            currentDisplayPaths.add(f.getAbsolutePath());
        }
        for (File f : zipList) {
            currentDisplayNames.add("📦 " + f.getName());
            currentDisplayPaths.add(f.getAbsolutePath());
        }
        for (File f : fileList) {
            currentDisplayNames.add(f.getName());
            currentDisplayPaths.add(f.getAbsolutePath());
        }
        return true;
    }

    private void populateFromMediaStoreFallback(String path, File dir) {
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean isRoot = path.equals(rootPath) || path.equals(rootPath + "/");

        Set<String> discoveredFolderNames = new HashSet<>();
        Map<String, String> folderPathMap = new HashMap<>();
        List<String> discoveredAudioPaths = new ArrayList<>();
        List<String> discoveredZipPaths = new ArrayList<>();

        String pinnedStr = libPrefs.getString("pinned_folders", "");
        List<String> pinnedList = new ArrayList<>();
        for (String s : pinnedStr.split("\\|")) {
            if (!s.isEmpty()) pinnedList.add(s);
        }

        boolean filterMediaOnly = bkPrefs.getBoolean("filter_only_media_folders", true);

        if (isRoot) {
            String[] standardDirs = new String[]{
                    "Music", "Download", "Podcasts", "Audiobooks", "Recordings", "Documents", "DCIM", "Movies"
            };
            for (String sDir : standardDirs) {
                File sub = new File(dir, sDir);
                if (!filterMediaOnly || folderContainsSupportedMedia(sub)) {
                    discoveredFolderNames.add(sDir);
                    folderPathMap.put(sDir, sub.getAbsolutePath());
                }
            }
        }

        String normPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        String prefix = normPath + "/";

        try {
            String[] proj = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME};
            String sel = MediaStore.Audio.Media.DATA + " LIKE ?";
            String[] selArgs = new String[]{prefix + "%"};
            try (Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, selArgs, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String d = c.getString(0);
                        if (d != null && d.startsWith(prefix)) {
                            String rel = d.substring(prefix.length());
                            int slashIdx = rel.indexOf("/");
                            if (slashIdx != -1) {
                                String folderName = rel.substring(0, slashIdx);
                                discoveredFolderNames.add(folderName);
                                folderPathMap.put(folderName, prefix + folderName);
                            } else {
                                if (!discoveredAudioPaths.contains(d)) {
                                    discoveredAudioPaths.add(d);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaStore audio query fallback error", e);
        }

        try {
            String[] proj = {MediaStore.Video.Media.DATA, MediaStore.Video.Media.DISPLAY_NAME};
            String sel = MediaStore.Video.Media.DATA + " LIKE ?";
            String[] selArgs = new String[]{prefix + "%"};
            try (Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, sel, selArgs, null)) {
                if (c != null) {
                    while (c.moveToNext()) {
                        String d = c.getString(0);
                        if (d != null && d.startsWith(prefix)) {
                            String rel = d.substring(prefix.length());
                            int slashIdx = rel.indexOf("/");
                            if (slashIdx != -1) {
                                String folderName = rel.substring(0, slashIdx);
                                discoveredFolderNames.add(folderName);
                                folderPathMap.put(folderName, prefix + folderName);
                            } else {
                                if (!discoveredAudioPaths.contains(d)) {
                                    discoveredAudioPaths.add(d);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "MediaStore video query fallback error", e);
        }

        for (String p : globalAudioPaths) {
            if (p != null && p.startsWith(prefix)) {
                String rel = p.substring(prefix.length());
                int slashIdx = rel.indexOf("/");
                if (slashIdx != -1) {
                    String folderName = rel.substring(0, slashIdx);
                    discoveredFolderNames.add(folderName);
                    folderPathMap.put(folderName, prefix + folderName);
                } else {
                    if (!discoveredAudioPaths.contains(p)) {
                        discoveredAudioPaths.add(p);
                    }
                }
            }
        }

        try {
            File[] rawFiles = dir.listFiles();
            if (rawFiles != null) {
                for (File f : rawFiles) {
                    if (!f.isHidden()) {
                        if (f.isDirectory()) {
                            if (!filterMediaOnly || pinnedList.contains(f.getAbsolutePath()) || folderContainsSupportedMedia(f)) {
                                discoveredFolderNames.add(f.getName());
                                folderPathMap.put(f.getName(), f.getAbsolutePath());
                            }
                        } else if (ZipMediaManager.isZipFile(f.getName())) {
                            if (!discoveredZipPaths.contains(f.getAbsolutePath())) {
                                discoveredZipPaths.add(f.getAbsolutePath());
                            }
                        } else if (ZipMediaManager.isAudioFile(f.getName())) {
                            if (!discoveredAudioPaths.contains(f.getAbsolutePath())) {
                                discoveredAudioPaths.add(f.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        List<String> pinnedFolders = new ArrayList<>();
        List<String> regularFolders = new ArrayList<>();
        for (String fName : discoveredFolderNames) {
            String fPath = folderPathMap.get(fName);
            if (fPath != null && pinnedList.contains(fPath)) {
                pinnedFolders.add(fName);
            } else {
                regularFolders.add(fName);
            }
        }

        pinnedFolders.sort(ZipMediaManager.NATURAL_ORDER_COMPARATOR);
        regularFolders.sort(ZipMediaManager.NATURAL_ORDER_COMPARATOR);

        Comparator<String> pathComparator = (p1, p2) -> {
            String n1 = new File(p1).getName();
            String n2 = new File(p2).getName();
            if (currentSortMode == 0) return ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(n1, n2);
            if (currentSortMode == 1) return ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(n2, n1);
            File f1 = new File(p1);
            File f2 = new File(p2);
            if (currentSortMode == 2) return Long.compare(f2.lastModified(), f1.lastModified());
            if (currentSortMode == 3) return Long.compare(f1.lastModified(), f2.lastModified());
            if (currentSortMode == 4) return Long.compare(f2.length(), f1.length());
            if (currentSortMode == 5) return Long.compare(f1.length(), f2.length());
            if (currentSortMode == 6) return getFileExtension(f1).compareToIgnoreCase(getFileExtension(f2));
            if (currentSortMode == 7) return getFileExtension(f2).compareToIgnoreCase(getFileExtension(f1));
            return 0;
        };

        discoveredAudioPaths.sort(pathComparator);
        discoveredZipPaths.sort(pathComparator);

        for (String fName : pinnedFolders) {
            currentDisplayNames.add("📌 " + fName);
            currentDisplayPaths.add(folderPathMap.get(fName));
        }
        for (String fName : regularFolders) {
            currentDisplayNames.add(fName);
            currentDisplayPaths.add(folderPathMap.get(fName));
        }
        for (String zPath : discoveredZipPaths) {
            currentDisplayNames.add("📦 " + new File(zPath).getName());
            currentDisplayPaths.add(zPath);
        }
        for (String aPath : discoveredAudioPaths) {
            currentDisplayNames.add(new File(aPath).getName());
            currentDisplayPaths.add(aPath);
        }
    }

    private void updateKnownMediaFolders() {
        Set<String> set = new HashSet<>();
        for (String p : globalAudioPaths) {
            if (p != null) {
                File parent = new File(p).getParentFile();
                while (parent != null) {
                    String pAbs = parent.getAbsolutePath();
                    if (pAbs.endsWith("/") && pAbs.length() > 1) {
                        pAbs = pAbs.substring(0, pAbs.length() - 1);
                    }
                    set.add(pAbs);
                    parent = parent.getParentFile();
                }
            }
        }
        knownMediaFoldersSet.clear();
        knownMediaFoldersSet.addAll(set);
        mediaFolderCache.clear();
    }

    private boolean folderContainsSupportedMedia(File folder) {
        if (folder == null || !folder.exists() || !folder.isDirectory()) return false;
        String folderPath = folder.getAbsolutePath();
        if (folderPath.endsWith("/") && folderPath.length() > 1) {
            folderPath = folderPath.substring(0, folderPath.length() - 1);
        }
        if (knownMediaFoldersSet.contains(folderPath)) {
            return true;
        }
        Boolean cached = mediaFolderCache.get(folderPath);
        if (cached != null) {
            return cached;
        }
        boolean hasMedia = checkFolderHasMediaOnDisk(folder, 3);
        mediaFolderCache.put(folderPath, hasMedia);
        if (hasMedia) {
            File parent = folder.getParentFile();
            while (parent != null) {
                String pAbs = parent.getAbsolutePath();
                if (pAbs.endsWith("/") && pAbs.length() > 1) {
                    pAbs = pAbs.substring(0, pAbs.length() - 1);
                }
                knownMediaFoldersSet.add(pAbs);
                parent = parent.getParentFile();
            }
        }
        return hasMedia;
    }

    private boolean checkFolderHasMediaOnDisk(File dir, int depth) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return false;
        String name = dir.getName();
        if (name.startsWith(".") || name.equalsIgnoreCase("Android")
                || name.equalsIgnoreCase("LOST.DIR") || name.equalsIgnoreCase(".Trash")
                || name.equalsIgnoreCase("System Volume Information")) {
            return false;
        }

        File[] children;
        try {
            children = dir.listFiles();
        } catch (Exception e) {
            return false;
        }
        if (children == null || children.length == 0) return false;

        for (File child : children) {
            if (!child.isDirectory() && !child.isHidden()) {
                String cName = child.getName();
                if (ZipMediaManager.isAudioFile(cName) || ZipMediaManager.isZipFile(cName)) {
                    return true;
                }
            }
        }

        if (depth > 0) {
            for (File child : children) {
                if (child.isDirectory() && !child.isHidden()) {
                    String cName = child.getName();
                    if (!cName.startsWith(".") && !cName.equalsIgnoreCase("Android")
                            && !cName.equalsIgnoreCase("LOST.DIR") && !cName.equalsIgnoreCase(".Trash")
                            && !cName.equalsIgnoreCase("System Volume Information")) {
                        if (checkFolderHasMediaOnDisk(child, depth - 1)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean hasMediaUnder(String folderPath) {
        if (folderPath == null) return false;
        return folderContainsSupportedMedia(new File(folderPath));
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) return "";
        return name.substring(lastIndexOf);
    }

    private void playNetworkStream(String rawUrl) {
        if (rawUrl == null || rawUrl.trim().isEmpty()) return;
        final String normalizedUrl = OnlineStreamManager.normalizeUrl(rawUrl);

        announceAccessibility("Connecting to stream. Please wait.");
        Toast.makeText(this, "Connecting to stream...", Toast.LENGTH_SHORT).show();

        OnlineStreamManager.resolveStreamAsync(normalizedUrl, new OnlineStreamManager.StreamResolveCallback() {
            @Override
            public void onResolved(@NonNull OnlineStreamManager.StreamInfo streamInfo) {
                try {
                    saveCurrentPosition();
                    currentAudioList.clear();

                    mediaPlayer.stop();
                    mediaPlayer.clearMediaItems();

                    MediaItem mediaItem = streamInfo.toMediaItem();
                    mediaPlayer.setMediaItem(mediaItem);
                    mediaPlayer.prepare();

                    mediaPlayer.setVolume(0f);
                    mediaPlayer.play();
                    fadeAudio(0.0f, 1.0f, 500, null);

                    resetAB();
                    updateMediaSessionState(PlaybackState.STATE_PLAYING);

                    mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));

                    currentPlayingFile = streamInfo.streamUrl;
                    String displayName = streamInfo.title != null && !streamInfo.title.isEmpty()
                            ? streamInfo.title
                            : OnlineStreamManager.getTitleFromUrl(streamInfo.originalUrl);
                    if (displayName == null || displayName.isEmpty()) {
                        displayName = streamInfo.originalUrl;
                    }

                    tvNowPlaying.setText("📻 " + displayName);
                    btnPlay.setText("Pause");

                    // Save stream to history
                    libPrefs.edit().putString("title_" + streamInfo.originalUrl, "📻 " + displayName).apply();
                    if (!streamInfo.originalUrl.equals(streamInfo.streamUrl)) {
                        libPrefs.edit().putString("title_" + streamInfo.streamUrl, "📻 " + displayName).apply();
                    }
                    addToLibrary("history", streamInfo.originalUrl);
                    addToLibrary("history_tracks", streamInfo.originalUrl);

                    announceAccessibility("Playing stream: " + displayName);

                } catch (Exception e) {
                    Log.e(TAG, "Stream Playback error", e);
                    announceAccessibility("Cannot play this stream");
                    Toast.makeText(MainActivity.this, "Cannot play this stream", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                announceAccessibility(errorMessage);
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileNameFromUri(Uri uri) {
        if (uri == null) return "Audio file";
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME}, null, null, null)) {
                if (c != null && c.moveToFirst()) {
                    int idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) name = c.getString(idx);
                }
            } catch (Exception ignored) {}
        }
        if (name == null || name.isEmpty()) {
            name = uri.getLastPathSegment();
        }
        if (name == null || name.isEmpty()) {
            name = "Audio file";
        }
        return name;
    }

    private void playExternalAudio(Uri audioUri) {
        if (audioUri == null) return;
        handleIncomingAudioUris(Collections.singletonList(audioUri));
    }

    private void handleIncomingAudioUris(List<Uri> uris) {
        if (uris == null || uris.isEmpty()) return;

        List<String> resolvedPaths = new ArrayList<>();
        for (Uri uri : uris) {
            if (uri == null) continue;
            File f = resolveOrCopyIncomingUri(uri);
            if (f != null && f.exists() && f.canRead()) {
                String path = f.getAbsolutePath();
                resolvedPaths.add(path);

                String title = f.getName();
                libPrefs.edit().putString("title_" + path, "🎵 " + title).apply();
                addToLibrary("history", path);
                addToLibrary("history_tracks", path);

                File parent = f.getParentFile();
                if (parent != null) {
                    addToLibrary("history_folders", parent.getAbsolutePath());
                    libPrefs.edit().putString("title_" + parent.getAbsolutePath(), "📁 " + parent.getName()).apply();
                }
            }
        }

        if (!resolvedPaths.isEmpty()) {
            switchTab(1);
            currentAudioList.clear();
            currentAudioList.addAll(resolvedPaths);
            playAudioFile(resolvedPaths.get(0));

            if (resolvedPaths.size() == 1) {
                String name = new File(resolvedPaths.get(0)).getName();
                announceAccessibility("Playing " + name);
                Toast.makeText(this, "Playing " + name, Toast.LENGTH_SHORT).show();
            } else {
                announceAccessibility("Playing " + resolvedPaths.size() + " shared tracks");
                Toast.makeText(this, "Playing " + resolvedPaths.size() + " shared tracks", Toast.LENGTH_SHORT).show();
            }
        } else {
            announceAccessibility("Cannot read shared audio file");
            Toast.makeText(this, "Cannot read shared audio file", Toast.LENGTH_SHORT).show();
        }
    }

    private File resolveOrCopyIncomingUri(Uri uri) {
        if (uri == null) return null;
        try {
            // 1. Direct file:// scheme
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                String p = uri.getPath();
                if (p != null) {
                    File f = new File(p);
                    if (f.exists() && f.canRead()) return f;
                }
            }

            // 2. Direct path from content:// query if accessible
            String directPath = getDirectPathFromContentUri(uri);
            if (directPath != null) {
                File f = new File(directPath);
                if (f.exists() && f.canRead()) return f;
            }

            // 3. Persistent destination in app storage
            File targetDir = getExternalFilesDir("SharedMedia");
            if (targetDir == null) targetDir = new File(getFilesDir(), "SharedMedia");
            if (!targetDir.exists()) targetDir.mkdirs();

            String fileName = getFileNameFromUri(uri);
            if (fileName == null || fileName.trim().isEmpty() || "Audio file".equals(fileName)) {
                fileName = "Shared_Audio_" + System.currentTimeMillis() + ".mp3";
            }
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            long uriSize = getUriFileSize(uri);
            File candidate = new File(targetDir, fileName);
            if (candidate.exists() && uriSize > 0 && candidate.length() == uriSize) {
                return candidate; // exact match already saved
            }

            File destFile = getUniqueDestinationFile(targetDir, fileName);
            try (InputStream in = getContentResolver().openInputStream(uri);
                 OutputStream out = new FileOutputStream(destFile)) {
                if (in == null) return null;
                byte[] buf = new byte[65536];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.flush();
            }
            if (destFile.exists() && destFile.length() > 0) {
                return destFile;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resolving/copying incoming URI: " + uri, e);
        }
        return null;
    }

    private String getDirectPathFromContentUri(Uri uri) {
        if (uri == null) return null;
        try {
            if ("content".equalsIgnoreCase(uri.getScheme())) {
                String[] proj = {MediaStore.Audio.Media.DATA};
                try (Cursor cursor = getContentResolver().query(uri, proj, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int idx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                        if (idx != -1) {
                            String p = cursor.getString(idx);
                            if (p != null && new File(p).exists()) {
                                return p;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private long getUriFileSize(Uri uri) {
        if (uri == null) return -1;
        try (Cursor c = getContentResolver().query(uri, new String[]{android.provider.OpenableColumns.SIZE}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (idx != -1) return c.getLong(idx);
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private File getUniqueDestinationFile(File dir, String fileName) {
        File file = new File(dir, fileName);
        if (!file.exists()) return file;

        String nameWithoutExt = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot != -1) {
            nameWithoutExt = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }

        int count = 1;
        while (file.exists() && count < 1000) {
            file = new File(dir, nameWithoutExt + "_" + count + ext);
            count++;
        }
        return file;
    }

    private Uri buildMediaUri(String path) {
        if (path == null) return Uri.EMPTY;
        if (path.startsWith("content://") || path.startsWith("http://") || path.startsWith("https://") || path.startsWith("file://")) {
            return Uri.parse(path);
        }
        File f = new File(path);
        if (f.canRead()) {
            return Uri.fromFile(f);
        }
        Uri contentUri = getContentUriForFilePath(path);
        if (contentUri != null) {
            return contentUri;
        }
        return Uri.fromFile(f);
    }

    private Uri getContentUriForFilePath(String filePath) {
        if (filePath == null) return null;
        try {
            String[] proj = {MediaStore.Audio.Media._ID};
            String sel = MediaStore.Audio.Media.DATA + "=?";
            String[] args = new String[]{filePath};
            try (Cursor c = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, args, null)) {
                if (c != null && c.moveToFirst()) {
                    long id = c.getLong(0);
                    return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                }
            }
            try (Cursor c = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj, sel, args, null)) {
                if (c != null && c.moveToFirst()) {
                    long id = c.getLong(0);
                    return ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error finding content URI for: " + filePath, e);
        }
        return null;
    }

    private void playAudioFile(String path) {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
            fadeAudio(-1f, 0.0f, 200, () -> executePlayAudioFile(path));
        } else {
            executePlayAudioFile(path);
        }
    }

    private void executePlayAudioFile(String path) {
        executePlayAudioFile(path, -1);
    }

    private void executePlayAudioFile(String path, long targetSeekMs) {
        try {
            saveCurrentPosition();
            if (!isBrowsingZip && !isBrowsingDrive) {
                currentZipName = "";
                currentZipFilePath = "";
                currentDriveFolderName = "";
                currentDriveFolderUrl = "";
                currentDriveTracks.clear();
            }

            if (currentAudioList.isEmpty()) {
                currentAudioList.add(path);
            }

            int startIndex = currentAudioList.indexOf(path);
            if (startIndex == -1) {
                currentAudioList.clear();
                currentAudioList.add(path);
                startIndex = 0;
            }

            List<MediaItem> mediaItems = new ArrayList<>();
            for (String p : currentAudioList) {
                Uri itemUri = buildMediaUri(p);
                String title = new File(p).getName();
                mediaItems.add(new MediaItem.Builder()
                        .setUri(itemUri)
                        .setMediaMetadata(new androidx.media3.common.MediaMetadata.Builder().setTitle(title).build())
                        .build());
            }

            mediaPlayer.stop();
            mediaPlayer.clearMediaItems();
            mediaPlayer.setMediaItems(mediaItems, startIndex, C.TIME_UNSET);

            if (targetSeekMs >= 0) {
                mediaPlayer.seekTo(startIndex, targetSeekMs);
            } else {
                boolean resumeEnabled = bkPrefs.getBoolean("resume_playback_enabled", true);
                if (resumeEnabled) {
                    int savedPos = memPrefs.getInt(path, 0);
                    if (savedPos > 3000) {
                        mediaPlayer.seekTo(startIndex, savedPos - 3000);
                    }
                }
            }

            mediaPlayer.prepare();

            mediaPlayer.setVolume(0f);
            mediaPlayer.play();
            updateMediaPlayerOutputDevice();
            fadeAudio(0.0f, 1.0f, 500, null);

            resetAB();
            updateMediaSessionState(PlaybackState.STATE_PLAYING);

            mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
            updateExoPlayerPlayMode();
            if (currentDir != null && !currentDir.isEmpty()) {
                updateFolderResumeButton(currentDir);
            }

        } catch (Exception e) {
            Log.e(TAG, "Playback error", e);
            announceAccessibility("Cannot play this file");
        }
    }

    private boolean tryPlayZipFromLibrary(String path) {
        if (path == null || path.isEmpty()) return false;

        boolean isZip = ZipMediaManager.isZipFile(path) || path.toLowerCase(Locale.ROOT).endsWith(".zip");
        String savedCacheDir = libPrefs.getString("zip_cache_" + path, "");
        String savedZipName = libPrefs.getString("zip_name_" + path, "");
        if (savedZipName.isEmpty()) {
            String customTitle = libPrefs.getString("title_" + path, "");
            if (customTitle.startsWith("📦 ")) {
                savedZipName = customTitle.substring(2).trim();
            } else {
                savedZipName = new File(path).getName();
            }
        }

        if (!isZip && savedCacheDir.isEmpty() && !libPrefs.contains("zip_name_" + path) && !libPrefs.contains("zip_cache_" + path)) {
            return false;
        }

        // 1. Check saved cache directory from libPrefs
        if (!savedCacheDir.isEmpty()) {
            File cDir = new File(savedCacheDir);
            if (ZipMediaManager.isZipAlreadyExtracted(cDir)) {
                List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cDir);
                if (!tracks.isEmpty()) {
                    switchTab(1);
                    playZipTracks(savedZipName, path, tracks, 0);
                    return true;
                }
            }
        }

        // 2. Search extracted cache directory via findExtractedCacheDir
        File foundCacheDir = ZipMediaManager.findExtractedCacheDir(this, path);
        if (foundCacheDir == null && !savedZipName.isEmpty()) {
            foundCacheDir = ZipMediaManager.findExtractedCacheDir(this, savedZipName);
        }
        if (foundCacheDir != null && ZipMediaManager.isZipAlreadyExtracted(foundCacheDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(foundCacheDir);
            if (!tracks.isEmpty()) {
                libPrefs.edit()
                        .putString("zip_cache_" + path, foundCacheDir.getAbsolutePath())
                        .putString("zip_name_" + path, savedZipName)
                        .putString("title_" + path, "📦 " + savedZipName)
                        .apply();
                switchTab(1);
                playZipTracks(savedZipName, path, tracks, 0);
                return true;
            }
        }

        // 3. Check if content URI
        if (path.startsWith("content://")) {
            try {
                Uri uri = Uri.parse(path);
                switchTab(1);
                handleZipIntent(uri);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error opening zip content URI: " + path, e);
            }
        }

        // 4. Check if direct file exists
        File directFile = new File(path);
        if (directFile.exists()) {
            switchTab(1);
            playZipFile(directFile, 0);
            return true;
        }

        // 5. Handled as ZIP, but neither cache nor file could be found
        announceAccessibility("ZIP archive or cache not found on device");
        Toast.makeText(this, "ZIP archive or cache not found on device", Toast.LENGTH_SHORT).show();
        return true;
    }

    private void showZipOptionsDialog(File zipFile) {
        String zipPath = zipFile.getAbsolutePath();
        String lastFileInZip = memPrefs.getString("folder_last_file_zip_" + zipPath, "");
        int lastPosInZip = memPrefs.getInt("folder_last_pos_zip_" + zipPath, 0);

        List<String> optionsList = new ArrayList<>();
        if (!lastFileInZip.isEmpty() && new File(lastFileInZip).exists()) {
            optionsList.add("▶ Resume: " + new File(lastFileInZip).getName() + " (" + formatTime(lastPosInZip) + ")");
        }
        optionsList.add("Play All Tracks");
        optionsList.add("Browse Tracks inside ZIP");

        String[] options = optionsList.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("📦 " + zipFile.getName())
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if (selected.startsWith("▶ Resume")) {
                        resumeZipPlayback(zipFile);
                    } else if ("Play All Tracks".equals(selected)) {
                        playZipFile(zipFile, 0);
                    } else if ("Browse Tracks inside ZIP".equals(selected)) {
                        browseZipFile(zipFile);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resumeZipPlayback(File zipFile) {
        File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
        if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            File alt = ZipMediaManager.findExtractedCacheDir(this, zipFile.getAbsolutePath());
            if (alt != null) cacheDir = alt;
        }
        if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                String lastFileInZip = memPrefs.getString("folder_last_file_zip_" + zipFile.getAbsolutePath(), "");
                int resumeIdx = 0;
                if (!lastFileInZip.isEmpty()) {
                    for (int i = 0; i < tracks.size(); i++) {
                        if (tracks.get(i).getAbsolutePath().equals(lastFileInZip)) {
                            resumeIdx = i;
                            break;
                        }
                    }
                }
                playZipTracks(zipFile.getName(), zipFile.getAbsolutePath(), tracks, resumeIdx);
                return;
            }
        }
        playZipFile(zipFile, 0);
    }

    private void playZipFile(File zipFile, int startTrackIndex) {
        File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
        if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            File alt = ZipMediaManager.findExtractedCacheDir(this, zipFile.getAbsolutePath());
            if (alt != null) cacheDir = alt;
        }
        if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                playZipTracks(zipFile.getName(), zipFile.getAbsolutePath(), tracks, startTrackIndex);
                return;
            }
        }

        AlertDialog progressDialog = createExtractionProgressDialog(zipFile.getName());
        progressDialog.show();
        announceAccessibility("Extracting ZIP archive " + zipFile.getName() + ". Please wait.");

        ZipMediaManager.extractZipAsync(this, zipFile, new ZipMediaManager.ExtractionCallback() {
            @Override
            public void onProgress(int currentTrack, int totalTracks, String trackName) {
                if (progressDialog.isShowing()) {
                    TextView tvMsg = progressDialog.findViewById(android.R.id.message);
                    if (tvMsg != null) {
                        tvMsg.setText(totalTracks > 0 ?
                                "Extracting (" + currentTrack + "/" + totalTracks + "): " + trackName :
                                "Extracting track " + currentTrack + ": " + trackName);
                    }
                }
            }

            @Override
            public void onSuccess(File cacheDir, List<File> audioTracks) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                updateZipCacheUI();
                playZipTracks(zipFile.getName(), zipFile.getAbsolutePath(), audioTracks, startTrackIndex);
            }

            @Override
            public void onError(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility(errorMessage);
            }
        });
    }

    private void browseZipFile(File zipFile) {
        File cacheDir = ZipMediaManager.getCacheDirForZip(this, zipFile);
        if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            File alt = ZipMediaManager.findExtractedCacheDir(this, zipFile.getAbsolutePath());
            if (alt != null) cacheDir = alt;
        }
        if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                isBrowsingZip = true;
                zipSourceFolderPath = currentDir;
                currentZipName = zipFile.getName();
                currentZipFilePath = zipFile.getAbsolutePath();
                loadZipDirectory(cacheDir);
                announceAccessibility("Browsing tracks in " + zipFile.getName());
                return;
            }
        }

        AlertDialog progressDialog = createExtractionProgressDialog(zipFile.getName());
        progressDialog.show();
        announceAccessibility("Extracting ZIP archive " + zipFile.getName() + ". Please wait.");

        ZipMediaManager.extractZipAsync(this, zipFile, new ZipMediaManager.ExtractionCallback() {
            @Override
            public void onProgress(int currentTrack, int totalTracks, String trackName) {
                if (progressDialog.isShowing()) {
                    TextView tvMsg = progressDialog.findViewById(android.R.id.message);
                    if (tvMsg != null) {
                        tvMsg.setText(totalTracks > 0 ?
                                "Extracting (" + currentTrack + "/" + totalTracks + "): " + trackName :
                                "Extracting track " + currentTrack + ": " + trackName);
                    }
                }
            }

            @Override
            public void onSuccess(File cacheDir, List<File> audioTracks) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                updateZipCacheUI();
                isBrowsingZip = true;
                zipSourceFolderPath = currentDir;
                currentZipName = zipFile.getName();
                currentZipFilePath = zipFile.getAbsolutePath();
                loadZipDirectory(cacheDir);
                announceAccessibility("Browsing " + audioTracks.size() + " tracks in " + zipFile.getName());
            }

            @Override
            public void onError(String errorMessage) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                announceAccessibility(errorMessage);
            }
        });
    }

    private void playZipTracks(String zipName, String zipPath, List<File> tracks, int startIndex) {
        playZipTracks(zipName, zipPath, tracks, startIndex, -1);
    }

    private void playZipTracks(String zipName, String zipPath, List<File> tracks, int startIndex, long targetSeekMs) {
        if (mediaPlayer != null && mediaPlayer.isPlaying() && currentFadeMultiplier > 0) {
            fadeAudio(-1f, 0.0f, 200, () -> executePlayZipTracks(zipName, zipPath, tracks, startIndex, targetSeekMs));
        } else {
            executePlayZipTracks(zipName, zipPath, tracks, startIndex, targetSeekMs);
        }
    }

    private void executePlayZipTracks(String zipName, String zipPath, List<File> tracks, int startIndex) {
        executePlayZipTracks(zipName, zipPath, tracks, startIndex, -1);
    }

    private void executePlayZipTracks(String zipName, String zipPath, List<File> tracks, int startIndex, long targetSeekMs) {
        try {
            saveCurrentPosition();
            currentZipName = zipName;
            currentZipFilePath = zipPath;

            if (tracks != null && !tracks.isEmpty()) {
                File cDir = tracks.get(0).getParentFile();
                if (cDir != null && cDir.exists()) {
                    libPrefs.edit()
                            .putString("zip_cache_" + zipPath, cDir.getAbsolutePath())
                            .putString("zip_name_" + zipPath, zipName)
                            .putString("title_" + zipPath, "📦 " + zipName)
                            .apply();
                }
            }

            currentAudioList.clear();
            List<MediaItem> mediaItems = new ArrayList<>();
            for (File t : tracks) {
                currentAudioList.add(t.getAbsolutePath());
                mediaItems.add(MediaItem.fromUri(Uri.fromFile(t)));
            }

            int finalIndex = startIndex;
            if (finalIndex < 0 || finalIndex >= tracks.size()) finalIndex = 0;

            mediaPlayer.stop();
            mediaPlayer.clearMediaItems();
            mediaPlayer.setMediaItems(mediaItems, finalIndex, C.TIME_UNSET);

            String startTrackPath = tracks.get(finalIndex).getAbsolutePath();
            if (targetSeekMs >= 0) {
                mediaPlayer.seekTo(finalIndex, targetSeekMs);
            } else {
                boolean resumeEnabled = bkPrefs.getBoolean("resume_playback_enabled", true);
                if (resumeEnabled) {
                    int savedPos = memPrefs.getInt("folder_last_pos_zip_" + zipPath, 0);
                    if (savedPos <= 0) {
                        savedPos = memPrefs.getInt(startTrackPath, 0);
                    }
                    if (savedPos > 3000) {
                        mediaPlayer.seekTo(finalIndex, savedPos - 3000);
                    } else if (savedPos > 0) {
                        mediaPlayer.seekTo(finalIndex, savedPos);
                    }
                }
            }

            mediaPlayer.prepare();
            mediaPlayer.setVolume(0f);
            mediaPlayer.play();
            updateMediaPlayerOutputDevice();
            fadeAudio(0.0f, 1.0f, 500, null);

            resetAB();
            updateMediaSessionState(PlaybackState.STATE_PLAYING);
            mediaPlayer.setPlaybackParameters(new PlaybackParameters(currentSpeed, 1.0f));
            updateExoPlayerPlayMode();

            String trackName = tracks.get(finalIndex).getName();
            tvNowPlaying.setText("🎵 [" + zipName + "] " + trackName);
            announceAccessibility("Playing ZIP: " + zipName + ". Track " + (finalIndex + 1) + " of " + tracks.size() + ": " + trackName);

        } catch (Exception e) {
            Log.e(TAG, "Error playing ZIP tracks", e);
            announceAccessibility("Cannot play ZIP tracks");
        }
    }

    private AlertDialog createExtractionProgressDialog(String title) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 20);

        android.widget.ProgressBar progressBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        layout.addView(progressBar);

        TextView tvMsg = new TextView(this);
        tvMsg.setId(android.R.id.message);
        tvMsg.setText("Extracting audio tracks...");
        tvMsg.setTextColor(Color.LTGRAY);
        tvMsg.setPadding(0, 20, 0, 0);
        layout.addView(tvMsg);

        return new AlertDialog.Builder(this)
                .setTitle("📦 " + title)
                .setView(layout)
                .setCancelable(false)
                .create();
    }

    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String[] permissions = new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
            boolean allGranted = true;
            for (String perm : permissions) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                requestPermissionsLauncher.launch(permissions);
            } else {
                indexAllAudioForSearch();
                loadDirectory(Environment.getExternalStorageDirectory().getAbsolutePath());
                handleIntent(getIntent());
                checkManageStoragePermission();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionsLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            } else {
                indexAllAudioForSearch();
                loadDirectory(Environment.getExternalStorageDirectory().getAbsolutePath());
                handleIntent(getIntent());
                checkManageStoragePermission();
            }
        }
    }

    private void checkManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hadManageStorageAccess = true;
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("📁 Storage Permission")
                        .setMessage("To view all folders and play audio files smoothly, please grant 'All files access' permission.")
                        .setPositiveButton("Grant", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                manageStorageLauncher.launch(intent);
                            } catch (Exception e) {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                    manageStorageLauncher.launch(intent);
                                } catch (Exception ex) {
                                    Log.e(TAG, "Cannot launch manage storage settings", ex);
                                }
                            }
                        })
                        .setNegativeButton("Later", null)
                        .show();
            }
        }
    }

    private void switchTab(int tabNumber) {
        currentSelectedTab = tabNumber;
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);

        containerPlayer.setVisibility(View.GONE);
        containerLibrary.setVisibility(View.GONE);
        containerSettings.setVisibility(View.GONE);

        int inactive = theme.surfaceColor;
        int active = theme.primaryColor;

        tabPlayer.setBackgroundColor(inactive);
        tabLibrary.setBackgroundColor(inactive);
        tabSettings.setBackgroundColor(inactive);

        String announcement = "";

        if (tabNumber == 1) {
            containerPlayer.setVisibility(View.VISIBLE);
            tabPlayer.setBackgroundColor(active);
            announcement = "Player tab selected";
        } else if (tabNumber == 2) {
            containerLibrary.setVisibility(View.VISIBLE);
            tabLibrary.setBackgroundColor(active);
            announcement = "Library tab selected";
            if (libraryAdapter != null) {
                libraryAdapter.notifyDataSetChanged();
            }
            updateLibraryHeader();
        } else if (tabNumber == 3) {
            containerSettings.setVisibility(View.VISIBLE);
            tabSettings.setBackgroundColor(active);
            announcement = "Settings tab selected";
        }

        announceAccessibility(announcement);
    }

    public void announceAccessibility(String text) {
        getWindow().getDecorView().announceForAccessibility(text);
    }

    // ================= Sleep Timer Implementation =================

    private void showSleepTimerDialog() {
        if (isSleepTimerActive || isSleepEndOfTrack) {
            long remainingMs = Math.max(0, sleepTimerEndTimeMs - System.currentTimeMillis());
            long minutes = (remainingMs / 1000) / 60;
            long seconds = (remainingMs / 1000) % 60;
            String timeStr = isSleepEndOfTrack ? "Timer: End of Track" : String.format("Timer: %d min %d sec left", minutes, seconds);

            String[] activeOptions = {"❌ Turn Off Sleep Timer", "➕ Add 5 Minutes", "🔄 Change Time"};
            new AlertDialog.Builder(this)
                    .setTitle(timeStr)
                    .setItems(activeOptions, (dialog, which) -> {
                        if (which == 0) {
                            cancelSleepTimer();
                            Toast.makeText(this, "Sleep Timer turned OFF", Toast.LENGTH_SHORT).show();
                            announceAccessibility("Sleep Timer turned OFF");
                        } else if (which == 1) {
                            extendSleepTimer(5 * 60 * 1000);
                            Toast.makeText(this, "Added 5 minutes to Sleep Timer", Toast.LENGTH_SHORT).show();
                            announceAccessibility("Added 5 minutes to Sleep Timer");
                        } else if (which == 2) {
                            cancelSleepTimer();
                            showSleepTimerPresetsDialog();
                        }
                    })
                    .setNegativeButton("Close", null)
                    .show();
        } else {
            showSleepTimerPresetsDialog();
        }
    }

    private void showSleepTimerPresetsDialog() {
        String[] timerOptions = {
                "⏱️ 15 Minutes",
                "⏱️ 30 Minutes",
                "⏱️ 1 Hour",
                "🎵 End of Current Track",
                "⚙️ Custom Minutes..."
        };
        new AlertDialog.Builder(this)
                .setTitle("Set Sleep Timer")
                .setItems(timerOptions, (dialog, which) -> {
                    if (which == 0) {
                        startSleepTimer(15 * 60 * 1000, "15 minutes");
                    } else if (which == 1) {
                        startSleepTimer(30 * 60 * 1000, "30 minutes");
                    } else if (which == 2) {
                        startSleepTimer(60 * 60 * 1000, "1 hour");
                    } else if (which == 3) {
                        startSleepTimerEndOfTrack();
                    } else if (which == 4) {
                        showCustomSleepTimerDialog();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCustomSleepTimerDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Minutes (e.g. 45)");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle("Custom Sleep Timer")
                .setMessage("Enter duration in minutes:")
                .setView(input)
                .setPositiveButton("Set", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (!val.isEmpty()) {
                        try {
                            int mins = Integer.parseInt(val);
                            if (mins > 0 && mins <= 600) {
                                startSleepTimer(mins * 60L * 1000L, mins + " minutes");
                            } else {
                                Toast.makeText(this, "Please enter between 1 and 600 minutes", Toast.LENGTH_SHORT).show();
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startSleepTimer(long durationMs, String label) {
        cancelSleepTimer();
        isSleepTimerActive = true;
        isSleepEndOfTrack = false;
        sleepTimerEndTimeMs = System.currentTimeMillis() + durationMs;

        sleepTimerRunnable = new Runnable() {
            @Override
            public void run() {
                long remainingMs = sleepTimerEndTimeMs - System.currentTimeMillis();
                if (remainingMs <= 0) {
                    onSleepTimerTriggered();
                } else {
                    updateSleepTimerUI();
                    sleepTimerHandler.postDelayed(this, 1000);
                }
            }
        };
        sleepTimerHandler.post(sleepTimerRunnable);
        updateSleepTimerUI();
        Toast.makeText(this, "Sleep Timer set for " + label, Toast.LENGTH_SHORT).show();
        announceAccessibility("Sleep Timer set for " + label);
    }

    private void startSleepTimerEndOfTrack() {
        cancelSleepTimer();
        isSleepTimerActive = true;
        isSleepEndOfTrack = true;
        updateSleepTimerUI();
        Toast.makeText(this, "Sleep Timer: Stop after current track", Toast.LENGTH_SHORT).show();
        announceAccessibility("Sleep Timer set for end of current track");
    }

    private void onSleepTimerTriggered() {
        cancelSleepTimer();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            fadeAudio(1.0f, 0.0f, 3000, () -> {
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                    updateMediaSessionState(PlaybackState.STATE_PAUSED);
                    btnPlay.setText("Play / Pause");
                    mediaPlayer.setVolume(1.0f);
                }
                announceAccessibility("Sleep timer finished. Playback paused.");
            });
        } else {
            announceAccessibility("Sleep timer finished.");
        }
    }

    private void cancelSleepTimer() {
        isSleepTimerActive = false;
        isSleepEndOfTrack = false;
        sleepTimerEndTimeMs = 0;
        if (sleepTimerRunnable != null) {
            sleepTimerHandler.removeCallbacks(sleepTimerRunnable);
            sleepTimerRunnable = null;
        }
        updateSleepTimerUI();
    }

    private void extendSleepTimer(long additionalMs) {
        if (isSleepTimerActive && !isSleepEndOfTrack) {
            sleepTimerEndTimeMs += additionalMs;
            updateSleepTimerUI();
        }
    }

    private void updateSleepTimerUI() {
        if (btnSleepTimer == null) return;
        if (isSleepEndOfTrack) {
            btnSleepTimer.setText("Sleep Timer: End of Track (Tap to cancel)");
        } else if (isSleepTimerActive) {
            long remainingMs = Math.max(0, sleepTimerEndTimeMs - System.currentTimeMillis());
            long minutes = (remainingMs / 1000) / 60;
            long seconds = (remainingMs / 1000) % 60;
            btnSleepTimer.setText(String.format("Sleep Timer: %02d:%02d left (Tap to cancel)", minutes, seconds));
        } else {
            btnSleepTimer.setText("Sleep Timer: OFF");
        }
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 1. If in Settings tab, exit Settings and go back to Player tab
                if (containerSettings.getVisibility() == View.VISIBLE) {
                    switchTab(1);
                    return;
                }

                // 2. If in Library tab
                if (containerLibrary.getVisibility() == View.VISIBLE) {
                    if (libraryMode == 3) {
                        libraryMode = 2;
                        loadPlaylistsList();
                        announceAccessibility("Back to playlists");
                        return;
                    }
                    if (libraryMode == 5) {
                        libraryMode = 4;
                        loadBookShelfList();
                        announceAccessibility("Back to book shelves");
                        return;
                    }
                    // Exit Library and return to Player tab
                    switchTab(1);
                    return;
                }

                // 3. In Player tab:
                if (isBrowsingZip) {
                    isBrowsingZip = false;
                    loadDirectory(zipSourceFolderPath);
                    announceAccessibility("Moved back to folder " + new File(zipSourceFolderPath).getName());
                    return;
                }

                if (isBrowsingDrive && containerPlayer.getVisibility() == View.VISIBLE) {
                    isBrowsingDrive = false;
                    String target = (driveSourceFolderPath != null && !driveSourceFolderPath.isEmpty())
                            ? driveSourceFolderPath
                            : Environment.getExternalStorageDirectory().getAbsolutePath();
                    loadDirectory(target);
                    announceAccessibility("Moved back to folder " + new File(target).getName());
                    return;
                }

                if (!folderBackStack.isEmpty()) {
                    String prev = folderBackStack.pop();
                    handleFolderBackStep(prev);
                    return;
                }

                if ("STORAGE_ROOT".equals(currentDir)) {
                    exitOrMinimizeApp();
                    return;
                }

                List<StorageInfo> storages = getAvailableStorageVolumes();
                boolean isStorageRoot = false;
                for (StorageInfo s : storages) {
                    if (currentDir.equals(s.path) || currentDir.equals(s.path + "/")) {
                        isStorageRoot = true;
                        break;
                    }
                }

                // If currently inside a subfolder, go to parent folder
                if (!isStorageRoot) {
                    File parent = new File(currentDir).getParentFile();
                    if (parent != null) {
                        loadDirectory(parent.getAbsolutePath());
                        announceAccessibility("Moved back to folder " + parent.getName());
                        return;
                    }
                }

                // If at a storage volume root and multiple storage devices exist, go back to storage devices list
                if (isStorageRoot && storages.size() > 1) {
                    loadDirectory("STORAGE_ROOT");
                    return;
                }

                // Otherwise minimize or exit app
                exitOrMinimizeApp();
            }
        });
    }

    private void exitOrMinimizeApp() {
        boolean bgPlayEnabled = bkPrefs.getBoolean("bg_playback_enabled", true);
        if (bgPlayEnabled) {
            moveTaskToBack(true);
            announceAccessibility("App minimized. Playing in background.");
        } else {
            saveCurrentPosition();
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isAppInForeground = true;
        if (currentEarMode == EAR_MODE_AUTO || currentEarMode == EAR_MODE_ON) {
            registerProximitySensor();
            if (currentEarMode == EAR_MODE_ON) {
                applyAudioOutputRouting(true);
            }
        }
        AudioRoutingManager.getInstance().onResume(this);
        if (audioManager != null) {
            previousStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        mediaFolderCache.clear();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean hasManager = Environment.isExternalStorageManager();
            if (hasManager && !hadManageStorageAccess) {
                hadManageStorageAccess = true;
                indexAllAudioForSearch();
                loadDirectory(currentDir != null && !currentDir.isEmpty() ? currentDir : Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isAppInForeground = false;
        releaseProximityWakeLock();
        saveCurrentPosition();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isAppInForeground = false;
        unregisterProximitySensor();
        releaseProximityWakeLock();
        if (currentEarMode == EAR_MODE_AUTO) {
            applyAudioOutputRouting(false);
        }
        if (!isChangingConfigurations()) {
            boolean bgPlayEnabled = bkPrefs.getBoolean("bg_playback_enabled", true);
            if (!bgPlayEnabled) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    saveCurrentPosition();
                    updateMediaSessionState(PlaybackState.STATE_PAUSED);
                    if (btnPlay != null) btnPlay.setText("Play");
                }
            }
        }
    }

    // ================= Theme & Appearance =================

    private void showThemeSelectionDialog() {
        List<ThemeManager.ThemeItem> themes = ThemeManager.getAllThemes();
        String[] displayNames = new String[themes.size()];
        ThemeManager.ThemeItem current = ThemeManager.getCurrentTheme(this);
        int checkedItem = 0;
        for (int i = 0; i < themes.size(); i++) {
            ThemeManager.ThemeItem t = themes.get(i);
            displayNames[i] = t.displayName + " (" + t.malayalamName + ")";
            if (t.id.equals(current.id)) {
                checkedItem = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("🎨 Choose App Skin / Theme (തീം തിരഞ്ഞെടുക്കുക)")
                .setSingleChoiceItems(displayNames, checkedItem, (dialog, which) -> {
                    ThemeManager.ThemeItem selected = themes.get(which);
                    ThemeManager.setCurrentTheme(this, selected.id);
                    applyCurrentTheme();
                    Toast.makeText(this, "Skin applied: " + selected.displayName, Toast.LENGTH_SHORT).show();
                    announceAccessibility("Applied theme " + selected.displayName);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyCurrentTheme() {
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);

        // Status bar and nav bar colors
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(theme.bgColor);
                getWindow().setNavigationBarColor(theme.bgColor);
            }
        } catch (Exception ignored) {}

        // Root Background
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(theme.bgColor);
        }

        // Top Tab Bar
        if (layoutTopTabBar != null) {
            layoutTopTabBar.setBackgroundColor(theme.surfaceColor);
        }
        if (tabPlayer != null && tabLibrary != null && tabSettings != null) {
            int active = theme.primaryColor;
            int inactive = theme.surfaceColor;
            tabPlayer.setBackgroundColor(currentSelectedTab == 1 ? active : inactive);
            tabLibrary.setBackgroundColor(currentSelectedTab == 2 ? active : inactive);
            tabSettings.setBackgroundColor(currentSelectedTab == 3 ? active : inactive);
        }

        // Card Surfaces
        applyCardSurface(cardNowPlaying, theme.surfaceColor, theme.primaryColor);
        applyCardSurface(cardPlaybackControls, theme.surfaceColor, theme.buttonColor);
        applyCardSurface(layoutLibHeader, theme.surfaceColor, theme.primaryColor);

        // Player Action Buttons
        if (btnSort != null) btnSort.setBackgroundColor(theme.buttonColor);
        if (btnStream != null) btnStream.setBackgroundColor(theme.buttonColor);
        if (btnQueue != null) btnQueue.setBackgroundColor(theme.buttonColor);

        // Playback Control Buttons
        if (btnPlay != null) btnPlay.setBackgroundColor(theme.primaryColor);
        if (btnPrev != null) btnPrev.setBackgroundColor(theme.buttonColor);
        if (btnNext != null) btnNext.setBackgroundColor(theme.buttonColor);
        if (btnRewind != null) btnRewind.setBackgroundColor(theme.buttonColor);
        if (btnForward != null) btnForward.setBackgroundColor(theme.buttonColor);
        if (btnPlayMode != null) btnPlayMode.setBackgroundColor(theme.buttonColor);
        if (btnSpeed != null) btnSpeed.setBackgroundColor(theme.buttonColor);
        if (btnJump != null) btnJump.setBackgroundColor(theme.buttonColor);
        if (btnLoop != null) btnLoop.setBackgroundColor(theme.buttonColor);
        if (btnBookmark != null) btnBookmark.setBackgroundColor(theme.buttonColor);
        if (btnSliderStepSetting != null) btnSliderStepSetting.setBackgroundColor(theme.buttonColor);

        // Seekbar Tint
        if (seekBarPlayback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                seekBarPlayback.setProgressTintList(ColorStateList.valueOf(theme.primaryColor));
                seekBarPlayback.setThumbTintList(ColorStateList.valueOf(theme.secondaryColor));
            } catch (Exception ignored) {}
        }

        // Library Buttons & Filters
        updateLibraryTabColors();
        updateHistoryFilterUI();
        if (btnLibBack != null) btnLibBack.setBackgroundColor(theme.buttonColor);
        if (btnLibNew != null) btnLibNew.setBackgroundColor(theme.buttonColor);
        if (btnClear != null) btnClear.setBackgroundColor(theme.buttonColor);

        // Settings Headers
        if (headerAppearance != null) headerAppearance.setTextColor(theme.headerColor);
        if (headerPlayback != null) headerPlayback.setTextColor(theme.headerColor);
        if (headerAudioEffects != null) headerAudioEffects.setTextColor(theme.headerColor);
        if (headerVolShortcuts != null) headerVolShortcuts.setTextColor(theme.headerColor);
        if (headerStorage != null) headerStorage.setTextColor(theme.headerColor);
        if (headerShortcuts != null) headerShortcuts.setTextColor(theme.headerColor);

        // Settings Buttons
        if (btnAppTheme != null) {
            btnAppTheme.setText("🎨 App Skin / Theme: " + theme.displayName);
            btnAppTheme.setBackgroundColor(theme.buttonColor);
        }

        if (headerUpdates != null) headerUpdates.setTextColor(theme.headerColor);
        if (btnCheckUpdate != null) btnCheckUpdate.setBackgroundColor(theme.primaryColor);

        updateResumeToggleUI();
        updateBgPlaybackToggleUI();
        updateFilterMediaFoldersToggleUI();
        updateAutoPlayNextFolderUI();
        updateVolShortcutsMasterToggleUI();
        updateScreenOffVolToggleUI();
        updateAudioSeparationButtonUI();
        updateEarSpeakerButtonUI();
        updateAutoCheckUpdateUI();
    }

    private void updateAutoCheckUpdateUI() {
        if (btnAutoCheckUpdateToggle == null) return;
        boolean enabled = AppUpdateManager.isAutoCheckEnabled(this);
        ThemeManager.ThemeItem theme = ThemeManager.getCurrentTheme(this);
        btnAutoCheckUpdateToggle.setText(enabled ? "Auto-check for updates: ON" : "Auto-check for updates: OFF");
        btnAutoCheckUpdateToggle.setBackgroundColor(enabled ? theme.primaryColor : theme.buttonColor);
    }

    private void applyCardSurface(View view, int surfaceColor, int strokeColor) {
        if (view == null) return;
        try {
            GradientDrawable gd = new GradientDrawable();
            gd.setColor(surfaceColor);
            gd.setCornerRadius(dpToPx(14));
            gd.setStroke(dpToPx(1), strokeColor);
            view.setBackground(gd);
        } catch (Exception ignored) {}
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (AudioRoutingManager.getInstance().isAudioSeparationEnabled(this)) {
            AudioRoutingManager.getInstance().disableAudioSeparation(this, false);
        }
        unregisterProximitySensor();
        releaseProximityWakeLock();
        applyAudioOutputRouting(false);
        super.onDestroy();
        saveCurrentPosition();
        cancelSleepTimer();
        if (volumeContentObserver != null) {
            try { getContentResolver().unregisterContentObserver(volumeContentObserver); } catch (Exception ignored) {}
            volumeContentObserver = null;
        }
        handler.removeCallbacks(trackFadeMonitorRunnable);
        if (pendingSeekFadeInRunnable != null) {
            handler.removeCallbacks(pendingSeekFadeInRunnable);
            pendingSeekFadeInRunnable = null;
        }
        if (volumeAnimator != null && volumeAnimator.isRunning()) {
            volumeAnimator.cancel();
            volumeAnimator = null;
        }
        if (mediaPlayer != null) {
            resetAB();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (equalizer != null) { equalizer.release(); equalizer = null; }
        if (loudnessEnhancer != null) { loudnessEnhancer.release(); loudnessEnhancer = null; }
        if (virtualizer != null) { virtualizer.release(); virtualizer = null; }

        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        dismissMediaNotification();
        if (instance == this) {
            instance = null;
        }
    }
}