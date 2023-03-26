package fr.matk.command.mitm

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class MitmCommand : NoOpCliktCommand(name = "mitm") {
    init {
        subcommands(
            MitmInstallSystemCertificateCommand()
        )
    }
}
