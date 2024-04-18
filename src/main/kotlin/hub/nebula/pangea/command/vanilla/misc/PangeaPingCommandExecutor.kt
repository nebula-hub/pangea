package hub.nebula.pangea.command.vanilla.misc

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.pretty

class PangeaPingCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
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