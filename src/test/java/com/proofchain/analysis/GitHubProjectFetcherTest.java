package com.proofchain.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubProjectFetcherTest {

    private GitHubProjectFetcher fetcher;

    @BeforeEach
    void setUp() {
        fetcher = new GitHubProjectFetcher(new ZipProjectExtractor());
    }

    @Test
    void testValidGitHubUrls() {
        assertTrue(fetcher.isValidGitHubUrl("https://github.com/username/project"));
        assertTrue(fetcher.isValidGitHubUrl("http://github.com/username/project"));
        assertTrue(fetcher.isValidGitHubUrl("https://github.com/username/project.git"));
        assertTrue(fetcher.isValidGitHubUrl("https://github.com/username/project/"));
        assertTrue(fetcher.isValidGitHubUrl("https://www.github.com/username/project"));
    }

    @Test
    void testInvalidGitHubUrls() {
        assertFalse(fetcher.isValidGitHubUrl(null));
        assertFalse(fetcher.isValidGitHubUrl(""));
        assertFalse(fetcher.isValidGitHubUrl("https://gitlab.com/username/project"));
        assertFalse(fetcher.isValidGitHubUrl("https://notgithub.com/foo/bar"));
        assertFalse(fetcher.isValidGitHubUrl("just-some-text"));
    }

    @Test
    void testExtractOwnerAndRepo() {
        String[] parts = fetcher.extractOwnerAndRepo("https://github.com/octocat/Hello-World");
        assertEquals("octocat", parts[0]);
        assertEquals("Hello-World", parts[1]);

        String[] partsWithGit = fetcher.extractOwnerAndRepo("https://github.com/octocat/Spoon-Knife.git");
        assertEquals("octocat", partsWithGit[0]);
        assertEquals("Spoon-Knife", partsWithGit[1]);
    }
}
