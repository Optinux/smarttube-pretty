package com.liskovsoft.smartyoutubetv2.common.utils;

import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;

/**
 * Computes Material You dynamic tonal palettes (API 31+ Monet system colors, API 27+ WallpaperColors,
 * or fallback tonal generation) and custom accent color overrides for the TV UI.
 */
public final class ThemePaletteHelper {
    // M3 Baseline Lavender fallback when device has no wallpaper/Monet colors
    private static final int FALLBACK_MATERIAL_YOU_SEED = 0xFFD0BCFF;

    public static final class M3Palette {
        public final boolean overrideSurfaces;
        public int surface;
        public int surfaceContainerLow;
        public int surfaceContainer;
        public int surfaceContainerHigh;
        public int surfaceContainerHighest;
        public int primary;
        public int onPrimary;
        public int primaryContainer;
        public int onPrimaryContainer;
        public int secondaryContainer;
        public int onSecondaryContainer;
        public int onSurface;
        public int onSurfaceVariant;
        public int outline;
        public int outlineVariant;
        public int orbBackground;
        public int orbFocusedBackground;
        public int orbIconColor;
        public int progressPrimary;

        M3Palette(boolean overrideSurfaces) {
            this.overrideSurfaces = overrideSurfaces;
        }
    }

    private static int sCachedSchemeIdx = -1;
    private static int sCachedAccentColor = 0;
    private static int sCachedSystemSeed = 0;
    private static M3Palette sCachedPalette;

    private ThemePaletteHelper() {
    }

    public static void invalidateCache() {
        sCachedPalette = null;
        sCachedSchemeIdx = -1;
    }

    /**
     * Returns an active dynamic palette when Material You or a custom accent color is enabled,
     * or {@code null} when the static XML color scheme should be used as-is.
     */
    @Nullable
    public static M3Palette getActivePalette(Context context) {
        if (context == null) {
            return null;
        }

        MainUIData uiData = MainUIData.instance(context);
        boolean isMaterialYou = uiData.isMaterialYouScheme();
        int customAccent = uiData.getAccentColor();

        if (!isMaterialYou && customAccent == 0) {
            return null;
        }

        int schemeIdx = uiData.getColorSchemes().indexOf(uiData.getColorScheme());
        int systemSeed = isMaterialYou ? getSystemSeedKey(context) : 0;

        if (sCachedPalette != null
                && sCachedSchemeIdx == schemeIdx
                && sCachedAccentColor == customAccent
                && sCachedSystemSeed == systemSeed) {
            return sCachedPalette;
        }

        M3Palette palette;
        if (isMaterialYou) {
            palette = buildMaterialYouPalette(context, uiData.isOledScheme());
            if (customAccent != 0) {
                applyAccentTokens(palette, customAccent);
                palette.orbFocusedBackground = palette.primaryContainer;
            }
        } else {
            palette = new M3Palette(false);
            applyAccentTokens(palette, customAccent);
            palette.orbFocusedBackground = palette.primaryContainer;
        }

        sCachedSchemeIdx = schemeIdx;
        sCachedAccentColor = customAccent;
        sCachedSystemSeed = systemSeed;
        sCachedPalette = palette;
        return palette;
    }

