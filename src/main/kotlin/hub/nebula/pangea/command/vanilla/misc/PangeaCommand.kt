package hub.nebula.pangea.command.vanilla.misc

import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.pretty

class PangeaCommand : PangeaSlashCommandDeclarationWrapper {
    override fun create() = command(
        "pangea",
        "pangea.description"
    ) {
        subCommand(
            "ping",
            "pangea.ping.description"
        ) {
            executor = PangeaPingCommandExecutor()
        }

        subCommand(
            "info",
            "pangea.info.description"
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

            context.sendEmbed {
                title = context.locale["commands.command.pangea.info.embedTitle"]
                description = context.locale["commands.command.pangea.info.embedDescription"]
                color = Constants.DEFAULT_COLOR
                thumbnail = context.jda.selfUser.effectiveAvatarUrl

                field {
                    name = context.locale["commands.command.pangea.info.embedServers"]
                    value = "`${context.jda.guilds.size}`"
                    inline = true
                }

                field {
                    name = "Kotlin / JVM"
                    value = "`${KotlinVersion.CURRENT} / ${System.getProperty("java.version")}`"
                }

                field {
                    name = context.locale["commands.command.pangea.info.embedRAMUsage"]
                    value = "`${usedMemory / 1024 / 1024}MB`"
                }
            }
        }
    }
}