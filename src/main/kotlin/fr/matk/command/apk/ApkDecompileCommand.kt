package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.service.android.Apktool
import fr.matk.service.android.Jadx
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.File

class ApkDecompileCommand : CliktCommand(name = "decompile") {
    private val apkFile by argument("apk_path", "Path to APK to decompile").file()

    private val logger by LoggerDelegate()

    private val apktoolFactory = Apktool.Factory().build(Apktool.VERSION_LATEST)
    private val jadxFactory = Jadx.Factory().build(Apktool.VERSION_LATEST)

    override fun run() {
        val outputFile = File("${apkFile.absolutePath}.out")

        Single.zip(apktoolFactory, jadxFactory, BiFunction { apktool: Apktool, jadx: Jadx -> Pair(apktool, jadx) })
            .flatMap { tools ->
                tools.first.decompileApk(apkFile, outputFile)
                    .flatMap { tools.second.findAndDecompileDexFiles(outputFile) }
            }
            .subscribe(
                { logger.info("APK decompiled to {}", outputFile) },
                { throwable -> logger.error("Unable to decompile APK {}", apkFile, throwable) })
    }

}
