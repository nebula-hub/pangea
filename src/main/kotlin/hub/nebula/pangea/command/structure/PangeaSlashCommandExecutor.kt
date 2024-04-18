package hub.nebula.pangea.command.structure

import hub.nebula.pangea.command.PangeaInteractionContext

abstract class PangeaSlashCommandExecutor {
    abstract suspend fun execute(context: PangeaInteractionContext)
}