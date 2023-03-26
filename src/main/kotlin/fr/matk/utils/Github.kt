package fr.matk.utils

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http.promisesBody
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.IOException
import java.net.URL
import java.util.concurrent.Callable

object Github : KoinComponent {

    private val logger by LoggerDelegate()
    private val client by inject<OkHttpClient>()
    private val moshi by inject<Moshi>()
    private val gistJsonAdapter by lazy { moshi.adapter(GithubResponse::class.java) }

    fun getReleaseUrl(repoOwner: String, repoName: String, version: String, filename: String) =
        URL("https://github.com/$repoOwner/$repoName/releases/download/${version}/$filename")

    fun getLatestVersionUrl(repoOwner: String, repoName: String) =
        URL("https://api.github.com/repos/${repoOwner}/${repoName}/releases/latest")

    fun retrieveLatestReleaseVersion(repoOwner: String, repoName: String): Single<String> {
        val resourceFactory = Callable {
            logger.debug("Resolving latest release version for {}/{}", repoOwner, repoName)

            val request = Request.Builder()
                .get()
                .url(getLatestVersionUrl(repoOwner, repoName))
                .addHeader("User-Agent", "Matk")
                .addHeader("Accept", "application/json")
                .build()

            client.newCall(request)
        }

        val factory = Function<Call, Single<String>> { call ->
            Single.create { emitter ->
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        emitter.onError(IOException("Unexpected code $response"))
                    } else if (!response.promisesBody()) {
                        emitter.onError(IOException("No content could be retrieved"))
                    } else {
                        response.body!!.source().use { source ->
                            gistJsonAdapter.fromJson(source)?.let {
                                logger.trace("Found version {} for {}/{}", it.tagName, repoOwner, repoName)

                                emitter.onSuccess(it.tagName)
                            } ?: {
                                emitter.onError(IOException("Response shouldn't be null"))
                            }
                        }
                    }
                }
            }
        }

        val disposeAction = Consumer<Call> {
            it.cancel()
        }

        return Single.using(resourceFactory, factory, disposeAction)
    }
}

@JsonClass(generateAdapter = true)
data class GithubResponse(@Json(name = "tag_name") var tagName: String)
