package hub.nebula.pangea.utils

fun pretty(content: String, prefix: String = Emojis.BLACK_HOLE): String {
    return if (content.isNotBlank()) {
        "$prefix **•** $content"
    } else {
        ""
    }
}