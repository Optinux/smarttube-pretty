package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.base.BasePresenter;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.updater.UpdateDialogHelper;
import com.liskovsoft.smartyoutubetv2.common.updater.UpdateDownloader;
import com.liskovsoft.smartyoutubetv2.common.updater.UpdateInstaller;
import com.liskovsoft.smartyoutubetv2.common.updater.UpdateManager;
import com.liskovsoft.smartyoutubetv2.common.updater.UpdateManifest;
import com.liskovsoft.smartyoutubetv2.common.utils.LoadingManager;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppUpdatePresenter extends BasePresenter<Void> implements MotherActivity.OnResult {
    private static final int REQUEST_CODE_INSTALL_PERMISSION = 4202;

    @SuppressLint("StaticFieldLeak")
    private static AppUpdatePresenter sInstance;

    private final UpdateManager mUpdateManager;
    private UpdateDownloader.DownloadHandle mActiveDownload;
    private File mPendingApkFile;
    private UpdateManifest mPendingManifest;
    private boolean mIsForceCheck;

    public AppUpdatePresenter(Context context) {
        super(context);
        mUpdateManager = UpdateManager.instance(context);
        mUpdateManager.setInstallListener(new UpdateManager.InstallListener() {
            @Override
            public void onInstallSucceeded() {
                mPendingApkFile = null;
                mPendingManifest = null;
                unhold();
            }

            @Override
            public void onInstallCancelled() {
                mPendingApkFile = null;
                mPendingManifest = null;
            }

            @Override
            public void onInstallFailed(String message) {
                Context ctx = getContext();
                if (ctx != null && message != null) {
                    MessageHelpers.showLongMessage(ctx, message);
                }
            }
        });
    }

    public static AppUpdatePresenter instance(Context context) {
        if (sInstance == null) {
            sInstance = new AppUpdatePresenter(context);
        }
        sInstance.setContext(context);
        return sInstance;
    }

    public static void unhold() {
        if (sInstance != null && sInstance.mActiveDownload != null) {
            sInstance.mActiveDownload.cancel();
            sInstance.mActiveDownload = null;
        }
        sInstance = null;
    }

    public void start(boolean forceCheck) {
        if (getContext() == null) {
            return;
        }

        if (!mUpdateManager.isConfigured()) {
            if (forceCheck) {
                MessageHelpers.showMessage(getContext(), R.string.update_not_found);
            }
            onFinish();
            return;
        }

        mIsForceCheck = forceCheck;

        if (forceCheck) {
            LoadingManager.showLoading(getContext(), true);
        }

        mUpdateManager.checkForUpdates(forceCheck, new UpdateManager.CheckCallback() {
            @Override
            public void onUpdateAvailable(UpdateManifest manifest, boolean requiresUpgrade) {
                if (getContext() == null) {
                    return;
                }
                if (mIsForceCheck) {
                    LoadingManager.showLoading(getContext(), false);
                    showUpdatePrompt(manifest, requiresUpgrade);
                } else if (requiresUpgrade || GeneralData.instance(getContext()).isOldUpdateNotificationsEnabled()) {
                    showUpdatePrompt(manifest, requiresUpgrade);
                } else {
                    pinUpdateSection(manifest);
                }
            }

            @Override
            public void onUpToDate() {
                if (mIsForceCheck && getContext() != null) {
                    LoadingManager.showLoading(getContext(), false);
                    MessageHelpers.showMessage(getContext(), R.string.update_not_found);
                }
                onFinish();
            }

            @Override
            public void onError(Exception error) {
                if (mIsForceCheck && getContext() != null) {
                    LoadingManager.showLoading(getContext(), false);
                    String detail = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                    MessageHelpers.showMessage(getContext(),
                            String.format("%s: %s", getContext().getString(R.string.update_error), detail));
                }
                onFinish();
            }
        });
    }

    private void showUpdatePrompt(UpdateManifest manifest, boolean requiresUpgrade) {
        if (getContext() == null) {
            return;
        }
        if (!mIsForceCheck && (getViewManager().isPlayerInForeground() || !Utils.isAppInForegroundFixed())) {
            return;
        }

        Activity activity = resolveActivity();
        if (activity != null) {
            UpdateDialogHelper.showUpdateAvailableDialog(
                    activity,
                    manifest,
                    requiresUpgrade,
                    () -> startDownloadAndInstall(manifest),
                    AppUpdatePresenter::unhold
            );
            return;
        }

        // Fallback to Leanback dialog when no foreground Activity reference is available.
        AppDialogPresenter dialogPresenter = AppDialogPresenter.instance(getContext());
        dialogPresenter.appendSingleButton(UiOptionItem.from(
                getContext().getString(R.string.install_update),
                optionItem -> {
                    dialogPresenter.closeDialog();
                    startDownloadAndInstall(manifest);
                },
                false
        ));
        if (!manifest.getChangelog().isEmpty()) {
            List<OptionItem> changelogOptions = new ArrayList<>();
            for (String line : manifest.getChangelog()) {
                changelogOptions.add(UiOptionItem.from(line));
            }
            dialogPresenter.appendStringsCategory(getContext().getString(R.string.update_changelog), changelogOptions);
        }
        dialogPresenter.showDialog(
                String.format("%s %s", getContext().getString(R.string.app_name), manifest.getVersionName()),
                AppUpdatePresenter::unhold
        );
    }

    private void pinUpdateSection(UpdateManifest manifest) {
        if (getContext() == null) {
            return;
        }

        BrowsePresenter.instance(getContext()).pinItem(
                getContext().getString(R.string.update_found),
                R.drawable.action_info,
                new ErrorFragmentData() {
                    @Override
                    public void onAction() {
                        startDownloadAndInstall(manifest);
                    }

                    @Override
                    public String getMessage() {
                        StringBuilder builder = new StringBuilder();
                        builder.append(String.format("%s %s", getContext().getString(R.string.app_name), manifest.getVersionName()));
                        if (!manifest.getChangelog().isEmpty()) {
                            builder.append(" ").append(getContext().getString(R.string.update_changelog)).append(":\n");
                            int limit = Math.min(manifest.getChangelog().size(), 30);
                            for (int i = 0; i < limit; i++) {
                                builder.append("- ").append(manifest.getChangelog().get(i)).append("\n");
                            }
                        }
                        return builder.toString();
                    }

                    @Override
                    public String getActionText() {
                        return getContext().getString(R.string.install_update);
                    }
                }
        );
    }

    private void startDownloadAndInstall(UpdateManifest manifest) {
        Activity activity = resolveActivity();
        final UpdateDialogHelper.ProgressDialogController progressDialog = activity != null
                ? UpdateDialogHelper.showDownloadProgressDialog(activity, manifest, () -> {
                    if (mActiveDownload != null) {
                        mActiveDownload.cancel();
                    }
                })
                : null;

        if (progressDialog == null && getContext() != null) {
            MessageHelpers.showMessage(getContext(), R.string.update_downloading);
        }

        mActiveDownload = mUpdateManager.downloadAndVerify(manifest, new UpdateManager.DownloadCallback() {
            @Override
            public void onProgress(long downloadedBytes, long totalBytes, int percent) {
                if (progressDialog != null) {
                    progressDialog.updateProgress(downloadedBytes, totalBytes, percent);
                }
            }

            @Override
            public void onVerifying() {
                if (progressDialog != null) {
                    progressDialog.showVerifying();
                }
            }

            @Override
            public void onReadyToInstall(File apkFile, UpdateManifest verifiedManifest) {
                mActiveDownload = null;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (getContext() != null && !verifiedManifest.getChangelog().isEmpty()) {
                    GeneralData.instance(getContext()).setChangelog(verifiedManifest.getChangelog());
                }
                proceedToInstall(apkFile, verifiedManifest);
            }

            @Override
            public void onCancelled() {
                mActiveDownload = null;
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                if (getContext() != null) {
                    MessageHelpers.showMessage(getContext(), R.string.update_cancelled);
                }
            }

            @Override
            public void onError(Exception error) {
                mActiveDownload = null;
                if (getContext() == null) {
                    return;
                }
                String msg = error.getMessage() != null ? error.getMessage() : getContext().getString(R.string.update_error);
                if (progressDialog != null) {
                    progressDialog.showError(msg, () -> startDownloadAndInstall(manifest), AppUpdatePresenter::unhold);
                } else {
                    MessageHelpers.showLongMessage(getContext(), msg);
                }
            }
        });
    }

    private void proceedToInstall(File apkFile, UpdateManifest manifest) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        if (!UpdateInstaller.canRequestPackageInstalls(context)) {
            mPendingApkFile = apkFile;
            mPendingManifest = manifest;

            Activity activity = resolveActivity();
            if (activity instanceof MotherActivity) {
                MotherActivity motherActivity = (MotherActivity) activity;
                UpdateDialogHelper.showInstallPermissionDialog(
                        motherActivity,
                        () -> {
                            motherActivity.addOnResult(this);
                            Intent settingsIntent = UpdateInstaller.createManageUnknownAppSourcesIntent(motherActivity);
                            motherActivity.startActivityForResult(settingsIntent, REQUEST_CODE_INSTALL_PERMISSION);
                        },
                        AppUpdatePresenter::unhold
                );
                return;
            }

            // Fallback if no MotherActivity is active.
            Intent settingsIntent = UpdateInstaller.createManageUnknownAppSourcesIntent(context);
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(settingsIntent);
            MessageHelpers.showLongMessage(context, R.string.update_install_permission_desc);
            return;
        }

        launchInstall(context, apkFile, manifest.getVersionCode());
    }

    private void launchInstall(Context context, File apkFile, long versionCode) {
        try {
            UpdateInstaller.install(context, apkFile, versionCode);
        } catch (IOException e) {
            MessageHelpers.showLongMessage(context,
                    String.format("%s: %s", context.getString(R.string.update_install_failed), e.getMessage()));
        }
    }

    @Override
    public void onResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_INSTALL_PERMISSION) {
            return;
        }
        Context context = getContext();
        if (context == null || mPendingApkFile == null || mPendingManifest == null) {
            return;
        }
        if (UpdateInstaller.canRequestPackageInstalls(context)) {
            File apkFile = mPendingApkFile;
            UpdateManifest manifest = mPendingManifest;
            mPendingApkFile = null;
            mPendingManifest = null;
            launchInstall(context, apkFile, manifest.getVersionCode());
        } else {
            MessageHelpers.showLongMessage(context, R.string.update_install_permission_denied);
        }
    }

    private Activity resolveActivity() {
        Context ctx = getContext();
        if (ctx instanceof Activity && Utils.checkActivity((Activity) ctx)) {
            return (Activity) ctx;
        }
        MotherActivity current = MotherActivity.getCurrentActivity();
        if (current != null && Utils.checkActivity(current)) {
            return current;
        }
        return null;
    }
}
