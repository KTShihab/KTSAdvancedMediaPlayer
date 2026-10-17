package com.example.ktsadvancedmediaplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderMetadataManager {

    private static final String TAG = "FolderMetadataManager";
    private static final String PREFS_NAME = "folder_metadata_cache";

    public static class FolderMeta {
        public final int fileCount;
        public final long totalDurationMs;
        public final boolean isCalculated;

        public FolderMeta(int fileCount, long totalDurationMs, boolean isCalculated) {
            this.fileCount = fileCount;
            this.totalDurationMs = totalDurationMs;
            this.isCalculated = isCalculated;
        }

        public String getShortDescription() {
            String filesTxt = fileCount == 1 ? "1 track" : fileCount + " tracks";
            if (totalDurationMs > 0) {
                return filesTxt + " • " + formatDurationShort(totalDurationMs);
            } else if (fileCount > 0) {
                return filesTxt;
            }
            return "";
        }

        public String getSpeechDescription() {
            String filesTxt = fileCount == 1 ? "1 track" : fileCount + " tracks";
            if (totalDurationMs > 0) {
                return filesTxt + ", total duration " + formatDurationSpeech(totalDurationMs);
            } else if (fileCount > 0) {
                return filesTxt;
            }
            return "";
        }
    }

    public static class FileMeta {
        public final long durationMs;
        public final long sizeBytes;
        public final boolean isCalculated;

        public FileMeta(long durationMs, long sizeBytes, boolean isCalculated) {
            this.durationMs = durationMs;
            this.sizeBytes = sizeBytes;
            this.isCalculated = isCalculated;
        }

        public String getShortDescription() {
            String sizeStr = ZipMediaManager.formatFileSize(sizeBytes);
            if (durationMs > 0) {
                return formatDurationShort(durationMs) + " • " + sizeStr;
            } else if (sizeBytes > 0) {
                return sizeStr;
            }
            return "";
        }

        public String getSpeechDescription() {
            String sizeStr = formatFileSizeSpeech(sizeBytes);
            if (durationMs > 0) {
                return "duration " + formatDurationSpeech(durationMs) + ", size " + sizeStr;
            } else if (sizeBytes > 0) {
                return "size " + sizeStr;
            }
            return "";
        }
    }

    public interface FolderMetaCallback {
        void onMetadataReady(@NonNull FolderMeta meta);
    }

    public interface FileMetaCallback {
        void onMetadataReady(@NonNull FileMeta meta);
    }

    private static final Map<String, FolderMeta> memoryCache = new ConcurrentHashMap<>();
    private static final Map<String, FileMeta> fileMemoryCache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    public static FolderMeta getCachedMeta(Context context, String path) {
        if (path == null || path.isEmpty()) return null;
        FolderMeta mem = memoryCache.get(path);
        if (mem != null && mem.isCalculated) return mem;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String val = prefs.getString("meta_" + path, null);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 3) {
                try {
                    int count = Integer.parseInt(parts[0]);
                    long duration = Long.parseLong(parts[1]);
                    long savedMod = Long.parseLong(parts[2]);

                    File f = new File(path);
                    if (f.exists() && f.lastModified() == savedMod) {
                        FolderMeta meta = new FolderMeta(count, duration, true);
                        memoryCache.put(path, meta);
                        return meta;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    @Nullable
    public static FileMeta getCachedFileMeta(Context context, String path) {
        if (path == null || path.isEmpty()) return null;
        FileMeta mem = fileMemoryCache.get(path);
        if (mem != null && mem.isCalculated) return mem;

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String val = prefs.getString("fmeta_" + path, null);
        if (val != null) {
            String[] parts = val.split(":");
            if (parts.length >= 3) {
                try {
                    long duration = Long.parseLong(parts[0]);
                    long size = Long.parseLong(parts[1]);
                    long savedMod = Long.parseLong(parts[2]);

                    File f = new File(path);
                    if (f.exists() && f.lastModified() == savedMod) {
                        FileMeta meta = new FileMeta(duration, size, true);
                        fileMemoryCache.put(path, meta);
                        return meta;
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static void getFileInfoAsync(Context context, String path, @NonNull FileMetaCallback callback) {
        if (path == null || path.isEmpty() || path.startsWith("http://") || path.startsWith("https://")) {
            callback.onMetadataReady(new FileMeta(0, 0, true));
            return;
        }

        File file = new File(path);
        if (!file.exists() || file.isDirectory()) {
            callback.onMetadataReady(new FileMeta(0, 0, true));
            return;
        }

        FileMeta cached = getCachedFileMeta(context, path);
        if (cached != null && cached.isCalculated) {
            callback.onMetadataReady(cached);
            return;
        }

        long fSize = file.length();
        FileMeta provisional = new FileMeta(0, fSize, false);
        callback.onMetadataReady(provisional);

        executor.execute(() -> {
            long duration = 0;
            try {
                String[] proj = {MediaStore.Audio.Media.DURATION};
                String sel = MediaStore.Audio.Media.DATA + " = ?";
                String[] selArgs = new String[]{path};
                try (Cursor cursor = context.getContentResolver().query(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, selArgs, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                        if (durCol != -1) {
                            duration = cursor.getLong(durCol);
                        }
                    }
                }
            } catch (Exception ignored) {}

            if (duration <= 0) {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                try {
                    mmr.setDataSource(path);
                    String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durStr != null) {
                        duration = Long.parseLong(durStr);
                    }
                } catch (Exception ignored) {}
                finally {
                    try { mmr.release(); } catch (Exception ignored) {}
                }
            }

            final long finalDur = duration;
            FileMeta calculated = new FileMeta(finalDur, fSize, true);
            fileMemoryCache.put(path, calculated);

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("fmeta_" + path, finalDur + ":" + fSize + ":" + file.lastModified()).apply();

            mainHandler.post(() -> callback.onMetadataReady(calculated));
        });
    }

    /**
     * Retrieves folder or ZIP file metadata asynchronously without freezing UI.
     */
    public static void getFolderInfoAsync(Context context, String path, @NonNull FolderMetaCallback callback) {
        if (path == null || path.isEmpty() || "STORAGE_ROOT".equals(path)) {
            callback.onMetadataReady(new FolderMeta(0, 0, true));
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            callback.onMetadataReady(new FolderMeta(0, 0, true));
            return;
        }

        FolderMeta cached = getCachedMeta(context, path);
        if (cached != null && cached.isCalculated) {
            callback.onMetadataReady(cached);
            return;
        }

        // Fast count from file system
        int initialCount = 0;
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            if (list != null) {
                for (File f : list) {
                    if (ZipMediaManager.isAudioFile(f.getName()) || ZipMediaManager.isZipFile(f.getName())) {
                        initialCount++;
                    }
                }
            }
        } else if (ZipMediaManager.isZipFile(file.getName())) {
            File cacheDir = ZipMediaManager.getCacheDirForZip(context, file);
            if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
                initialCount = ZipMediaManager.getExtractedAudioTracks(cacheDir).size();
            }
        }

        FolderMeta provisional = new FolderMeta(initialCount, 0, false);
        callback.onMetadataReady(provisional);

        final int countedFiles = initialCount;
        executor.execute(() -> {
            try {
                FolderMeta calculated = calculateFolderMeta(context, file, countedFiles);
                memoryCache.put(path, calculated);

                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                prefs.edit().putString("meta_" + path,
                        calculated.fileCount + ":" + calculated.totalDurationMs + ":" + file.lastModified()
                ).apply();

                mainHandler.post(() -> callback.onMetadataReady(calculated));
            } catch (Exception e) {
                Log.w(TAG, "Failed to calculate metadata for " + path, e);
            }
        });
    }

    private static FolderMeta calculateFolderMeta(Context context, File dir, int fileCount) {
        if (ZipMediaManager.isZipFile(dir.getName())) {
            return calculateZipMeta(context, dir);
        }
        if (!dir.isDirectory()) {
            return new FolderMeta(0, 0, true);
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return new FolderMeta(0, 0, true);
        }

        int audioCount = 0;
        long totalDurationMs = 0;
        Set<String> filesNeedingRetriever = new HashSet<>();

        for (File f : files) {
            if (ZipMediaManager.isAudioFile(f.getName())) {
                audioCount++;
                filesNeedingRetriever.add(f.getAbsolutePath());
            }
        }

        if (audioCount == 0) {
            return new FolderMeta(0, 0, true);
        }

        // 1. Try querying MediaStore in bulk for fast duration lookup
        try {
            String dirPath = dir.getAbsolutePath();
            String[] proj = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.SIZE};
            String sel = MediaStore.Audio.Media.DATA + " LIKE ? AND " + MediaStore.Audio.Media.DATA + " NOT LIKE ?";
            String[] selArgs = new String[]{dirPath + "/%", dirPath + "/%/%"};

            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, selArgs, null)) {
                if (cursor != null) {
                    int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    int sizeCol = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                    while (cursor.moveToNext()) {
                        String data = cursor.getString(dataCol);
                        long dur = cursor.getLong(durCol);
                        long sz = (sizeCol != -1) ? cursor.getLong(sizeCol) : 0;
                        if (sz <= 0) {
                            try { sz = new File(data).length(); } catch (Exception ignored) {}
                        }
                        if (dur > 0 && filesNeedingRetriever.contains(data)) {
                            totalDurationMs += dur;
                            filesNeedingRetriever.remove(data);
                            fileMemoryCache.put(data, new FileMeta(dur, sz, true));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "MediaStore query error: " + e.getMessage());
        }

        // 2. For remaining files not in MediaStore (or unindexed), use MediaMetadataRetriever
        if (!filesNeedingRetriever.isEmpty()) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            for (String path : filesNeedingRetriever) {
                try {
                    mmr.setDataSource(path);
                    String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long dur = 0;
                    if (durStr != null) {
                        dur = Long.parseLong(durStr);
                        totalDurationMs += dur;
                    }
                    long sz = new File(path).length();
                    fileMemoryCache.put(path, new FileMeta(dur, sz, true));
                } catch (Exception ignored) {
                }
            }
            try { mmr.release(); } catch (Exception ignored) {}
        }

        return new FolderMeta(audioCount, totalDurationMs, true);
    }

    private static FolderMeta calculateZipMeta(Context context, File zipFile) {
        File cacheDir = ZipMediaManager.getCacheDirForZip(context, zipFile);
        if (!ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            File alt = ZipMediaManager.findExtractedCacheDir(context, zipFile.getAbsolutePath());
            if (alt != null) cacheDir = alt;
        }

        List<File> tracks;
        if (ZipMediaManager.isZipAlreadyExtracted(cacheDir)) {
            tracks = ZipMediaManager.getExtractedAudioTracks(cacheDir);
        } else {
            // Count from zip entries without extracting
            List<String> entryNames = ZipMediaManager.inspectZipAudioTracks(zipFile);
            return new FolderMeta(entryNames.size(), 0, false);
        }

        long totalDurationMs = 0;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        for (File t : tracks) {
            try {
                mmr.setDataSource(t.getAbsolutePath());
                String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long dur = 0;
                if (durStr != null) {
                    dur = Long.parseLong(durStr);
                    totalDurationMs += dur;
                }
                fileMemoryCache.put(t.getAbsolutePath(), new FileMeta(dur, t.length(), true));
            } catch (Exception ignored) {}
        }
        try { mmr.release(); } catch (Exception ignored) {}

        return new FolderMeta(tracks.size(), totalDurationMs, true);
    }

    /**
     * Calculates total duration for a list of audio files synchronously using MediaStore and cache.
     */
    public static long calculateTotalDurationForFiles(Context context, List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) return 0;
        long totalDuration = 0;
        Set<String> needed = new HashSet<>(filePaths);

        try {
            String[] proj = {MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
            try (Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)) {
                if (cursor != null) {
                    int dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int durCol = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
                    while (cursor.moveToNext()) {
                        String data = cursor.getString(dataCol);
                        if (needed.contains(data)) {
                            long dur = cursor.getLong(durCol);
                            if (dur > 0) {
                                totalDuration += dur;
                                needed.remove(data);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        if (!needed.isEmpty()) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            for (String p : needed) {
                if (p.startsWith("http://") || p.startsWith("https://")) continue;
                try {
                    mmr.setDataSource(p);
                    String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (durStr != null) {
                        totalDuration += Long.parseLong(durStr);
                    }
                } catch (Exception ignored) {}
            }
            try { mmr.release(); } catch (Exception ignored) {}
        }

        return totalDuration;
    }

    public static String formatDurationShort(long ms) {
        if (ms <= 0) return "00:00";
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;

        if (h > 0) {
            return String.format(Locale.getDefault(), "%dh %02dm", h, m);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        }
    }

    public static String formatDurationSpeech(long ms) {
        if (ms <= 0) return "0 seconds";
        long totalSec = ms / 1000;
        long h = totalSec / 3600;
        long m = (totalSec % 3600) / 60;
        long s = totalSec % 60;

        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(h).append(h == 1 ? " hour " : " hours ");
        }
        if (m > 0 || h > 0) {
            sb.append(m).append(m == 1 ? " minute " : " minutes ");
        }
        if (s > 0 || sb.length() == 0) {
            sb.append(s).append(s == 1 ? " second" : " seconds");
        }
        return sb.toString().trim();
    }

    public static String formatFileSizeSpeech(long bytes) {
        if (bytes <= 0) return "0 bytes";
        if (bytes < 1024) {
            return bytes + (bytes == 1 ? " byte" : " bytes");
        } else if (bytes < 1024 * 1024) {
            double kb = bytes / 1024.0;
            return String.format(Locale.getDefault(), "%.1f kilobytes", kb);
        } else if (bytes < 1024L * 1024L * 1024L) {
            double mb = bytes / (1024.0 * 1024.0);
            return String.format(Locale.getDefault(), "%.1f megabytes", mb);
        } else {
            double gb = bytes / (1024.0 * 1024.0 * 1024.0);
            return String.format(Locale.getDefault(), "%.1f gigabytes", gb);
        }
    }
}
