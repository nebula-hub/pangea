package hub.nebula.pangea.command.structure

import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import hub.nebula.pangea.api.localization.PangeaLocale
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.DiscordLocale
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
    var baseName = ""
    private val enUsLocale = PangeaLocale("en-us")
    private val ptBrLocale = PangeaLocale("pt-br")

    fun subCommand(name: String, description: String, baseName: String? = null, block: PangeaSlashCommandDeclarationBuilder.() -> Unit) {
        val subCommand = PangeaSlashCommandDeclarationBuilder(name, description)
        subCommand.block()
        subCommands.add(subCommand)

        if (baseName != null) {
            this.baseName = baseName
        }
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

    fun addOption(vararg option: OptionData, isSubcommand: Boolean = false, baseName: String) {
        option.forEach { op ->
            if (isSubcommand) {
                op.setDescriptionLocalizations(mapOf(
                    DiscordLocale.PORTUGUESE_BRAZILIAN to ptBrLocale["commands.command.${baseName}.${name}.options.${op.name}.description"],
                    DiscordLocale.ENGLISH_US to enUsLocale["commands.command.${baseName}.${name}.options.${op.name}.description"]
                ))
            } else {
                op.setDescriptionLocalizations(mapOf(
                    DiscordLocale.PORTUGUESE_BRAZILIAN to ptBrLocale["commands.command.$baseName.options.${op.name}.description"],
                    DiscordLocale.ENGLISH_US to enUsLocale["commands.command.$baseName.options.${op.name}.description"]
                ))
            }
            options.add(op)
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
            setDescriptionLocalizations(mapOf(
                DiscordLocale.ENGLISH_US to enUsLocale["commands.command.$name.description"],
                DiscordLocale.PORTUGUESE_BRAZILIAN to ptBrLocale["commands.command.$name.description"]
            ))

            defaultPermissions = DefaultMemberPermissions.enabledFor(permissions)

            this.addOptions(options)
            subCommands.forEach { subCmd ->
                addSubcommands(
                    Subcommand(subCmd.name, subCmd.description) {
                        setDescriptionLocalizations(mapOf(
                            DiscordLocale.ENGLISH_US to enUsLocale["commands.command.${baseName}.${subCmd.name}.description"],
                            DiscordLocale.PORTUGUESE_BRAZILIAN to ptBrLocale["commands.command.${baseName}.${subCmd.name}.description"]
                        ))
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