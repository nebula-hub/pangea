package hub.nebula.pangea.command.vanilla.admin.declaration

import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.admin.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class AdminCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        const val LOCALE_PREFIX = "commands.command.admin"
    }

    override fun create() = command(
        "admin",
        "admin.description"
    ) {
        addPermission(
            Permission.KICK_MEMBERS,
            Permission.BAN_MEMBERS,
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_SERVER,
            Permission.MESSAGE_MANAGE
        )

        subCommand(
            "ban",
            "admin.ban.description",
            "admin"
        ) {
            addOption(
                OptionData(
                    OptionType.USER,
                    "member",
                    "admin.ban.member.description",
                    true
                ),
                OptionData(
                    OptionType.STRING,
                    "reason",
                    "admin.ban.reason.description",
                    false
                ),
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = AdminBanCommandExecutor()
        }

        subCommand(
            "checkban",
            "admin.checkban.description"
        ) {
            addOption(
                OptionData(
                    OptionType.STRING,
                    "member_id",
                    "admin.checkban.member_id.description",
                    true
                ),
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = AdminCheckBanCommandExecutor()
        }
    }
}