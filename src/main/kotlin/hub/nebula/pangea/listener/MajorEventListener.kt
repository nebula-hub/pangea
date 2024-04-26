package hub.nebula.pangea.listener

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.edit
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.component.PangeaComponentId
import hub.nebula.pangea.database.dao.Guild
import hub.nebula.pangea.listener.data.VoiceState
import hub.nebula.pangea.utils.edit
import hub.nebula.pangea.utils.pretty
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

class MajorEventListener(val pangea: PangeaInstance) : ListenerAdapter() {
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val logger = KotlinLogging.logger(this::class.jvmName)

    override fun onGenericComponentInteractionCreate(event: GenericComponentInteractionCreateEvent) {
        coroutineScope.launch {
            if (event.isFromGuild) {
                newSuspendedTransaction {
                    Guild.getOrInsert(event.guild!!.idLong)
                }
            }
        }
    }

    override fun onGenericInteractionCreate(event: GenericInteractionCreateEvent) {
        coroutineScope.launch {
            if (event.isFromGuild) {
                newSuspendedTransaction {
                    Guild.getOrInsert(event.guild!!.idLong)
                }
            }

            when (event) {
                is SlashCommandInteractionEvent -> {
                    val commandName = event.fullCommandName.split(" ").first()

                    val command = pangea.commandManager[commandName]?.create()

                    if (command != null) {
                        val context = PangeaInteractionContext(event, pangea)

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
                is ModalInteractionEvent -> {
                    logger.info { "Modal ${event.modalId} submitted by ${event.user.name} (${event.user.id})" }

                    val modalId = try {
                        PangeaComponentId(event.modalId)
                    } catch (e: IllegalArgumentException) {
                        logger.info { "I don't recongnize this ID, probably it's expired." }
                        return@launch
                    }

                    val callbackId = pangea.interactionManager.componentCallbacks[modalId.uniqueId]

                    val context = PangeaInteractionContext(
                        event,
                        pangea
                    )

                    callbackId?.invoke(context)
                }
                is ButtonInteractionEvent -> {
                    logger.info { "Button ${event.componentId} pressed by ${event.user.name} (${event.user.id})" }

                    val componentId = try {
                        PangeaComponentId(event.componentId)
                    } catch (e: IllegalArgumentException) {
                        logger.info { "I don't recongnize this ID, probably it's expired." }
                        return@launch
                    }

                    val callbackId = pangea.interactionManager.componentCallbacks[componentId.uniqueId]
                    val context = PangeaInteractionContext(
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
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        coroutineScope.launch {
            val componentId = try {
                PangeaComponentId(event.componentId)
            } catch (e: IllegalArgumentException) {
                logger.info { "I don't recongnize this ID, probably it's expired." }
                return@launch
            }

            try {
                val callback = pangea.interactionManager.stringSelectMenuCallbacks[componentId.uniqueId]
                val context = PangeaInteractionContext(
                    event,
                    pangea
                )

                if (callback == null) {
                    event.deferEdit().await().edit {
                        actionRow(
                            event.component.asDisabled()
                        )
                    }

                    context.reply(true) {
                        pretty(
                            context.locale["commands.selectMenus.expired"]
                        )
                    }

                    return@launch
                }

                callback.invoke(context, event.values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        coroutineScope.launch {
            val guild = event.guild

            val registeredGuild = newSuspendedTransaction {
                Guild.getOrInsert(guild.idLong)
            }

            if (registeredGuild.autorole) {
                val roles = registeredGuild.autoroleRolesIds?.mapNotNull {
                    guild.getRoleById(it)
                }

                if (roles != null) {
                    for (role in roles) {
                        guild.addRoleToMember(event.member, role).await()
                    }
                }
            }
        }
    }

    override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
        coroutineScope.launch {
            // checar se a guild tá no banco de dados, se nao tiver, adicionar (so preciso lembrar como fas)
            newSuspendedTransaction {
                Guild.getOrInsert(event.guild.idLong)
            }// :thumbsup:

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

            logger.info { "Checking for unsaved guilds in the database..." }

            event.jda.guilds.forEach {
                newSuspendedTransaction {
                    Guild.getOrInsert(it.idLong)
                }
            }

            logger.info { "Logging in with ${event.jda.gatewayIntents.size} intents." }

            pangea.config.comet.nodes.forEach {
                pangea.lavakord.addNode(
                    "ws://${it.host}:${it.port}",
                    it.password
                )
            }

            logger.info { "Lavalink node connected." }

            val self = event.jda.selfUser
            logger.info { "${self.name} is ready!"}

            val commands = pangea.commandManager.handle()
            logger.info { "Registered ${commands?.size} commands!" }

            scheduledExecutorService.scheduleAtFixedRate({ changeStatus(event) }, 0, 10, TimeUnit.MINUTES)
            scheduledExecutorService.scheduleAtFixedRate({ updateLavalinkStatus(event) }, 0, 1, TimeUnit.MINUTES)
        }
    }

    private fun updateLavalinkStatus(event: ReadyEvent) {
        val lavakord = pangea.lavakord
        val channel = event.jda.getGuildById(pangea.config.mainLand.id)!!.getGuildChannelById(pangea.config.mainLand.lavalinkChannel) as TextChannel

        val text = StringBuilder().apply{
            appendLine("```")
            appendLine("· STATUS: ${if (lavakord.isActive) "Connected" else "Disconnected"}")
            appendLine("· PLUGINS:")
            lavakord.options.plugins.plugins.forEach {
                appendLine("· » ${it.name} (${it.version})")
            }
            appendLine("· NODES:")
            lavakord.nodes.forEachIndexed { index, it ->
                appendLine("· » NODE ${index +1}:")
                appendLine("· - » Status: ${if (it.available) "Connected" else "Disconnected"}")
                appendLine("· - » Name: ${it.name}")
                appendLine("· - » CPU: ${it.lastStatsEvent?.cpu?.systemLoad.toString().split(".")[0]}% (Cores: ${it.lastStatsEvent?.cpu?.cores})")
                appendLine("· - » Memory: ${it.lastStatsEvent?.memory?.used!! / 1024 / 1024}MB/${it.lastStatsEvent?.memory?.allocated!! / 1024 / 1024}MB")
                appendLine("· - » Players: ${it.lastStatsEvent?.players}")
                if (it.lastStatsEvent?.uptime == null) {
                    appendLine("· - » Uptime: N/A")
                } else {
                    val uptime = it.lastStatsEvent?.uptime!!
                    val days = TimeUnit.MILLISECONDS.toDays(uptime)
                    val hours = TimeUnit.MILLISECONDS.toHours(uptime) % 24
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(uptime) % 60
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(uptime) % 60
                    appendLine("· - » Uptime: ${days}d, ${hours}h, ${minutes}m, ${seconds}s")
                }
            }
            appendLine("```")
            appendLine("Last updated at <t:${System.currentTimeMillis() / 1000}:R>")
        }

        GlobalScope.launch {
            val lastMessage = channel.retrieveMessageById(channel.latestMessageId).await()

            if (lastMessage != null && lastMessage.author.idLong == event.jda.selfUser.idLong) {
                lastMessage.edit(text.toString()).await()
            } else {
                channel.sendMessage(text.toString()).queue()
            }

            logger.info { "Updating lavalink status..." }
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
