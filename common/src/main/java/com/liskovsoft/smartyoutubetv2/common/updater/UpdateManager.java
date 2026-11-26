package com.liskovsoft.smartyoutubetv2.common.updater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordinates manifest checks, resumable APK downloads, cryptographic verification, installation, and cleanup.
 */
public final class UpdateManager {
    private static final String TAG = UpdateManager.class.getSimpleName();
    private static final long AUTO_CHECK_COOLDOWN_MS = 30 * 60 * 1000L; // 30 minutes

    public interface CheckCallback {
        void onUpdateAvailable(UpdateManifest manifest, boolean requiresUpgrade);
        void onUpToDate();
        void onError(Exception error);
    }

    public interface DownloadCallback {
        void onProgress(long downloadedBytes, long totalBytes, int percent);
        void onVerifying();
        void onReadyToInstall(File apkFile, UpdateManifest manifest);
        void onCancelled();
        void onError(Exception error);
    }

    public interface InstallListener {
        void onInstallSucceeded();
        void onInstallCancelled();
        void onInstallFailed(String message);
    }

    @SuppressLint("StaticFieldLeak")
    private static UpdateManager sInstance;

    private final Context mContext;
    private final UpdateDownloader mDownloader;
    private final ExecutorService mExecutor;
    private final Handler mMainHandler;
    private volatile InstallListener mInstallListener;
    private volatile boolean mCheckInFlight;

    private UpdateManager(Context context) {
        mContext = context.getApplicationContext();
        mDownloader = new UpdateDownloader();
        mExecutor = Executors.newSingleThreadExecutor();
        mMainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized UpdateManager instance(Context context) {
        if (sInstance == null) {
            sInstance = new UpdateManager(context);
        }
        return sInstance;
    }

    public void setInstallListener(InstallListener listener) {
        mInstallListener = listener;
    }

    public boolean isConfigured() {
        return getManifestUrl() != null;
    }

    public String getManifestUrl() {
        String[] urls = mContext.getResources().getStringArray(R.array.update_urls);
        if (urls == null) {
            return null;
        }
        for (String url : urls) {
            if (url != null && !url.trim().isEmpty()) {
                return url.trim();
            }
        }
        return null;
    }

    public void cleanupStaleDownloadsOnStartup() {
        UpdatePrefs prefs = UpdatePrefs.instance(mContext);
        long pendingVersion = prefs.getPendingVersionCode();
        long installedVersion = UpdateVerifier.getInstalledVersionCode(mContext);
        if (pendingVersion > 0 && installedVersion >= pendingVersion) {
            prefs.clearPendingInstall();
            mExecutor.execute(() -> UpdateDownloader.cleanupUpdatesDir(mContext));
        }
    }

    public void checkForUpdates(boolean forceCheck, CheckCallback callback) {
        String manifestUrl = getManifestUrl();
        if (manifestUrl == null) {
            post(() -> callback.onUpToDate());
            return;
        }

        UpdatePrefs prefs = UpdatePrefs.instance(mContext);
        if (!forceCheck) {
            if (!prefs.isAutoCheckEnabled()) {
                return;
            }
            long elapsed = System.currentTimeMillis() - prefs.getLastCheckTimeMs();
            if (elapsed >= 0 && elapsed < AUTO_CHECK_COOLDOWN_MS) {
                return;
            }
        }

        if (mCheckInFlight) {
            return;
        }
        mCheckInFlight = true;

        mExecutor.execute(() -> {
            try {
                UpdateManifest manifest = mDownloader.fetchManifest(manifestUrl);
                prefs.setLastCheckTimeMs(System.currentTimeMillis());

                long installedVersion = UpdateVerifier.getInstalledVersionCode(mContext);
                if (installedVersion > 0 && manifest.getVersionCode() > installedVersion) {
                    boolean requiresUpgrade = manifest.getMinSupportedVersionCode() > 0
                            && installedVersion < manifest.getMinSupportedVersionCode();
                    post(() -> callback.onUpdateAvailable(manifest, requiresUpgrade));
                } else {
                    post(callback::onUpToDate);
                }
            } catch (Exception e) {
                Log.w(TAG, "Update check failed: %s", e.getMessage());
                post(() -> callback.onError(e));
            } finally {
                mCheckInFlight = false;
            }
        });
    }

    public UpdateDownloader.DownloadHandle downloadAndVerify(UpdateManifest manifest, DownloadCallback callback) {
        UpdateDownloader.DownloadHandle handle = new UpdateDownloader.DownloadHandle();

        mExecutor.execute(() -> {
            File downloadedFile = null;
            try {
                String safeName = "SmartTube-Pretty-v" + manifest.getVersionCode() + ".apk";
                downloadedFile = mDownloader.downloadApk(
                        mContext,
                        manifest.getApkUrl(),
                        manifest.getSha256(),
                        safeName,
                        handle,
                        (downloaded, total, percent) -> post(() -> callback.onProgress(downloaded, total, percent))
                );

                if (handle.isCancelled()) {
                    post(callback::onCancelled);
                    return;
                }

                post(callback::onVerifying);
                long installedVersion = UpdateVerifier.getInstalledVersionCode(mContext);
                UpdateVerifier.verifyDownloadedApk(mContext, downloadedFile, manifest.getSha256(), installedVersion);

                File verifiedFile = downloadedFile;
                post(() -> callback.onReadyToInstall(verifiedFile, manifest));
            } catch (UpdateVerifier.VerificationException e) {
                // Corrupt or untrusted APK must not be kept on disk or resumed.
                UpdateDownloader.deleteQuietly(downloadedFile);
                UpdatePrefs.instance(mContext).clearDownloadMetadata();
                post(() -> callback.onError(e));
            } catch (Exception e) {
                if (handle.isCancelled()) {
                    post(callback::onCancelled);
                } else {
                    post(() -> callback.onError(e));
                }
            }
        });

        return handle;
    }

    public interface SimpleFileCallback {
        void onSuccess(File file);
        void onError(Exception error);
    }

    public UpdateDownloader.DownloadHandle downloadBridgeApk(
            String url,
            File destinationFile,
            UpdateDownloader.ProgressListener progress,
            SimpleFileCallback callback) {
        UpdateDownloader.DownloadHandle handle = new UpdateDownloader.DownloadHandle();
        mExecutor.execute(() -> {
            try {
                File file = mDownloader.downloadSimpleFile(url, destinationFile, handle, progress);
                if (handle.isCancelled()) {
                    return;
                }
                post(() -> callback.onSuccess(file));
            } catch (Exception e) {
                post(() -> callback.onError(e));
            }
        });
        return handle;
    }

    void notifyInstallSucceeded() {
        InstallListener listener = mInstallListener;
        if (listener != null) {
            post(listener::onInstallSucceeded);
        }
    }

    void notifyInstallCancelled() {
        InstallListener listener = mInstallListener;
        if (listener != null) {
            post(listener::onInstallCancelled);
        }
    }

    void notifyInstallFailed(String message) {
        InstallListener listener = mInstallListener;
        if (listener != null) {
            post(() -> listener.onInstallFailed(message));
        }
    }

    private void post(Runnable runnable) {
        mMainHandler.post(runnable);
    }
}
