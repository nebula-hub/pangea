package hub.nebula.pangea.utils

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageEditBuilder
import dev.schlaubi.lavakord.audio.Link
import hub.nebula.pangea.command.PangeaInteractionContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.utils.messages.MessageEditData

fun InlineMessage<*>.pretty(content: String, prefix: String = Emojis.STAR) {
    if (content.isNotBlank()) {
        this.content = "$prefix **•** $content"
    }
}

suspend fun Message.edit(block: InlineMessage<*>.() -> Unit): Message? {
    val msg = MessageEditBuilder {
        apply(block)
    }

    return this.editMessage(msg.build()).await()
}

fun prettyStr(content: String, prefix: String = Emojis.STAR): String {
    return if (content.isNotBlank()) {
        "$prefix **•** $content"
    } else {
        ""
    }
}

fun Guild.iconUrl(): String? {
    val iconId = this.iconId ?: return null
    val extension = if (this.iconId?.startsWith("a_") == true) "gif" else "png"

    return "https://cdn.discordapp.com/icons/${id}/$iconId.$extension?size=2048"
}

suspend fun Link.connect(context: PangeaInteractionContext, channelId: Long) {
    this.connect(channelId.toString())
}

suspend fun InteractionHook.edit(block: InlineMessage<*>.() -> Unit): Message? {
    val msg = MessageEditBuilder {
        apply(block)
    }

    return this.editOriginal(msg.build()).await()
}