package com.liskovsoft.smartyoutubetv2.common.updater;

import android.content.Context;

import com.liskovsoft.sharedutils.okhttp.OkHttpManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * HTTPS-only manifest fetcher and resumable APK downloader with no cleartext fallback.
 */
public final class UpdateDownloader {
    private static final long MAX_MANIFEST_BYTES = 1024 * 1024; // 1 MiB safety cap
    private static final int STREAM_BUFFER_BYTES = 32 * 1024;
    private static final long PROGRESS_THROTTLE_MS = 120L;

    public interface ProgressListener {
        void onProgress(long downloadedBytes, long totalBytes, int percent);
    }

    public static final class DownloadHandle {
        private final AtomicBoolean mCancelled = new AtomicBoolean(false);
        private volatile Call mActiveCall;

        void attachCall(Call call) {
            mActiveCall = call;
            if (mCancelled.get() && call != null) {
                call.cancel();
            }
        }

        public void cancel() {
            mCancelled.set(true);
            Call call = mActiveCall;
            if (call != null) {
                call.cancel();
            }
        }

        public boolean isCancelled() {
            return mCancelled.get();
        }
    }

    private final OkHttpClient mClient;

    public UpdateDownloader() {
        mClient = OkHttpManager.instance().getClient().newBuilder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .addInterceptor(chain -> {
                    Request req = chain.request();
                    if (!req.url().isHttps()) {
                        throw new IOException("Refusing non-HTTPS update request");
                    }
                    return chain.proceed(req);
                })
                .addNetworkInterceptor(chain -> {
                    Request req = chain.request();
                    if (!req.url().isHttps()) {
                        throw new IOException("Refusing non-HTTPS redirect");
                    }
                    return chain.proceed(req);
                })
                .build();
    }

