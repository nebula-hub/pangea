package hub.nebula.pangea.command.vanilla.music

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.schlaubi.lavakord.rest.loadItem
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.music.declaration.MusicCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import kotlin.reflect.jvm.jvmName

class MusicPlayCommandExecutor : PangeaSlashCommandExecutor() {
    private val logger = KotlinLogging.logger(this::class.jvmName)

    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.guild == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.guildOnly"]
                )
            }
            return
        }

        if (!context.pangeaGuild!!.dj) {
            context.reply(true) {
                pretty(
                    "This function is not active! Use `/pangeaserver view` to see more details."
                )
            }

            return
        }

        val query = context.getOption("name")!!.asString
        val source = context.getOption("source")?.asString
        val instance = PangeaInstance.pangeaPlayers.getOrPut(context.guild.idLong) { PangeaPlayerManager(context.pangea.lavakord.getLink(context.guild.id)) }
        val memberVoiceState = context.pangea.voiceStateManager[context.member!!.idLong]

        if (memberVoiceState == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.voiceChannelOnly"]
                )
            }
            return
        }

        context.defer()

        val search = if (query.isValidUrl()) {
            query
        } else {
            "${source ?: "spsearch"}:$query"
        }

        instance.link.connect(context, memberVoiceState.channelId)

        when (val item = instance.link.loadItem(search)) {
            is LoadResult.TrackLoaded -> {
                val isEmpty = instance.scheduler.queue.isEmpty()

                sendPlayingTrackEmbed(context, item.data, isEmpty)
            }

            is LoadResult.PlaylistLoaded -> {
                val mutable = item.data.tracks.toMutableList()

                val removed = mutable.removeAt(0)

                instance.scheduler.queue(removed)

                delay(2000).also {
                    instance.scheduler.queue.addAll(mutable)
                }

                val message = StringBuilder().apply {
                    appendLine(
                        "## ${context.locale["$LOCALE_PREFIX.play.playlistAdded", item.data.info.name, query]}"
                    )
                }

                if (item.data.tracks.size > 10) {
                    item.data.tracks.take(10).forEachIndexed { index, it ->
                        message.appendLine(
                            context.locale["$LOCALE_PREFIX.play.playlistAddedDescription", (index + 1).toString(), it.info.title, it.info.uri.toString(), it.info.author, it.info.sourceName]
                        )
                    }

                    message.appendLine()
                    message.appendLine(
                        context.locale["$LOCALE_PREFIX.play.playlistMoreSongs", (item.data.tracks.size - 10).toString()]
                    )
                } else {
                    item.data.tracks.forEachIndexed { index, it ->
                        message.appendLine(
                            context.locale["$LOCALE_PREFIX.play.playlistAddedDescription", (index + 1).toString(), it.info.title, it.info.uri.toString(), it.info.author, it.info.sourceName]
                        )
                    }
                }

                context.reply {
                    embed {
                        color = Constants.DEFAULT_COLOR
                        description = message.toString()
                    }
                }
            }

            is LoadResult.SearchResult -> {
                val track = item.data.tracks.firstOrNull()

                if (track != null) {
                    instance.scheduler.queue(track)

                    val isEmpty = instance.scheduler.queue.isEmpty()

                    sendPlayingTrackEmbed(context, track, isEmpty)
                } else {
                    context.reply(true) {
                        pretty(
                            context.locale["$LOCALE_PREFIX.play.noMatches", query]
                        )
                    }
                }
            }

            is LoadResult.NoMatches -> {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.play.noMatches", query]
                    )
                }
            }

            is LoadResult.LoadFailed -> {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.play.loadFailed", item.data.message.toString()]
                    )
                }
            }

            else -> {
                logger.info { "An error occurred when playing the track: ${item.data}" }

                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.play.loadFailedStackstrace", item.data.toString()]
                    )
                }
            }
        }
    }

    private suspend fun sendPlayingTrackEmbed(context: PangeaInteractionContext, track: Track, queueEmpty: Boolean = false) = context.reply(false) {
        embed {
            url = track.info.uri
            title = "${track.info.title} (${track.info.sourceName})"
            color = Constants.DEFAULT_COLOR
            thumbnail = track.info.artworkUrl + "?size=2048"

            footer {
                name = if (queueEmpty) context.locale["$LOCALE_PREFIX.play.playingNow"] else context.locale["$LOCALE_PREFIX.play.addedToQueue"]
            }

            description = StringBuilder().apply {
                appendLine(
                    context.locale["$LOCALE_PREFIX.play.author", track.info.author]
                )
                appendLine(
                    context.locale["$LOCALE_PREFIX.play.length", track.info.length.humanize()]
                )
                appendLine()
                if (!queueEmpty) appendLine(
                    context.locale["$LOCALE_PREFIX.play.queuePosition", PangeaInstance.pangeaPlayers[context.guild!!.idLong]!!.scheduler.queue.size.toString()]
                )
            }.toString()

        }
    }
}