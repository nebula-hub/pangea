package hub.nebula.pangea.listener

import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.listener.vanilla.PangeaReadyEvent
import hub.nebula.pangea.listener.vanilla.PangeaSlashCommandEvent
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MajorEventListener(private val pangea: PangeaInstance) : ListenerAdapter() {
    private val coroutineScope = PangeaInstance.coroutineScope
    private val commandManager = pangea.commandManager

    override fun onReady(event: ReadyEvent) {
        coroutineScope.launch {
            PangeaReadyEvent(pangea).handle(event)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        coroutineScope.launch {
           PangeaSlashCommandEvent(pangea).handle(event)
        }
    }
}