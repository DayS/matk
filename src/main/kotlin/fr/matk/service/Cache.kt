package fr.matk.service

import fr.matk.utils.LoggerDelegate
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL


class Cache(private val cacheFolder: File) : KoinComponent {
    private val client by inject<OkHttpClient>()

    companion object {
        private val logger by LoggerDelegate()
    }

    fun cachedFile(relativePath: String) = File(cacheFolder, relativePath)

    fun getOrDownload(relativePath: String, url: URL): Single<File> =
        getOrFetch(relativePath, download(url, cachedFile(relativePath)))

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

    fun download(url: URL, destination: File) = Completable.fromAction {
        logger.debug("Downloading file {} into {}", url, destination)

        val request = Request.Builder()
            .get()
            .url(url)
            .addHeader("User-Agent", "Matk")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            } else {
                FileOutputStream(destination).use { fileOutput ->
                    response.body?.byteStream()?.use {
                        it.copyTo(fileOutput)
                    }
                }
            }
        }
    }.toSingle { destination }

}
