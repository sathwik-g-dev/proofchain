package com.proofchain.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ZipProjectExtractorTest {

    private ZipProjectExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ZipProjectExtractor();
    }

    @Test
    void testValidZipExtraction() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry1 = new ZipEntry("src/main/java/com/example/App.java");
            zos.putNextEntry(entry1);
            zos.write("public class App {}".getBytes());
            zos.closeEntry();

            ZipEntry entry2 = new ZipEntry("pom.xml");
            zos.putNextEntry(entry2);
            zos.write("<project></project>".getBytes());
            zos.closeEntry();
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Path extracted = extractor.extract(bais);

        assertNotNull(extracted);
        assertTrue(Files.exists(extracted));
        assertTrue(Files.exists(extracted.resolve("src/main/java/com/example/App.java")));
        assertTrue(Files.exists(extracted.resolve("pom.xml")));

        extractor.deleteDirectoryRecursively(extracted);
        assertFalse(Files.exists(extracted));
    }

    @Test
    void testZipSlipPathTraversalProtection() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Malicious entry attempting path traversal
            ZipEntry maliciousEntry = new ZipEntry("../evil.txt");
            zos.putNextEntry(maliciousEntry);
            zos.write("malicious content".getBytes());
            zos.closeEntry();
        } catch (IOException e) {
            fail("Failed to build zip: " + e.getMessage());
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(SecurityException.class, () -> extractor.extract(bais));
    }

    @Test
    void testEmptyZipHandling() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // No entries
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        Path extracted = extractor.extract(bais);

        assertNotNull(extracted);
        assertTrue(Files.exists(extracted));
        try (var files = Files.list(extracted)) {
            assertEquals(0, files.count());
        }

        extractor.deleteDirectoryRecursively(extracted);
    }
}
