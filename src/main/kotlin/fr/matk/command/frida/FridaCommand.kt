package fr.matk.command.frida

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands

class FridaCommand : NoRunCliktCommand(name = "frida") {
    init {
        subcommands(
            FridaInstallServerCommand(),
            FridaStartServerCommand()
        )
    }
}
