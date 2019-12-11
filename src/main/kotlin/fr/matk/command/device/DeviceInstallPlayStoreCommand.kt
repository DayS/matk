package fr.matk.command.device

import com.github.ajalt.clikt.core.CliktCommand
import fr.matk.command.deviceOption
import fr.matk.service.android.Adb
import fr.matk.service.android.PlayStore
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import java.io.File

class DeviceInstallPlayStoreCommand : CliktCommand(name = "install-play-store", help = "Download and install play store binaries on a device (Useful on emulators)") {
    private val device by deviceOption()

    private val logger by LoggerDelegate()
    private val adb = Adb(device)
    private val playStore = PlayStore()

    override fun run() {
        val remoteSystemFile = File("/system")

        Single.zip(adb.getCpuAbi(), adb.getAndroidVersion(), BiFunction<String, String, Pair<String, String>> { cpuAbi, androidVersion -> Pair(cpuAbi, androidVersion) })
            .flatMap { (cpuAbi, version) -> playStore.downloadAndExtract(cpuAbi, version) }
            .flatMap { folder ->
                adb.root()
                    .flatMap { adb.remount() }
                    .flatMap { adb.pushFile(File(folder, "etc"), remoteSystemFile) }
                    .flatMap { adb.pushFile(File(folder, "framework"), remoteSystemFile) }
                    .flatMap { adb.pushFile(File(folder, "app"), remoteSystemFile) }
                    .flatMap { adb.pushFile(File(folder, "priv-app"), remoteSystemFile) }
            }
            .flatMap { adb.reboot() }
            .subscribe(
                { logger.info("Play store installed") },
                { throwable -> logger.error("Unable to install playstore on device", throwable) })
    }

}
