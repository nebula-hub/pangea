package hub.nebula.pangea.command.vanilla.dev

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor

class UnregisterAllCommandsCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.user.idLong != 236167700777271297L) {
            context.reply(true) {
                content = "Nope."
            }
            return
        }

        val allCommands = context.jda.retrieveCommands().await()

        allCommands.forEach {
            it.delete().await()
        }

        context.reply {
            content = "Unregistered all ${allCommands.size} commands."
        }
    }
}