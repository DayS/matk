package fr.matk.service

import fr.matk.utils.LoggerDelegate
import io.reactivex.Completable
import io.reactivex.Single
import java.io.File
import java.io.FileOutputStream
import java.net.URL


class Cache(private val cacheFolder: File) {

    companion object {
        private val logger by LoggerDelegate()
    }

    private fun cachedFile(relativePath: String) = File(cacheFolder, relativePath)

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

    fun download(url: URL, destination: File) = Completable.create { emitter ->
        logger.debug("Downloading file {} into {}", url, destination)

        FileOutputStream(destination).use { fileOutput ->
            url.openStream().use {
                it.copyTo(fileOutput)
            }
        }

        emitter.onComplete()
    }.toSingle { destination }

}
