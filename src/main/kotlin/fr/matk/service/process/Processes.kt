package fr.matk.service.process

import fr.matk.utils.LoggerDelegate
import io.reactivex.Observable
import io.reactivex.functions.Consumer
import io.reactivex.functions.Function
import org.slf4j.Logger
import java.io.File
import java.io.IOException
import java.util.concurrent.Callable

object Processes {

    private val logger: Logger by LoggerDelegate()

    fun execute(
        vararg commandAndArgs: String,
        env: Map<String, String> = emptyMap(),
        directory: File? = null,
        verbose: Boolean = false,
        validExitCodes: List<Int> = listOf(0)
    ): Observable<String> {
        return execute(commandAndArgs.asList(), env, directory, verbose, validExitCodes)
    }

    fun execute(
        commandAndArgs: List<String>,
        env: Map<String, String> = emptyMap(),
        directory: File? = null,
        verbose: Boolean = false,
        validExitCodes: List<Int> = listOf(0)
    ): Observable<String> {
        val resourceFactory = Callable {
            val b = ProcessBuilder(commandAndArgs)
            b.environment().putAll(env)
            if (directory != null) {
                b.directory(directory)
            }

            try {
                b.start().apply {
                    logger.debug("\u001B[34;1m\$ {}\u001B[0m", commandAndArgs.joinToString(" "))
                }
            } catch (e: IOException) {
                throw StartProcessException("Unable to start process", e)
            }
        }

        val factory = Function<Process, Observable<String>> { process ->
            Observable.create { emitter ->
                try {
                    emitter.setCancellable { process.destroy() }

                    process.inputStream.bufferedReader().use { reader ->
                        while (true) {
                            reader.readLine()?.also {
                                logger.trace("▸ {}", it)
                                emitter.onNext(it)
                            } ?: break
                        }
                    }

                    val exitCode = process.waitFor()

                    if (validExitCodes.contains(exitCode)) {
                        logger.debug("")
                        emitter.onComplete()
                    } else {
                        val errorContent = process.errorStream.bufferedReader().use { it.readText() }

                        logger.debug("=> Exit with error code {}", exitCode)
                        logger.debug("")

                        emitter.onError(ExecProcessException(exitCode, errorContent))
                    }
                } catch (e: Exception) {
                    if (e !is InterruptedProcessException) {
                        emitter.onError(e)
                    }
                }
            }
        }

        val disposeAction = Consumer<Process> {
            it.destroy()
        }

        return Observable.using(resourceFactory, factory, disposeAction)
    }

}
