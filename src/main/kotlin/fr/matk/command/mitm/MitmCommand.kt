package fr.matk.command.mitm

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands

class MitmCommand : NoRunCliktCommand(name = "mitm") {
    init {
        subcommands(
            MitmInstallSystemCertificateCommand()
        )
    }
}
