package fr.matk.command.apk

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class ApkCommand : NoOpCliktCommand(name = "apk", help = "Interaction with APK files : pull, decompile, recompile, ...") {
    init {
        subcommands(
            ApkPullCommand(),
            ApkDownloadCommand(),
            ApkDecompileCommand(),
            ApkRecompileCommand()
        )
    }
}
