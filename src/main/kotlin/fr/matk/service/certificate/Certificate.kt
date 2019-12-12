package fr.matk.service.certificate

import fr.matk.service.Cache
import fr.matk.service.process.Processes
import fr.matk.utils.LoggerDelegate
import fr.matk.utils.Platform
import io.reactivex.Single
import org.koin.core.KoinComponent
import org.koin.core.inject
import java.io.File

sealed class Source {
    object Charles : Source()
    data class DirectFile(val file: File) : Source()
}

abstract class Certificate {
    class Factory : KoinComponent {
        fun build(source: Source) = when (source) {
            is Source.Charles -> CharlesCertificate()
            is Source.DirectFile -> FileCertificate(source.file)
        }
    }

    abstract fun extractCertificate(): Single<File>

    fun computeHash(path: File): Single<String> {
        return Processes.execute("openssl", "x509", "-inform", "PEM", "-subject_hash_old", "-in", path.absolutePath)
            .firstOrError()
    }
}

class FileCertificate(val file: File) : Certificate() {
    override fun extractCertificate() = Single.just<File>(file)
}

class CharlesCertificate : Certificate(), KoinComponent {
    private val cache by inject<Cache>()

    companion object {
        private val logger by LoggerDelegate()
    }

    override fun extractCertificate() = Single.defer<File> {
        logger.debug("Extracting Charles's SSL certificate")

        val binary = resolveCharlesBinary()

        val relativeCertificatePath = "certificates/charles.pem"
        val certificateFile = cache.cachedFile(relativeCertificatePath)

        cache.getOrFetch(
            relativeCertificatePath,
            Processes.execute(binary.absolutePath, "ssl", "export", certificateFile.absolutePath)
                .ignoreElements()
                .toSingleDefault(certificateFile)
        )
    }

    private fun resolveCharlesBinary(): File = when (val platform = Platform.get()) {
        Platform.MacOs -> File("/Applications/Charles.app/Contents/MacOS/Charles")
        Platform.Linux -> File("Charles")
        else -> throw Exception("$platform not supported for automatic certificate extraction from Charles")
    }
}

