package com.example.ktsadvancedmediaplayer;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ThemeManager {

    private static final String PREF_NAME = "KTS_Media_Bookmarks";
    private static final String KEY_CURRENT_THEME = "app_skin_theme_id";

    public static class ThemeItem {
        public final String id;
        public final String displayName;
        public final String malayalamName;
        public final int primaryColor;
        public final int secondaryColor;
        public final int bgColor;
        public final int surfaceColor;
        public final int buttonColor;
        public final int headerColor;

        public ThemeItem(String id, String displayName, String malayalamName,
                         int primaryColor, int secondaryColor,
                         int bgColor, int surfaceColor, int buttonColor, int headerColor) {
            this.id = id;
            this.displayName = displayName;
            this.malayalamName = malayalamName;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.bgColor = bgColor;
            this.surfaceColor = surfaceColor;
            this.buttonColor = buttonColor;
            this.headerColor = headerColor;
        }
    }

    private static final List<ThemeItem> THEMES = new ArrayList<>();

    static {
        // 1. Royal Violet (Default)
        THEMES.add(new ThemeItem(
                "royal_violet",
                "💜 Royal Violet",
                "റോയൽ വയലറ്റ്",
                0xFF7C4DFF, // Primary Violet Neon
                0xFFB388FF, // Secondary Lavender
                0xFF121019, // Deep Background
                0xFF1D1829, // Surface Card
                0xFF2A223B, // Button
                0xFFD1C4E9  // Heading
        ));

        // 2. Emerald Cyber
        THEMES.add(new ThemeItem(
                "emerald_cyber",
                "💚 Emerald Cyber",
                "സൈബർ ഗ്രീൻ",
                0xFF00E676, // Neon Mint
                0xFF69F0AE, // Light Sage
                0xFF0A130E, // Cyber Forest
                0xFF112318, // Jade Surface
                0xFF183625, // Button
                0xFFB9F6CA  // Heading
        ));

        // 3. Oceanic Azure
        THEMES.add(new ThemeItem(
                "oceanic_azure",
                "💙 Oceanic Azure",
                "ഓഷ്യൻ ബ്ലൂ",
                0xFF00B0FF, // Electric Cyan
                0xFF80D8FF, // Sky Blue
                0xFF0A121D, // Abyss Navy
                0xFF112033, // Sapphire Surface
                0xFF18314F, // Button
                0xFF80D8FF  // Heading
        ));

        // 4. Sunset Gold
        THEMES.add(new ThemeItem(
                "sunset_gold",
                "🧡 Sunset Gold",
                "സൺസെറ്റ് ഗോൾഡ്",
                0xFFFF9100, // Vibrant Amber
                0xFFFFD54F, // Warm Gold
                0xFF150E0A, // Dark Mocha
                0xFF241710, // Bronze Surface
                0xFF382317, // Button
                0xFFFFE082  // Heading
        ));

        // 5. Neon Synthwave
        THEMES.add(new ThemeItem(
                "neon_synthwave",
                "💖 Neon Synthwave",
                "നിയോൺ സിന്ത്‌വേവ്",
                0xFFFF4081, // Hot Neon Pink
                0xFFEA80FC, // Cyber Magenta
                0xFF140A18, // Synth Dark
                0xFF26122C, // Velvet Magenta Surface
                0xFF3A1A43, // Button
                0xFFFF80AB  // Heading
        ));

        // 6. AMOLED Pure Dark
        THEMES.add(new ThemeItem(
                "amoled_dark",
                "🖤 AMOLED Pure Dark",
                "പ്യുവർ ഡാർക്ക്",
                0xFFFFFFFF, // Crisp White
                0xFFBDBDBD, // Silver
                0xFF000000, // Pure Black
                0xFF121212, // Dark Gray Surface
                0xFF222222, // Button
                0xFFE0E0E0  // Heading
        ));
    }

    public static List<ThemeItem> getAllThemes() {
        return Collections.unmodifiableList(THEMES);
    }

    public static ThemeItem getCurrentTheme(Context context) {
        if (context == null) return THEMES.get(0);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String themeId = prefs.getString(KEY_CURRENT_THEME, "royal_violet");
        for (ThemeItem t : THEMES) {
            if (t.id.equals(themeId)) {
                return t;
            }
        }
        return THEMES.get(0);
    }

    public static void setCurrentTheme(Context context, String themeId) {
        if (context == null || themeId == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_CURRENT_THEME, themeId).apply();
    }
}
