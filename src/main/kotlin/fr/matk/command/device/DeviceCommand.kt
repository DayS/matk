package fr.matk.command.device

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class DeviceCommand : NoOpCliktCommand(name = "device") {
    init {
        subcommands(
            DeviceInstallPlayStoreCommand()
        )
    }
}
