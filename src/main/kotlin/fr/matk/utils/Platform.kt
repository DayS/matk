package fr.matk.utils

sealed class Platform {
    object Linux : Platform()
    object Windows : Platform()
    object MacOs : Platform()

    companion object {
        fun get(): Platform {
            val osName = System.getProperty("os.name").toLowerCase()
            return when {
                osName.contains("windows") -> Windows
                osName.contains("mac") -> MacOs
                else -> Linux
            }
        }
    }
}
