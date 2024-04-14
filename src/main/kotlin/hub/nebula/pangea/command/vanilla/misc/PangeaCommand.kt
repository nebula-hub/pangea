package hub.nebula.pangea.command.vanilla.misc

import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.pretty

class PangeaCommand : PangeaSlashCommandDeclarationWrapper {
    override fun create() = command("pangea", "Pangea") {
        subCommand("ping", "Ping Command" ) {
            executor = PangeaPingCommandExecutor()
        }
    }

    inner class PangeaPingCommandExecutor : PangeaSlashCommandExecutor {
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
}