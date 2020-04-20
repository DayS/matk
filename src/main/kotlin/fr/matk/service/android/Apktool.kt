package fr.matk.service.android

import fr.matk.extensions.joinToString
import fr.matk.service.Cache
import fr.matk.service.process.Processes
import fr.matk.utils.Github
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

class Apktool private constructor(private val binary: File) {
    companion object {
        const val VERSION_LATEST = "latest"
        private val logger by LoggerDelegate()
    }

    class Factory : KoinComponent {
        private val cache by inject<Cache>()

        fun build(version: String) = Single.defer<Apktool> {
            logger.debug("Retrieving Apktool {}", version)

            resolveVersion(version)
                .flatMap { resolvedVersion ->
                    val cleanedVersion = resolvedVersion.replace("v", "");
                    val url = Github.getReleaseUrl("iBotPeaches", "Apktool", resolvedVersion, "apktool_${cleanedVersion}.jar")

                    cache.getOrDownload("apktool/apktool-${cleanedVersion}.jar", url)
                }
                .map { Apktool(it) }
        }

        private fun resolveVersion(version: String) = when (version) {
            VERSION_LATEST -> Github.retrieveLatestReleaseVersion("iBotPeaches", "Apktool")
            else -> Single.just(version)
        }

    }

    fun decompileApk(apkPath: File, outputPath: File) = Single.defer<File> {
        logger.info("Decompiling resources and code as Smali from {}", apkPath)

        exec("decode", "--force", "--output", outputPath.absolutePath, apkPath.absolutePath)
            .flatMapObservable { Processes.execute("unzip", apkPath.absolutePath, "classes*.dex", "-d", outputPath.absolutePath, validExitCodes = listOf(0, 11)) }
            .ignoreElements()
            .andThen(Single.just(outputPath))
    }

    fun recompileApk(projectPath: File, apkPath: File, debuggable: Boolean): Single<File> {
        val prefixCommands = if (debuggable) arrayOf("--debug") else emptyArray()

        return exec("build", *prefixCommands, "--no-crunch", "-o", apkPath.absolutePath, projectPath.absolutePath)
            .map { apkPath }
    }

    private fun exec(vararg commands: String): Single<String> =
        Processes.execute("java", "-jar", binary.absolutePath, *commands)
            .joinToString()

}
