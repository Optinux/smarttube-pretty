package com.liskovsoft.smartyoutubetv2.common.updater;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.content.FileProvider;

import com.liskovsoft.sharedutils.mylogger.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Installs verified APKs using PackageInstaller (API 21+) or FileProvider ACTION_VIEW (API < 21 / fallback),
 * and handles Android 8.0+ REQUEST_INSTALL_PACKAGES permission checks.
 */
public final class UpdateInstaller {
    private static final String TAG = UpdateInstaller.class.getSimpleName();
    private static final int STREAM_BUFFER_BYTES = 64 * 1024;
    private static final int INSTALL_REQUEST_CODE = 4201;

    private UpdateInstaller() {}

    public static boolean canRequestPackageInstalls(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return true;
        }
        return context.getPackageManager().canRequestPackageInstalls();
    }

    @TargetApi(26)
    public static Intent createManageUnknownAppSourcesIntent(Context context) {
        Uri pkgUri = Uri.parse("package:" + context.getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, pkgUri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            return intent;
        }
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkgUri);
    }

    public static void install(Context context, File apkFile, long targetVersionCode) throws IOException {
        if (apkFile == null || !apkFile.isFile()) {
            throw new IOException("APK file does not exist");
        }

        Context appContext = context.getApplicationContext();
        UpdatePrefs.instance(appContext).setPendingInstall(apkFile.getAbsolutePath(), targetVersionCode);

        if (Build.VERSION.SDK_INT >= 21) {
            try {
                installViaPackageInstaller(appContext, apkFile);
                return;
            } catch (Exception e) {
                Log.w(TAG, "PackageInstaller failed, falling back to ACTION_VIEW: %s", e.getMessage());
            }
        }

        installViaActionView(appContext, apkFile);
    }

    @TargetApi(21)
    private static void installViaPackageInstaller(Context appContext, File apkFile) throws IOException {
        PackageInstaller packageInstaller = appContext.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(appContext.getPackageName());
        params.setSize(apkFile.length());

        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);
            byte[] buffer = new byte[STREAM_BUFFER_BYTES];
            try (InputStream in = new FileInputStream(apkFile);
                 OutputStream out = session.openWrite("update.apk", 0, apkFile.length())) {
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                session.fsync(out);
            }

            Intent callbackIntent = new Intent(appContext, UpdateInstallReceiver.class);
            callbackIntent.setAction(UpdateInstallReceiver.ACTION_INSTALL_STATUS);
            callbackIntent.putExtra(UpdateInstallReceiver.EXTRA_APK_PATH, apkFile.getAbsolutePath());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }

            PendingIntent statusIntent = PendingIntent.getBroadcast(
                    appContext,
                    INSTALL_REQUEST_CODE,
                    callbackIntent,
                    flags
            );
            session.commit(statusIntent.getIntentSender());
        } catch (IOException | SecurityException | IllegalStateException e) {
            if (session != null) {
                session.abandon();
            }
            throw new IOException("PackageInstaller session failed", e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    @SuppressLint("SetWorldReadable")
    private static void installViaActionView(Context context, File apkFile) throws IOException {
        Uri apkUri;
        if (Build.VERSION.SDK_INT >= 17) {
            try {
                apkUri = FileProvider.getUriForFile(
                        context,
                        context.getPackageName() + ".update_provider",
                        apkFile
                );
            } catch (IllegalArgumentException e) {
                if (Build.VERSION.SDK_INT >= 24) {
                    throw new IOException("Unable to expose APK via FileProvider", e);
                }
                //noinspection ResultOfMethodCallIgnored
                apkFile.setReadable(true, false);
                apkUri = Uri.fromFile(apkFile);
            }
        } else {
            //noinspection ResultOfMethodCallIgnored
            apkFile.setReadable(true, false);
            apkUri = Uri.fromFile(apkFile);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }
}
