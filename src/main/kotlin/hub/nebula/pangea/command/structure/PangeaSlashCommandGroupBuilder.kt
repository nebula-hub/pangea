package hub.nebula.pangea.command.structure

class PangeaSlashCommandGroupBuilder(
    val name: String,
    val description: String
) {
    val subCommands = mutableListOf<PangeaSlashCommandDeclarationBuilder>()

    fun subCommand(name: String, description: String, block: PangeaSlashCommandDeclarationBuilder.() -> Unit) {
        val subCommand = PangeaSlashCommandDeclarationBuilder(name, description)
        subCommand.block()
        subCommands.add(subCommand)
    }

    fun getSubCommand(name: String): PangeaSlashCommandDeclarationBuilder? {
        return subCommands.find { it.name == name }
    }
}