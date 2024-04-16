package hub.nebula.pangea.utils

import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.component.PangeaButtonContext

fun Boolean.toLocalized(context: PangeaCommandContext): String {
    return if (this) context.locale["commands.common.enabled"] else context.locale["commands.common.disabled"]
}

fun Boolean.toLocalized(context: PangeaButtonContext): String {
    return if (this) context.locale["commands.common.enabled"] else context.locale["commands.common.disabled"]
}