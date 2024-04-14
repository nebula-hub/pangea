package hub.nebula.pangea

import hub.nebula.pangea.api.command.PangeaCommandManager
import hub.nebula.pangea.configuration.GeneralConfig.PangeaConfig
import hub.nebula.pangea.listener.MajorEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder

class PangeaInstance(
    val config: PangeaConfig
) {
    companion object {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    lateinit var jda: JDA
    lateinit var commandManager: PangeaCommandManager

    fun start() {
        jda = JDABuilder.createDefault(config.token)
            .build()
        commandManager = PangeaCommandManager(this)

        jda.addEventListener(MajorEventListener(this))
        jda.awaitReady()
    }
}