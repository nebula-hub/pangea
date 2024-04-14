package hub.nebula.pangea.command

import hub.nebula.pangea.api.command.PangeaSlashCommandDeclarationBuilder

interface PangeaSlashCommandDeclarationWrapper {
    fun create(): PangeaSlashCommandDeclarationBuilder

    fun command(name: String, description: String, block: PangeaSlashCommandDeclarationBuilder.() -> Unit): PangeaSlashCommandDeclarationBuilder {
        return PangeaSlashCommandDeclarationBuilder(name, description).apply(block)
    }
}