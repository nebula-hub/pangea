package hub.nebula.pangea.command.component

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.InlineEmbed
import dev.minn.jda.ktx.messages.InlineMessage
import dev.minn.jda.ktx.messages.MessageCreateBuilder
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.localization.PangeaLocale
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

class PangeaButtonContext(
    private val event: ButtonInteractionEvent,
    val pangea: PangeaInstance
) {
    val jda = event.jda
    val guild = event.guild
    val channel = event.channel
    val member = event.member
    val user = event.user
    val locale = if (event.isFromGuild) {
        PangeaLocale(event.guildLocale.locale.lowercase())
    } else {
        PangeaLocale(event.userLocale.locale.lowercase())
    }

    suspend fun defer(ephemeral: Boolean = false) = event.deferReply(ephemeral).await()

    suspend fun deferEdit(ephemeral: Boolean = false) = event.deferEdit().await()

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

    fun terminate() {
        pangea.interactionManager.buttonCallbacks.remove(PangeaComponentId.invoke(event.componentId).uniqueId)
    }
}