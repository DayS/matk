package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import fr.matk.service.android.AndroidSdk
import fr.matk.service.android.Apktool
import fr.matk.utils.LoggerDelegate
import java.io.File

class ApkRecompileCommand : CliktCommand(name = "recompile") {
    private val projectPath by argument("project_path", "Path to project to recompile")
    private val outputApkPath by option("-o", "--output-apk-path", metavar = "output_apk_path", help = "Path to APK to generate")
    private val keystorePath by option("-k", "--keystore-path", metavar = "keystore_path", help = "Keystore to use for APK signature or use a generated one")
    private val keystoreAlias by option("-a", "--keystore-alias", metavar = "keystore_alias", help = "Keystore's alias").default("matk")
    private val keystorePassword by option("-p", "--keystore-password", metavar = "keystore_password", help = "Keystore's password").default("matkpass")
    private val debuggable by option("-d", "--debuggable", metavar = "local_file", help = "Make APK debuggable").flag()

    private val logger by LoggerDelegate()
    private val apktoolFactory = Apktool.Factory().build(Apktool.VERSION_LATEST)
    private val androidSdk = AndroidSdk.Factory().build()

    override fun run() {
        val projectFile = File(projectPath)
        val outputApkFile = File(outputApkPath ?: "$projectPath.apk")
        val alignedApkFile = File(outputApkFile.parentFile, "${outputApkFile.nameWithoutExtension}-aligned.apk")

        apktoolFactory.flatMap { it.recompileApk(projectFile, outputApkFile, debuggable) }
            .flatMap { androidSdk.retrieveKeystore(keystorePath?.let { File(it) }, keystoreAlias, keystorePassword) }
            .flatMapCompletable { androidSdk.signApk(outputApkFile, it, keystoreAlias, keystorePassword) }
            .andThen { androidSdk.alignApk(outputApkFile, alignedApkFile) }
            .subscribe(
                { logger.info("APK pulled") },
                { throwable -> logger.error("Unable to pull APK", throwable) })
    }

}
