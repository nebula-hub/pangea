package hub.nebula.pangea.command

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.vanilla.admin.AdminCommand
import hub.nebula.pangea.command.vanilla.misc.PangeaCommand
import hub.nebula.pangea.command.vanilla.music.MusicCommand
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.commands.Command
import kotlin.reflect.jvm.jvmName

class PangeaCommandManager(private val pangea: PangeaInstance) {
    val logger = KotlinLogging.logger(this::class.jvmName)
    val commands = mutableListOf<PangeaSlashCommandDeclarationWrapper>()

    operator fun get(name: String): PangeaSlashCommandDeclarationWrapper? {
        return commands.find { it.create().name == name }
    }

    private fun register(command: PangeaSlashCommandDeclarationWrapper) {
        commands.add(command)
    }

    suspend fun handle(): MutableList<Command>? {
        val action = pangea.jda.updateCommands()

        commands.forEach { command ->
            action.addCommands(
                command.create().build()
            )
            logger.info { "Registered /${command.create().name} command!" }
        }

        return action.await()
    }

    init {
        // ===[ Miscellaneous ]===
        register(PangeaCommand())

        // ===[ Admin ]===
        register(AdminCommand())

        // ===[ Music ]===
        register(MusicCommand())
    }
}