    private static int getSystemSeedKey(Context context) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                return ContextCompat.getColor(context, android.R.color.system_accent1_200)
                        ^ ContextCompat.getColor(context, android.R.color.system_neutral1_900);
            } catch (Exception ignored) {
            }
        }
        return extractWallpaperSeed(context);
    }

    private static M3Palette buildMaterialYouPalette(Context context, boolean isOled) {
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                M3Palette palette = new M3Palette(true);
                int n1_900 = ContextCompat.getColor(context, android.R.color.system_neutral1_900);
                int n1_100 = ContextCompat.getColor(context, android.R.color.system_neutral1_100);
                int n2_200 = ContextCompat.getColor(context, android.R.color.system_neutral2_200);
                int n2_700 = ContextCompat.getColor(context, android.R.color.system_neutral2_700);
                int n2_800 = ContextCompat.getColor(context, android.R.color.system_neutral2_800);

                int a1_100 = ContextCompat.getColor(context, android.R.color.system_accent1_100);
                int a1_200 = ContextCompat.getColor(context, android.R.color.system_accent1_200);
                int a1_300 = ContextCompat.getColor(context, android.R.color.system_accent1_300);
                int a1_700 = ContextCompat.getColor(context, android.R.color.system_accent1_700);
                int a1_800 = ContextCompat.getColor(context, android.R.color.system_accent1_800);
                int a2_100 = ContextCompat.getColor(context, android.R.color.system_accent2_100);
                int a2_700 = ContextCompat.getColor(context, android.R.color.system_accent2_700);

                if (isOled) {
                    palette.surface = Color.BLACK;
                    palette.surfaceContainerLow = ColorUtils.blendARGB(Color.BLACK, n1_900, 0.45f);
                    palette.surfaceContainer = ColorUtils.blendARGB(Color.BLACK, n1_900, 0.75f);
                    palette.surfaceContainerHigh = n1_900;
                    palette.surfaceContainerHighest = n2_800;
                } else {
                    palette.surface = ColorUtils.blendARGB(n1_900, Color.BLACK, 0.35f);
                    palette.surfaceContainerLow = n1_900;
                    palette.surfaceContainer = ColorUtils.blendARGB(n1_900, n2_800, 0.40f);
                    palette.surfaceContainerHigh = ColorUtils.blendARGB(n1_900, n2_800, 0.78f);
                    palette.surfaceContainerHighest = n2_800;
                }

                palette.primary = a1_200;
                palette.onPrimary = a1_800;
                palette.primaryContainer = a1_700;
                palette.onPrimaryContainer = a1_100;
                palette.secondaryContainer = a2_700;
                palette.onSecondaryContainer = a2_100;
                palette.onSurface = n1_100;
                palette.onSurfaceVariant = n2_200;
                palette.outline = a1_200;
                palette.outlineVariant = n2_700;
                palette.orbBackground = palette.surfaceContainerHigh;
                palette.orbFocusedBackground = a1_700;
                palette.orbIconColor = n1_100;
                palette.progressPrimary = a1_300;
                return palette;
            } catch (Exception ignored) {
            }
        }

        int seed = extractWallpaperSeed(context);
        M3Palette palette = new M3Palette(true);
        applySurfaceTokens(palette, seed, isOled);
        applyAccentTokens(palette, seed);
        palette.orbBackground = palette.surfaceContainerHigh;
        palette.orbFocusedBackground = palette.primaryContainer;
        palette.orbIconColor = palette.onSurface;
        return palette;
    }

    private static int extractWallpaperSeed(Context context) {
        if (Build.VERSION.SDK_INT >= 27) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(context);
                if (wm != null) {
                    WallpaperColors colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM);
                    if (colors != null) {
                        return colors.getPrimaryColor().toArgb();
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return FALLBACK_MATERIAL_YOU_SEED;
    }

    private static void applySurfaceTokens(M3Palette palette, int seedColor, boolean isOled) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(seedColor | 0xFF000000, hsl);
        float hue = hsl[0];
        float tintSat = hsl[1] < 0.08f ? 0.02f : clamp(hsl[1] * 0.22f, 0.05f, 0.18f);

        if (isOled) {
            palette.surface = Color.BLACK;
            palette.surfaceContainerLow = hslColor(hue, tintSat, 0.05f);
            palette.surfaceContainer = hslColor(hue, tintSat, 0.09f);
            palette.surfaceContainerHigh = hslColor(hue, tintSat, 0.13f);
            palette.surfaceContainerHighest = hslColor(hue, tintSat, 0.17f);
        } else {
            palette.surface = hslColor(hue, tintSat, 0.07f);
            palette.surfaceContainerLow = hslColor(hue, tintSat, 0.10f);
            palette.surfaceContainer = hslColor(hue, tintSat, 0.13f);
            palette.surfaceContainerHigh = hslColor(hue, tintSat, 0.17f);
            palette.surfaceContainerHighest = hslColor(hue, tintSat, 0.22f);
        }

        palette.onSurface = 0xFFECEDF2;
        palette.onSurfaceVariant = hslColor(hue, clamp(tintSat * 0.85f, 0.03f, 0.14f), 0.72f);
        palette.outlineVariant = hslColor(hue, tintSat, 0.24f);
    }

    public static void applyAccentTokens(M3Palette palette, int seedColor) {
        int opaqueSeed = seedColor | 0xFF000000;
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(opaqueSeed, hsl);

        if (hsl[1] < 0.08f) {
            // Monochrome / neutral accent
            palette.primary = 0xFFE2E2E8;
            palette.onPrimary = 0xFF16161A;
            palette.primaryContainer = 0xFF383842;
            palette.onPrimaryContainer = 0xFFFFFFFF;
            palette.secondaryContainer = 0xFF2D2D35;
            palette.onSecondaryContainer = 0xFFF2F2F7;
            palette.outline = 0xFFD4D4DD;
            palette.progressPrimary = 0xFFE2E2E8;
            return;
        }

        float hue = hsl[0];
        float sat = clamp(hsl[1], 0.45f, 0.92f);

        palette.primary = hslColor(hue, sat, 0.76f);
        palette.onPrimary = hslColor(hue, clamp(sat, 0.40f, 0.75f), 0.18f);
        palette.primaryContainer = hslColor(hue, clamp(sat * 0.78f, 0.32f, 0.75f), 0.26f);
        palette.onPrimaryContainer = hslColor(hue, clamp(sat * 0.85f, 0.35f, 0.90f), 0.90f);
        palette.secondaryContainer = hslColor(hue, clamp(sat * 0.40f, 0.15f, 0.42f), 0.22f);
        palette.onSecondaryContainer = hslColor(hue, clamp(sat * 0.45f, 0.15f, 0.55f), 0.92f);
        palette.outline = palette.primary;
        palette.progressPrimary = hslColor(hue, clamp(hsl[1], 0.55f, 0.95f), clamp(hsl[2], 0.58f, 0.74f));
    }

    /**
     * Generates a readable swatch color for displaying in UI controls.
     */
    public static int toDisplayAccent(int color) {
        if (color == 0) {
            return 0;
        }
        return color | 0xFF000000;
    }

    public static String formatHex(int color) {
        return String.format("#%06X", (0xFFFFFF & color));
    }

    private static int hslColor(float h, float s, float l) {
        return ColorUtils.HSLToColor(new float[]{h, s, l});
    }

    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
}
