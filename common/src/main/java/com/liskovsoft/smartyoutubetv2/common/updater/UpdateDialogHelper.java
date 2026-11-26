package com.liskovsoft.smartyoutubetv2.common.updater;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.utils.ThemePaletteHelper;

import java.util.List;
import java.util.Locale;

/**
 * TV-friendly Material 3 styled dialogs for update prompt, download/verification progress, and install permission.
 */
public final class UpdateDialogHelper {

    public static final class ProgressDialogController {
        private final AlertDialog mDialog;
        private final TextView mStatusView;
        private final ProgressBar mProgressBar;
        private final TextView mDetailView;
        private final Context mContext;

        private ProgressDialogController(
                Context context,
                AlertDialog dialog,
                TextView statusView,
                ProgressBar progressBar,
                TextView detailView) {
            mContext = context;
            mDialog = dialog;
            mStatusView = statusView;
            mProgressBar = progressBar;
            mDetailView = detailView;
        }

        public void updateProgress(long downloadedBytes, long totalBytes, int percent) {
            if (!mDialog.isShowing()) {
                return;
            }
            mStatusView.setText(mContext.getString(R.string.update_downloading));
            if (totalBytes > 0 && percent >= 0) {
                mProgressBar.setIndeterminate(false);
                mProgressBar.setProgress(percent);
                mDetailView.setText(String.format(
                        Locale.US,
                        "%d%%  •  %s / %s",
                        percent,
                        formatBytes(downloadedBytes),
                        formatBytes(totalBytes)
                ));
            } else {
                mProgressBar.setIndeterminate(true);
                mDetailView.setText(formatBytes(downloadedBytes));
            }
        }

        public void showVerifying() {
            if (!mDialog.isShowing()) {
                return;
            }
            mDialog.setTitle(R.string.update_verifying);
            mStatusView.setText(R.string.update_verifying);
            mProgressBar.setIndeterminate(true);
            mDetailView.setText("");
        }

        public void showError(String errorMessage, Runnable onRetry, Runnable onCancel) {
            if (!mDialog.isShowing()) {
                return;
            }
            mDialog.setTitle(R.string.update_error);
            mStatusView.setText(errorMessage != null ? errorMessage : mContext.getString(R.string.update_error));
            mProgressBar.setIndeterminate(false);
            mDetailView.setText("");

            Button positiveBtn = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveBtn != null) {
                positiveBtn.setVisibility(View.VISIBLE);
                positiveBtn.setText(R.string.update_retry);
                positiveBtn.setOnClickListener(v -> {
                    dismiss();
                    if (onRetry != null) {
                        onRetry.run();
                    }
                });
                positiveBtn.requestFocus();
            }

