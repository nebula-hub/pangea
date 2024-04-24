package hub.nebula.pangea.command.vanilla.misc

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.misc.declaration.PangeaCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.Song
import hub.nebula.pangea.utils.Constants
import net.dv8tion.jda.api.interactions.components.buttons.Button
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.lang.management.ManagementFactory

class PangeaInfoCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        context.defer(false)

        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory

        val songs = newSuspendedTransaction {
            Song.all().toList()
        }

        val groupedSongs = songs.groupBy { it.title }
            .mapValues { (_, songs) -> songs.maxByOrNull { it.playCount } }

        val text = StringBuilder().apply {
            groupedSongs.values.sortedByDescending { it?.playCount }.take(10).forEachIndexed {index, it ->
                appendLine("${index + 1}. [${it?.title}](${it?.uri}) - Played ${it?.playCount} times")
            }
        }

        val uptime = ManagementFactory.getRuntimeMXBean().uptime

        val seconds = uptime / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        context.reply {
            embed {
                title = context.locale["$LOCALE_PREFIX.info.embedTitle"]
                description = context.locale["$LOCALE_PREFIX.info.embedDescription"]
                color = Constants.DEFAULT_COLOR
                thumbnail = context.jda.selfUser.effectiveAvatarUrl

                field {
                    name = context.locale["$LOCALE_PREFIX.info.embedServers"]
                    value = "`${context.jda.guilds.size}`"
                    inline = true
                }

                field {
                    name = "Kotlin / JVM"
                    value = "`${KotlinVersion.CURRENT} / ${System.getProperty("java.version")}`"
                    inline = true
                }

                field {
                    name = context.locale["$LOCALE_PREFIX.info.embedRAMUsage"]
                    value = "`${usedMemory / 1024 / 1024}MB / ${totalMemory / 1024 / 1024}MB`"
                    inline = true
                }

                field {
                    name = "Uptime"
                    value = "`${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s`"
                    inline = true
                }

                field {
                    name = context.locale["$LOCALE_PREFIX.info.mostPlayedSong"]
                    value = text.toString()
                    inline = false
                }
            }

            actionRow(
                Button.link(
                    "https://github.com/nebula-hub/pangea",
                    context.locale["$LOCALE_PREFIX.info.sourceCode"]
                ),
                Button.link(
                    "https://discord.com/oauth2/authorize?client_id=${context.jda.selfUser.idLong}&permissions=8&scope=bot",
                    context.locale["$LOCALE_PREFIX.info.inviteMe"]
                ),
                Button.link(
                    "https://discord.gg/7jC6BUZKKC",
                    context.locale["$LOCALE_PREFIX.info.supportServer"]
                )
            )
        }
    }
}