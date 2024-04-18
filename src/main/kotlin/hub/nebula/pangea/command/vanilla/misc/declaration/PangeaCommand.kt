package hub.nebula.pangea.command.vanilla.misc.declaration

import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.misc.*

class PangeaCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        const val LOCALE_PREFIX = "commands.command.pangea"
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

        subCommand(
            "config",
            "pangea.config.description",
            this@command.name
        ) {
            executor = PangeaConfigCommandExecutor()
        }
    }
}