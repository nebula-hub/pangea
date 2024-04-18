package hub.nebula.pangea.command.vanilla.music

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.music.declaration.MusicCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.iconUrl
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class MusicQueueCommandExecutor : PangeaSlashCommandExecutor() {
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
                        .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.queue.previous"]) {
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

                            it.deferEdit()?.editOriginalEmbeds(embed)?.await()
                        },
                    context.pangea.interactionManager
                        .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.queue.next"]) {
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

                            it.deferEdit()?.editOriginalEmbeds(embed)?.await()
                        },
                    context.pangea.interactionManager
                        .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.queue.shuffle"]) {
                            instance.scheduler.queue.shuffle()

                            it.deferEdit()?.editOriginalEmbeds(
                                Embed {
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
                            )?.await()
                        }
                )
            }
        } else {
            if (instance.link.player.playingTrack != null) {
                context.reply {
                    embed {
                        title = context.locale["$LOCALE_PREFIX.queue.title", instance.scheduler.queue.size.toString()]
                        color = Constants.DEFAULT_COLOR
                        thumbnail = context.guild.iconUrl() ?: context.jda.selfUser.effectiveAvatarUrl

                        description = context.locale["$LOCALE_PREFIX.queue.noSongs"]

                        field {
                            name = context.locale["$LOCALE_PREFIX.queue.playingNow"]
                            value = "**[${instance.link.player.playingTrack?.info?.title}](${instance.link.player.playingTrack?.info?.uri})** - ${instance.link.player.playingTrack?.info?.author} (${instance.link.player.playingTrack?.info?.sourceName})"
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