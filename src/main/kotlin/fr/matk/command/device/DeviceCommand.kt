package fr.matk.command.device

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands

class DeviceCommand : NoRunCliktCommand(name = "device") {
    init {
        subcommands(
            DeviceInstallPlayStoreCommand(),
            DeviceMitmCommand()
        )
    }
}
