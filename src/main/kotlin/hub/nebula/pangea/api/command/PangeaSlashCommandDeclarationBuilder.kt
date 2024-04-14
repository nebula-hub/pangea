package hub.nebula.pangea.api.command

import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class PangeaSlashCommandDeclarationBuilder(
    val name: String,
    val description: String,
    var executor: PangeaSlashCommandExecutor? = null
) {
    val subCommands = mutableListOf<PangeaSlashCommandDeclarationBuilder>()
    val subCommandGroups = mutableListOf<PangeaSlashCommandGroupBuilder>()
    val permissions = mutableListOf<Permission>()

    fun subCommand(name: String, description: String, block: PangeaSlashCommandDeclarationBuilder.() -> Unit) {
        val subCommand = PangeaSlashCommandDeclarationBuilder(name, description)
        subCommand.block()
        subCommands.add(subCommand)
    }

    fun subCommandGroup(name: String, description: String, block: PangeaSlashCommandGroupBuilder.() -> Unit) {
        val group = PangeaSlashCommandGroupBuilder(name, description)
        group.block()
        subCommandGroups.add(group)
    }

    fun addPermission(vararg permission: Permission) {
        permissions.addAll(permission)
    }

    fun getSubCommand(name: String): PangeaSlashCommandDeclarationBuilder? {
        return subCommands.find { it.name == name }
    }

    fun getSubCommandGroup(name: String): PangeaSlashCommandGroupBuilder? {
        return subCommandGroups.find { it.name == name }
    }

    fun build(): SlashCommandData {
        val commandData = Command(name, description)

        commandData.defaultPermissions = DefaultMemberPermissions.enabledFor(permissions)

        subCommands.forEach {
            commandData.addSubcommands(
                Subcommand(it.name, it.description)
            )
        }
        subCommandGroups.forEach {
            commandData.addSubcommandGroups(
                SubcommandGroup(it.name, it.description).apply {
                    it.subCommands.forEach { subCommand ->
                        addSubcommands(
                            Subcommand(subCommand.name, subCommand.description)
                        )
                    }
                }
            )
        }

        return commandData
    }
}