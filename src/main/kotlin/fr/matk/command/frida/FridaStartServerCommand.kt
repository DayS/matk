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

class FridaStartServerCommand : CliktCommand(name = "start-server", help = "Start the instance of Frida server installed on the device") {
    private val device by deviceOption()
    private val fridaRemoteFile by option("-r", "--remote-path", help = "Path where Frida server will be installed")
        .file()
        .default(File(FridaServer.DEFAULT_REMOTE_PATH))

    private val logger by LoggerDelegate()
    private val adb = Adb(device)

    override fun run() {
        logger.debug("Starting Frida server from $fridaRemoteFile")

        adb.shell(fridaRemoteFile.absolutePath)
            .subscribe(
                { logger.info("Frida server started") },
                { throwable -> logger.error("Unable to start Frida server on device", throwable) })
    }

}
