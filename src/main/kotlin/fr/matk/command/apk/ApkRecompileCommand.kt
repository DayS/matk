package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.service.android.AndroidSdk
import fr.matk.service.android.Apktool
import fr.matk.utils.LoggerDelegate
import io.reactivex.Completable
import java.io.File

class ApkRecompileCommand : CliktCommand(name = "recompile") {
    private val projectFile by argument("project_path", "Path to project to recompile").file(
        mustBeReadable = true,
        canBeFile = false
    )
    private val outputApkFile by option("-o", "--output-apk-path", metavar = "output_apk_path", help = "Path to APK to generate")
        .file()
    private val keystoreFile by option("-k", "--keystore-path", metavar = "keystore_path", help = "Keystore to use for APK signature or use a generated one").file()
    private val keystoreAlias by option("-a", "--keystore-alias", metavar = "keystore_alias", help = "Keystore's alias").default("matk")
    private val keystorePassword by option("-p", "--keystore-password", metavar = "keystore_password", help = "Keystore's password").default("matkpass")
    private val debuggable by option("-d", "--debuggable", metavar = "local_file", help = "Make APK debuggable").flag()

    private val logger by LoggerDelegate()
    private val apktoolFactory = Apktool.Factory().build(Apktool.VERSION_LATEST)
    private val androidSdk = AndroidSdk.Factory().build()

    override fun run() {
        val outputApkFile = this.outputApkFile ?: File("${projectFile.absolutePath}.apk")
        val alignedApkFile = File(outputApkFile.parentFile, "${outputApkFile.nameWithoutExtension}-aligned.apk")

        apktoolFactory.flatMap { it.recompileApk(projectFile, outputApkFile, debuggable) }
            .doOnSuccess { logger.info("APK recompiled to {}. Signing it now", outputApkFile) }
            .flatMap { androidSdk.retrieveKeystore(keystoreFile, keystoreAlias, keystorePassword) }
            .flatMapCompletable { androidSdk.signApk(outputApkFile, it, keystoreAlias, keystorePassword) }
            .andThen(Completable.defer { androidSdk.alignApk(outputApkFile, alignedApkFile) })
            .subscribe(
                { logger.info("APK recompiled to {}", alignedApkFile) },
                { throwable -> logger.error("Unable to pull APK", throwable) })
    }

}
