package com.proofchain.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubProjectFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitHubProjectFetcher.class);

    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "^(?:https?://)?(?:www\\.)?github\\.com/([A-Za-z0-9_.-]+)/([A-Za-z0-9_.-]+?)(?:\\.git)?(?:/.*)?$"
    );

    private final ZipProjectExtractor zipProjectExtractor;
    private final HttpClient httpClient;

    public GitHubProjectFetcher(ZipProjectExtractor zipProjectExtractor) {
        this.zipProjectExtractor = zipProjectExtractor;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    /**
     * Validates whether a given string is a valid public GitHub repository URL.
     */
    public boolean isValidGitHubUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return GITHUB_URL_PATTERN.matcher(url.trim()).matches();
    }

    /**
     * Extracts owner and repository name as a pair [owner, repo].
     */
    public String[] extractOwnerAndRepo(String url) {
        Matcher matcher = GITHUB_URL_PATTERN.matcher(url.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid GitHub repository URL: " + url);
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }
        return new String[]{owner, repo};
    }

    /**
     * Safely downloads a public GitHub repository zipball and extracts it using ZipProjectExtractor.
     *
     * @param gitHubUrl the public GitHub URL
     * @return Path to extracted repository source files
     * @throws IOException on download or extraction failure
     */
    public Path fetchAndExtract(String gitHubUrl) throws IOException {
        String[] parts = extractOwnerAndRepo(gitHubUrl);
        String owner = parts[0];
        String repo = parts[1];

        log.info("Fetching public GitHub repository archive for {}/{}", owner, repo);

        // Attempt 1: GitHub API zipball endpoint (handles default branch automatically)
        String apiUrl = String.format("https://api.github.com/repos/%s/%s/zipball", owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("User-Agent", "ProofChain-Source-Analyzer")
                .header("Accept", "application/vnd.github+json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                return zipProjectExtractor.extract(response.body());
            }

            log.warn("GitHub API zipball returned status {}. Attempting direct archive downloads for main/master branches...", response.statusCode());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading repository from GitHub", e);
        }

        // Attempt 2: Direct branch zip downloads (main, then master)
        String[] branches = {"main", "master"};
        for (String branch : branches) {
            String branchUrl = String.format("https://github.com/%s/%s/archive/refs/heads/%s.zip", owner, repo, branch);
            HttpRequest branchRequest = HttpRequest.newBuilder()
                    .uri(URI.create(branchUrl))
                    .header("User-Agent", "ProofChain-Source-Analyzer")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            try {
                HttpResponse<InputStream> response = httpClient.send(branchRequest, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() == 200) {
                    return zipProjectExtractor.extract(response.body());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while downloading repository from GitHub", e);
            }
        }

        throw new IOException(String.format("Could not access or download public GitHub repository %s/%s. Please ensure the repository is public and accessible.", owner, repo));
    }
}
