package com.example.ktsadvancedmediaplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDriveManager {

    private static final String TAG = "GoogleDriveManager";

    public static class DriveTrack {
        public final String id;
        public final String title;
        public final String streamUrl;

        public DriveTrack(String id, String title, String streamUrl) {
            this.id = id;
            this.title = title;
            this.streamUrl = streamUrl;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public interface DriveFolderCallback {
        void onTracksLoaded(String folderName, List<DriveTrack> tracks);
        void onError(String errorMessage);
    }

    private static final Pattern FOLDER_URL_PATTERN = Pattern.compile("/folders/([0-9A-Za-z_-]{10,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_URL_PATTERN = Pattern.compile("/file/d/([0-9A-Za-z_-]{10,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern OPEN_ID_PATTERN = Pattern.compile("[?&]id=([0-9A-Za-z_-]{10,})", Pattern.CASE_INSENSITIVE);
    private static final Pattern UC_ID_PATTERN = Pattern.compile("/uc\\?.*id=([0-9A-Za-z_-]{10,})", Pattern.CASE_INSENSITIVE);

    public static boolean isGoogleDriveUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("drive.google.com") || lower.contains("docs.google.com") || lower.contains("drive.usercontent.google.com");
    }

    public static boolean isDriveFolder(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("/folders/") || (lower.contains("drive.google.com") && lower.contains("#list"));
    }

    public static boolean isDriveFile(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("/file/d/") || lower.contains("/uc?") || lower.contains("drive.usercontent.google.com/download");
    }

    public static String extractDriveId(String url) {
        if (url == null) return null;

        Matcher matcher = FOLDER_URL_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1);

        matcher = FILE_URL_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1);

        matcher = OPEN_ID_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1);

        matcher = UC_ID_PATTERN.matcher(url);
        if (matcher.find()) return matcher.group(1);

        // Raw ID check (typically 25-45 characters alphanumeric)
        if (url.matches("^[0-9A-Za-z_-]{15,50}$")) {
            return url;
        }

        return null;
    }

    public static String getDirectStreamUrl(String fileId) {
        if (fileId == null || fileId.isEmpty()) return "";
        return "https://drive.usercontent.google.com/download?id=" + fileId + "&export=download&authuser=0";
    }

    public static String getFallbackStreamUrl(String fileId) {
        if (fileId == null || fileId.isEmpty()) return "";
        return "https://drive.google.com/uc?export=download&id=" + fileId;
    }

    public static void fetchFolderTracksAsync(Context context, String folderIdOrUrl, DriveFolderCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final String folderId = extractDriveId(folderIdOrUrl);

        if (folderId == null || folderId.isEmpty()) {
            mainHandler.post(() -> callback.onError("Invalid Google Drive folder link or ID."));
            return;
        }

        new Thread(() -> {
            try {
                SharedPreferences prefs = context.getSharedPreferences("KTS_Media_Library", Context.MODE_PRIVATE);
                String apiKey = prefs.getString("gdrive_api_key", "").trim();

                // 1. Attempt official API if key provided
                if (!apiKey.isEmpty()) {
                    try {
                        List<DriveTrack> apiTracks = fetchViaDriveApi(folderId, apiKey);
                        if (!apiTracks.isEmpty()) {
                            apiTracks.sort((t1, t2) -> ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(t1.title, t2.title));
                            mainHandler.post(() -> callback.onTracksLoaded("Google Drive", apiTracks));
                            return;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Drive API query failed, falling back to web parser: " + e.getMessage());
                    }
                }

                // 2. Fetch via embeddedfolderview endpoint
                String embedUrl = "https://drive.google.com/embeddedfolderview?id=" + folderId + "#list";
                String html = httpGet(embedUrl);

                String folderTitle = "Google Drive Folder";
                Matcher titleMatcher = Pattern.compile("<title>([^<]+)</title>", Pattern.CASE_INSENSITIVE).matcher(html);
                if (titleMatcher.find()) {
                    String rawTitle = titleMatcher.group(1).trim();
                    rawTitle = rawTitle.replace("- Google Drive", "").replace("Google Drive -", "").trim();
                    if (!rawTitle.isEmpty() && !rawTitle.equalsIgnoreCase("Meet Google Drive – One place for all your files")) {
                        folderTitle = rawTitle;
                    }
                }

                Map<String, String> idToTitleMap = new HashMap<>();

                // Matcher 1: Standard flip-entry HTML
                // <a href="https://drive.google.com/file/d/ID/edit?usp=sharing"...><div class="flip-entry-title">Title.mp3</div>
                Pattern flipPattern = Pattern.compile("<a[^>]+href=\"https://drive\\.google\\.com/(?:file/d/|open\\?id=)([0-9A-Za-z_-]+)[^\"]*\"[^>]*>.*?<div class=\"flip-entry-title\">([^<]+)</div>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher flipMatcher = flipPattern.matcher(html);
                while (flipMatcher.find()) {
                    String id = flipMatcher.group(1);
                    String title = unescapeHtml(flipMatcher.group(2).trim());
                    if (ZipMediaManager.isAudioFile(title)) {
                        idToTitleMap.put(id, title);
                    }
                }

                // Matcher 2: Alternative entry layout with flip-entry-title span
                Pattern spanPattern = Pattern.compile("href=\"[^\"]*(?:/file/d/|id=)([0-9A-Za-z_-]{15,})[^\"]*\"[^>]*>.*?flip-entry-title[^>]*>([^<]+)<", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher spanMatcher = spanPattern.matcher(html);
                while (spanMatcher.find()) {
                    String id = spanMatcher.group(1);
                    String title = unescapeHtml(spanMatcher.group(2).trim());
                    if (ZipMediaManager.isAudioFile(title)) {
                        idToTitleMap.put(id, title);
                    }
                }

                // Matcher 3: Data-id and aria-label
                Pattern dataIdPattern = Pattern.compile("data-id=\"([0-9A-Za-z_-]{15,})\"[^>]*aria-label=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
                Matcher dataIdMatcher = dataIdPattern.matcher(html);
                while (dataIdMatcher.find()) {
                    String id = dataIdMatcher.group(1);
                    String title = unescapeHtml(dataIdMatcher.group(2).trim());
                    if (ZipMediaManager.isAudioFile(title)) {
                        idToTitleMap.put(id, title);
                    }
                }

                // Matcher 4: JSON initial state arrays: ["FILE_ID", ... "FILE_NAME.mp3"]
                Pattern jsonArrayPattern = Pattern.compile("\\[\"([0-9A-Za-z_-]{20,40})\"\\s*,\\s*\\[\"([^\"]+\\.(?:mp3|wav|m4a|ogg|aac|flac|opus|m4b|mp4|mkv|webm|3gp|wma))\"", Pattern.CASE_INSENSITIVE);
                Matcher jsonMatcher = jsonArrayPattern.matcher(html);
                while (jsonMatcher.find()) {
                    String id = jsonMatcher.group(1);
                    String title = unescapeHtml(jsonMatcher.group(2).trim());
                    if (ZipMediaManager.isAudioFile(title)) {
                        idToTitleMap.put(id, title);
                    }
                }

                // Matcher 5: General /file/d/ links with extension in link or text
                Pattern generalFilePattern = Pattern.compile("/file/d/([0-9A-Za-z_-]{20,40})/(?:view|preview|edit)?[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE);
                Matcher generalMatcher = generalFilePattern.matcher(html);
                while (generalMatcher.find()) {
                    String id = generalMatcher.group(1);
                    String title = unescapeHtml(generalMatcher.group(2).trim());
                    if (ZipMediaManager.isAudioFile(title)) {
                        idToTitleMap.put(id, title);
                    }
                }

                if (idToTitleMap.isEmpty()) {
                    // Try the direct folder page
                    String directUrl = "https://drive.google.com/drive/folders/" + folderId;
                    String directHtml = httpGet(directUrl);

                    Matcher directJsonMatcher = jsonArrayPattern.matcher(directHtml);
                    while (directJsonMatcher.find()) {
                        String id = directJsonMatcher.group(1);
                        String title = unescapeHtml(directJsonMatcher.group(2).trim());
                        if (ZipMediaManager.isAudioFile(title)) {
                            idToTitleMap.put(id, title);
                        }
                    }
                }

                if (idToTitleMap.isEmpty()) {
                    mainHandler.post(() -> callback.onError("No audio files found or folder is not publicly accessible. Ensure folder sharing is set to 'Anyone with the link'."));
                    return;
                }

                List<DriveTrack> tracks = new ArrayList<>();
                for (Map.Entry<String, String> entry : idToTitleMap.entrySet()) {
                    String id = entry.getKey();
                    String title = entry.getValue();
                    tracks.add(new DriveTrack(id, title, getDirectStreamUrl(id)));
                }

                // Sort in natural track order
                tracks.sort((t1, t2) -> ZipMediaManager.NATURAL_ORDER_COMPARATOR.compare(t1.title, t2.title));

                final String finalTitle = folderTitle;
                mainHandler.post(() -> callback.onTracksLoaded(finalTitle, tracks));

            } catch (Exception e) {
                Log.e(TAG, "Error fetching Google Drive folder: " + folderIdOrUrl, e);
                mainHandler.post(() -> callback.onError("Error loading Google Drive folder: " + e.getLocalizedMessage()));
            }
        }).start();
    }

    private static List<DriveTrack> fetchViaDriveApi(String folderId, String apiKey) throws Exception {
        String apiUrl = "https://www.googleapis.com/drive/v3/files?q='" + folderId + "'+in+parents+and+trashed=false&fields=files(id,name,mimeType)&key=" + apiKey;
        String json = httpGet(apiUrl);
        List<DriveTrack> tracks = new ArrayList<>();

        JSONObject root = new JSONObject(json);
        if (root.has("files")) {
            JSONArray files = root.getJSONArray("files");
            for (int i = 0; i < files.length(); i++) {
                JSONObject item = files.getJSONObject(i);
                String id = item.optString("id");
                String name = item.optString("name");
                String mime = item.optString("mimeType", "");

                if (ZipMediaManager.isAudioFile(name) || mime.startsWith("audio/") || mime.startsWith("video/")) {
                    tracks.add(new DriveTrack(id, name, getDirectStreamUrl(id)));
                }
            }
        }
        return tracks;
    }

    private static String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,application/json,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        conn.setInstanceFollowRedirects(true);

        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            if (newUrl != null && !newUrl.isEmpty()) {
                return httpGet(newUrl);
            }
        }

        if (responseCode >= 400) {
            throw new Exception("HTTP error code " + responseCode);
        }

        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    private static String unescapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .trim();
    }
}
