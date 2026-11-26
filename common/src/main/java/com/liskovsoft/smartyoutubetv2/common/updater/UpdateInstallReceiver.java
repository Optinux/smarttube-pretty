package com.liskovsoft.smartyoutubetv2.common.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.R;

/**
 * Receives PackageInstaller session status callbacks and cleans up downloaded APKs on completion.
 */
public class UpdateInstallReceiver extends BroadcastReceiver {
    public static final String ACTION_INSTALL_STATUS =
            "com.liskovsoft.smartyoutubetv2.common.updater.INSTALL_STATUS";
    public static final String EXTRA_APK_PATH = "extra_apk_path";
    private static final String TAG = UpdateInstallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || Build.VERSION.SDK_INT < 21) {
            return;
        }

        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        String message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);

        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirmIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirmIntent != null) {
                confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    context.startActivity(confirmIntent);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch installer confirmation: %s", e.getMessage());
                    UpdateManager.instance(context).notifyInstallFailed(e.getMessage());
                }
            }
            return;
        }

        if (status == PackageInstaller.STATUS_SUCCESS) {
            UpdatePrefs.instance(context).clearPendingInstall();
            UpdateDownloader.cleanupUpdatesDir(context);
            UpdateManager.instance(context).notifyInstallSucceeded();
            return;
        }

        Log.w(TAG, "Install failed with status=%d message=%s", status, message);

        if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
            String detail = message != null && !message.trim().isEmpty()
                    ? message
                    : context.getString(R.string.update_install_failed);
            MessageHelpers.showLongMessage(context, detail);
            UpdateManager.instance(context).notifyInstallFailed(detail);
        } else {
            UpdateManager.instance(context).notifyInstallCancelled();
        }
    }
}
