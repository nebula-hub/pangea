package hub.nebula.pangea.command.vanilla.music

import dev.arbjerg.lavalink.protocol.v4.Track
import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.messages.Embed
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.music.declaration.MusicCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.humanize
import hub.nebula.pangea.utils.pretty
import hub.nebula.pangea.utils.toLocalized
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class MusicNowPlayingCommandExecutor : PangeaSlashCommandExecutor() {
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

                            it.deferEdit()?.editOriginalEmbeds(
                                createNowPlayingEmbed(it, instance, instance.link.player.playingTrack!!)
                            )?.await()
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

                            it.deferEdit()?.editOriginalEmbeds(
                                createNowPlayingEmbed(it, instance, instance.link.player.playingTrack!!)
                            )?.await()
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

                            it.deferEdit()?.editOriginalEmbeds(
                                createNowPlayingEmbed(it, instance, instance.link.player.playingTrack!!)
                            )?.await()
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

                            it.deferEdit()?.editOriginalEmbeds(
                                createNowPlayingEmbed(it, instance, instance.link.player.playingTrack!!)
                            )?.await()
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

    private fun createNowPlayingEmbed(context: PangeaInteractionContext, instance: PangeaPlayerManager, track: Track) = Embed {
        url = track.info.uri
        title = context.locale["$LOCALE_PREFIX.nowplaying.title", track.info.title, track.info.sourceName]
        color = Constants.DEFAULT_COLOR
        thumbnail = track.info.artworkUrl + "?size=2048"

        description = StringBuilder().apply {
            appendLine(
                context.locale["$LOCALE_PREFIX.nowplaying.author", track.info.author]
            )
            appendLine(
                context.locale["$LOCALE_PREFIX.nowplaying.length", track.info.length.humanize()]
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