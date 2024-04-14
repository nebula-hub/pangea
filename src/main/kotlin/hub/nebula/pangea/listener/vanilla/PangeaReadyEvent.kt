package hub.nebula.pangea.listener.vanilla

import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import kotlin.reflect.jvm.jvmName

class PangeaReadyEvent(private val event: ReadyEvent) {
    val logger = KotlinLogging.logger(this::class.jvmName)

    suspend fun handle() {
        logger.info { "Pangea is ready!" }
    }
}