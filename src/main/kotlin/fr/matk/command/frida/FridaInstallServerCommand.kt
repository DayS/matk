package fr.matk.command.frida

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.command.deviceOption
import fr.matk.service.android.Adb
import fr.matk.service.frida.FridaServer
import fr.matk.utils.LoggerDelegate
import java.io.File

class FridaInstallServerCommand : CliktCommand(name = "install-server", help = "Install an instance of Frida server on the device") {
    private val device by deviceOption()
    private val fridaRemoteFile by option("-r", "--remote-path", help = "Path where Frida server will be installed")
        .file()
        .default(File(FridaServer.DEFAULT_REMOTE_PATH))
    private val fridaVersion by option("-v", "--version", help = "Frida server's version. 'auto' for resolving with local frida instance, 'latest' or empty for downloading latest, or a specific version")
        .default(FridaServer.VERSION_AUTO)

    private val logger by LoggerDelegate()
    private val adb = Adb(device)

    override fun run() {
        logger.debug("Installing Frida server")

        adb.getCpuAbi()
            .flatMap { FridaServer.Factory().build(fridaVersion, it) }
            .flatMap { it.install(adb, fridaRemoteFile) }
            .subscribe(
                { logger.info("Frida server installed") },
                { throwable -> logger.error("Unable to install Frida server on device", throwable) })
    }

}
