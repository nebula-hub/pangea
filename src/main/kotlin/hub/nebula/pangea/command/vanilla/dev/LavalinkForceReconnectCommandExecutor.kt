package hub.nebula.pangea.command.vanilla.dev

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.listener.MajorEventListener

class LavalinkForceReconnectCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.user.idLong != 236167700777271297L) {
            context.reply(true) {
                content = "Nope."
            }
            return
        }

        context.pangea.lavakord.nodes.forEach {
            context.pangea.lavakord.removeNode(it)
        }

        MajorEventListener.connectLavalinkNode(context.pangea)

        context.reply {
            content = "Reconnected."
        }
    }
}