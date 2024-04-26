package hub.nebula.pangea.command.vanilla.dev.declaration

import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.dev.LavalinkForceReconnectCommandExecutor
import hub.nebula.pangea.command.vanilla.dev.UnregisterAllCommandsCommandExecutor

class ToolsCommand : PangeaSlashCommandDeclarationWrapper {
    override fun create() = command("tools", "bot dev tools", isPrivate = true) {
        subCommandGroup("lavalink", "lavalink related", this@command.name) {
            subCommand("force-reconnect", "reconnect lavalink's node", isPrivate = true) {
                executor = LavalinkForceReconnectCommandExecutor()
            }
        }

        subCommandGroup("commands", "commands related", this@command.name) {
            subCommand("unregister", "unregister all commands", isPrivate = true) {
                executor = UnregisterAllCommandsCommandExecutor()
            }
        }
    }
}