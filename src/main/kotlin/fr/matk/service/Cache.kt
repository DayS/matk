package fr.matk.service

import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.internal.http.promisesBody
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.Callable


class Cache(private val cacheFolder: File) : KoinComponent {
    private val client by inject<OkHttpClient>()

    companion object {
        private val logger by LoggerDelegate()
    }

    fun cachedFile(relativePath: String) = File(cacheFolder, relativePath)

    fun getOrDownload(relativePath: String, url: URL): Single<File> =
        getOrFetch(relativePath, Single.defer { download(url, cachedFile(relativePath)) })

    fun getOrFetch(relativePath: String, asyncFetch: Single<File>) = Single.defer<File> {
        val cachedFile = cachedFile(relativePath)

        logger.debug("Looking for local cached file {}", cachedFile)

        if (cachedFile.isFile && cachedFile.length() > 0) {
            Single.just(cachedFile)
        } else {
            cachedFile.parentFile.mkdirs()
            asyncFetch
        }
    }

    fun download(url: URL, destination: File): Single<File> {
        val resourceFactory = Callable {
            logger.debug("Downloading file {} into {}", url, destination)

            val request = Request.Builder()
                .get()
                .url(url)
                .addHeader("User-Agent", "Matk")
                .build()

            client.newCall(request)
        }

        val factory = Function<Call, Single<File>> { call ->
            Single.create { emitter ->
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        emitter.onError(IOException("Unexpected code $response"))
                    } else if (!response.promisesBody()) {
                        emitter.onError(IOException("No content could be retrieved"))
                    } else {
                        logger.trace("Downloading content ({} octets)", response.headers("Content-Length").firstOrNull())
                        FileOutputStream(destination).use { fileOutput ->
                            response.body?.byteStream()?.use {
                                it.copyTo(fileOutput)
                            }
                        }
                        emitter.onSuccess(destination)
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
