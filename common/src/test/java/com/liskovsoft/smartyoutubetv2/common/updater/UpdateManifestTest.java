package com.liskovsoft.smartyoutubetv2.common.updater;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class UpdateManifestTest {
    private static final String VALID_SHA_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String VALID_SHA_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    public void parsesValidManifestAndSelectsPreferredAbi() {
        String json = "{"
                + "\"versionCode\": 24730005,"
                + "\"versionName\": \"32.83-ci-5\","
                + "\"apkUrl\": \"https://github.com/Optinux/smarttube-pretty/releases/download/ci-5/universal.apk\","
                + "\"sha256\": \"" + VALID_SHA_A + "\","
                + "\"size\": 30000000,"
                + "\"minSupportedVersionCode\": 24730000,"
                + "\"changelog\": [\"Fix updater\", \"Sign release builds\"],"
                + "\"apks\": {"
                + "  \"arm64-v8a\": {"
                + "    \"url\": \"https://github.com/Optinux/smarttube-pretty/releases/download/ci-5/arm64.apk\","
                + "    \"sha256\": \"" + VALID_SHA_B.toUpperCase() + "\","
                + "    \"size\": 18000000"
                + "  },"
                + "  \"universal\": {"
                + "    \"url\": \"https://github.com/Optinux/smarttube-pretty/releases/download/ci-5/universal.apk\","
                + "    \"sha256\": \"" + VALID_SHA_A + "\","
                + "    \"size\": 30000000"
                + "  }"
                + "}"
                + "}";

        UpdateManifest manifest = UpdateManifest.parse(json, new String[]{"arm64-v8a", "armeabi-v7a"});
        assertEquals(24730005L, manifest.getVersionCode());
        assertEquals("32.83-ci-5", manifest.getVersionName());
        assertEquals("https://github.com/Optinux/smarttube-pretty/releases/download/ci-5/arm64.apk", manifest.getApkUrl());
        assertEquals(VALID_SHA_B, manifest.getSha256());
        assertEquals(18000000L, manifest.getSizeBytes());
        assertEquals(24730000L, manifest.getMinSupportedVersionCode());
        assertEquals(Arrays.asList("Fix updater", "Sign release builds"), manifest.getChangelog());
    }

    @Test
    public void fallsBackToUniversalApkWhenAbiNotListed() {
        String json = "{"
                + "\"versionCode\": 24730006,"
                + "\"versionName\": \"32.84\","
                + "\"apkUrl\": \"https://github.com/Optinux/smarttube-pretty/releases/download/v32.84/universal.apk\","
                + "\"sha256\": \"" + VALID_SHA_A + "\","
                + "\"changelog\": \"Line one\\nLine two\""
                + "}";

        UpdateManifest manifest = UpdateManifest.parse(json, new String[]{"mips64"});
        assertEquals("https://github.com/Optinux/smarttube-pretty/releases/download/v32.84/universal.apk", manifest.getApkUrl());
        assertEquals(VALID_SHA_A, manifest.getSha256());
        assertEquals(0L, manifest.getMinSupportedVersionCode());
        assertEquals(Arrays.asList("Line one", "Line two"), manifest.getChangelog());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsCleartextHttpApkUrl() {
        String json = "{"
                + "\"versionCode\": 24730007,"
                + "\"versionName\": \"32.84\","
                + "\"apkUrl\": \"http://example.com/app.apk\","
                + "\"sha256\": \"" + VALID_SHA_A + "\""
                + "}";

        UpdateManifest.parse(json, new String[]{"arm64-v8a"});
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsInvalidSha256() {
        String json = "{"
                + "\"versionCode\": 24730007,"
                + "\"versionName\": \"32.84\","
                + "\"apkUrl\": \"https://example.com/app.apk\","
                + "\"sha256\": \"not-a-sha256\""
                + "}";

        UpdateManifest.parse(json, new String[]{"arm64-v8a"});
    }
}
