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

    private val LOGGER: Logger by LoggerDelegate()

    fun execute(
        vararg commandAndArgs: String,
        env: Map<String, String> = emptyMap(),
        directory: File? = null
    ): Observable<String> {
        return execute(commandAndArgs.asList(), env, directory)
    }

    fun execute(
        commandAndArgs: List<String>,
        env: Map<String, String> = emptyMap(),
        directory: File? = null
    ): Observable<String> {
        val resourceFactory = Callable<Process> {
            LOGGER.trace("Execute process {}", commandAndArgs)

            val b = ProcessBuilder(commandAndArgs)
            b.environment().putAll(env)
            if (directory != null) {
                b.directory(directory)
            }

            try {
                b.start()
            } catch (e: IOException) {
                throw StartProcessException("Unable to start process", e)
            }
        }

        val factory = Function<Process, Observable<String>> { process ->
            val output = Observable.create<String> { emitter ->
                process.inputStream.bufferedReader().use {
                    it.lineSequence().forEach { line -> emitter.onNext(line) }
                    emitter.onComplete()
                }
            }

            val error = Observable.create<String> { emitter ->
                process.errorStream.bufferedReader().use {
                    val error = it.lineSequence().joinToString("\n")
                    if (error.isNotBlank()) {
                        emitter.onError(ExecProcessException(-1, error))
                    } else {
                        emitter.onComplete()
                    }
                }
            }

            val completion = Observable.create<String> { emitter ->
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    emitter.onError(ExecProcessException(exitCode, null))
                } else {
                    emitter.onComplete()
                }
            }

            Observable.merge(output, error)
                .concatWith(completion)
        }

        val disposeAction = Consumer<Process> {
            it.destroy()
        }

        return Observable.using<String, Process>(resourceFactory, factory, disposeAction)
    }

}
