package hub.nebula.pangea.command

abstract class PangeaSlashCommandExecutor {
    abstract suspend fun execute(context: PangeaCommandContext)
}