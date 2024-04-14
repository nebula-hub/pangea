package hub.nebula.pangea.command.vanilla.misc

import dev.minn.jda.ktx.messages.SendDefaults.content
import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor

class CalcCommand: PangeaSlashCommandDeclarationWrapper {
    override fun create() = command("calc", "Calculate a math expression") {
        executor = CalcCommandExecutor()
    }


    inner class CalcCommandExecutor : PangeaSlashCommandExecutor {
        override suspend fun execute(context: PangeaCommandContext) {
            context.defer(false)
            context.reply(false) {}
                content = "calc command todo"
            }
        }
    }
