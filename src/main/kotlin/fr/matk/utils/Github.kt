package fr.matk.utils

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.IOException
import java.net.URL

object Github : KoinComponent {

    private val logger by LoggerDelegate()
    private val rest by inject<Rest>()
    private val moshi by inject<Moshi>()
    private val gistJsonAdapter by lazy { moshi.adapter(GithubResponse::class.java) }

    fun getReleaseUrl(repoOwner: String, repoName: String, version: String, filename: String) =
        URL("https://github.com/$repoOwner/$repoName/releases/download/${version}/$filename")

    fun getLatestVersionUrl(repoOwner: String, repoName: String) =
        URL("https://api.github.com/repos/${repoOwner}/${repoName}/releases/latest")

    fun retrieveLatestReleaseVersion(repoOwner: String, repoName: String): Single<String> {
        return rest.restGet(getLatestVersionUrl(repoOwner, repoName))
            .doOnSubscribe { logger.debug("Resolving latest release version for {}/{}", repoOwner, repoName) }
            .flatMap {
                gistJsonAdapter.fromJson(it)?.let {
                    logger.trace("Found version {} for {}/{}", it.tagName, repoOwner, repoName)
                    Single.just(it.tagName)
                } ?: Single.error(IOException("Response shouldn't be null"))
            }
    }
}

@JsonClass(generateAdapter = true)
data class GithubResponse(@Json(name = "tag_name") var tagName: String)
