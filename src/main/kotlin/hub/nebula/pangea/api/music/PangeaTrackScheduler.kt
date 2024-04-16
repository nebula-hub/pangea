package hub.nebula.pangea.api.music

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.audio.*
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.structure.MusicProgressBar
import mu.KotlinLogging
import java.util.LinkedList
import kotlin.reflect.jvm.jvmName

class PangeaTrackScheduler(private val link: Link) {
    val queue = LinkedList<Track>()
    val logger = KotlinLogging.logger(this::class.jvmName)
    lateinit var musicProgressBar: MusicProgressBar

    suspend fun queue(track: Track) {
        if (link.player.playingTrack != null) {
            logger.info { "The bot is still playing songs, queueing..." }
            queue.offer(track)
        } else {
            logger.info { "No songs, yay, playing ${track.info.title} (${track.info.sourceName}) immediately!" }
            musicProgressBar = MusicProgressBar(track.info.length)
            link.player.playTrack(track)
            musicProgressBar.start()
        }
    }

    suspend fun nextTrack() = link.player.playTrack(queue.poll())

    suspend fun terminate() {
        link.player.stopTrack()
        queue.clear()
        PangeaInstance.pangeaPlayers.remove(link.guildId.toLong())
        link.disconnectAudio()
        link.destroy()
    }

    init {
        link.player.on<TrackEvent> {
            when (this) {
                is TrackEndEvent -> {
                    if (queue.isNotEmpty()) {
                        musicProgressBar.stop()
                        nextTrack()
                    }
                }

                is TrackExceptionEvent -> {
                    logger.info { "An error occurred when playing the track: ${this.exception}" }
                }
            }
        }
    }
}