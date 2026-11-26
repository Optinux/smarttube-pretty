package com.liskovsoft.smartyoutubetv2.common.updater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Verifies SHA-256 digests and APK package/signing certificate compatibility before installation.
 */
public final class UpdateVerifier {
    private static final int BUFFER_SIZE = 64 * 1024;

    private UpdateVerifier() {}

    public static final class VerificationException extends Exception {
        public VerificationException(String message) {
            super(message);
        }

        public VerificationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static String computeSha256(File file) throws IOException {
        MessageDigest digest = newSha256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream in = new FileInputStream(file)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return toHex(digest.digest());
    }

    public static boolean verifySha256(File file, String expectedHex) throws IOException {
        if (file == null || !file.isFile() || !UpdateManifest.isValidSha256(expectedHex)) {
            return false;
        }
        String actualHex = computeSha256(file);
        byte[] actualBytes = hexToBytes(actualHex);
        byte[] expectedBytes = hexToBytes(expectedHex.trim().toLowerCase(Locale.US));
        return MessageDigest.isEqual(actualBytes, expectedBytes);
    }

    public static void verifyDownloadedApk(
            Context context,
            File apkFile,
            String expectedSha256,
            long installedVersionCode) throws VerificationException {
        if (apkFile == null || !apkFile.isFile() || apkFile.length() <= 0) {
            throw new VerificationException("Downloaded APK file is missing or empty");
        }

        try {
            if (!verifySha256(apkFile, expectedSha256)) {
                throw new VerificationException("SHA-256 checksum mismatch");
            }
        } catch (IOException e) {
            throw new VerificationException("Failed to compute APK SHA-256", e);
        }

        verifyApkMetadataAndSignature(context, apkFile, installedVersionCode);
    }

    @SuppressLint("PackageManagerGetSignatures")
    @SuppressWarnings("deprecation")
    private static void verifyApkMetadataAndSignature(
            Context context,
            File apkFile,
            long installedVersionCode) throws VerificationException {
        PackageManager pm = context.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;

        PackageInfo archiveInfo = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), flags);
        if (archiveInfo == null) {
            throw new VerificationException("Downloaded file is not a valid Android APK");
        }

        String packageName = context.getPackageName();
        if (!packageName.equals(archiveInfo.packageName)) {
            throw new VerificationException(
                    "Package ID mismatch: expected " + packageName + ", got " + archiveInfo.packageName);
        }

        long archiveVersionCode = getVersionCode(archiveInfo);
        if (installedVersionCode > 0 && archiveVersionCode < installedVersionCode) {
            throw new VerificationException(
                    "APK versionCode (" + archiveVersionCode + ") is older than installed (" + installedVersionCode + ")");
        }

        Set<String> installedCerts = getSigningCertDigests(getInstalledPackageInfo(pm, packageName, flags));
        Set<String> archiveCerts = getSigningCertDigests(archiveInfo);

        if (!installedCerts.isEmpty() && !archiveCerts.isEmpty()) {
            boolean matches = false;
            for (String cert : archiveCerts) {
                if (installedCerts.contains(cert)) {
                    matches = true;
                    break;
                }
            }
            if (!matches) {
                throw new VerificationException(
                        "Signing certificate does not match the installed app. Uninstall the previous build once to switch signing keys.");
            }
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    @SuppressWarnings("deprecation")
    private static PackageInfo getInstalledPackageInfo(PackageManager pm, String packageName, int flags) {
        try {
            return pm.getPackageInfo(packageName, flags);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public static long getInstalledVersionCode(Context context) {
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return getVersionCode(info);
        } catch (PackageManager.NameNotFoundException e) {
            return -1L;
        }
    }

    @SuppressWarnings("deprecation")
    private static long getVersionCode(PackageInfo info) {
        if (info == null) {
            return -1L;
        }
        if (Build.VERSION.SDK_INT >= 28) {
            return info.getLongVersionCode();
        }
        return info.versionCode;
    }

    @SuppressWarnings("deprecation")
    private static Set<String> getSigningCertDigests(PackageInfo info) {
        Set<String> digests = new HashSet<>();
        if (info == null) {
            return digests;
        }

        Signature[] signatures = null;
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
            SigningInfo signingInfo = info.signingInfo;
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        }
        if (signatures == null || signatures.length == 0) {
            signatures = info.signatures;
        }
        if (signatures == null) {
            return digests;
        }

        for (Signature signature : signatures) {
            if (signature != null) {
                MessageDigest md = newSha256Digest();
                digests.add(toHex(md.digest(signature.toByteArray())));
            }
        }
        return digests;
    }

    private static MessageDigest newSha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return out;
    }
}
