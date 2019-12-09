package fr.matk.utils

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

object Zip {

    fun extractFiles(zip: File, outputFolder: File) {
        ZipFile(zip).use { zipFile ->
            zipFile.stream().forEach { zipEntry ->
                extractFile(zipFile, zipEntry, File(outputFolder, zipEntry.name))
            }
        }
    }

    fun extractFile(zip: File, fileEntry: String, outputFile: File) {
        ZipFile(zip).use { zipFile -> extractFile(zipFile, zipFile.getEntry(fileEntry), outputFile) }
    }

    fun extractFile(zipFile: ZipFile, zipEntry: ZipEntry, outputFile: File) {
        outputFile.parentFile.mkdirs()

        if (!zipEntry.isDirectory) {
            FileOutputStream(outputFile).use { out ->
                zipFile.getInputStream(zipEntry).use {
                    it.copyTo(out)
                }
            }
        }
    }

}