            Button negativeBtn = mDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeBtn != null) {
                negativeBtn.setOnClickListener(v -> {
                    dismiss();
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });
            }
        }

        public void dismiss() {
            if (mDialog.isShowing()) {
                try {
                    mDialog.dismiss();
                } catch (IllegalArgumentException ignored) {
                    // Activity may already be torn down.
                }
            }
        }
    }

    private UpdateDialogHelper() {}

    public static void showUpdateAvailableDialog(
            Activity activity,
            UpdateManifest manifest,
            boolean requiresUpgrade,
            Runnable onInstall,
            Runnable onLater) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        float density = activity.getResources().getDisplayMetrics().density;
        int padH = (int) (24f * density);
        int padV = (int) (12f * density);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.setFocusable(true);

        TextView bodyView = new TextView(activity);
        bodyView.setPadding(padH, padV, padH, padV);
        bodyView.setTextSize(15f);
        bodyView.setLineSpacing(0f, 1.2f);
        bodyView.setTextColor(0xFFECEDF2);
        bodyView.setText(buildUpdateSummary(activity, manifest, requiresUpgrade));
        scrollView.addView(bodyView);

        String title = String.format("%s %s", activity.getString(R.string.app_name), manifest.getVersionName());

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AppDialog)
                .setTitle(title)
                .setView(scrollView)
                .setCancelable(!requiresUpgrade)
                .setPositiveButton(R.string.install_update, (d, which) -> {
                    if (onInstall != null) {
                        onInstall.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (onLater != null) {
                        onLater.run();
                    }
                })
                .create();

        dialog.setOnCancelListener(d -> {
            if (onLater != null) {
                onLater.run();
            }
        });

        dialog.show();
        ThemePaletteHelper.applyDialogWindowSurface(activity, dialog);
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.requestFocus();
        }
    }

    public static ProgressDialogController showDownloadProgressDialog(
            Activity activity,
            UpdateManifest manifest,
            Runnable onCancel) {
        View view = LayoutInflater.from(activity).inflate(R.layout.update_progress_dialog, null, false);
        TextView statusView = view.findViewById(R.id.update_progress_status);
        ProgressBar progressBar = view.findViewById(R.id.update_progress_bar);
        TextView detailView = view.findViewById(R.id.update_progress_detail);

        ThemePaletteHelper.M3Palette palette = ThemePaletteHelper.getActivePalette(activity);
        if (palette != null && Build.VERSION.SDK_INT >= 21) {
            ColorStateList tint = ColorStateList.valueOf(palette.progressPrimary);
            progressBar.setProgressTintList(tint);
            progressBar.setIndeterminateTintList(tint);
        }

        if (manifest.getSizeBytes() > 0) {
            detailView.setText(String.format(Locale.US, "0%%  •  0 MB / %s", formatBytes(manifest.getSizeBytes())));
        }

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AppDialog)
                .setTitle(String.format("%s %s", activity.getString(R.string.app_name), manifest.getVersionName()))
                .setView(view)
                .setCancelable(false)
                .setPositiveButton(R.string.update_retry, null)
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .create();

        dialog.show();
        ThemePaletteHelper.applyDialogWindowSurface(activity, dialog);

        Button positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveBtn != null) {
            positiveBtn.setVisibility(View.GONE);
        }
        Button negativeBtn = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeBtn != null) {
            negativeBtn.requestFocus();
        }

        return new ProgressDialogController(activity, dialog, statusView, progressBar, detailView);
    }

    public static void showInstallPermissionDialog(
            Activity activity,
            Runnable onOpenSettings,
            Runnable onCancel) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(activity, R.style.AppDialog)
                .setTitle(R.string.update_install_permission_title)
                .setMessage(R.string.update_install_permission_desc)
                .setPositiveButton(R.string.update_open_settings, (d, which) -> {
                    if (onOpenSettings != null) {
                        onOpenSettings.run();
                    }
                })
                .setNegativeButton(android.R.string.cancel, (d, which) -> {
                    if (onCancel != null) {
                        onCancel.run();
                    }
                })
                .create();

        dialog.show();
        ThemePaletteHelper.applyDialogWindowSurface(activity, dialog);
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.requestFocus();
        }
    }

    private static String buildUpdateSummary(Context context, UpdateManifest manifest, boolean requiresUpgrade) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.update_version_info, manifest.getVersionName(), manifest.getVersionCode()));
        if (manifest.getSizeBytes() > 0) {
            sb.append("  (").append(formatBytes(manifest.getSizeBytes())).append(")");
        }
        sb.append("\n\n");

        if (requiresUpgrade) {
            sb.append(context.getString(R.string.update_required_notice)).append("\n\n");
        }

        List<String> changelog = manifest.getChangelog();
        if (!changelog.isEmpty()) {
            sb.append(context.getString(R.string.update_changelog)).append(":\n");
            int limit = Math.min(changelog.size(), 30);
            for (int i = 0; i < limit; i++) {
                sb.append("• ").append(changelog.get(i)).append("\n");
            }
        }
        return sb.toString().trim();
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double mb = bytes / (1024.0 * 1024.0);
        if (mb >= 1.0) {
            return String.format(Locale.US, "%.1f MB", mb);
        }
        double kb = bytes / 1024.0;
        return String.format(Locale.US, "%.0f KB", kb);
    }
}
