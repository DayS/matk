package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.file
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import fr.matk.service.android.Apktool
import fr.matk.service.android.Jadx
import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Zip
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File

class ApkDecompileCommand : CliktCommand(name = "decompile") {
    private val apkFile by argument("apk_path", "Path to APK to decompile").file(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )
    private val outputDir by argument("output_dir", "Output folder for decompiled app").file(
        canBeFile = false
    ).optional()

    private val logger by LoggerDelegate()

    private val apktoolFactory = Apktool.Factory().build(Apktool.VERSION_LATEST)
    private val jadxFactory = Jadx.Factory().build(Apktool.VERSION_LATEST)

    override fun run() {
        val outputFile = outputDir ?: File("${apkFile.absolutePath}.out")

        Single.zip(apktoolFactory, jadxFactory) { apktool: Apktool, jadx: Jadx -> Pair(apktool, jadx) }
            .flatMapObservable { tools ->
                if (apkFile.extension == "xapk") {
                    logger.info("XAPK detected. Extracting all split files")
                    decompileXapk(tools, apkFile, outputFile)
                } else {
                    decompileApk(tools, apkFile, outputFile, true)
                        .toObservable()
                }
            }
            .subscribe(
                { logger.info("APK decompiled to {}", outputFile) },
                { throwable -> logger.error("Unable to decompile APK {}", apkFile, throwable) })
    }

    private fun decompileXapk(tools: Pair<Apktool, Jadx>, apkFile: File, outputFile: File): Observable<File> {
        val mainApkFile = resolveMainDexFile()

        return Zip.extractFiles(apkFile, outputFile)
            .filter { it.extension == "apk" }
            .flatMapSingle {
                logger.debug("Extracting file {}", it)
                decompileApk(tools, it, File("${it.absolutePath}.out"), it.name == mainApkFile)
            }
    }

    private fun resolveMainDexFile(): String? {
        Zip.readFile(apkFile, "manifest.json").bufferedReader().use {
            val manifest = Gson().fromJson(it, XapkManifest::class.java)
            return manifest.splitApks.firstOrNull { it.id == "base" }?.file
        }
    }

    private fun decompileApk(tools: Pair<Apktool, Jadx>, apkFile: File, outputFile: File, mainApkFile: Boolean): Single<File> {
        return tools.second.decompileApk(apkFile, outputFile, mainApkFile)
    }
}

data class XapkManifest(
    @SerializedName("split_apks")
    val splitApks: List<XapkManifestSplitApk>
)

data class XapkManifestSplitApk(
    val id: String,
    val file: String
)
