package fr.matk.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun getLogger(forClass: Class<*>): Logger = LoggerFactory.getLogger(forClass)

fun <T : Any> getClassForLogging(javaClass: Class<T>): Class<*> =
    javaClass.enclosingClass?.takeIf { it.kotlin.isCompanion } ?: javaClass

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, Logger> {
    override fun getValue(thisRef: R, property: KProperty<*>) = getLogger(getClassForLogging(thisRef.javaClass))
}
