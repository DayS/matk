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
        val resourceFactory = Callable<Process> {
            logger.trace("Execute process {}", commandAndArgs)

            val b = ProcessBuilder(commandAndArgs)
            b.environment().putAll(env)
            if (directory != null) {
                b.directory(directory)
            }

            b.redirectError(b.redirectInput())

            try {
                b.start()
            } catch (e: IOException) {
                throw StartProcessException("Unable to start process", e)
            }
        }

        val factory = Function<Process, Observable<String>> { process ->
            val output = Observable.create<String> { emitter ->
                try {
                    process.inputStream.bufferedReader().use {
                        it.forEachLine { line ->
                            if (emitter.isDisposed) {
                                throw InterruptedProcessException()
                            }

                            if (verbose) {
                                logger.trace("[{}] {}", process.pid(), line)
                            }
                            emitter.onNext(line)
                        }
                    }
                    emitter.onComplete()
                } catch (e: Exception) {
                    if (e !is InterruptedProcessException) {
                        emitter.onError(e)
                    }
                }
            }

            val completion = Observable.create<String> { emitter ->
                val exitCode = process.waitFor()

                if (verbose) {
                    logger.trace("[{}] Exit {}", process.pid(), exitCode)
                }

                if (validExitCodes.contains(exitCode)) {
                    emitter.onComplete()
                } else {
                    emitter.onError(ExecProcessException(exitCode, null))
                }
            }

            output.concatWith(completion)
        }

        val disposeAction = Consumer<Process> {
            it.destroy()
        }

        return Observable.using<String, Process>(resourceFactory, factory, disposeAction)
    }

}
