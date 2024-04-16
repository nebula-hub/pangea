package hub.nebula.pangea.utils

import java.net.URI

fun pretty(content: String, prefix: String = Emojis.STAR): String {
    return if (content.isNotBlank()) {
        "$prefix **•** $content"
    } else {
        ""
    }
}

fun String.isValidUrl(): Boolean {
    try {
        URI(this).toURL().openConnection()
        return true
    } catch (e: Exception) {
        return false
    }
}

fun Long.humanize(): String {
    // this gets the time in millseconds and translate it to "mm:ss"
    val minutes = this / 60000
    val seconds = (this % 60000) / 1000
    return "${minutes}m ${seconds}s"
}