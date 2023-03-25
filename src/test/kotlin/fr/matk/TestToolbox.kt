package fr.matk

import java.io.File

fun getResourceAsFile(path: String): File? = object {}.javaClass.getResource(path)?.let { File(it.toURI()) }

fun getResourceAsText(path: String): String? = object {}.javaClass.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
