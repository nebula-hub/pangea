package hub.nebula.pangea.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PangeaCommandContext(
    private val event: SlashCommandInteractionEvent
) {
    val jda = event.jda
    val guild = event.guild
    val channel = event.channel
    val member = event.member
    val user = event.user
    val command = event.fullCommandName

    suspend fun defer(ephemeral: Boolean) = event.deferReply(ephemeral).await()

    suspend fun reply(ephemeral: Boolean, block: InlineMessage<*>.() -> Unit): ISnowflake? {
        val msg = MessageCreateBuilder {
            apply(block)
        }

        return if (event.isAcknowledged) {
            event.hook.setEphemeral(ephemeral).sendMessage(msg.build()).await()
        } else {
            event.reply(msg.build()).await()
        }
    }
}