package hub.nebula.pangea.command.structure

interface PangeaSlashCommandDeclarationWrapper {
    fun create(): PangeaSlashCommandDeclarationBuilder

    fun command(name: String, description: String, isPrivate: Boolean = false, block: PangeaSlashCommandDeclarationBuilder.() -> Unit): PangeaSlashCommandDeclarationBuilder {
        return PangeaSlashCommandDeclarationBuilder(name, description, isPrivate).apply(block)
    }
}