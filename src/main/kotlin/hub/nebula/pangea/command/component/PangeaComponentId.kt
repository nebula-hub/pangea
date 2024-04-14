package hub.nebula.pangea.command.component

import java.util.UUID

class PangeaComponentId(val uniqueId: UUID) {
    companion object {
        const val prefix = "pangea"

        operator fun invoke(componentIdWithPrefix: String): PangeaComponentId {
            require(componentIdWithPrefix.startsWith("$prefix:")) { "It's not mine." }
            return PangeaComponentId(UUID.fromString(componentIdWithPrefix.substringAfter(":")))
        }
    }

    override fun toString() = "$prefix:$uniqueId"
}