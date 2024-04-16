package hub.nebula.pangea.api.music

import dev.schlaubi.lavakord.audio.Link

class PangeaPlayerManager(
    val link: Link
) {
    val scheduler = PangeaTrackScheduler(link)
}