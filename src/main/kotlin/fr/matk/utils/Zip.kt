package fr.matk.utils

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

object Zip {

    fun extractFiles(zip: File, outputFolder: File, entryMatcher: ((zipEntry: ZipEntry) -> Boolean)? = null) = Observable.create<File> { emitter ->
        ZipFile(zip).use { zipFile ->
            zipFile.stream().forEach { zipEntry ->
                if (entryMatcher?.invoke(zipEntry) != false) {
                    val outputFile = File(outputFolder, zipEntry.name)
                    extractFile(zipFile, zipEntry, outputFile)
                    emitter.onNext(outputFile)
                }
            }
        }
        emitter.onComplete()
    }.subscribeOn(Schedulers.io())

    fun extractFile(zip: File, fileEntry: String, outputFile: File) {
        ZipFile(zip).use {
            val zipEntry = it.getEntry(fileEntry) ?: throw ZipException("Zip entry not found in at $fileEntry")
            extractFile(it, zipEntry, outputFile)
        }
    }

    fun extractFile(zipFile: ZipFile, zipEntry: ZipEntry, outputFile: File) {
        outputFile.parentFile.mkdirs()

        FileOutputStream(outputFile).use { out ->
            readFile(zipFile, zipEntry).use {
                it.copyTo(out)
            }
        }
    }

    fun readFile(zip: File, fileEntry: String): InputStream {
        return ZipFile(zip).let {
            val zipEntry = it.getEntry(fileEntry) ?: throw ZipException("Zip entry not found in at $fileEntry")
            readFile(it, zipEntry)
        }
    }

    fun readFile(zipFile: ZipFile, zipEntry: ZipEntry): InputStream {
        if (!zipEntry.isDirectory) {
            return zipFile.getInputStream(zipEntry)
        }
        throw ZipException("Zip entry at ${zipEntry.name} is a directory")
    }

}
