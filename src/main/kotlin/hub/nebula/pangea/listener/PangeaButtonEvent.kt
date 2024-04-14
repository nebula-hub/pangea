package hub.nebula.pangea.listener

import dev.minn.jda.ktx.coroutines.await
import dev.minn.jda.ktx.events.listener
import hub.nebula.pangea.PangeaInstance
import hub.nebula.pangea.command.component.PangeaButtonContext
import hub.nebula.pangea.command.component.PangeaComponentId
import hub.nebula.pangea.utils.pretty
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import kotlin.reflect.jvm.jvmName

class PangeaButtonEvent(val pangea: PangeaInstance) {
    val logger = KotlinLogging.logger(this::class.jvmName)

    fun handle() = pangea.jda.listener<ButtonInteractionEvent> {
        val event = it

        logger.info { "Button ${event.componentId} pressed by ${event.user.name} (${event.user.id})" }

        val componentId = try {
            PangeaComponentId(event.componentId)
        } catch (e: IllegalArgumentException) {
            logger.info { "I don't recongnize this ID, probably it's expired." }
            return@listener
        }

        val callbackId = pangea.interactionManager.buttonCallbacks[componentId.uniqueId]
        val context = PangeaButtonContext(
            event
        )

        if (callbackId == null) {
            it.editButton(
                it.button.asDisabled()
            ).await()

            context.reply(true) {
                pretty(
                    context.locale["commands.buttons.expired"]
                )
            }

            return@listener
        }

        callbackId.invoke(context)
    }
}