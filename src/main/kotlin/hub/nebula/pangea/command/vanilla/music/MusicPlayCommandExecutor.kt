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

class MusicPlayCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.guild == null) {
            context.fail(true) {
                pretty(
                    context.locale["commands.guildOnly"]
                )
            }
            return
        }

        if (!context.pangeaGuild!!.dj) {
            context.fail(true) {
                pretty(
                    context.locale["commands.modules.dj"]
                )
            }
            return
        }

        if (context.member?.voiceState == null) {
            context.fail(true) {
                pretty(
                    context.locale["commands.voiceChannelOnly"]
                )
            }
            return
        }

        val query: String = context.option("name")!!
        val source: String = context.option("source") ?: "spsearch"

        context.defer()

        val result = if (query.isValidUrl()) {
            query
        } else {
            "$source:$query"
        }

        val link = context.pangea.lavakord.getLink(context.guild.id)

        val voiceStateFromCache = context.pangea.voiceStateManager[context.member.idLong]

        if (voiceStateFromCache == null) {
            context.fail(true) {
                pretty(
                    context.locale["commands.modules.dj.reEnterVoiceChannel"]
                )
            }
            return
        }

        val instance = PangeaInstance.pangeaPlayers.getOrPut(context.guild.idLong) { PangeaPlayerManager(link, context, voiceStateFromCache.channelId) }

        val anotherVoiceStateFromCache = context.pangea.voiceStateManager[context.member.idLong]

        if (anotherVoiceStateFromCache?.channelId != instance.voiceChannel) {
            context.fail(true) {
                pretty(
                    context.locale["commands.command.play.notInTheSameVoiceChannel"]
                )
            }
            return
        }

        when (val item = instance.link.loadItem(result)) {
            is LoadResult.TrackLoaded -> {
                // when one track is loaded by the link
                val isEmpty = instance.scheduler.queue.isEmpty()

                instance.link.connect(context, voiceStateFromCache.channelId)
                instance.scheduler.queue(item.data)
                sendPlayingTrackEmbed(context, item.data, isEmpty)
            }

            is LoadResult.SearchResult -> {
                // search result from query

                val firstSong = item.data.tracks.first()

                instance.link.connect(context, voiceStateFromCache.channelId)
                instance.scheduler.queue(firstSong)

                val isEmpty = instance.scheduler.queue.isEmpty()

                sendPlayingTrackEmbed(context, firstSong, isEmpty)
            }

            is LoadResult.PlaylistLoaded -> {
                // load all songs from a playlist
                val asMutable = item.data.tracks.toMutableList()
                val first = asMutable.removeAt(0) // it will remove and return the first track

                instance.link.connect(context, voiceStateFromCache.channelId)
                instance.scheduler.queue(first)
                instance.scheduler.queue.addAll(asMutable)

                val message = StringBuilder().apply {
                    appendLine(
                        "## ${context.locale["$LOCALE_PREFIX.play.playlistAdded", item.data.info.name, result]}"
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

            is LoadResult.NoMatches -> {
                context.fail(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.play.noMatches", query]
                    )
                }
            }

            else -> {
                context.fail(true) {
                    pretty(
                        "Something has gone wrong... e: `${item.data.toString()}`"
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