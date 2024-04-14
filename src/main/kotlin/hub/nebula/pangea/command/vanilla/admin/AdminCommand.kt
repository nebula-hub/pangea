package hub.nebula.pangea.command.vanilla.admin

import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import net.dv8tion.jda.api.Permission

class AdminCommand : PangeaSlashCommandDeclarationWrapper {
    override fun create() = command("admin", "admin commands") {
        addPermission(
            Permission.BAN_MEMBERS,
            Permission.KICK_MEMBERS,
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_SERVER
        )

        subCommand("ban", "ban a member") {
            executor = BanCommandExecutor()
        }
    }

    inner class BanCommandExecutor : PangeaSlashCommandExecutor {
        override suspend fun execute(context: PangeaCommandContext) {
            context.defer(false)

        }
    }
}