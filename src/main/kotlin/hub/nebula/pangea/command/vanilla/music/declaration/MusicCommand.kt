package hub.nebula.pangea.command.vanilla.music.declaration

import dev.minn.jda.ktx.interactions.commands.choice
import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.music.*
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class MusicCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        const val LOCALE_PREFIX = "commands.command.music"
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
                    .choice("Spotify (Default)", "spsearch")
                    .choice("SoundCloud", "scsearch"),
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = MusicPlayCommandExecutor()
        }

        subCommand(
            "search",
            "$LOCALE_PREFIX.search.description",
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
                    .choice("Spotify (Default)", "spsearch")
                    .choice("SoundCloud", "scsearch"),
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = MusicSearchCommandExecutor()
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
}