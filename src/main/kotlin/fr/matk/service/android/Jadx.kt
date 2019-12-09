package fr.matk.service.android

import fr.matk.extensions.joinToString
import fr.matk.service.Cache
import fr.matk.service.process.Processes
import fr.matk.utils.Github
import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Zip
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

class Jadx private constructor(private val binary: File) {
    companion object {
        const val VERSION_LATEST = "latest"
        private val logger by LoggerDelegate()
        private val patternDexFile by lazy { Regex(".+\\.dex$", RegexOption.IGNORE_CASE) }
    }

    class Factory : KoinComponent {
        private val cache by inject<Cache>()

        fun build(version: String) = Single.defer<Jadx> {
            logger.debug("Retrieving Jadx {}", version)

            resolveVersion(version)
                .flatMap { resolvedVersion -> cache.getOrFetch("jadx/jadx-${resolvedVersion}/bin/jadx", downloadAndExtract(resolvedVersion)) }
                .map { Jadx(it) }
        }

        private fun downloadAndExtract(version: String): Single<File> {
            val cleanedVersion = version.replace("v", "");
            val url = Github.getReleaseUrl("skylot", "jadx", version, "jadx-${cleanedVersion}.zip")

            return cache.getOrDownload("jadx/jadx-${version}.zip", url)
                .map {
                    val outputFolder = File(it.parentFile, it.nameWithoutExtension)
                    Zip.extractFiles(it, outputFolder)
                    File(outputFolder, "bin/jadx").apply {
                        setExecutable(true)
                    }
                }
        }

        private fun resolveVersion(version: String) = when (version) {
            VERSION_LATEST -> Github.retrieveLatestReleaseVersion("skylot", "jadx")
            else -> Single.just(version)
        }

    }

    fun findAndDecompileDexFiles(lookupFolder: File) = Single.defer<File> {
        logger.info("Looking for DEX to decompile as Java files from {}", lookupFolder)

        Single.just(lookupFolder)
            .flattenAsObservable { it.listFiles { _, name -> name.matches(patternDexFile) }!!.toList() }
            .doOnNext { logger.debug("Decompiling DEX {}", it) }
            .concatMapSingle { decompileDex(it, lookupFolder) }
            .ignoreElements()
            .toSingleDefault(lookupFolder)
    }

    fun decompileDex(dexPath: File, outputPath: File): Single<File> =
        exec("--output-dir", outputPath.absolutePath, "--no-res", "--show-bad-code", "--export-gradle", dexPath.absolutePath)
            .map { outputPath }

    private fun exec(vararg commands: String): Single<String> =
        Processes.execute(binary.absolutePath, *commands)
            .joinToString()

}
