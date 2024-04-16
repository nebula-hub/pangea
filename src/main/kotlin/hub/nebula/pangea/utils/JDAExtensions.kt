package hub.nebula.pangea.utils

import dev.minn.jda.ktx.messages.InlineMessage

fun InlineMessage<*>.pretty(content: String, prefix: String = Emojis.STAR) {
    if (content.isNotBlank()) {
        this.content = "$prefix **•** $content"
    }
}