package hub.nebula.pangea.api.music

import dev.schlaubi.lavakord.audio.Link
import hub.nebula.pangea.command.PangeaInteractionContext

class PangeaPlayerManager(
    val link: Link,
    val context: PangeaInteractionContext
) {
    val scheduler = PangeaTrackScheduler(link, context)
}