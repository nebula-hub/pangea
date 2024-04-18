package hub.nebula.pangea.command.vanilla.music

import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.music.declaration.MusicCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.pretty

class MusicSkipCommandExecutor : PangeaSlashCommandExecutor() {
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