package fr.matk.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.URL

internal class GithubTest {

    @Test
    fun getReleaseUrl() {
        assertEquals(
            URL("https://github.com/gitOwner/gitRepo/releases/download/releaseVersion/releaseVersion"),
            Github.getReleaseUrl("gitOwner", "gitRepo", "releaseVersion", "releaseVersion")
        )
    }

    @Test
    fun getLatestVersionUrl() {
        assertEquals(
            URL("https://api.github.com/repos/gitOwner/gitRepo/releases/latest"),
            Github.getLatestVersionUrl("gitOwner", "gitRepo")
        )
    }
    
}
