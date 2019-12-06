package fr.matk.command

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.options.option

fun ParameterHolder.deviceOption(): RawOption = option("--device", metavar = "<device_id>", help = "Device's serial id")
