package hub.nebula.pangea.command.vanilla.music

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.interactions.commands.choice
import dev.minn.jda.ktx.messages.Embed
import dev.schlaubi.lavakord.rest.loadItem
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.humanize
import hub.nebula.pangea.utils.isValidUrl
import hub.nebula.pangea.utils.pretty
import kotlinx.coroutines.delay
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class MusicCommand : PangeaSlashCommandDeclarationWrapper {
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
                    "music.player.name.description",
                    true
                ),
                OptionData(
                    OptionType.STRING,
                    "source",
                    "music.player.source.description",
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

                    context.reply {
                        pretty(
                            "Adicionando ${item.data.tracks.size} músicas à fila!"
                        )
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
                                "Nenhum resultado encontrado para a busca!"
                            )
                        }
                    }
                }

                is LoadResult.NoMatches -> {
                    context.reply {
                        pretty(
                            "Nenhum resultado encontrado para a busca!"
                        )
                    }
                }

                is LoadResult.LoadFailed -> {
                    context.reply(true) {
                        pretty(
                            "Algo deu errado ao tocar a música: `${item.data.message}`"
                        )
                    }
                }

                else -> {
                    context.reply {
                        pretty(
                            "Algo de errado aconteceu e eu não sei o que é! xd"
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
                    name = if (queueEmpty) "Tocando agora!" else "Música adicionada à fila!"
                }

                description = StringBuilder().apply {
                    appendLine("· **Artista**: ${track.info.author}")
                    appendLine("· **Duração**: ${track.info.length.humanize()}")
                    appendLine()
                    if (!queueEmpty) appendLine("· **Posição na fila**: ${PangeaInstance.pangeaPlayers[context.guild!!.idLong]!!.scheduler.queue.size}°")
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
                        context.locale["commands.music.instance"]
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
                        title = context.locale["commands.music.pages.title", "${instance.scheduler.queue.size}"]
                        color = Constants.DEFAULT_COLOR
                        thumbnail = context.jda.selfUser.effectiveAvatarUrl

                        description = if (pages[currentPage].size == 1) {
                            pages[currentPage].joinToString("\n") { track ->
                                val info = track.info
                                val title = info.title
                                val author = info.author
                                val source = info.sourceName
                                val uri = info.uri ?: "https://discord.com" // xd

                                context.locale["commands.music.pages.description", title, author, source, uri]
                            }
                        } else {
                            pages[currentPage].joinToString("\n") { track ->
                                val info = track.info
                                val title = info.title
                                val author = info.author
                                val source = info.sourceName
                                val uri = info.uri

                                "${instance.scheduler.queue.indexOf(track) + 1} • **[$title]($uri)** - $author ($source)"
                            }
                        }

                        footer {
                            name = "Você está na página ${currentPage + 1} de ${pages.size}!"
                        }

                        field {
                            name = "Tocando agora"
                            value = instance.link.player.playingTrack?.info?.title ?: "Nenhuma música tocando no momento!"
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
                                            "Você já está na primeira página!"
                                        )
                                    }
                                }

                                val embed = Embed {
                                    title = "Fila de músicas - ${instance.scheduler.queue.size} músicas"
                                    color = Constants.DEFAULT_COLOR
                                    thumbnail = context.jda.selfUser.effectiveAvatarUrl

                                    footer {
                                        name = "Você está na página ${currentPage + 1} de ${pages.size}!"
                                    }

                                    description = pages[currentPage].joinToString("\n") { track ->
                                        val info = track.info
                                        val title = info.title
                                        val author = info.author
                                        val source = info.sourceName
                                        val uri = info.uri

                                        "${instance.scheduler.queue.indexOf(track) + 1} • **[$title]($uri)** - $author ($source)"
                                    }

                                    field {
                                        name = "Tocando agora"
                                        value = instance.link.player.playingTrack?.info?.title ?: "Nenhuma música tocando no momento!"
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
                                            "Você já está na última página!"
                                        )
                                    }
                                }

                                val embed = Embed {
                                    title = "Fila de músicas"
                                    color = Constants.DEFAULT_COLOR
                                    thumbnail = context.jda.selfUser.effectiveAvatarUrl

                                    footer {
                                        name = "Você está na página ${currentPage + 1} de ${pages.size}!"
                                    }

                                    description = pages[currentPage].joinToString("\n") { track ->
                                        val info = track.info
                                        val title = info.title
                                        val author = info.author
                                        val source = info.sourceName
                                        val uri = info.uri

                                        "${instance.scheduler.queue.indexOf(track) + 1} • **[$title]($uri)** - $author ($source)"
                                    }

                                    field {
                                        name = context.locale["commands.command.music.nowPlaying.nowPlaying"]
                                        value = instance.link.player.playingTrack?.info?.title ?: context.locale["commands.command.music.instanceNotFound"]
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
                            title = context.locale["commands.command.music.nowPlaying.musicQueue"]
                            color = Constants.DEFAULT_COLOR
                            thumbnail = context.jda.selfUser.effectiveAvatarUrl

                            field {
                                name = context.locale["commands.command.music.nowPlaying.nowPlaying"]
                                value = instance.link.player.playingTrack?.info?.title ?: context.locale["commands.command.music.instanceNotFound"]
                                inline = true
                            }
                        }
                    }
                } else {
                    context.reply {
                        pretty(
                            context.locale["commands.command.music.instanceNotFound"]
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
                        title = "${info.title} (${info.sourceName})"
                        color = Constants.DEFAULT_COLOR
                        thumbnail = track.info.artworkUrl + "?size=2048"

                        description = StringBuilder().apply {
                            appendLine("· **${context.locale["commands.command.music.nowPlaying.embedFieldAuthor"]}**: ${info.author}")
                            appendLine("· **${context.locale["commands.command.music.nowPlaying.embedFieldDuration"]}**: ${info.length.humanize()}")
                        }.toString()
                    }
                }
            } else {
                context.reply(true) {
                    pretty(
                        context.locale["commands.command.music.thereIsntAnyMusicPlaying"]
                    )
                }
            }
        }
    }
}