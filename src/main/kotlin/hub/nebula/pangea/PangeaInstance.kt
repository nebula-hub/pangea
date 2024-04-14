package hub.nebula.pangea

import hub.nebula.pangea.configuration.GeneralConfig.PangeaConfig
import hub.nebula.pangea.listener.MajorEventListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder

class PangeaInstance(
    private val config: PangeaConfig
) {
    companion object {
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    lateinit var jda: JDA

    fun start() {
        jda = JDABuilder.createDefault(config.token)
            .addEventListeners(MajorEventListener())
            .build()

        jda.awaitReady()
    }
}