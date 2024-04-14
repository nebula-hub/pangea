package hub.nebula.pangea

import dev.minn.jda.ktx.jdabuilder.light
import hub.nebula.pangea.command.PangeaCommandManager
import hub.nebula.pangea.command.component.PangeaComponentManager
import hub.nebula.pangea.configuration.GeneralConfig.PangeaConfig
import hub.nebula.pangea.listener.PangeaButtonEvent
import hub.nebula.pangea.listener.PangeaReadyEvent
import hub.nebula.pangea.listener.PangeaSlashCommandEvent
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.interactions.DiscordLocale
import net.dv8tion.jda.api.interactions.commands.localization.ResourceBundleLocalizationFunction

class PangeaInstance(
    val config: PangeaConfig
) {
    lateinit var jda: JDA
    lateinit var commandManager: PangeaCommandManager
    lateinit var interactionManager: PangeaComponentManager

    fun start() {
        jda = light(config.token, enableCoroutines = true)
        val locales = ResourceBundleLocalizationFunction
            .fromBundles(
                "commands",
                DiscordLocale.PORTUGUESE_BRAZILIAN,
                DiscordLocale.ENGLISH_US
            ) // TODO: More localizations
            .build()
        interactionManager = PangeaComponentManager()
        commandManager = PangeaCommandManager(locales, this)
        PangeaSlashCommandEvent(this).handle()
        PangeaButtonEvent(this).handle()
        PangeaReadyEvent(this).handle()
        jda.awaitReady()
    }
}