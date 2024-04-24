package hub.nebula.pangea.command.structure

class PangeaSlashCommandGroupBuilder(
    val name: String,
    val description: String
) {
    val subCommands = mutableListOf<PangeaSlashCommandDeclarationBuilder>()

    fun subCommand(name: String, description: String, isPrivate: Boolean = false, block: PangeaSlashCommandDeclarationBuilder.() -> Unit) {
        val subCommand = PangeaSlashCommandDeclarationBuilder(name, description, isPrivate)
        subCommand.block()
        subCommands.add(subCommand)
    }

    fun getSubCommand(name: String): PangeaSlashCommandDeclarationBuilder? {
        return subCommands.find { it.name == name }
    }
}