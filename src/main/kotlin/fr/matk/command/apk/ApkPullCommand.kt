package fr.matk.command.apk

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import fr.matk.command.CommandException
import fr.matk.command.deviceOption
import fr.matk.service.android.Adb
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single
import java.io.File

class ApkPullCommand : CliktCommand(name = "pull") {
    private val packageName by argument("package_name", "APK identifier as it may appears on the play store. It doesn't have to be an exact match")
    private val localFile by argument("local_file", "Path to a file on the host where the APK should be pulled").optional()
    private val device by deviceOption()
    private val exactMatch by option("-e", "--exact", help = "Indicate if the <package_name> should be an exact match").flag()

    private val logger by LoggerDelegate()
    private val adb = Adb(device)

    override fun run() {
        adb.searchPackages(packageName, exactMatch)
            .toList()
            .flatMap { packages ->
                if (packages.size > 1) {
                    Single.error(CommandException("Too many packages result (${packages.size})"))
                } else {
                    val apkPath = packages.first().path
                    val localPath = File(localFile ?: "${apkPath.absolutePath}.apk")

                    logger.info("Found APK %s. Pulling to %s", apkPath, localPath)

                    adb.pullFile(apkPath, localPath)
                }
            }
            .subscribe(
                { logger.info("APK pulled") },
                { throwable -> logger.error("Unable to pull APK", throwable) })
    }

}
