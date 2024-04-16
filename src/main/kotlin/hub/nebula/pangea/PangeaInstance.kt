package hub.nebula.pangea

import com.github.benmanes.caffeine.cache.Caffeine
import dev.schlaubi.lavakord.LavaKord
import dev.schlaubi.lavakord.Plugin
import dev.schlaubi.lavakord.jda.*
import hub.nebula.pangea.api.music.PangeaPlayerManager
import hub.nebula.pangea.api.music.plugin.LavaSearch
import hub.nebula.pangea.api.music.plugin.LavaSrc
import hub.nebula.pangea.command.PangeaCommandManager
import hub.nebula.pangea.command.component.PangeaComponentManager
import hub.nebula.pangea.configuration.GeneralConfig.PangeaConfig
import hub.nebula.pangea.listener.MajorEventListener
import hub.nebula.pangea.listener.data.VoiceState
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.localization.ResourceBundleLocalizationFunction
import net.dv8tion.jda.api.requests.GatewayIntent

class PangeaInstance(
    val config: PangeaConfig
) {
    companion object {
        val pangeaPlayers = mutableMapOf<Long, PangeaPlayerManager>()
    }

    lateinit var jda: JDA
    lateinit var commandManager: PangeaCommandManager
    lateinit var interactionManager: PangeaComponentManager
    lateinit var lavakord: LavaKord
    val voiceStateManager = Caffeine
        .newBuilder()
        .build<Long, VoiceState>()
        .asMap()

    suspend fun start() {
        val (lava, discord) = JDABuilder.createDefault(config.token)
            .setEnabledIntents(
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES,
                GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                GatewayIntent.SCHEDULED_EVENTS,
                GatewayIntent.MESSAGE_CONTENT
            )
            .buildWithLavakord {
                plugins {
                    install(LavaSearch)
                    install(LavaSrc)
                }
            }
        jda = discord
        lavakord = lava
        interactionManager = PangeaComponentManager()
        val locales = ResourceBundleLocalizationFunction
            .fromBundles("commands", DiscordLocale.PORTUGUESE_BRAZILIAN, DiscordLocale.ENGLISH_US)
            .build()

        commandManager = PangeaCommandManager(locales, this)

        jda.addEventListener(
            MajorEventListener(this)
        )
        jda.awaitReady()
    }
}