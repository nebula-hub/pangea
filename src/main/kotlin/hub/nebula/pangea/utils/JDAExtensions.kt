package hub.nebula.pangea.utils

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.schlaubi.lavakord.audio.Link
import hub.nebula.pangea.command.PangeaCommandContext
import net.dv8tion.jda.api.entities.Guild

fun InlineMessage<*>.pretty(content: String, prefix: String = Emojis.STAR) {
    if (content.isNotBlank()) {
        this.content = "$prefix **•** $content"
    }
}

fun Guild.iconUrl(): String? {
    val iconId = this.iconId ?: return null
    val extension = if (this.iconId?.startsWith("a_") == true) "gif" else "png"

    return "https://cdn.discordapp.com/icons/${id}/$iconId.$extension?size=2048"
}

suspend fun Link.connect(context: PangeaCommandContext, channelId: Long) {
    this.connect(channelId.toString()).also {
        context.guild!!.audioManager.isSelfDeafened = true
    }
}