package hub.nebula.pangea.command

interface PangeaSlashCommandExecutor {
    suspend fun execute(context: PangeaCommandContext)
}