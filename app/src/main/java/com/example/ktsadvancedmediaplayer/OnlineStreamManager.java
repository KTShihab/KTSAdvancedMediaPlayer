package com.example.ktsadvancedmediaplayer;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OnlineStreamManager {

    private static final String TAG = "OnlineStreamManager";
    public static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 KTSMediaPlayer/1.0";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final Pattern PLS_FILE_PATTERN = Pattern.compile("^File\\d+=(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLS_TITLE_PATTERN = Pattern.compile("^Title\\d+=(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern ASX_REF_PATTERN = Pattern.compile("<(?:ref|REF)\\s+(?:href|HREF)\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern M3U_TITLE_PATTERN = Pattern.compile("^#EXTINF:.*?,\\s*(.+)$", Pattern.CASE_INSENSITIVE);

    public static class StreamInfo {
        @NonNull public final String originalUrl;
        @NonNull public final String streamUrl;
        @Nullable public final String title;
        @Nullable public final String mimeType;

        public StreamInfo(@NonNull String originalUrl, @NonNull String streamUrl, @Nullable String title, @Nullable String mimeType) {
            this.originalUrl = originalUrl;
            this.streamUrl = streamUrl;
            this.title = title;
            this.mimeType = mimeType;
        }

        public MediaItem toMediaItem() {
            MediaItem.Builder builder = new MediaItem.Builder().setUri(Uri.parse(streamUrl));
            if (mimeType != null) {
                builder.setMimeType(mimeType);
            }
            if (title != null && !title.isEmpty()) {
                builder.setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build());
            }
            return builder.build();
        }
    }

    public interface StreamResolveCallback {
        void onResolved(@NonNull StreamInfo streamInfo);
        void onError(@NonNull String errorMessage);
    }

    /**
     * Normalizes the URL string by trimming whitespace and ensuring http/https scheme.
     */
    public static String normalizeUrl(String input) {
        if (input == null) return "";
        String trimmed = input.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() > 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            if (trimmed.contains(".") && !trimmed.contains(" ")) {
                trimmed = "http://" + trimmed;
            }
        }
        return trimmed;
    }

    /**
     * Resolves an online stream URL asynchronously: handles redirects, parses playlist
     * formats (.pls, .m3u, .asx), identifies HLS (.m3u8) / DASH (.mpd), and extracts ICY headers.
     */
    public static void resolveStreamAsync(@NonNull String rawUrl, @NonNull StreamResolveCallback callback) {
        final String initialUrl = normalizeUrl(rawUrl);
        if (initialUrl.isEmpty() || (!initialUrl.startsWith("http://") && !initialUrl.startsWith("https://"))) {
            mainHandler.post(() -> callback.onError("Invalid stream URL."));
            return;
        }

        executor.execute(() -> {
            try {
                StreamInfo info = inspectAndResolve(initialUrl);
                mainHandler.post(() -> callback.onResolved(info));
            } catch (Exception e) {
                Log.w(TAG, "Stream inspection failed, falling back to direct URL: " + e.getMessage());
                // Fallback directly to the input URL so ExoPlayer can still attempt to play it
                String fallbackMime = getMimeTypeFromUrl(initialUrl);
                String displayTitle = getTitleFromUrl(initialUrl);
                StreamInfo fallbackInfo = new StreamInfo(initialUrl, initialUrl, displayTitle, fallbackMime);
                mainHandler.post(() -> callback.onResolved(fallbackInfo));
            }
        });
    }

    private static StreamInfo inspectAndResolve(String urlStr) {
        String currentUrl = urlStr;
        String resolvedTitle = null;
        String detectedMime = null;
        int redirects = 0;

        while (redirects < 6) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(currentUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", DEFAULT_USER_AGENT);
                conn.setRequestProperty("Accept", "*/*");
                conn.setRequestProperty("Icy-MetaData", "1");

                int code = conn.getResponseCode();

                // Handle redirects (including cross-protocol HTTP <-> HTTPS)
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP
                        || code == HttpURLConnection.HTTP_SEE_OTHER || code == 307 || code == 308) {
                    String location = conn.getHeaderField("Location");
                    if (location != null && !location.isEmpty()) {
                        if (!location.startsWith("http://") && !location.startsWith("https://")) {
                            location = new URL(url, location).toString();
                        }
                        currentUrl = location;
                        redirects++;
                        continue;
                    }
                }

                // Check ICY station name header
                String icyName = conn.getHeaderField("icy-name");
                if (icyName == null) icyName = conn.getHeaderField("ice-name");
                if (icyName == null) icyName = conn.getHeaderField("x-audiocast-name");
                if (icyName != null && !icyName.trim().isEmpty()) {
                    resolvedTitle = icyName.trim();
                }

                String contentType = conn.getContentType();
                if (contentType != null) {
                    contentType = contentType.toLowerCase(Locale.ROOT);
                } else {
                    contentType = "";
                }

                String lowerUrl = currentUrl.toLowerCase(Locale.ROOT);

                // 1. PLS Playlist
                if (lowerUrl.contains(".pls") || contentType.contains("scpls")) {
                    StreamInfo plsResult = parsePls(conn.getInputStream(), currentUrl);
                    if (plsResult != null) {
                        return plsResult;
                    }
                }

                // 2. ASX Playlist
                if (lowerUrl.contains(".asx") || lowerUrl.contains(".wax") || contentType.contains("asx")) {
                    StreamInfo asxResult = parseAsx(conn.getInputStream(), currentUrl);
                    if (asxResult != null) {
                        return asxResult;
                    }
                }

                // 3. M3U or HLS Playlist
                if (lowerUrl.contains(".m3u8") || contentType.contains("vnd.apple.mpegurl") || contentType.contains("application/x-mpegurl")) {
                    detectedMime = MimeTypes.APPLICATION_M3U8;
                } else if (lowerUrl.contains(".m3u") || contentType.contains("mpegurl")) {
                    // Could be HLS or classic M3U
                    StreamInfo m3uResult = parseM3uOrHls(conn.getInputStream(), currentUrl);
                    if (m3uResult != null) {
                        return m3uResult;
                    }
                }

                // 4. DASH stream
                if (lowerUrl.contains(".mpd") || contentType.contains("dash+xml")) {
                    detectedMime = MimeTypes.APPLICATION_MPD;
                }

                break;
            } catch (Exception e) {
                Log.d(TAG, "Inspection step exception on " + currentUrl + ": " + e.getMessage());
                break;
            } finally {
                if (conn != null) {
                    try { conn.disconnect(); } catch (Exception ignored) {}
                }
            }
        }

        if (detectedMime == null) {
            detectedMime = getMimeTypeFromUrl(currentUrl);
        }
        if (resolvedTitle == null) {
            resolvedTitle = getTitleFromUrl(currentUrl);
        }

        return new StreamInfo(urlStr, currentUrl, resolvedTitle, detectedMime);
    }

    private static StreamInfo parsePls(InputStream in, String baseUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            String foundUrl = null;
            String foundTitle = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                Matcher fm = PLS_FILE_PATTERN.matcher(line);
                if (fm.find() && foundUrl == null) {
                    foundUrl = fm.group(1).trim();
                }
                Matcher tm = PLS_TITLE_PATTERN.matcher(line);
                if (tm.find() && foundTitle == null) {
                    foundTitle = tm.group(1).trim();
                }
            }

            if (foundUrl != null && !foundUrl.isEmpty()) {
                if (!foundUrl.startsWith("http://") && !foundUrl.startsWith("https://")) {
                    foundUrl = new URL(new URL(baseUrl), foundUrl).toString();
                }
                String mime = getMimeTypeFromUrl(foundUrl);
                return new StreamInfo(baseUrl, foundUrl, foundTitle, mime);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse PLS: " + e.getMessage());
        }
        return null;
    }

    private static StreamInfo parseAsx(InputStream in, String baseUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(" ");
            }
            Matcher m = ASX_REF_PATTERN.matcher(sb.toString());
            if (m.find()) {
                String foundUrl = m.group(1).trim();
                if (!foundUrl.startsWith("http://") && !foundUrl.startsWith("https://")) {
                    foundUrl = new URL(new URL(baseUrl), foundUrl).toString();
                }
                String mime = getMimeTypeFromUrl(foundUrl);
                return new StreamInfo(baseUrl, foundUrl, null, mime);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse ASX: " + e.getMessage());
        }
        return null;
    }

    private static StreamInfo parseM3uOrHls(InputStream in, String baseUrl) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            String foundUrl = null;
            String foundTitle = null;
            boolean isHls = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.startsWith("#EXT-X-TARGETDURATION") ||
                        line.startsWith("#EXT-X-STREAM-INF") ||
                        line.startsWith("#EXT-X-MEDIA-SEQUENCE")) {
                    isHls = true;
                    break;
                }

                Matcher tm = M3U_TITLE_PATTERN.matcher(line);
                if (tm.find() && foundTitle == null) {
                    foundTitle = tm.group(1).trim();
                }

                if (!line.startsWith("#")) {
                    if (line.startsWith("http://") || line.startsWith("https://")) {
                        if (foundUrl == null) foundUrl = line;
                    } else if (foundUrl == null) {
                        try {
                            foundUrl = new URL(new URL(baseUrl), line).toString();
                        } catch (Exception ignored) {}
                    }
                }
            }

            if (isHls) {
                return new StreamInfo(baseUrl, baseUrl, foundTitle, MimeTypes.APPLICATION_M3U8);
            }

            if (foundUrl != null && !foundUrl.isEmpty()) {
                String mime = getMimeTypeFromUrl(foundUrl);
                return new StreamInfo(baseUrl, foundUrl, foundTitle, mime);
            }
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse M3U: " + e.getMessage());
        }
        return null;
    }

    @Nullable
    public static String getMimeTypeFromUrl(String url) {
        if (url == null) return null;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains(".m3u8")) return MimeTypes.APPLICATION_M3U8;
        if (lower.contains(".mpd")) return MimeTypes.APPLICATION_MPD;
        if (lower.contains(".mp3")) return MimeTypes.AUDIO_MPEG;
        if (lower.contains(".aac")) return MimeTypes.AUDIO_AAC;
        if (lower.contains(".ogg") || lower.contains(".oga") || lower.contains(".opus")) return MimeTypes.AUDIO_OGG;
        if (lower.contains(".flac")) return MimeTypes.AUDIO_FLAC;
        if (lower.contains(".wav")) return MimeTypes.AUDIO_WAV;
        if (lower.contains(".m4a")) return MimeTypes.AUDIO_MP4;
        return null;
    }

    @Nullable
    public static String getTitleFromUrl(String url) {
        if (url == null) return null;
        try {
            Uri uri = Uri.parse(url);
            String lastPath = uri.getLastPathSegment();
            if (lastPath != null && !lastPath.isEmpty() && !lastPath.contains("=") && !lastPath.contains("&")) {
                int dot = lastPath.lastIndexOf('.');
                return (dot > 0) ? lastPath.substring(0, dot) : lastPath;
            }
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) {
                return host;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
