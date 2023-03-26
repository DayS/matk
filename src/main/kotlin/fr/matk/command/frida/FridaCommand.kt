package fr.matk.command.frida

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class FridaCommand : NoOpCliktCommand(name = "frida") {
    init {
        subcommands(
            FridaInstallServerCommand(),
            FridaStartServerCommand()
        )
    }
}
