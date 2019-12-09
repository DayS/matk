package fr.matk.command.apk

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands

class ApkCommand : NoRunCliktCommand(name = "apk", help = "Interaction with APK files : pull, decompile, recompile, ...") {
    init {
        subcommands(
            ApkPullCommand(),
            ApkDecompileCommand()
        )
    }
}
