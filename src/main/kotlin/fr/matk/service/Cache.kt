package fr.matk.service

import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Rest
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
    private val rest by inject<Rest>()

    companion object {
        private val logger by LoggerDelegate()
    }

    fun cachedFile(relativePath: String) = File(cacheFolder, relativePath)

    fun getOrDownload(relativePath: String, url: URL): Single<File> =
        getOrFetch(relativePath, Single.defer { rest.download(url, cachedFile(relativePath)) })

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

}
