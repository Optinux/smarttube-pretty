package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.text.TextUtils;

import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.sharedutils.locale.LocaleUtility;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.SubtitleManager.SubtitleStyle;
import com.liskovsoft.smartyoutubetv2.common.misc.MediaServiceManager;
import com.liskovsoft.smartyoutubetv2.common.prefs.DeArrowData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData.ColorScheme;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.SponsorBlockData;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Categorizes SmartTube's settings items into logical M3 Expressive groups and computes live
 * status summaries and detail chips from active preferences.
 */
final class SettingsHubHelper {
    static final class RailEntry {
        static final int TYPE_HEADER = 0;
        static final int TYPE_QUICK_CONTROLS = 1;
        static final int TYPE_CATEGORY = 2;

        final int type;
        final String headerTitle;
        final SettingsItem settingsItem;
        boolean isFirstInGroup;
        boolean isLastInGroup;

        private RailEntry(int type, String headerTitle, SettingsItem settingsItem) {
            this.type = type;
            this.headerTitle = headerTitle;
            this.settingsItem = settingsItem;
        }

        static RailEntry header(String title) {
            return new RailEntry(TYPE_HEADER, title, null);
        }

        static RailEntry quickControls() {
            return new RailEntry(TYPE_QUICK_CONTROLS, null, null);
        }

        static RailEntry category(SettingsItem item) {
            return new RailEntry(TYPE_CATEGORY, null, item);
        }
    }

    private SettingsHubHelper() {
    }

    static List<RailEntry> buildRailEntries(Context ctx, List<SettingsItem> rawItems) {
        List<RailEntry> entries = new ArrayList<>();
        RailEntry quick = RailEntry.quickControls();
        quick.isFirstInGroup = true;
        quick.isLastInGroup = true;
        entries.add(quick);

        if (rawItems == null || rawItems.isEmpty()) {
            return entries;
        }

        List<SettingsItem> accountGroup = new ArrayList<>();
        List<SettingsItem> appearanceGroup = new ArrayList<>();
        List<SettingsItem> playbackGroup = new ArrayList<>();
        List<SettingsItem> systemGroup = new ArrayList<>();

        for (SettingsItem item : rawItems) {
            if (item == null) {
                continue;
            }
            int res = item.imageResId;
            if (res == R.drawable.settings_account || res == R.drawable.settings_language || res == R.drawable.settings_cast) {
                accountGroup.add(item);
            } else if (res == R.drawable.settings_main_ui || res == R.drawable.settings_app || res == R.drawable.settings_search) {
                appearanceGroup.add(item);
            } else if (res == R.drawable.settings_player || res == R.drawable.settings_afr || res == R.drawable.settings_subtitles) {
                playbackGroup.add(item);
            } else {
                systemGroup.add(item);
            }
        }

        appendGroup(entries, ctx.getString(R.string.settings_group_account), accountGroup);
        appendGroup(entries, ctx.getString(R.string.settings_group_appearance), appearanceGroup);
        appendGroup(entries, ctx.getString(R.string.settings_group_playback), playbackGroup);
        appendGroup(entries, ctx.getString(R.string.settings_group_extensions), systemGroup);
        return entries;
    }

    private static void appendGroup(List<RailEntry> entries, String title, List<SettingsItem> items) {
        if (items.isEmpty()) {
            return;
        }
        entries.add(RailEntry.header(title));
        for (int i = 0; i < items.size(); i++) {
            RailEntry entry = RailEntry.category(items.get(i));
            entry.isFirstInGroup = i == 0;
            entry.isLastInGroup = i == items.size() - 1;
            entries.add(entry);
        }
    }

    static String getSubtitleSummary(Context ctx, SettingsItem item) {
        if (ctx == null || item == null) {
            return "";
        }
        int res = item.imageResId;
        if (res == R.drawable.settings_account) {
            Account account = MediaServiceManager.instance().getSelectedAccount();
            if (account != null && !TextUtils.isEmpty(account.getName())) {
                return account.getName();
            }
            return ctx.getString(R.string.settings_not_signed_in);
        }
        if (res == R.drawable.settings_language) {
            Locale locale = LocaleUtility.getCurrentLocale(ctx);
            return locale.getDisplayLanguage() + " (" + locale.getCountry() + ")";
        }
        if (res == R.drawable.settings_main_ui) {
            ColorScheme scheme = MainUIData.instance(ctx).getColorScheme();
            return scheme != null ? ctx.getString(scheme.nameResId) : "";
        }
        if (res == R.drawable.settings_player) {
            float speed = PlayerData.instance(ctx).getSpeed();
            return String.format(Locale.US, "%.2gx", speed);
        }
        if (res == R.drawable.settings_afr) {
            return PlayerData.instance(ctx).isAfrEnabled()
                    ? ctx.getString(R.string.settings_active)
                    : ctx.getString(R.string.settings_off);
        }
        if (res == R.drawable.settings_subtitles) {
            SubtitleStyle style = PlayerData.instance(ctx).getSubtitleStyle();
            return style != null && style.nameResId > 0 ? ctx.getString(style.nameResId) : "";
        }
        if (res == R.drawable.settings_block) {
            return SponsorBlockData.instance(ctx).isSponsorBlockEnabled()
                    ? ctx.getString(R.string.settings_enabled)
                    : ctx.getString(R.string.settings_disabled);
        }
        if (res == R.drawable.settings_dearrow) {
            return DeArrowData.instance(ctx).isDeArrowEnabled()
                    ? ctx.getString(R.string.settings_enabled)
                    : ctx.getString(R.string.settings_disabled);
        }
        if (res == R.drawable.settings_about) {
            return getAppVersion(ctx);
        }
        return "";
    }

