package fr.matk.service.android

import fr.matk.extensions.joinToString
import fr.matk.service.process.Processes
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.util.regex.Pattern

class Adb(private val deviceId: String?, private val adbCommand: String = "adb") {

    companion object {
        private val patternPackage: Pattern by lazy { Pattern.compile("^package:(.+\\.apk)=(.+)$", Pattern.CASE_INSENSITIVE) }
    }

    /**
     * Execute an ADB command on the connected device
     */
    fun execOnDevice(vararg commands: String): Observable<String> {
        val prefixCommands = deviceId?.let { listOf(adbCommand, "-s", deviceId) } ?: listOf(adbCommand)

        return Processes.execute(prefixCommands + commands)
    }

    /**
     * Pull a file from the connected device
     */
    fun pullFile(remotePath: File, localPath: File): Single<File> = execOnDevice("pull", remotePath.absolutePath, localPath.absolutePath)
        .ignoreElements()
        .toSingle { localPath }

    /**
     * Push a file to the connected device
     */
    fun pushFile(localPath: File, remotePath: File, chmod: Int? = null): Single<File> =
        execOnDevice("push", localPath.absolutePath, remotePath.absolutePath).let {
            if (chmod != null) {
                it.flatMap { shell("chmod", chmod.toString(), remotePath.absolutePath) }
            } else {
                it
            }
        }
            .ignoreElements()
            .toSingle { remotePath }

    fun root(): Single<String> = execOnDevice("root").joinToString()

    fun remount(): Single<String> = execOnDevice("remount").joinToString()

    fun reboot(): Single<String> = execOnDevice("reboot").joinToString()

    /**
     * Execute a shell command on the connected device
     */
    fun shell(vararg commands: String): Observable<String> = execOnDevice("shell", *commands)

    fun getCpuAbi(): Single<String> = shell("getprop", "ro.product.cpu.abi").joinToString()

    fun getAndroidVersion(): Single<String> = shell("getprop", "ro.build.version.release")
        .joinToString()
        .map { version: String -> if (version.length < 2) "$version.0" else version }

    fun getPackages(): Observable<Package> = execOnDevice("shell", "pm", "list", "packages", "-f")
        .filter { it.isNotBlank() }
        .map { patternPackage.matcher(it) }
        .filter { it.matches() }
        .map { Package(it.group(2), File(it.group(1))) }

    fun searchPackages(pattern: String, exactMatch: Boolean = false): Observable<Package> = getPackages()
        .filter { if (exactMatch) it.packageId == pattern else it.packageId.contains(pattern, true) }

    data class Package(val packageId: String, val path: File)

}
