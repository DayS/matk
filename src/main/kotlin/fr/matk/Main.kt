package fr.matk

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import fr.matk.command.apk.ApkCommand
import fr.matk.service.Cache
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File

val matkModule = module {
    single { Cache(File(System.getProperty("user.home"), ".matk/cache")) }
}

class Matk : NoRunCliktCommand()

fun main(args: Array<String>) {
    startKoin {
        modules(matkModule)
    }

    Matk()
        .subcommands(ApkCommand())
        .main(args)
}