    static List<String> getDetailChips(Context ctx, SettingsItem item) {
        if (ctx == null || item == null) {
            return Arrays.asList();
        }
        MainUIData uiData = MainUIData.instance(ctx);
        PlayerData playerData = PlayerData.instance(ctx);
        int res = item.imageResId;

        if (res == R.drawable.settings_main_ui) {
            ColorScheme scheme = uiData.getColorScheme();
            String schemeStr = scheme != null ? ctx.getString(scheme.nameResId) : "";
            int accent = uiData.getAccentColor();
            String accentStr = accent != 0 ? ThemePaletteHelper.formatHex(accent) : ctx.getString(R.string.accent_color_default);
            String gridStr = ctx.getString(R.string.video_grid_scale) + ": " + String.format(Locale.US, "%.2gx", uiData.getVideoGridScale());
            return Arrays.asList(schemeStr, accentStr, gridStr);
        }
        if (res == R.drawable.settings_account) {
            Account account = MediaServiceManager.instance().getSelectedAccount();
            String name = account != null && !TextUtils.isEmpty(account.getName())
                    ? account.getName()
                    : ctx.getString(R.string.settings_not_signed_in);
            return Arrays.asList(name);
        }
        if (res == R.drawable.settings_player) {
            String speed = String.format(Locale.US, "%.2gx", playerData.getSpeed());
            String afr = ctx.getString(R.string.auto_frame_rate) + ": " + (playerData.isAfrEnabled()
                    ? ctx.getString(R.string.settings_active)
                    : ctx.getString(R.string.settings_off));
            return Arrays.asList(speed, afr);
        }
        if (res == R.drawable.settings_block) {
            SponsorBlockData sb = SponsorBlockData.instance(ctx);
            String state = sb.isSponsorBlockEnabled() ? ctx.getString(R.string.settings_enabled) : ctx.getString(R.string.settings_disabled);
            String count = sb.getEnabledCategories().size() + " categories";
            return Arrays.asList(state, count);
        }
        if (res == R.drawable.settings_dearrow) {
            DeArrowData da = DeArrowData.instance(ctx);
            String state = da.isDeArrowEnabled() ? ctx.getString(R.string.settings_enabled) : ctx.getString(R.string.settings_disabled);
            return Arrays.asList(state);
        }
        String single = getSubtitleSummary(ctx, item);
        return TextUtils.isEmpty(single) ? Arrays.asList() : Arrays.asList(single);
    }

    static String getCategoryDescription(Context ctx, SettingsItem item) {
        if (ctx == null || item == null) {
            return "";
        }
        int res = item.imageResId;
        if (res == R.drawable.settings_account) {
            return "Switch Google/YouTube accounts, sign in with TV code, or protect profiles with a settings PIN.";
        }
        if (res == R.drawable.settings_cast) {
            return "Link mobile phones and browsers to cast videos directly to this TV screen.";
        }
        if (res == R.drawable.settings_language) {
            return "Choose app interface language, content region, and localized feed preferences.";
        }
        if (res == R.drawable.settings_app) {
            return "Configure startup section, pinned sidebar items, content filters, remote key remapping, and background playback.";
        }
        if (res == R.drawable.settings_main_ui) {
            return "Personalize Material 3 Expressive color schemes, accent colors, video card previews, grid density, and UI scale.";
        }
        if (res == R.drawable.settings_player) {
            return "Fine-tune default video codecs and resolution presets, playback speed, buffering, seek steps, and player controls.";
        }
        if (res == R.drawable.settings_afr) {
            return "Automatically match HDMI display refresh rate to video framerate (24Hz, 50Hz, 60Hz) for judder-free playback.";
        }
        if (res == R.drawable.settings_subtitles) {
            return "Customize caption styles, typography scale, vertical position, and preferred subtitle languages.";
        }
        if (res == R.drawable.settings_search) {
            return "Manage voice recognition engine, search history, and instant query suggestions.";
        }
        if (res == R.drawable.settings_block) {
            return "Automatically skip or highlight sponsors, intros, outros, and self-promotions via SponsorBlock.";
        }
        if (res == R.drawable.settings_dearrow) {
            return "Replace sensational clickbait titles and thumbnails with community-curated summaries.";
        }
        if (res == R.drawable.settings_backup) {
            return "Back up and restore your preferences, pinned sections, and settings across devices.";
        }
        if (res == R.drawable.settings_about) {
            return "View app build version, diagnostic info, and open-source details.";
        }
        return "";
    }

    private static String getAppVersion(Context ctx) {
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0);
            return info.versionName != null ? "v" + info.versionName : "";
        } catch (Exception e) {
            return "";
        }
    }
}
