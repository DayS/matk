package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import fr.matk.command.CommandException
import fr.matk.command.deviceOption
import fr.matk.service.android.Adb
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import java.io.File

class ApkPullCommand : CliktCommand(name = "pull") {
    private val packageName by argument("package_name", "APK identifier as it may appears on the play store. It doesn't have to be an exact match")
    private val localFile by argument("local_file", "Path to a file on the host where the APK should be pulled").file().optional()
    private val device by deviceOption()
    private val exactMatch by option("-e", "--exact", help = "Indicate if the <package_name> should be an exact match").flag()

    private val logger by LoggerDelegate()
    private val adb = Adb(device)

    override fun run() {
        adb.searchPackages(packageName, exactMatch)
            .toList()
            .flatMap { packages ->
                if (packages.size > 1) {
                    logger.error("Too many packages result (${packages.size}): \n" + packages.joinToString("\n") { it.packageId })
                    Single.error(CommandException("Too many packages result (${packages.size})"))
                } else {
                    val apkPackage = packages.first()
                    val apkPath = apkPackage.path
                    val localPath = localFile ?: File("${apkPackage.packageId}.apk")

                    logger.info("Found APK {}. Pulling to {}", apkPath, localPath)

                    adb.pullFile(apkPath, localPath)
                }
            }
            .subscribe(
                { logger.info("APK pulled") },
                { throwable -> logger.error("Unable to pull APK", throwable) })
    }

}
