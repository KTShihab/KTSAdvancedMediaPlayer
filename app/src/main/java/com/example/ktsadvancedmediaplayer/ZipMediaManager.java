package com.example.ktsadvancedmediaplayer;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipMediaManager {

    private static final String TAG = "ZipMediaManager";
    private static final String CACHE_SUBDIR = "zip_media_cache";
    private static final String MARKER_FILE = ".extraction_complete";

    public interface ExtractionCallback {
        void onProgress(int currentTrack, int totalTracks, String trackName);
        void onSuccess(File cacheDir, List<File> audioTracks);
        void onError(String errorMessage);
    }

    public static final NaturalOrderComparator NATURAL_ORDER_COMPARATOR = new NaturalOrderComparator();

    public static final Comparator<File> FILE_NATURAL_ORDER_COMPARATOR = (f1, f2) -> {
        if (f1 == null && f2 == null) return 0;
        if (f1 == null) return -1;
        if (f2 == null) return 1;
        return NATURAL_ORDER_COMPARATOR.compare(f1.getAbsolutePath(), f2.getAbsolutePath());
    };

    public static boolean isZipFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".zip");
    }

    public static boolean isAudioFile(String filename) {
        if (filename == null) return false;
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".mp3") || lower.endsWith(".wav") ||
                lower.endsWith(".m4a") || lower.endsWith(".ogg") ||
                lower.endsWith(".aac") || lower.endsWith(".flac") ||
                lower.endsWith(".opus") || lower.endsWith(".m4b") ||
                lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
                lower.endsWith(".avi") || lower.endsWith(".webm") ||
                lower.endsWith(".3gp") || lower.endsWith(".wma") ||
                lower.endsWith(".mid") || lower.endsWith(".midi");
    }

    public static File getZipBaseCacheDir(Context context) {
        File base = context.getExternalCacheDir();
        if (base == null) {
            base = context.getCacheDir();
        }
        File cacheDir = new File(base, CACHE_SUBDIR);
        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }
        return cacheDir;
    }

    public static File getCacheDirForZip(Context context, File zipFile) {
        File baseDir = getZipBaseCacheDir(context);
        String safeName = zipFile.getName().replaceAll("[^a-zA-Z0-9._-]", "_");
        String folderName = safeName + "_" + zipFile.length() + "_" + zipFile.lastModified();
        return new File(baseDir, folderName);
    }

    public static File getCacheDirForUri(Context context, Uri uri, String displayName, long size) {
        File baseDir = getZipBaseCacheDir(context);
        String safeName = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String folderName = safeName + "_" + size + "_" + Math.abs(uri.toString().hashCode());
        return new File(baseDir, folderName);
    }

    public static File findExtractedCacheDir(Context context, String zipNameOrPath) {
        if (zipNameOrPath == null || zipNameOrPath.isEmpty()) return null;
        File baseDir = getZipBaseCacheDir(context);
        if (!baseDir.exists() || !baseDir.isDirectory()) return null;

        File directDir = new File(zipNameOrPath);
        if (directDir.exists() && directDir.isDirectory() && isZipAlreadyExtracted(directDir)) {
            return directDir;
        }

        String name = directDir.getName();
        if (name.contains("?")) {
            name = name.substring(0, name.indexOf("?"));
        }
        String safeName = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            safeName = safeName.substring(0, safeName.length() - 4);
        }

        File[] subDirs = baseDir.listFiles();
        if (subDirs == null) return null;

        for (File dir : subDirs) {
            if (dir.isDirectory()) {
                String dName = dir.getName();
                if ((dName.startsWith(safeName + "_") || dName.equalsIgnoreCase(safeName)) && isZipAlreadyExtracted(dir)) {
                    List<File> tracks = getExtractedAudioTracks(dir);
                    if (!tracks.isEmpty()) return dir;
                }
            }
        }

        if (safeName.length() >= 3) {
            for (File dir : subDirs) {
                if (dir.isDirectory() && dir.getName().toLowerCase(Locale.ROOT).contains(safeName.toLowerCase(Locale.ROOT)) && isZipAlreadyExtracted(dir)) {
                    List<File> tracks = getExtractedAudioTracks(dir);
                    if (!tracks.isEmpty()) return dir;
                }
            }
        }

        return null;
    }

    public static boolean isZipAlreadyExtracted(File cacheDir) {
        if (!cacheDir.exists() || !cacheDir.isDirectory()) return false;
        File marker = new File(cacheDir, MARKER_FILE);
        return marker.exists();
    }

    public static List<File> getExtractedAudioTracks(File cacheDir) {
        List<File> tracks = new ArrayList<>();
        collectAudioFiles(cacheDir, tracks);
        tracks.sort(FILE_NATURAL_ORDER_COMPARATOR);
        return tracks;
    }

    private static void collectAudioFiles(File directory, List<File> outList) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collectAudioFiles(f, outList);
            } else if (isAudioFile(f.getName())) {
                outList.add(f);
            }
        }
    }

    public static List<String> inspectZipAudioTracks(File zipFile) {
        List<String> list = new ArrayList<>();
        if (zipFile == null || !zipFile.exists()) return list;
        try (ZipFile zf = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String name = entry.getName();
                    if (isAudioFile(name) && !name.contains("__MACOSX") && !name.startsWith(".")) {
                        list.add(name);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error inspecting ZIP tracks: " + zipFile.getName(), e);
        }
        return list;
    }

    public static void extractZipAsync(Context context, File zipFile, ExtractionCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        File cacheDir = getCacheDirForZip(context, zipFile);

        if (isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(cacheDir, tracks));
                return;
            }
        }

        new Thread(() -> {
            try {
                if (cacheDir.exists()) {
                    deleteRecursive(cacheDir);
                }
                //noinspection ResultOfMethodCallIgnored
                cacheDir.mkdirs();

                List<ZipEntry> audioEntries = new ArrayList<>();
                try (ZipFile zf = new ZipFile(zipFile)) {
                    Enumeration<? extends ZipEntry> entries = zf.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();
                        if (!entry.isDirectory() && isAudioFile(entry.getName())) {
                            audioEntries.add(entry);
                        }
                    }

                    // Sort entries in natural order before extraction
                    audioEntries.sort((e1, e2) -> NATURAL_ORDER_COMPARATOR.compare(e1.getName(), e2.getName()));

                    if (audioEntries.isEmpty()) {
                        mainHandler.post(() -> callback.onError("No audio tracks found in this ZIP archive."));
                        return;
                    }

                    int total = audioEntries.size();
                    byte[] buffer = new byte[8192];
                    String canonicalDest = cacheDir.getCanonicalPath();

                    for (int i = 0; i < total; i++) {
                        ZipEntry entry = audioEntries.get(i);
                        String entryName = entry.getName();
                        final int currentIdx = i + 1;

                        mainHandler.post(() -> callback.onProgress(currentIdx, total, new File(entryName).getName()));

                        File outputFile = new File(cacheDir, entryName);
                        String canonicalOutput = outputFile.getCanonicalPath();
                        if (!canonicalOutput.startsWith(canonicalDest + File.separator) && !canonicalOutput.equals(canonicalDest)) {
                            throw new SecurityException("Zip Slip security violation: " + entryName);
                        }

                        File parent = outputFile.getParentFile();
                        if (parent != null && !parent.exists()) {
                            //noinspection ResultOfMethodCallIgnored
                            parent.mkdirs();
                        }

                        try (InputStream is = new BufferedInputStream(zf.getInputStream(entry));
                             FileOutputStream fos = new FileOutputStream(outputFile);
                             BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {
                            int len;
                            while ((len = is.read(buffer)) != -1) {
                                bos.write(buffer, 0, len);
                            }
                            bos.flush();
                        }
                    }
                }

                // Create completion marker
                File marker = new File(cacheDir, MARKER_FILE);
                //noinspection ResultOfMethodCallIgnored
                marker.createNewFile();

                List<File> tracks = getExtractedAudioTracks(cacheDir);
                mainHandler.post(() -> callback.onSuccess(cacheDir, tracks));

            } catch (Exception e) {
                Log.e(TAG, "Error extracting ZIP: " + zipFile.getName(), e);
                mainHandler.post(() -> callback.onError("Error extracting ZIP: " + e.getLocalizedMessage()));
            }
        }).start();
    }

    public static void extractZipFromUriAsync(Context context, Uri uri, ExtractionCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        String displayName = "archive.zip";
        long size = 0;
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (nameIndex != -1) displayName = cursor.getString(nameIndex);
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
            }
        } catch (Exception ignored) {}

        File cacheDir = getCacheDirForUri(context, uri, displayName, size);
        if (isZipAlreadyExtracted(cacheDir)) {
            List<File> tracks = getExtractedAudioTracks(cacheDir);
            if (!tracks.isEmpty()) {
                mainHandler.post(() -> callback.onSuccess(cacheDir, tracks));
                return;
            }
        }

        final String finalDisplayName = displayName;
        new Thread(() -> {
            try {
                if (cacheDir.exists()) {
                    deleteRecursive(cacheDir);
                }
                //noinspection ResultOfMethodCallIgnored
                cacheDir.mkdirs();

                String canonicalDest = cacheDir.getCanonicalPath();
                byte[] buffer = new byte[8192];
                int trackCount = 0;

                try (InputStream is = context.getContentResolver().openInputStream(uri);
                     BufferedInputStream bis = new BufferedInputStream(is);
                     ZipInputStream zis = new ZipInputStream(bis)) {

                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        if (!entry.isDirectory() && isAudioFile(entry.getName())) {
                            trackCount++;
                            final int currentNum = trackCount;
                            final String currentName = new File(entry.getName()).getName();
                            mainHandler.post(() -> callback.onProgress(currentNum, -1, currentName));

                            File outputFile = new File(cacheDir, entry.getName());
                            String canonicalOutput = outputFile.getCanonicalPath();
                            if (!canonicalOutput.startsWith(canonicalDest + File.separator) && !canonicalOutput.equals(canonicalDest)) {
                                throw new SecurityException("Zip Slip violation: " + entry.getName());
                            }

                            File parent = outputFile.getParentFile();
                            if (parent != null && !parent.exists()) {
                                //noinspection ResultOfMethodCallIgnored
                                parent.mkdirs();
                            }

                            try (FileOutputStream fos = new FileOutputStream(outputFile);
                                 BufferedOutputStream bos = new BufferedOutputStream(fos, 8192)) {
                                int len;
                                while ((len = zis.read(buffer)) != -1) {
                                    bos.write(buffer, 0, len);
                                }
                                bos.flush();
                            }
                        }
                        zis.closeEntry();
                    }
                }

                if (trackCount == 0) {
                    mainHandler.post(() -> callback.onError("No audio tracks found in " + finalDisplayName));
                    return;
                }

                // Create completion marker
                File marker = new File(cacheDir, MARKER_FILE);
                //noinspection ResultOfMethodCallIgnored
                marker.createNewFile();

                List<File> tracks = getExtractedAudioTracks(cacheDir);
                mainHandler.post(() -> callback.onSuccess(cacheDir, tracks));

            } catch (Exception e) {
                Log.e(TAG, "Error extracting ZIP from URI", e);
                mainHandler.post(() -> callback.onError("Error extracting ZIP: " + e.getLocalizedMessage()));
            }
        }).start();
    }

    public static long getZipCacheSize(Context context) {
        File baseDir = getZipBaseCacheDir(context);
        return calculateDirectorySize(baseDir);
    }

    private static long calculateDirectorySize(File dir) {
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

    public static boolean clearZipCache(Context context) {
        File baseDir = getZipBaseCacheDir(context);
        return deleteRecursive(baseDir);
    }

    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return fileOrDirectory.delete();
    }

    public static void zipDirectory(File sourceFolder, File destZipFile) throws IOException {
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            throw new IOException("Source is not a valid directory: " + sourceFolder.getAbsolutePath());
        }
        File parent = destZipFile.getParentFile();
        if (parent != null && !parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        if (destZipFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            destZipFile.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(destZipFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos)) {
            zipSubFolder(sourceFolder, sourceFolder, zos);
            zos.finish();
        }
    }

    private static void zipSubFolder(File rootDir, File currentFolder, ZipOutputStream zos) throws IOException {
        File[] files = currentFolder.listFiles();
        if (files == null) return;
        byte[] buffer = new byte[8192];
        for (File file : files) {
            if (file.isDirectory()) {
                File[] children = file.listFiles();
                if (children == null || children.length == 0) {
                    String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                    if (!relativePath.endsWith("/")) relativePath += "/";
                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zipEntry.setTime(file.lastModified());
                    zos.putNextEntry(zipEntry);
                    zos.closeEntry();
                } else {
                    zipSubFolder(rootDir, file, zos);
                }
            } else {
                String relativePath = rootDir.toURI().relativize(file.toURI()).getPath();
                ZipEntry zipEntry = new ZipEntry(relativePath);
                zipEntry.setTime(file.lastModified());
                zos.putNextEntry(zipEntry);
                try (FileInputStream fis = new FileInputStream(file);
                     BufferedInputStream bis = new BufferedInputStream(fis)) {
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        zos.write(buffer, 0, bytesRead);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        if (exp <= 0) return bytes + " B";
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public static class NaturalOrderComparator implements Comparator<String> {
        @Override
        public int compare(String a, String b) {
            if (a == null && b == null) return 0;
            if (a == null) return -1;
            if (b == null) return 1;

            int ia = 0, ib = 0;
            int nza, nzb;
            char ca, cb;

            while (true) {
                nza = nzb = 0;

                ca = charAt(a, ia);
                cb = charAt(b, ib);

                while (Character.isSpaceChar(ca) || ca == '0') {
                    if (ca == '0') {
                        nza++;
                    } else {
                        nza = 0;
                    }
                    ca = charAt(a, ++ia);
                }

                while (Character.isSpaceChar(cb) || cb == '0') {
                    if (cb == '0') {
                        nzb++;
                    } else {
                        nzb = 0;
                    }
                    cb = charAt(b, ++ib);
                }

                if (Character.isDigit(ca) && Character.isDigit(cb)) {
                    int bias = 0;
                    while (true) {
                        ca = charAt(a, ia);
                        cb = charAt(b, ib);

                        if (!Character.isDigit(ca) && !Character.isDigit(cb)) {
                            if (bias != 0) return bias;
                            break;
                        }
                        if (!Character.isDigit(ca)) {
                            return -1;
                        }
                        if (!Character.isDigit(cb)) {
                            return 1;
                        }
                        if (ca < cb) {
                            if (bias == 0) bias = -1;
                        } else if (ca > cb) {
                            if (bias == 0) bias = 1;
                        }
                        ia++;
                        ib++;
                    }
                }

                if (ca == 0 && cb == 0) {
                    return Integer.compare(nza, nzb);
                }

                if (Character.toLowerCase(ca) < Character.toLowerCase(cb)) {
                    return -1;
                }
                if (Character.toLowerCase(ca) > Character.toLowerCase(cb)) {
                    return 1;
                }

                ia++;
                ib++;
            }
        }

        private static char charAt(String s, int i) {
            return i >= s.length() ? 0 : s.charAt(i);
        }
    }
}
