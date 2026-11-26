package com.liskovsoft.smartyoutubetv2.common.updater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistent preferences for the built-in app updater.
 */
public final class UpdatePrefs {
    public static final String PREFS_NAME = "com.liskovsoft.smartyoutubetv2.updater.preferences";

    private static final String KEY_AUTO_CHECK_ENABLED = "auto_check_enabled";
    private static final String KEY_LAST_CHECK_TIME_MS = "last_check_time_ms";
    private static final String KEY_DOWNLOAD_URL = "download_url";
    private static final String KEY_DOWNLOAD_SHA256 = "download_sha256";
    private static final String KEY_PENDING_APK_PATH = "pending_apk_path";
    private static final String KEY_PENDING_VERSION_CODE = "pending_version_code";

    @SuppressLint("StaticFieldLeak")
    private static UpdatePrefs sInstance;
    private final SharedPreferences mPrefs;

    private UpdatePrefs(Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized UpdatePrefs instance(Context context) {
        if (sInstance == null) {
            sInstance = new UpdatePrefs(context);
        }
        return sInstance;
    }

    public boolean isAutoCheckEnabled() {
        return mPrefs.getBoolean(KEY_AUTO_CHECK_ENABLED, true);
    }

    public void setAutoCheckEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_AUTO_CHECK_ENABLED, enabled).apply();
    }

    public long getLastCheckTimeMs() {
        return mPrefs.getLong(KEY_LAST_CHECK_TIME_MS, 0L);
    }

    public void setLastCheckTimeMs(long timestampMs) {
        mPrefs.edit().putLong(KEY_LAST_CHECK_TIME_MS, timestampMs).apply();
    }

    public String getDownloadUrl() {
        return mPrefs.getString(KEY_DOWNLOAD_URL, null);
    }

    public String getDownloadSha256() {
        return mPrefs.getString(KEY_DOWNLOAD_SHA256, null);
    }

    public void setDownloadMetadata(String url, String sha256) {
        mPrefs.edit()
                .putString(KEY_DOWNLOAD_URL, url)
                .putString(KEY_DOWNLOAD_SHA256, sha256)
                .apply();
    }

    public void clearDownloadMetadata() {
        mPrefs.edit()
                .remove(KEY_DOWNLOAD_URL)
                .remove(KEY_DOWNLOAD_SHA256)
                .apply();
    }

    public String getPendingApkPath() {
        return mPrefs.getString(KEY_PENDING_APK_PATH, null);
    }

    public long getPendingVersionCode() {
        return mPrefs.getLong(KEY_PENDING_VERSION_CODE, -1L);
    }

    public void setPendingInstall(String apkPath, long versionCode) {
        mPrefs.edit()
                .putString(KEY_PENDING_APK_PATH, apkPath)
                .putLong(KEY_PENDING_VERSION_CODE, versionCode)
                .apply();
    }

    public void clearPendingInstall() {
        mPrefs.edit()
                .remove(KEY_PENDING_APK_PATH)
                .remove(KEY_PENDING_VERSION_CODE)
                .apply();
    }
}
