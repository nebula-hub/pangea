package hub.nebula.pangea.listener

import dev.minn.jda.ktx.events.listener
import hub.nebula.pangea.PangeaInstance
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.jvm.jvmName

class PangeaReadyEvent(private val pangea: PangeaInstance) {
    private val logger = KotlinLogging.logger(this::class.jvmName)
    private val scheduledExecutorService = Executors.newScheduledThreadPool(1)

    fun handle() = pangea.jda.listener<ReadyEvent> {
        val event = it

        val self = event.jda.selfUser
        logger.info { "${self.name} is ready!"}

        val commands = pangea.commandManager.handle()
        logger.info { "Registered ${commands?.size} commands!" }
        scheduledExecutorService.scheduleAtFixedRate({ changeStatus(event) }, 0, 10, TimeUnit.MINUTES)
    }

    private fun changeStatus(event: ReadyEvent) {
        logger.info { "Changing actual status..." }

        val activities = pangea.config.activities
        val activity = activities.random()

        event.jda.presence.activity = Activity.of(
            Activity.ActivityType.fromKey(activity.type),
            activity.name
        )
    }
}