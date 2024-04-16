package hub.nebula.pangea.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.components.getOption
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.localization.PangeaLocale
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class PangeaCommandContext(
    val event: SlashCommandInteractionEvent,
    val pangea: PangeaInstance
) {
    val jda = event.jda
    val guild = event.guild
    val channel = event.channel
    val member = event.member
    val user = event.user
    val command = event.fullCommandName
    val locale = if (event.isFromGuild) {
        PangeaLocale(event.guildLocale.locale.lowercase())
    } else {
        PangeaLocale(event.userLocale.locale.lowercase())
    }

    fun getOption(name: String) = event.getOption(name)

    suspend fun commands() = jda.retrieveCommands().await()

    suspend fun defer(ephemeral: Boolean = false) = event.deferReply(ephemeral).await()

    suspend fun reply(ephemeral: Boolean = false, block: InlineMessage<*>.() -> Unit): ISnowflake? {
        val msg = MessageCreateBuilder {
            apply(block)
        }

        return if (event.isAcknowledged) {
            event.hook.setEphemeral(ephemeral).sendMessage(msg.build()).await()
        } else {
            val defer = defer(ephemeral)

            defer.sendMessage(msg.build()).await()
        }
    }

    suspend fun sendEmbed(ephemeral: Boolean = false, block: InlineEmbed.() -> Unit) {
        reply(ephemeral) {
            embed {
                apply(block)

                footer {
                    name = locale["commands.commandExecutedBy", user.name]
                    iconUrl = user.avatarUrl
                }
            }
        }
    }
}