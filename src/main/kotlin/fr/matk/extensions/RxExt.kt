package fr.matk.extensions

import io.reactivex.Observable

fun Observable<String>.joinToString(separator: String = "\n") = this
    .toList()
    .map { it.joinToString(separator) }
