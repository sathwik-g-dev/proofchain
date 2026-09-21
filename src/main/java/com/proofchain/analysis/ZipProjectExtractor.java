package com.proofchain.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ZipProjectExtractor {

    private static final Logger log = LoggerFactory.getLogger(ZipProjectExtractor.class);

    private static final int MAX_ENTRIES = 5000;
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 50 * 1024 * 1024; // 50MB

    private static final Set<String> IGNORED_DIRECTORY_PREFIXES = Set.of(
            ".git", "target", "build", "node_modules", ".idea", ".vscode", ".mvn", ".gradle", "bin", "out"
    );

    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            ".class", ".jar", ".war", ".ear", ".exe", ".dll", ".so", ".dylib", ".o", ".pyc"
    );

    /**
     * Extracts an input stream containing a ZIP archive into a secure temporary directory.
     * Enforces path traversal (Zip Slip) and Zip Bomb protections.
     *
     * @param zipInputStream the raw stream of the ZIP archive
     * @return the Path of the temporary directory containing extracted files
     * @throws IOException on I/O error
     * @throws SecurityException if a Zip Slip path traversal attempt is detected
     */
    public Path extract(InputStream zipInputStream) throws IOException {
        Path tempDir = Files.createTempDirectory("proofchain_extract_");
        Path canonicalTarget = tempDir.toRealPath();

        int entryCount = 0;
        long totalSize = 0;

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRIES) {
                    throw new SecurityException("ZIP archive contains too many files (max: " + MAX_ENTRIES + ")");
                }

                String rawName = entry.getName().replace('\\', '/');

                // Basic sanitation checks
                if (rawName.contains("../") || rawName.contains("/..") || rawName.equals("..")) {
                    throw new SecurityException("Malicious path detected in ZIP entry: " + entry.getName());
                }

                Path resolvedPath = canonicalTarget.resolve(rawName).normalize();

                // Zip Slip check
                if (!resolvedPath.startsWith(canonicalTarget)) {
                    throw new SecurityException("Zip Slip path traversal attempt: " + entry.getName());
                }

                // Check if entry is within an ignored directory
                if (isIgnoredPath(rawName)) {
                    zis.closeEntry();
                    continue;
                }

                boolean isDir = entry.isDirectory() || rawName.endsWith("/");
                if (isDir) {
                    Files.createDirectories(resolvedPath);
                } else {
                    // Check ignored extension
                    if (isIgnoredExtension(rawName)) {
                        zis.closeEntry();
                        continue;
                    }

                    if (Files.isDirectory(resolvedPath)) {
                        zis.closeEntry();
                        continue;
                    }

                    if (resolvedPath.getParent() != null) {
                        Files.createDirectories(resolvedPath.getParent());
                    }

                    // Extract file with size tracking
                    long entrySize = Files.copy(zis, resolvedPath, StandardCopyOption.REPLACE_EXISTING);
                    totalSize += entrySize;

                    if (totalSize > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                        throw new SecurityException("ZIP archive exceeds maximum allowable uncompressed size (50MB)");
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {
            // Clean up temporary directory on failure
            deleteDirectoryRecursively(tempDir);
            throw e;
        }

        return canonicalTarget;
    }

    private boolean isIgnoredPath(String path) {
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (IGNORED_DIRECTORY_PREFIXES.contains(segment.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isIgnoredExtension(String path) {
        String lower = path.toLowerCase();
        for (String ext : IGNORED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely deletes a directory and all of its contents.
     *
     * @param directory the directory to delete
     */
    public void deleteDirectoryRecursively(Path directory) {
        if (directory == null || !Files.exists(directory)) {
            return;
        }
        try {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.warn("Failed to clean up temporary directory {}: {}", directory, e.getMessage());
        }
    }
}
