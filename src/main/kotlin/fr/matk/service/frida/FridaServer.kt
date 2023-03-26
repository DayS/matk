package fr.matk.service.frida

import fr.matk.service.Cache
import fr.matk.service.android.Adb
import fr.matk.service.process.Processes
import fr.matk.utils.Github
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

class FridaServer private constructor(private val binary: File) {
    companion object {
        const val DEFAULT_REMOTE_PATH = "/data/local/tmp/frida-server"

        const val VERSION_AUTO = "auto"
        const val VERSION_LATEST = "latest"

        private val logger by LoggerDelegate()
    }

    class Factory : KoinComponent {
        private val cache by inject<Cache>()
        private val fridaClient by lazy { FridaClient() }

        fun build(version: String, cpuAbi: String) = Single.defer<FridaServer> {
            logger.debug("Retrieving Frida with version {} and Cpu abi {}", version, cpuAbi)

            resolveVersion(version)
                .flatMap { resolvedVersion ->
                    val filterCpuAbi = filterCpuAbi(cpuAbi)
                    cache.getOrFetch("frida/${resolvedVersion}/${filterCpuAbi}/frida-server", Single.defer { downloadAndExtractRelease(resolvedVersion, filterCpuAbi) })
                }
                .map { FridaServer(it) }
        }

        private fun resolveVersion(version: String) = when (version) {
            VERSION_AUTO -> fridaClient.getLocalVersion()
            VERSION_LATEST -> Github.retrieveLatestReleaseVersion("frida", "frida")
            else -> Single.just(version)
        }

        private fun filterCpuAbi(cbuAbi: String) = when {
            cbuAbi.startsWith("arm64") -> "arm64"
            else -> cbuAbi
        }

        private fun downloadAndExtractRelease(version: String, cpuAbi: String): Single<File> {
            return cache.getOrDownload(
                "frida/${version}/${cpuAbi}/frida-server.xz",
                Github.getReleaseUrl("frida", "frida", version, "frida-server-${version}-android-${cpuAbi}.xz")
            ).flatMap { xzPath ->
                Processes.execute("xz", "-d", xzPath.absolutePath).ignoreElements()
                    .andThen(Single.just(File(xzPath.absolutePath.replace(Regex("\\.xz$", RegexOption.IGNORE_CASE), ""))))
            }
        }
    }

    fun install(adb: Adb, remotePath: File) = Single.defer<File> {
        logger.debug("Installing Frida server from $remotePath")

        adb.root()
            .flatMap { adb.pushFile(binary, remotePath, 755) }
    }

    fun retrievePid(adb: Adb): Single<Int> {
        return adb.shell("ps", "-A")
            .filter { it.contains("frida-server") }
            .firstOrError()
            .map { it.split(Regex("\\s+"))[1].toInt() }
    }

}
