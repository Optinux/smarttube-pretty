package com.liskovsoft.smartyoutubetv2.common.updater;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Cleans up cached update APKs after the app package has been replaced (e.g. via ACTION_VIEW fallback).
 */
public class PackageReplacedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            return;
        }
        UpdatePrefs.instance(context).clearPendingInstall();
        UpdateDownloader.cleanupUpdatesDir(context);
    }
}
