package hub.nebula.pangea.listener

import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.listener.vanilla.PangeaReadyEvent
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MajorEventListener : ListenerAdapter() {
    private val coroutineScope = PangeaInstance.coroutineScope

    override fun onReady(event: ReadyEvent) {
        coroutineScope.launch {
            PangeaReadyEvent(event).handle()
        }
    }
}