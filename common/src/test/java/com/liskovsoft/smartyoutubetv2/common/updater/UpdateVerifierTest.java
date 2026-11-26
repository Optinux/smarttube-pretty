package com.liskovsoft.smartyoutubetv2.common.updater;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateVerifierTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void verifiesMatchingSha256AndRejectsMismatch() throws Exception {
        File file = tempFolder.newFile("sample.apk");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("smarttube-pretty-apk-payload".getBytes(StandardCharsets.UTF_8));
        }

        String digest = UpdateVerifier.computeSha256(file);
        assertEquals(64, digest.length());
        assertTrue(UpdateVerifier.verifySha256(file, digest));
        assertTrue(UpdateVerifier.verifySha256(file, digest.toUpperCase()));
        assertFalse(UpdateVerifier.verifySha256(
                file,
                "0000000000000000000000000000000000000000000000000000000000000000"
        ));
    }
}
