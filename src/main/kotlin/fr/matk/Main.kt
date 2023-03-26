package fr.matk

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.squareup.moshi.Moshi
import fr.matk.command.apk.ApkCommand
import fr.matk.command.device.DeviceCommand
import fr.matk.command.frida.FridaCommand
import fr.matk.command.mitm.MitmCommand
import fr.matk.service.Cache
import fr.matk.utils.Rest
import okhttp3.OkHttpClient
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

val matkModule = module {
    single { Cache(File(System.getProperty("user.home"), ".matk/cache")) }
    single { OkHttpClient() }
    single { Rest() }
    single { Moshi.Builder().build() }
}

class Matk : NoOpCliktCommand()

fun main(args: Array<String>) {
    startKoin {
        modules(matkModule)
    }

    Matk()
        .subcommands(ApkCommand())
        .subcommands(DeviceCommand())
        .subcommands(FridaCommand())
        .subcommands(MitmCommand())
        .main(args)
}
