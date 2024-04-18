package hub.nebula.pangea.utils

import hub.nebula.pangea.command.PangeaInteractionContext

fun Boolean.toLocalized(context: PangeaInteractionContext): String {
    return if (this) context.locale["commands.common.enabled"] else context.locale["commands.common.disabled"]
}