package com.liskovsoft.smartyoutubetv2.common.updater;

import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Immutable representation and strict parser for the remote update manifest JSON.
 */
public final class UpdateManifest {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    private final long mVersionCode;
    private final String mVersionName;
    private final String mApkUrl;
    private final String mSha256;
    private final long mSizeBytes;
    private final List<String> mChangelog;
    private final long mMinSupportedVersionCode;

    public UpdateManifest(
            long versionCode,
            String versionName,
            String apkUrl,
            String sha256,
            long sizeBytes,
            List<String> changelog,
            long minSupportedVersionCode) {
        mVersionCode = versionCode;
        mVersionName = versionName;
        mApkUrl = apkUrl;
        mSha256 = sha256;
        mSizeBytes = sizeBytes;
        mChangelog = changelog != null
                ? Collections.unmodifiableList(new ArrayList<>(changelog))
                : Collections.emptyList();
        mMinSupportedVersionCode = minSupportedVersionCode;
    }

    public long getVersionCode() {
        return mVersionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public String getApkUrl() {
        return mApkUrl;
    }

    public String getSha256() {
        return mSha256;
    }

    public long getSizeBytes() {
        return mSizeBytes;
    }

    public List<String> getChangelog() {
        return mChangelog;
    }

    public long getMinSupportedVersionCode() {
        return mMinSupportedVersionCode;
    }

    public static UpdateManifest parse(String jsonText) throws IllegalArgumentException {
        return parse(jsonText, getDeviceSupportedAbis());
    }

    public static UpdateManifest parse(String jsonText, String[] supportedAbis) throws IllegalArgumentException {
        if (jsonText == null || jsonText.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty update manifest");
        }

        final JSONObject root;
        try {
            root = new JSONObject(jsonText);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Malformed update manifest JSON", e);
        }

        long versionCode = root.optLong("versionCode", -1);
        if (versionCode <= 0) {
            throw new IllegalArgumentException("Manifest missing valid versionCode");
        }

        String versionName = root.optString("versionName", "").trim();
        if (versionName.isEmpty()) {
            throw new IllegalArgumentException("Manifest missing versionName");
        }

        long minSupportedVersionCode = Math.max(0L, root.optLong("minSupportedVersionCode", 0L));
        List<String> changelog = parseChangelog(root);

        String selectedUrl = null;
        String selectedSha256 = null;
        long selectedSize = 0L;

        JSONObject apksObj = root.optJSONObject("apks");
        if (apksObj != null) {
            List<String> candidates = new ArrayList<>();
            if (supportedAbis != null) {
                for (String abi : supportedAbis) {
                    if (abi != null && !abi.trim().isEmpty()) {
                        candidates.add(abi.trim());
                    }
                }
            }
            candidates.add("universal");

            for (String candidate : candidates) {
                JSONObject abiObj = apksObj.optJSONObject(candidate);
                if (abiObj != null) {
                    String candidateUrl = abiObj.optString("url", "").trim();
                    String candidateSha = abiObj.optString("sha256", "").trim();
                    if (!candidateUrl.isEmpty() && !candidateSha.isEmpty()) {
                        selectedUrl = candidateUrl;
                        selectedSha256 = candidateSha;
                        selectedSize = Math.max(0L, abiObj.optLong("size", 0L));
                        break;
                    }
                }
            }
        }

        if (selectedUrl == null) {
            selectedUrl = root.optString("apkUrl", "").trim();
            selectedSha256 = root.optString("sha256", "").trim();
            selectedSize = Math.max(0L, root.optLong("size", 0L));
        }

        if (!isHttpsUrl(selectedUrl)) {
            throw new IllegalArgumentException("Manifest apkUrl must use HTTPS");
        }

        if (!isValidSha256(selectedSha256)) {
            throw new IllegalArgumentException("Manifest missing valid SHA-256 digest");
        }

        return new UpdateManifest(
                versionCode,
                versionName,
                selectedUrl,
                selectedSha256.toLowerCase(Locale.US),
                selectedSize,
                changelog,
                minSupportedVersionCode
        );
    }

    public static boolean isHttpsUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        try {
            Uri uri = Uri.parse(url.trim());
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && !uri.getHost().trim().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidSha256(String hex) {
        return hex != null && SHA256_PATTERN.matcher(hex.trim()).matches();
    }

    @SuppressWarnings("deprecation")
    public static String[] getDeviceSupportedAbis() {
        if (Build.VERSION.SDK_INT >= 21 && Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0) {
            return Build.SUPPORTED_ABIS;
        }
        return new String[]{Build.CPU_ABI, Build.CPU_ABI2};
    }

    private static List<String> parseChangelog(JSONObject root) {
        List<String> lines = new ArrayList<>();
        Object raw = root.opt("changelog");
        if (raw instanceof JSONArray) {
            JSONArray array = (JSONArray) raw;
            for (int i = 0; i < array.length(); i++) {
                String item = array.optString(i, "").trim();
                if (!item.isEmpty()) {
                    lines.add(item);
                }
            }
        } else if (raw instanceof String) {
            String[] split = ((String) raw).split("\\r?\\n");
            for (String line : split) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        }
        return lines;
    }
}
