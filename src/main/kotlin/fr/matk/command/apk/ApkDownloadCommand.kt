package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.command.CommandException
import fr.matk.service.store.Aptoide
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import java.io.File

class ApkDownloadCommand : CliktCommand(name = "download") {
    private val packageName by argument("package_name", "APK identifier as it may appears on the play store. It doesn't have to be an exact match")
    private val localFile by argument("local_file", "Path to a file on the host where the APK should be pulled").file().optional()
    private val exactMatch by option("-e", "--exact", help = "Indicate if the <package_name> should be an exact match").flag()
    private val countryCode by option("-c", "--country", help = "Indicate which country code to use for play store").default("en")

    private val logger by LoggerDelegate()
    private val aptoide = Aptoide()

    override fun run() {
        aptoide.searchPackages(packageName, exactMatch, countryCode)
            .toList()
            .flatMap { items ->
                if (items.size > 1) {
                    logger.error("Too many result (${items.size}): \n" + items.joinToString("\n") { "${it.name} (${it.packageId})" })
                    Single.error(CommandException("Too many result (${items.size})"))
                } else {
                    val item = items.first()
                    val apkPath = item.file.path
                    val localPath = localFile ?: File("${item.packageId}.apk")

                    logger.info("Found APK {}. Downloading to {}", apkPath, localPath)

                    aptoide.downloadPackage(apkPath, localPath)
                }
            }
            .subscribe(
                { logger.info("APK downloaded") },
                { throwable -> logger.error("Unable to download APK", throwable) })
    }

}
