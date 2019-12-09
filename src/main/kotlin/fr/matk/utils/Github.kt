package fr.matk.utils

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL

object Github {

    private val logger by LoggerDelegate()
    private val client by lazy { OkHttpClient() }
    private val moshi by lazy { Moshi.Builder().build() }
    private val gistJsonAdapter by lazy { moshi.adapter(GithubResponse::class.java) }

    fun getReleaseUrl(repoOwner: String, repoName: String, version: String, filename: String) =
        URL("https://github.com/$repoOwner/$repoName/releases/download/${version}/$filename")

    fun getLatestVersionUrl(repoOwner: String, repoName: String) =
        URL("https://api.github.com/repos/${repoOwner}/${repoName}/releases/latest")

    fun retrieveLatestReleaseVersion(repoOwner: String, repoName: String) = Single.create<String> { emitter ->
        logger.debug("Resolving latest release version for {}/{}", repoOwner, repoName)

        val request = Request.Builder()
            .get()
            .url(getLatestVersionUrl(repoOwner, repoName))
            .addHeader("User-Agent", "Matk")
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emitter.onError(IOException("Unexpected code $response"))
            } else {
                gistJsonAdapter.fromJson(response.body!!.source())?.let {
                    logger.trace("Found version {} for {}/{}", it.tagName, repoOwner, repoName)

                    emitter.onSuccess(it.tagName)
                }
            }
        }
    }

}

@JsonClass(generateAdapter = true)
data class GithubResponse(@Json(name = "tag_name") var tagName: String)
