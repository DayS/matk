package fr.matk.service.process

import java.io.IOException

abstract class ProcessException(message: String? = null, cause: Throwable? = null) : IOException(message, cause)

class StartProcessException(message: String? = null, cause: Throwable? = null) : ProcessException(message, cause)

class ExecProcessException(val exitCode: Int, val output: String?, cause: Throwable? = null) :
    ProcessException("Process execution failed with code $exitCode:\n$output", cause)

class InterruptedProcessException : InterruptedException()
