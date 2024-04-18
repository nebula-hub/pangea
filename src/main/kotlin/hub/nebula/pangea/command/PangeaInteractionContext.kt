package hub.nebula.pangea.command

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.*
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.localization.PangeaLocale
import hub.nebula.pangea.database.dao.Guild
import hub.nebula.pangea.database.dao.User
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageEditData
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.SecureRandom

class PangeaInteractionContext(
    val event: GenericInteractionCreateEvent,
    val pangea: PangeaInstance
) {
    val jda = event.jda
    val guild = event.guild
    val channel = event.channel
    val random = SecureRandom()
    val member = event.member
    val pangeaUser = transaction {
        User.findOrCreate(event.user.idLong)
    }
    val pangeaGuild = transaction {
        if (event.isFromGuild) {
            Guild.getOrInsert(guild!!.idLong)
        } else null
    }
    val user = event.user
    val command = if (event is SlashCommandInteractionEvent) {
        event.fullCommandName
    } else null
    val locale = if (event.isFromGuild) {
        PangeaLocale(event.guildLocale.locale.lowercase())
    } else {
        PangeaLocale(event.userLocale.locale.lowercase())
    }

    fun getOption(name: String) = if (event is SlashCommandInteractionEvent) {
        event.getOption(name)
    } else null

    fun getValue(name: String) = when (event) {
        is ModalInteractionEvent -> event.getValue(name)
        else -> throw IllegalStateException("Cannot get value from this event type.")
    }

    suspend fun defer(ephemeral: Boolean = false) = when (event) {
        is SlashCommandInteractionEvent -> {
            if (event.isAcknowledged) {
                event.hook
            } else {
                event.deferReply().setEphemeral(ephemeral).await()
            }
        }
        is ButtonInteractionEvent -> {
            if (event.isAcknowledged) {
                event.hook
            } else {
                event.deferReply().setEphemeral(ephemeral).await()
            }
        }
        is ModalInteractionEvent -> {
            if (event.isAcknowledged) {
                event.hook
            } else {
                event.deferReply().setEphemeral(ephemeral).await()
            }
        }
        else -> throw IllegalStateException("Cannot defer this event type.")
    }

    suspend fun reply(ephemeral: Boolean = false, block: InlineMessage<*>.() -> Unit): ISnowflake? {
        val msg = MessageCreateBuilder {
            apply(block)
        }

        return when (event) {
            is SlashCommandInteractionEvent -> {
                if (event.isAcknowledged) {
                    event.hook.setEphemeral(ephemeral).sendMessage(msg.build()).await()
                } else {
                    val defer = defer(ephemeral)

                    defer?.sendMessage(msg.build())?.await()
                }
            }
            is ModalInteractionEvent -> {
                if (event.isAcknowledged) {
                    event.hook.setEphemeral(ephemeral).sendMessage(msg.build()).await()
                } else {
                    val defer = defer(ephemeral)

                    defer?.sendMessage(msg.build())?.await()
                }
            }
            is ButtonInteractionEvent -> {
                if (event.isAcknowledged) {
                    event.hook.setEphemeral(ephemeral).sendMessage(msg.build()).await()
                } else {
                    val defer = defer(ephemeral)

                    defer?.sendMessage(msg.build())?.await()
                }
            }

            else -> throw IllegalStateException("Cannot reply to this event type.")
        }
    }

    suspend fun edit(block: InlineMessage<*>.() -> Unit) = when (event) {
        is ButtonInteractionEvent -> {
            val msg = MessageEditBuilder {
                apply(block)
            }

            if (event.isAcknowledged) {
                event.hook.editOriginal(msg.build()).await()
            } else {
                event.deferEdit().await()?.editOriginal(msg.build())?.await()
            }
        }

        is ModalInteractionEvent -> {
            val msg = MessageEditBuilder {
                apply(block)
            }

            if (event.isAcknowledged) {
                event.hook.editOriginal(msg.build()).await()
            } else {
                event.deferEdit().await()?.editOriginal(msg.build())?.await()
            }
        }

        else -> throw IllegalStateException("Cannot edit this event type.")
    }

    suspend fun deferEdit(): InteractionHook? = when(event) {
        is ButtonInteractionEvent -> {
            if (event.isAcknowledged) {
                event.hook
            } else {
                event.deferEdit().await()
            }
        }
        is ModalInteractionEvent -> {
            if (event.isAcknowledged) {
                event.hook
            } else {
                event.deferEdit().await()
            }
        }
        else -> throw IllegalStateException("Cannot defer edit this event type.")
    }

    suspend fun sendModal(modal: Modal) = when (event) {
        is SlashCommandInteractionEvent -> event.replyModal(modal).await()
        is ButtonInteractionEvent -> event.replyModal(modal).await()
        else -> throw IllegalStateException("Cannot send modal to this event type.")
    }

    suspend fun retrieveUserById(userId: Long) = jda.retrieveUserById(userId).await()

}