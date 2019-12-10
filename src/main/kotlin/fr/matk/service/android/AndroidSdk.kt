package fr.matk.service.android

import fr.matk.service.Cache
import fr.matk.service.process.Processes
import fr.matk.utils.LoggerDelegate
import io.reactivex.Completable
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File
import java.io.FileNotFoundException

class AndroidSdk private constructor(private val androidHome: File) : KoinComponent {
    private val cache by inject<Cache>()

    companion object {
        private val logger by LoggerDelegate()
    }

    class Factory : KoinComponent {
        fun build(androidHome: File? = null): AndroidSdk {
            if (androidHome == null) {
                return AndroidSdk(File(System.getenv()["ANDROID_HOME"] ?: throw IllegalStateException("\$ANDROID_HOME env variable must be set")))
            }
            return AndroidSdk(androidHome)
        }
    }

    fun resolveLatestBuildTool(): File {
        logger.debug("Retrieving lsat build-tool from {}", androidHome)

        val files = File(androidHome, "build-tools").listFiles()
        return if (files.isNullOrEmpty()) {
            throw FileNotFoundException("No build-tools found in $androidHome")
        } else {
            files.last()
        }
    }

    fun generateKeystore(alias: String, password: String): Single<File> {
        logger.info("Retrieving keystore")

        val relativePath = "sdk/resign.keystore"
        val fullPath = cache.cachedFile(relativePath)

        return cache.getOrFetch(
            relativePath,
            Processes.execute(
                "keytool", "-genkey", "-v",
                "-keystore", fullPath.absolutePath, "-alias", alias,
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
                "-dname", "CN=Unknown, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown",
                "-storepass", password, "-keypass", password
            )
                .ignoreElements()
                .andThen(Single.just(fullPath))
        )
    }

    fun retrieveKeystore(path: File?, alias: String, password: String): Single<File> {
        if (path == null) {
            return generateKeystore(alias, password);
        }
        return Single.just(path)
    }

    fun signApk(apkPath: File, keystorePath: File, keystoreAlias: String, keystorePassword: String) = Completable.defer {
        logger.info("Signing APK {} with keystore {}", apkPath, keystorePassword)

        Processes.execute(
            "jarsigner", "-sigalg", "SHA1withRSA", "-digestalg", "SHA1",
            "-keystore", keystorePath.absolutePath, "-storepass", keystorePassword, "-keypass", keystorePassword, apkPath.absolutePath, keystoreAlias
        ).ignoreElements()
    }

    fun alignApk(apkPath: File, alignedZipPath: File): Completable = Completable.defer {
        logger.info("Aligning APK {} into {}", apkPath, alignedZipPath)

        val buildTool = resolveLatestBuildTool()
        val zipAlign = File(buildTool, "zipalign")
        Processes.execute(zipAlign.absolutePath, "-f", "-v", "4", apkPath.absolutePath, alignedZipPath.absolutePath)
            .ignoreElements()
    }
}
