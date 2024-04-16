package hub.nebula.pangea.command.vanilla.music

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.choice
import dev.minn.jda.ktx.messages.Embed
import dev.minn.jda.ktx.messages.invoke
import dev.schlaubi.lavakord.rest.loadItem
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.*
import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import kotlin.reflect.jvm.jvmName

class MusicCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        val LOCALE_PREFIX = "commands.command.music"
    }

    override fun create() = command(
        "music",
        "music.description"
    ) {
        subCommand(
            "play",
            "music.play.description"
        ) {
            addOption(
                OptionData(
                    OptionType.STRING,
                    "name",
                    "music.play.name.description",
                    true
                ),
                OptionData(
                    OptionType.STRING,
                    "source",
                    "music.play.source.description",
                    false
                ).choice("YouTube", "ytsearch")
                    .choice("Spotify", "spsearch")
                    .choice("SoundCloud", "scsearch")
            )

            executor = MusicPlayCommandExecutor()
        }

        subCommand(
            "queue",
            "music.queue.description"
        ) {
            executor = MusicQueueCommandExecutor()
        }

        subCommand(
            "nowplaying",
            "music.nowplaying.description"
        ) {
            executor = MusicNowPlayingCommandExecutor()
        }
    }

    inner class MusicPlayCommandExecutor : PangeaSlashCommandExecutor() {
        private val logger = KotlinLogging.logger(this::class.jvmName)

        override suspend fun execute(context: PangeaCommandContext) {
            if (context.guild == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.guildOnly"]
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

            instance.link.connect(memberVoiceState.channelId.toString())

            when (val item = instance.link.loadItem(search)) {
                is LoadResult.TrackLoaded -> {
                    val isEmpty = instance.scheduler.queue.isEmpty()

                    sendPlayingTrackEmbed(context, item.data, isEmpty)
                }

                is LoadResult.PlaylistLoaded -> {
                    val mutable = item.data.tracks.toMutableList()

                    instance.scheduler.queue(mutable.removeAt(0))

                    delay(2000).also {
                        instance.scheduler.queue.addAll(mutable)
                    }

                    val message = StringBuilder().apply {
                        appendLine(
                            pretty(
                                context.locale["$LOCALE_PREFIX.play.playlistAdded", item.data.info.name, query]
                            )
                        )
                    }

                    if (item.data.tracks.size > 10) {
                        item.data.tracks.take(10).forEachIndexed { index, it ->
                            message.appendLine(
                                pretty(
                                    context.locale["$LOCALE_PREFIX.play.playlistAddedDescription", (index + 1).toString(), it.info.title, it.info.uri.toString(), it.info.author, it.info.sourceName]
                                )
                            )
                        }

                        message.appendLine()
                        message.appendLine(
                            pretty(
                                context.locale["$LOCALE_PREFIX.play.playlistMoreSongs", (item.data.tracks.size - 10).toString()]
                            )
                        )
                    } else {
                        item.data.tracks.forEachIndexed { index, it ->
                            message.appendLine(
                                pretty(
                                    context.locale["$LOCALE_PREFIX.play.playlistAddedDescription", (index + 1).toString(), it.info.title, it.info.uri.toString(), it.info.author, it.info.sourceName]
                                )
                            )
                        }
                    }

                    context.reply {
                        content = message.toString()
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

        private suspend fun sendPlayingTrackEmbed(context: PangeaCommandContext, track: Track, queueEmpty: Boolean = false) = context.reply(false) {
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

    inner class MusicQueueCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            if (context.guild == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.guildOnly"]
                    )
                }
                return
            }

            val instance = PangeaInstance.pangeaPlayers[context.guild.idLong]

            if (instance == null) {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.instanceNotFound"]
                    )
                }
                return
            }

            var currentPage = 0
            val pages = instance.scheduler.queue.chunked(10)

            println(instance.scheduler.queue.size)

            if (pages.isNotEmpty()) {
                context.reply {
                    embed {
                        title = context.locale["$LOCALE_PREFIX.queue.title", instance.scheduler.queue.size.toString()]
                        color = Constants.DEFAULT_COLOR
                        thumbnail = context.guild.iconUrl() ?: context.jda.selfUser.effectiveAvatarUrl

                        description = if (pages[currentPage].size == 1) {
                            pages[currentPage].joinToString("\n") { track ->
                                val info = track.info
                                val title = info.title
                                val author = info.author
                                val source = info.sourceName
                                val uri = info.uri ?: "https://discord.com" // xd

                                context.locale["$LOCALE_PREFIX.queue.onlyOneSongDescription", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri, author, source]
                            }
                        } else {
                            pages[currentPage].joinToString("\n") { track ->
                                val info = track.info
                                val title = info.title
                                val author = info.author
                                val source = info.sourceName
                                val uri = info.uri ?: "https://discord.com" // xd

                                context.locale["$LOCALE_PREFIX.queue.description", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri, author, source]
                            }
                        }

                        if (pages.isNotEmpty()) {
                            footer {
                                name = context.locale["$LOCALE_PREFIX.queue.currentPage", (currentPage + 1).toString(), pages.size.toString()]
                            }
                        } else {
                            footer {
                                name = context.locale["$LOCALE_PREFIX.queue.currentPage", 1.toString(), 1.toString()]
                            }
                        }

                        field {
                            name = context.locale["$LOCALE_PREFIX.queue.playingNow"]
                            value = "**[${instance.link.player.playingTrack?.info?.title}](${instance.link.player.playingTrack?.info?.uri})** - ${instance.link.player.playingTrack?.info?.author} (${instance.link.player.playingTrack?.info?.sourceName})"
                            inline = true
                        }
                    }

                    actionRow(
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Voltar") {
                                if (currentPage > 0) {
                                    currentPage--
                                } else {
                                    it.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.queue.alreadyOnFirstPage"]
                                        )
                                    }
                                }

                                val embed = Embed {
                                    title = context.locale["$LOCALE_PREFIX.queue.title", instance.scheduler.queue.size.toString()]
                                    color = Constants.DEFAULT_COLOR
                                    thumbnail = context.guild.iconUrl() ?: context.jda.selfUser.effectiveAvatarUrl

                                    footer {
                                        name = context.locale["$LOCALE_PREFIX.queue.currentPage", (currentPage + 1).toString(), pages.size.toString()]
                                    }

                                    description = pages[currentPage].joinToString("\n") { track ->
                                        val info = track.info
                                        val title = info.title
                                        val author = info.author
                                        val source = info.sourceName
                                        val uri = info.uri

                                        context.locale["$LOCALE_PREFIX.queue.description", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri.toString(), author, source]
                                    }

                                    field {
                                        name = context.locale["$LOCALE_PREFIX.queue.playingNow"]
                                        value = "**[${instance.link.player.playingTrack?.info?.title}](${instance.link.player.playingTrack?.info?.uri})** - ${instance.link.player.playingTrack?.info?.author} (${instance.link.player.playingTrack?.info?.sourceName})"
                                        inline = true
                                    }
                                }

                                it.deferEdit().editOriginalEmbeds(embed).await()
                        },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Próxima") {
                                if (currentPage < pages.size - 1) {
                                    currentPage++
                                } else {
                                    context.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.queue.alreadyOnLastPage"]
                                        )
                                    }
                                }

                                val embed = Embed {
                                    title = context.locale["$LOCALE_PREFIX.queue.title", instance.scheduler.queue.size.toString()]
                                    color = Constants.DEFAULT_COLOR
                                    thumbnail = context.guild.iconUrl() ?: context.jda.selfUser.effectiveAvatarUrl

                                    footer {
                                        name = context.locale["$LOCALE_PREFIX.queue.currentPage", (currentPage + 1).toString(), pages.size.toString()]
                                    }

                                    description = pages[currentPage].joinToString("\n") { track ->
                                        val info = track.info
                                        val title = info.title
                                        val author = info.author
                                        val source = info.sourceName
                                        val uri = info.uri

                                        context.locale["$LOCALE_PREFIX.queue.description", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri.toString(), author, source]
                                    }

                                    field {
                                        name = context.locale["$LOCALE_PREFIX.queue.playingNow"]
                                        value = "**[${instance.link.player.playingTrack?.info?.title}](${instance.link.player.playingTrack?.info?.uri})** - ${instance.link.player.playingTrack?.info?.author} (${instance.link.player.playingTrack?.info?.sourceName})"
                                        inline = true
                                    }
                                }

                                it.deferEdit().editOriginalEmbeds(embed).await()
                        }
                    )
                }
            } else {
                if (instance.link.player.playingTrack != null) {
                    context.reply {
                        embed {
                            title = context.locale["$LOCALE_PREFIX.nowPlaying.musicQueue"]
                            color = Constants.DEFAULT_COLOR
                            thumbnail = context.guild.iconUrl() ?: context.jda.selfUser.effectiveAvatarUrl

                            field {
                                name = context.locale["$LOCALE_PREFIX.nowPlaying.nowPlaying"]
                                value = instance.link.player.playingTrack?.info?.title ?: context.locale["$LOCALE_PREFIX.instanceNotFound"]
                                inline = true
                            }
                        }
                    }
                } else {
                    context.reply {
                        pretty(
                            context.locale["$LOCALE_PREFIX.queue.noSongsInQueue"]
                        )
                    }
                }
            }
        }
    }

    inner class MusicNowPlayingCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            if (context.guild == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.guildOnly"]
                    )
                }
                return
            }

            val instance = PangeaInstance.pangeaPlayers[context.guild.idLong]

            if (instance == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.command.music.instanceNotFound"]
                    )
                }
                return
            }

            val track = instance.link.player.playingTrack

            if (track != null) {
                context.reply {
                    embed {
                        val info = track.info

                        url = info.uri
                        title = context.locale["$LOCALE_PREFIX.nowPlaying.title", info.title, info.sourceName]
                        color = Constants.DEFAULT_COLOR
                        thumbnail = track.info.artworkUrl + "?size=2048"

                        description = StringBuilder().apply {
                            appendLine(
                                context.locale["$LOCALE_PREFIX.nowPlaying.author", info.author]
                            )
                            appendLine(
                                context.locale["$LOCALE_PREFIX.nowPlaying.length", info.length.humanize()]
                            )
                        }.toString()
                    }
                }
            } else {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.thereIsntAnySongPlaying"]
                    )
                }
            }
        }
    }
}