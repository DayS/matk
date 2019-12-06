package fr.matk

import com.github.ajalt.clikt.core.NoRunCliktCommand
import com.github.ajalt.clikt.core.subcommands
import fr.matk.command.apk.ApkCommand

class Matk : NoRunCliktCommand()

fun main(args: Array<String>) = Matk()
    .subcommands(ApkCommand())
    .main(args)
