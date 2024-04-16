package hub.nebula.pangea.listener

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.component.PangeaButtonContext
import hub.nebula.pangea.command.component.PangeaComponentId
import hub.nebula.pangea.listener.data.VoiceState
import hub.nebula.pangea.utils.pretty
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

class MajorEventListener(val pangea: PangeaInstance) : ListenerAdapter() {
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val logger = KotlinLogging.logger(this::class.jvmName)

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        coroutineScope.launch {
            logger.info { "Button ${event.componentId} pressed by ${event.user.name} (${event.user.id})" }

            val componentId = try {
                PangeaComponentId(event.componentId)
            } catch (e: IllegalArgumentException) {
                logger.info { "I don't recongnize this ID, probably it's expired." }
                return@launch
            }

            val callbackId = pangea.interactionManager.buttonCallbacks[componentId.uniqueId]
            val context = PangeaButtonContext(
                event,
                pangea
            )

            if (callbackId == null) {
                event.editButton(
                    event.button.asDisabled()
                ).await()

                context.reply(true) {
                    pretty(
                        context.locale["commands.buttons.expired"]
                    )
                }

                return@launch
            }

            callbackId.invoke(context)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        coroutineScope.launch {
            val commandName = event.fullCommandName.split(" ").first()

            val command = pangea.commandManager[commandName]?.create()

            if (command != null) {
                val context = PangeaCommandContext(event, pangea)

                val subCommandGroupName = event.subcommandGroup
                val subCommandName = event.subcommandName

                val subCommandGroup =
                    if (subCommandGroupName != null) command.getSubCommandGroup(subCommandGroupName) else null
                val subCommand = if (subCommandName != null) {
                    if (subCommandGroup != null) {
                        subCommandGroup.getSubCommand(subCommandName)
                    } else {
                        command.getSubCommand(subCommandName)
                    }
                } else null

                if (subCommand != null) {
                    subCommand.executor?.execute(context)
                } else if (subCommandGroupName == null && subCommandName == null) {
                    command.executor?.execute(context)
                }

                logger.info { "${context.user.name} (${context.user.id}) used /${event.fullCommandName} command in ${context.guild?.name} (${context.guild?.id})" }
            }
        }
    }

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        coroutineScope.launch {
            if (event is GuildVoiceUpdateEvent) {
                val member = event.member
                val newState = event.channelJoined
                val oldState = event.channelLeft

                if (newState != null) {
                    pangea.voiceStateManager[member.idLong] = VoiceState(
                        newState.idLong,
                        member.idLong,
                        newState.guild.idLong
                    )
                }

                if (oldState != null) {
                    pangea.voiceStateManager.remove(member.idLong)
                }
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        coroutineScope.launch {
            event.jda.presence.setPresence(
                OnlineStatus.DO_NOT_DISTURB,
                Activity.of(
                    Activity.ActivityType.PLAYING,
                    "Pangea is loading resources..."
                )
            )

            delay(2000)

            logger.info { "Logging in with ${event.jda.gatewayIntents.size} intents." }

            pangea.lavakord.addNode(
                "ws://${pangea.config.comet.host}:${pangea.config.comet.port}",
                pangea.config.comet.password
            )

            logger.info { "Lavalink node connected." }

            val self = event.jda.selfUser
            logger.info { "${self.name} is ready!"}

            val commands = pangea.commandManager.handle()
            logger.info { "Registered ${commands?.size} commands!" }

            scheduledExecutorService.scheduleAtFixedRate({ changeStatus(event) }, 0, 10, TimeUnit.MINUTES)
        }
    }

    private fun changeStatus(event: ReadyEvent) {
        logger.info { "Changing actual status..." }

        val activities = pangea.config.activities
        val activity = activities.random()

        event.jda.presence.setPresence(
            OnlineStatus.ONLINE,
            Activity.of(
                Activity.ActivityType.fromKey(activity.type),
                activity.name
            )
        )
    }
}