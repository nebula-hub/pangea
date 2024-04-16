package hub.nebula.pangea.command.vanilla.misc

import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.interactions.components.buttons.Button

class PangeaCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        val LOCALE_PREFIX = "commands.command.pangea"
    }

    override fun create() = command(
        "pangea",
        "pangea.description"
    ) {
        subCommand(
            "ping",
            "pangea.ping.description",
            this@command.name
        ) {
            executor = PangeaPingCommandExecutor()
        }

        subCommand(
            "info",
            "pangea.info.description",
            this@command.name
        ) {
            executor = PangeaInfoCommandExecutor()
        }
    }

    inner class PangeaPingCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            context.defer(false)

            val messageList = listOf(
                pretty(
                    "Pong! (${context.jda.shardInfo.shardId}/${context.jda.shardInfo.shardTotal} shards)"
                ),
                pretty(
                    "Gateway: `${context.jda.gatewayPing}ms`"
                ),
                pretty(
                    "REST: `${context.jda.restPing.complete()}ms`"
                )
            )

            context.reply(false) {
                content = messageList.joinToString("\n")
            }
        }
    }

    inner class PangeaInfoCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            context.defer(false)

            val runtime = Runtime.getRuntime()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory

            context.reply {
                embed {
                    title = context.locale["$LOCALE_PREFIX.info.embedTitle"]
                    description = context.locale["$LOCALE_PREFIX.info.embedDescription"]
                    color = Constants.DEFAULT_COLOR
                    thumbnail = context.jda.selfUser.effectiveAvatarUrl

                    field {
                        name = context.locale["$LOCALE_PREFIX.info.embedServers"]
                        value = "`${context.jda.guilds.size}`"
                        inline = true
                    }

                    field {
                        name = "Kotlin / JVM"
                        value = "`${KotlinVersion.CURRENT} / ${System.getProperty("java.version")}`"
                    }

                    field {
                        name = context.locale["$LOCALE_PREFIX.info.embedRAMUsage"]
                        value = "`${usedMemory / 1024 / 1024}MB`"
                    }
                }

                actionRow(
                    Button.link(
                        "https://github.com/nebula-hub/pangea",
                        context.locale["$LOCALE_PREFIX.info.sourceCode"]
                    ),
                    Button.link(
                        "https://discord.com/oauth2/authorize?client_id=${context.jda.selfUser.idLong}&permissions=8&scope=bot",
                        context.locale["$LOCALE_PREFIX.info.inviteMe"]
                    )
                )
            }
        }
    }
}