    public UpdateManifest fetchManifest(String manifestUrl) throws IOException {
        if (!UpdateManifest.isHttpsUrl(manifestUrl)) {
            throw new IOException("Update manifest URL must use HTTPS");
        }

        Request request = new Request.Builder()
                .url(manifestUrl)
                .header("Accept", "application/json")
                .header("Cache-Control", "no-cache")
                .get()
                .build();

        try (Response response = mClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code() + " fetching update manifest");
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body for update manifest");
            }
            long contentLength = body.contentLength();
            if (contentLength > MAX_MANIFEST_BYTES) {
                throw new IOException("Update manifest exceeds size limit");
            }
            String json = body.string();
            return UpdateManifest.parse(json);
        }
    }

    public File downloadApk(
            Context context,
            String url,
            String expectedSha256,
            String fileName,
            DownloadHandle handle,
            ProgressListener listener) throws IOException {
        if (!UpdateManifest.isHttpsUrl(url)) {
            throw new IOException("APK download URL must use HTTPS");
        }

        File updatesDir = getUpdatesDir(context);
        if (!updatesDir.exists() && !updatesDir.mkdirs()) {
            throw new IOException("Cannot create update cache directory");
        }

        File targetFile = new File(updatesDir, fileName);
        File partFile = new File(updatesDir, fileName + ".part");
        UpdatePrefs prefs = UpdatePrefs.instance(context);

        if (targetFile.isFile() && expectedSha256 != null && UpdateVerifier.verifySha256(targetFile, expectedSha256)) {
            long len = targetFile.length();
            if (listener != null) {
                listener.onProgress(len, len, 100);
            }
            return targetFile;
        }
        deleteQuietly(targetFile);

        boolean sameDownload = url.equals(prefs.getDownloadUrl())
                && (expectedSha256 == null || expectedSha256.equalsIgnoreCase(prefs.getDownloadSha256()));
        if (!sameDownload) {
            deleteQuietly(partFile);
        }
        prefs.setDownloadMetadata(url, expectedSha256);

        boolean allowResume = partFile.isFile() && partFile.length() > 0;
        downloadWithOptionalResume(url, partFile, allowResume, handle, listener);

        if (targetFile.exists() && !targetFile.delete()) {
            throw new IOException("Cannot replace previous APK file");
        }
        if (!partFile.renameTo(targetFile)) {
            throw new IOException("Failed to finalize downloaded APK");
        }
        prefs.clearDownloadMetadata();
        return targetFile;
    }

    public File downloadSimpleFile(
            String url,
            File destinationFile,
            DownloadHandle handle,
            ProgressListener listener) throws IOException {
        if (!UpdateManifest.isHttpsUrl(url)) {
            throw new IOException("Download URL must use HTTPS");
        }
        File parent = destinationFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create destination directory");
        }
        File partFile = new File(destinationFile.getParentFile(), destinationFile.getName() + ".part");
        deleteQuietly(partFile);
        deleteQuietly(destinationFile);
        downloadWithOptionalResume(url, partFile, false, handle, listener);
        if (!partFile.renameTo(destinationFile)) {
            throw new IOException("Failed to finalize downloaded file");
        }
        return destinationFile;
    }

    private void downloadWithOptionalResume(
            String url,
            File partFile,
            boolean resume,
            DownloadHandle handle,
            ProgressListener listener) throws IOException {
        long existingBytes = resume ? partFile.length() : 0L;
        Request.Builder reqBuilder = new Request.Builder().url(url).get();
        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=" + existingBytes + "-");
        }

        Call call = mClient.newCall(reqBuilder.build());
        if (handle != null) {
            handle.attachCall(call);
            if (handle.isCancelled()) {
                throw new IOException("Download cancelled");
            }
        }

        try (Response response = call.execute()) {
            int code = response.code();

            if (code == 416 && existingBytes > 0L) {
                // Range invalid (e.g. remote file changed): restart cleanly from byte 0.
                deleteQuietly(partFile);
                downloadWithOptionalResume(url, partFile, false, handle, listener);
                return;
            }

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + code + " downloading APK");
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty response body downloading APK");
            }

            boolean append = false;
            long baseBytes = 0L;
            long totalBytes = body.contentLength();

            if (code == 206 && existingBytes > 0L) {
                String contentRange = response.header("Content-Range");
                if (isValidContentRange(contentRange, existingBytes)) {
                    append = true;
                    baseBytes = existingBytes;
                    long rangeTotal = parseTotalFromContentRange(contentRange);
                    if (rangeTotal > 0L) {
                        totalBytes = rangeTotal;
                    } else if (totalBytes > 0L) {
                        totalBytes += existingBytes;
                    }
                } else {
                    deleteQuietly(partFile);
                }
            } else if (existingBytes > 0L) {
                // Server returned 200 OK instead of 206; overwrite from byte 0.
                deleteQuietly(partFile);
            }

            writeBodyToFile(body, partFile, append, baseBytes, totalBytes, handle, listener);
        }
    }

    private static void writeBodyToFile(
            ResponseBody body,
            File partFile,
            boolean append,
            long baseBytes,
            long totalBytes,
            DownloadHandle handle,
            ProgressListener listener) throws IOException {
        byte[] buffer = new byte[STREAM_BUFFER_BYTES];
        long downloaded = baseBytes;
        long lastReportMs = 0L;
        int lastPercent = -1;

        try (InputStream in = body.byteStream();
             FileOutputStream out = new FileOutputStream(partFile, append)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                if ((handle != null && handle.isCancelled()) || Thread.currentThread().isInterrupted()) {
                    throw new IOException("Download cancelled");
                }
                out.write(buffer, 0, read);
                downloaded += read;

                if (listener != null) {
                    long now = System.currentTimeMillis();
                    int percent = totalBytes > 0
                            ? (int) Math.min(100L, (downloaded * 100L) / totalBytes)
                            : -1;
                    if (now - lastReportMs >= PROGRESS_THROTTLE_MS || (percent >= 0 && percent != lastPercent)) {
                        lastReportMs = now;
                        lastPercent = percent;
                        listener.onProgress(downloaded, totalBytes, percent);
                    }
                }
            }
            out.flush();
        }

        if (listener != null) {
            int finalPercent = totalBytes > 0 ? (int) Math.min(100L, (downloaded * 100L) / totalBytes) : 100;
            listener.onProgress(downloaded, totalBytes > 0 ? totalBytes : downloaded, finalPercent);
        }
    }

    private static boolean isValidContentRange(String contentRange, long expectedStart) {
        if (contentRange == null) {
            return false;
        }
        String trimmed = contentRange.trim();
        return trimmed.startsWith("bytes " + expectedStart + "-");
    }

    private static long parseTotalFromContentRange(String contentRange) {
        if (contentRange == null) {
            return -1L;
        }
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0 || slash + 1 >= contentRange.length()) {
            return -1L;
        }
        String totalPart = contentRange.substring(slash + 1).trim();
        if ("*".equals(totalPart)) {
            return -1L;
        }
        try {
            return Long.parseLong(totalPart);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    public static File getUpdatesDir(Context context) {
        return new File(context.getApplicationContext().getCacheDir(), "updates");
    }

    public static void cleanupUpdatesDir(Context context) {
        File dir = getUpdatesDir(context);
        if (!dir.isDirectory()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteQuietly(file);
            }
        }
        UpdatePrefs.instance(context).clearDownloadMetadata();
    }

    public static void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }
}
