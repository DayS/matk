package fr.matk.service.frida

import fr.matk.extensions.joinToString
import fr.matk.service.process.Processes
import fr.matk.utils.LoggerDelegate
import io.reactivex.Single

class FridaClient {
    companion object {
        private val logger by LoggerDelegate()
    }

    fun getLocalVersion() = Single.defer<String> {
        logger.debug("Resolving Frida version for installed client")

        Processes.execute("frida", "--version").joinToString()
    }
}
