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
                    process.inputStream.bufferedReader().use {
                        it.forEachLine { line ->
                            if (emitter.isDisposed) {
                                throw InterruptedProcessException()
                            }

                            logger.trace("▸ {}", line)
                            emitter.onNext(line)
                        }
                    }

                    val exitCode = process.waitFor()

                    if (validExitCodes.contains(exitCode)) {
                        emitter.onComplete()
                    } else {
                        val errorContent = process.errorStream.bufferedReader().use { it.readText() }
                        emitter.onError(ExecProcessException(exitCode, errorContent))

                        logger.debug("=> Exit with error code {}", exitCode)
                    }
                } catch (e: Exception) {
                    if (e !is InterruptedProcessException) {
                        emitter.onError(e)
                    }
                } finally {
                    process.destroy()
                    logger.debug("")
                }
            }
        }

        val disposeAction = Consumer<Process> {
            it.destroy()
        }

        return Observable.using(resourceFactory, factory, disposeAction)
    }

}
