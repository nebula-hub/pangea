package hub.nebula.pangea.api.music

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.coroutines.await
import dev.schlaubi.lavakord.audio.*
import dev.schlaubi.lavakord.audio.player.*
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.database.dao.Song
import hub.nebula.pangea.database.table.Songs
import hub.nebula.pangea.utils.pretty
import mu.KotlinLogging
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.LinkedList
import kotlin.reflect.jvm.jvmName

class PangeaTrackScheduler(private val link: Link, private val context: PangeaInteractionContext) {
    val queue = LinkedList<Track>()
    val logger = KotlinLogging.logger(this::class.jvmName)

    suspend fun queue(track: Track) {
        if (link.player.playingTrack != null) {
            logger.info { "The bot is still playing songs, queueing..." }
            queue.offer(track)
        } else {
            logger.info { "No songs, yay, playing ${track.info.title} (${track.info.sourceName}) immediately!" }
            try {
                link.player.playTrack(track)
            } catch (e: Exception) {
                link.disconnectAudio()
                PangeaInstance.pangeaPlayers.remove(link.guildId.toLong())

                context.channel?.sendMessage("I've lost connection, please try again")?.await()
            }
        }
    }

    suspend fun togglePause() = link.player.pause(!link.player.paused)

    suspend fun toggleBassBoost(state: Boolean) {
        link.player.applyFilters {
            if (state) {
                equalizers.add(
                    Equalizer(
                        1,
                        0.5f
                    )
                )
            } else {
                equalizers.removeIf { it.band == 1 }
            }
        }
    }

    suspend fun toggle8D(state: Boolean) {
        link.player.applyFilters {
            if (state) {
                rotation {
                    rotationHz = 0.2
                }
            } else {
                unsetRotation()
            }
        }
    }

    suspend fun toggleNightcore(state: Boolean) {
        link.player.applyFilters {
            if (state) {
                timescale {
                    speed = 1.2
                    pitch = 1.2
                    rate = 1.0
                }
            } else {
                unsetTimescale()
            }
        }
    }

    suspend fun toggleVaporwave(state: Boolean) {
        link.player.applyFilters {
            if (state) {
                timescale {
                    speed = 0.8
                    pitch = 0.8
                    rate = 1.0
                }
            } else {
                unsetTimescale()
            }
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
            when (val event = this) {
                is TrackStartEvent -> {
                    logger.info { "Adding song to the database!" }
                    newSuspendedTransaction {
                        val songTitle = event.track.info.title
                        val songUri = event.track.info.uri
                        val songPlatform = event.track.info.sourceName

                        val song = Song.find {
                            Songs.title eq songTitle
                        }.singleOrNull()

                        if (song == null) {
                            Song.new {
                                title = songTitle
                                uri = songUri!!
                                platform = songPlatform
                                playCount = 1
                            }
                        } else {
                            song.playCount++
                        }
                    }
                    val voiceData = context.pangea.voiceStateManager[context.member!!.idLong]
                    val voiceChannel = context.guild?.getVoiceChannelById(voiceData!!.channelId)

                    voiceChannel?.modifyStatus("${track.info.author} - ${track.info.title}")?.await()
                }

                is TrackEndEvent -> {
                    if (queue.isNotEmpty()) {
                        nextTrack()
                    } else {
                        val voiceData = context.pangea.voiceStateManager[context.member!!.idLong]
                        val voiceChannel = context.guild?.getVoiceChannelById(voiceData!!.channelId)

                        voiceChannel?.modifyStatus("The queue is empty.")?.await()
                    }
                }

                is TrackExceptionEvent -> {
                    logger.info { "An error occurred when playing the track: ${event.exception}" }

                    context.reply {
                        content = "An error occurred when playing the track: ${event.exception}"
                    }
                }

                is TrackStuckEvent -> {
                    logger.info { "The track is stuck!" }

                    context.channel?.sendMessage("The track is stuck! Try again or select another source to play the music.")?.queue()
                }

                else -> {
                    logger.info { "Unknown event: $event" }
                }
            }
        }
    }
}