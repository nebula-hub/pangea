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
import hub.nebula.pangea.command.component.PangeaButtonContext
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
        "$LOCALE_PREFIX.description"
    ) {
        subCommand(
            "play",
            "$LOCALE_PREFIX.play.description",
            this@command.name
        ) {
            addOption(
                OptionData(
                    OptionType.STRING,
                    "name",
                    "$LOCALE_PREFIX.play.name.description",
                    true
                ),
                OptionData(
                    OptionType.STRING,
                    "source",
                    "$LOCALE_PREFIX.play.source.description",
                    false
                ).choice("YouTube", "ytsearch")
                    .choice("Spotify", "spsearch")
                    .choice("SoundCloud", "scsearch"),
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = MusicPlayCommandExecutor()
        }

        subCommand(
            "queue",
            "$LOCALE_PREFIX.queue.description",
            this@command.name
        ) {
            executor = MusicQueueCommandExecutor()
        }

        subCommand(
            "nowplaying",
            "$LOCALE_PREFIX.nowplaying.description",
            this@command.name
        ) {
            executor = MusicNowPlayingCommandExecutor()
        }

        subCommand(
            "skip",
            "$LOCALE_PREFIX.skip.description",
            this@command.name
        ) {
            executor = MusicSkipCommandExecutor()
        }

        subCommand(
            "stop",
            "$LOCALE_PREFIX.stop.description",
            this@command.name
        ) {
            executor = MusicStopCommandExecutor()
        }

        subCommand(
            "pause",
            "$LOCALE_PREFIX.pause.description",
            this@command.name
        ) {
            executor = MusicPauseCommandExecutor()
        }

        subCommand(
            "resume",
            "$LOCALE_PREFIX.resume.description",
            this@command.name
        ) {
            executor = MusicResumeCommandExecutor()
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

                    val message = StringBuilder()

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
                            title = context.locale["$LOCALE_PREFIX.play.playlistAdded", item.data.info.name, query]
                            color = Constants.DEFAULT_COLOR
                            description = message.toString()
                        }

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

                                context.locale["$LOCALE_PREFIX.queue.embedDescription", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri, author, source]
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

                                        context.locale["$LOCALE_PREFIX.queue.embedDescription", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri.toString(), author, source]
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

                                        context.locale["$LOCALE_PREFIX.queue.embedDescription", (instance.scheduler.queue.indexOf(track) + 1).toString(), title, uri.toString(), author, source]
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
                        context.locale["$LOCALE_PREFIX.instanceNotFound"]
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
                        title = context.locale["$LOCALE_PREFIX.nowplaying.title", info.title, info.sourceName]
                        color = Constants.DEFAULT_COLOR
                        thumbnail = track.info.artworkUrl + "?size=2048"

                        description = StringBuilder().apply {
                            appendLine(
                                context.locale["$LOCALE_PREFIX.nowplaying.author", info.author]
                            )
                            appendLine(
                                context.locale["$LOCALE_PREFIX.nowplaying.length", info.length.humanize()]
                            )
                        }.toString()

                        field {
                            name = context.locale["$LOCALE_PREFIX.nowplaying.effects"]
                            value = StringBuilder().apply {
                                appendLine("· **Bass Boost**: ${instance.link.player.filters.equalizers.any { it.band == 1 }.toLocalized(context)}")
                                appendLine("· **8D**: ${(instance.link.player.filters.rotation != null).toLocalized(context)}")
                                appendLine("· **Nightcore**: ${(instance.link.player.filters.timescale != null).toLocalized(context)}")
                                appendLine("· **Vaporwave**: ${(instance.link.player.filters.timescale != null).toLocalized(context)}")
                            }.toString()
                        }
                    }

                    actionRow(
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "BB") {
                                val currentState = PangeaInstance.pangeaPlayers[context.guild.idLong]

                                if (currentState == null) {
                                    it.reply(true) {
                                        pretty(
                                            it.locale["$LOCALE_PREFIX.instanceNotFound"]
                                        )
                                    }
                                    return@createButtonForUser
                                }

                                if (instance.link.player.filters.equalizers.any { eq -> eq.band == 1 }) {
                                    instance.scheduler.toggleBassBoost(false)
                                } else {
                                    instance.scheduler.toggleBassBoost(true)
                                }

                                it.deferEdit().editOriginalEmbeds(
                                    createNowPlayingEmbed(it, instance, track)
                                ).await()
                            },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "8D") {
                                val currentState = PangeaInstance.pangeaPlayers[context.guild.idLong]

                                if (currentState == null) {
                                    it.reply(true) {
                                        pretty(
                                            it.locale["$LOCALE_PREFIX.instanceNotFound"]
                                        )
                                    }
                                    return@createButtonForUser
                                }

                                if (instance.link.player.filters.rotation != null) {
                                    instance.scheduler.toggle8D(false)
                                } else {
                                    instance.scheduler.toggle8D(true)
                                }

                                it.deferEdit().editOriginalEmbeds(
                                    createNowPlayingEmbed(it, instance, track)
                                ).await()
                            },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Nightcore") {
                                val currentState = PangeaInstance.pangeaPlayers[context.guild.idLong]

                                if (currentState == null) {
                                    it.reply(true) {
                                        pretty(
                                            it.locale["$LOCALE_PREFIX.instanceNotFound"]
                                        )
                                    }
                                    return@createButtonForUser
                                }

                                if (instance.link.player.filters.timescale != null) {
                                    instance.scheduler.toggleNightcore(false)
                                } else {
                                    instance.scheduler.toggleNightcore(true)
                                }

                                it.deferEdit().editOriginalEmbeds(
                                    createNowPlayingEmbed(it, instance, track)
                                ).await()
                            },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Vaporwave") {
                                val currentState = PangeaInstance.pangeaPlayers[context.guild.idLong]

                                if (currentState == null) {
                                    it.reply(true) {
                                        pretty(
                                            it.locale["$LOCALE_PREFIX.instanceNotFound"]
                                        )
                                    }
                                    return@createButtonForUser
                                }

                                if (instance.link.player.filters.timescale != null) {
                                    instance.scheduler.toggleVaporwave(false)
                                } else {
                                    instance.scheduler.toggleVaporwave(true)
                                }

                                it.deferEdit().editOriginalEmbeds(
                                    createNowPlayingEmbed(it, instance, track)
                                ).await()
                            }
                    )
                }
            } else {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.thereIsntAnySongPlaying"]
                    )
                }
            }
        }

        private fun createNowPlayingEmbed(context: PangeaButtonContext, instance: PangeaPlayerManager, track: Track) = Embed {
            val info = track.info

            url = info.uri
            title = context.locale["$LOCALE_PREFIX.nowplaying.title", info.title, info.sourceName]
            color = Constants.DEFAULT_COLOR
            thumbnail = track.info.artworkUrl + "?size=2048"

            description = StringBuilder().apply {
                appendLine(
                    context.locale["$LOCALE_PREFIX.nowplaying.author", info.author]
                )
                appendLine(
                    context.locale["$LOCALE_PREFIX.nowplaying.length", info.length.humanize()]
                )
            }.toString()

            field {
                name = context.locale["$LOCALE_PREFIX.nowplaying.effects"]
                value = StringBuilder().apply {
                    appendLine("· **Bass Boost**: ${instance.link.player.filters.equalizers.any { it.band == 1 }.toLocalized(context)}")
                    appendLine("· **8D**: ${(instance.link.player.filters.rotation != null).toLocalized(context)}")
                    appendLine("· **Nightcore**: ${(instance.link.player.filters.timescale != null && instance.link.player.filters.timescale!!.pitch == 1.2 ).toLocalized(context)}")
                    appendLine("· **Vaporwave**: ${(instance.link.player.filters.timescale != null && instance.link.player.filters.timescale!!.pitch == 0.8).toLocalized(context)}")
                }.toString()
            }
        }
    }

    inner class MusicSkipCommandExecutor : PangeaSlashCommandExecutor() {
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

            if (instance.link.player.playingTrack != null) {
                instance.link.player.stopTrack()
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.skip.skipped"]
                    )
                }
            } else {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.skip.noSongsToSkip"]
                    )
                }
            }
        }
    }

    inner class MusicStopCommandExecutor : PangeaSlashCommandExecutor() {
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

            instance.scheduler.terminate()

            context.reply(true) {
                pretty(
                    context.locale["$LOCALE_PREFIX.stop.stopped"]
                )
            }
        }
    }

    inner class MusicPauseCommandExecutor : PangeaSlashCommandExecutor() {
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

            if (instance.link.player.paused) {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.pause.alreadyPaused"]
                    )
                }
            } else {
                instance.scheduler.togglePause()

                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.pause.paused"]
                    )
                }
            }
        }
    }

    inner class MusicResumeCommandExecutor : PangeaSlashCommandExecutor() {
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

            if (!instance.link.player.paused) {
                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.resume.alreadyResumed"]
                    )
                }
            } else {
                instance.scheduler.togglePause()

                context.reply(true) {
                    pretty(
                        context.locale["$LOCALE_PREFIX.resume.resumed"]
                    )
                }
            }
        }
    }
}