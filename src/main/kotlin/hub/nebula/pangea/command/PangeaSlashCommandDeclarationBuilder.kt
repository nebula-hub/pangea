package hub.nebula.pangea.command

import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class PangeaSlashCommandDeclarationBuilder(
    val name: String,
    val description: String,
    var executor: PangeaSlashCommandExecutor? = null
) {
    val subCommands = mutableListOf<PangeaSlashCommandDeclarationBuilder>()
    val subCommandGroups = mutableListOf<PangeaSlashCommandGroupBuilder>()
    val permissions = mutableListOf<Permission>()
    val options = mutableListOf<OptionData>()

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
        permission.forEach {
            permissions.add(it)
        }
    }

    fun addOption(vararg option: OptionData) {
        option.forEach {
            options.add(it)
        }
    }

    fun getSubCommand(name: String): PangeaSlashCommandDeclarationBuilder? {
        return subCommands.find { it.name == name }
    }

    fun getSubCommandGroup(name: String): PangeaSlashCommandGroupBuilder? {
        return subCommandGroups.find { it.name == name }
    }

    fun build(): SlashCommandData {
        val commandData = Command(name, description) {
            defaultPermissions = DefaultMemberPermissions.enabledFor(permissions)

            this.addOptions(options)
            subCommands.forEach { subCmd ->
                addSubcommands(
                    Subcommand(subCmd.name, subCmd.description) {
                        this.addOptions(subCmd.options)
                    }
                )
            }

            subCommandGroups.forEach {
                addSubcommandGroups(
                    SubcommandGroup(it.name, it.description).apply {
                        it.subCommands.forEach { subCommand ->
                            addSubcommands(
                                Subcommand(subCommand.name, subCommand.description) {
                                    this.addOptions(subCommand.options)
                                }
                            )
                        }
                    }
                )
            }

        }

        return commandData
    }
}