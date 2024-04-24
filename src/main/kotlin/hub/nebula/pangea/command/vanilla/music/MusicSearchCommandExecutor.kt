package hub.nebula.pangea.command.vanilla.music

import dev.arbjerg.lavalink.protocol.v4.LoadResult
import dev.schlaubi.lavakord.rest.loadItem
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.music.declaration.MusicCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.connect
import hub.nebula.pangea.utils.isValidUrl
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class MusicSearchCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.guild == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.guildOnly"]
                )
            }
            return
        }

        val guild = context.pangeaGuild!!

        if (!guild.dj) {
            context.reply(true) {
                pretty(
                    context.locale["commands.modules.dj.disabled"]
                )
            }
            return
        }

        val query = context.getOption("name")!!.asString
        val source = context.getOption("source")?.asString ?: "spsearch"
        val instance = PangeaInstance.pangeaPlayers.getOrPut(context.guild.idLong) { PangeaPlayerManager(context.pangea.lavakord.getLink(context.guild.id), context) }
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
            "$source:$query"
        }


        when (val item = instance.link.loadItem(search)) {
            is LoadResult.SearchResult -> {
                val songs = item.data.tracks.take(10)

                try {
                    context.reply(true) {
                        embed {
                            title = context.locale["$LOCALE_PREFIX.search.title"]
                            description = context.locale["$LOCALE_PREFIX.search.iFound", songs.size.toString()]
                        }

                        actionRow(
                            context.pangea.interactionManager
                                .stringSelectMenuForUser(context.user, {
                                    setMaxValues(1)

                                    for ((i, song) in songs.withIndex()) {
                                        val content = "${i + 1}. ${song.info.title}"

                                        addOption(content, song.info.uri!!, content)
                                    }
                                }) { selectMenuContext, strings ->
                                    selectMenuContext.deferEdit()

                                    selectMenuContext.edit {
                                        actionRow(
                                            Button.of(
                                                ButtonStyle.SUCCESS,
                                                "-",
                                                selectMenuContext.locale["$LOCALE_PREFIX.search.selectedSongSuccessfully"]
                                            ).asDisabled()
                                        )
                                    }

                                    instance.link.connect(context, memberVoiceState.channelId)

                                    val selected = songs.first { it.info.uri == strings.first()}

                                    instance.scheduler.queue(selected)

                                    if (instance.scheduler.queue.isNotEmpty()) {
                                        selectMenuContext.reply {
                                            pretty(
                                                selectMenuContext.locale["$LOCALE_PREFIX.search.addedToQueue", selected.info.title, selected.info.uri!!]
                                            )
                                        }
                                    } else {
                                        selectMenuContext.reply {
                                            pretty(
                                                selectMenuContext.locale["$LOCALE_PREFIX.search.playingNow", selected.info.title, selected.info.uri!!]
                                            )
                                        }
                                    }
                                }
                        )
                    }
                } catch (e: InsufficientPermissionException) {
                    context.reply(true) {
                        pretty(
                            context.locale["$LOCALE_PREFIX.play.loadFailed", e.message.toString()]
                        )
                    }
                }
            }

             else -> {
                 context.reply {
                     pretty(
                         context.locale["$LOCALE_PREFIX.search.noResults", query]
                     )
                 }
             }
        }
    }
}