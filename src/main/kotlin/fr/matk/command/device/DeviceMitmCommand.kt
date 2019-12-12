package fr.matk.command.device

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupChoice
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.command.deviceOption
import fr.matk.extensions.joinToString
import fr.matk.service.android.Adb
import fr.matk.service.android.PlayStore
import fr.matk.service.certificate.Certificate
import fr.matk.service.certificate.Source
import fr.matk.utils.LoggerDelegate
import java.io.File

sealed class SourceOption(name: String) : OptionGroup(name) {
    abstract fun getSource(): Source

    class File : SourceOption("Use a direct path to certificate") {
        val path by option("--path").file().required()

        override fun getSource(): Source = Source.DirectFile(path)
    }

    class Charles : SourceOption("Use Charles Proxy to export his own certificate") {
        override fun getSource() = Source.Charles
    }
}

class DeviceMitmCommand : CliktCommand(name = "mitm", help = "Install a SSL certificate as system certificate to enable MITM attack") {
    val source by option("-s", "--source", help = "Must be one of : charles, file").groupChoice(
        "charles" to SourceOption.Charles(),
        "file" to SourceOption.File()
    ).required()

    private val device by deviceOption()

    private val logger by LoggerDelegate()
    private val adb = Adb(device)
    private val playStore = PlayStore()

    override fun run() {
        val certificate = Certificate.Factory().build(source.getSource())

        logger.debug("Installing certificate from {}", source.getSource())

        certificate.extractCertificate()
            .flatMap { file ->
                certificate.computeHash(file)
                    .flatMap { hash ->
                        logger.debug("Installing certificate {} with hash {}", file, hash)

                        adb.root()
                            .flatMap { adb.shell("mount", "-o", "rw,remount", "/system").joinToString() }
                            .flatMap { adb.pushFile(file, File("/system/etc/security/cacerts/$hash.0"), 644) }
                            .flatMap { adb.reboot() }
                    }
            }
            .subscribe(
                { logger.info("Certificate installed") },
                { throwable -> logger.error("Unable to install certificate on device", throwable) })
    }

}
