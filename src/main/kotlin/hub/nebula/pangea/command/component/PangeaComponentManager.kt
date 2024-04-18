package hub.nebula.pangea.command.component

import com.github.benmanes.caffeine.cache.Caffeine
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.LayoutComponent
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu
import net.dv8tion.jda.api.interactions.modals.Modal
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class PangeaComponentManager {
    companion object {
        val delay = 15.minutes
    }
    // Loritta's version is more beaultiful
    val componentCallbacks = Caffeine
        .newBuilder()
        .expireAfterWrite(delay.toJavaDuration())
        .build<UUID, suspend (PangeaInteractionContext) -> Unit>()
        .asMap()

    val stringSelectMenuCallbacks = Caffeine
        .newBuilder()
        .expireAfterWrite(delay.toJavaDuration())
        .build<UUID, suspend (PangeaInteractionContext, List<String>) -> Unit>()
        .asMap()

    fun createButtonForUser(
        targetUser: User,
        style: ButtonStyle,
        label: String = "",
        builder: (ButtonBuilder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext) -> (Unit)
    ) = createButton(targetUser.idLong, style, label, builder, callback)

    fun createButton(
        targetUserId: Long,
        style: ButtonStyle,
        label: String = "",
        builder: (ButtonBuilder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext) -> (Unit)
    ) = button(
        style,
        label,
        builder
    ) {
        if (targetUserId != it.user.idLong) {
            it.reply(true) {
                pretty(
                    it.locale["commands.onlyUserCanInteractWithThisComponent", it.user.asMention, targetUserId.toString()]
                )
            }

            return@button
        }

        callback.invoke(it)
    }

    fun button(
        style: ButtonStyle,
        label: String = "",
        builder: (ButtonBuilder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext) -> (Unit)
    ): Button {
        val buttonId = UUID.randomUUID()
        componentCallbacks[buttonId] = callback

        return Button.of(
            style,
            PangeaComponentId(buttonId).toString(),
            label
        ).let {
            ButtonBuilder(it).apply(builder).button
        }
    }

    fun createModal(
        title: String,
        builder: (ModalBuilder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext) -> (Unit)
    ) = modal(
        title,
        builder,
    ) {
        callback.invoke(it)
    }

    fun stringSelectMenuForUser(
        target: User,
        builder: (StringSelectMenu.Builder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext, List<String>) -> (Unit)
    ) = stringSelectMenu(
        builder
    ) { context, strings ->
        if (target.idLong != context.user.idLong) {
            context.reply(true) {
                pretty(
                    context.locale["commands.onlyUserCanInteractWithThisComponent", context.user.asMention, target.id]
                )
            }
            return@stringSelectMenu
        }

        callback.invoke(context, strings)
    }

    fun stringSelectMenu(
        builder: (StringSelectMenu.Builder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext, List<String>) -> (Unit)
    ): StringSelectMenu {
        val selectMenuId = UUID.randomUUID()
        stringSelectMenuCallbacks[selectMenuId] = callback
        return StringSelectMenu.create(PangeaComponentId(selectMenuId).toString())
            .apply(builder)
            .build()
    }

    fun modal(
        title: String,
        builder: (ModalBuilder).() -> (Unit) = {},
        callback: suspend (PangeaInteractionContext) -> (Unit)
    ): Modal {
        val modalId = UUID.randomUUID()
        componentCallbacks[modalId] = callback

        return Modal.create(
            PangeaComponentId(modalId).toString(),
            title
        ).let {
            ModalBuilder(it).apply(builder).modal.build()
        }
    }

    class ModalBuilder(internal var modal: Modal.Builder) {
        @get:JvmSynthetic
        var title: String
            @Deprecated("", level = DeprecationLevel.ERROR)
            get() = throw UnsupportedOperationException()
            set(value) {
                modal = modal.setTitle(value)
            }

        @get:JvmSynthetic
        var components: List<LayoutComponent>
            @Deprecated("", level = DeprecationLevel.ERROR)
            get() = throw UnsupportedOperationException()
            set(value) {
                modal.addComponents(value)
            }
    }

    class ButtonBuilder(internal var button: Button) {

        @get:JvmSynthetic
        var emoji: Emoji
            @Deprecated("", level = DeprecationLevel.ERROR)
            get() = throw UnsupportedOperationException()
            set(value) {
                button = button.withEmoji(value)
            }

        var disabled
            get() = button.isDisabled
            set(value) {
                this.button = button.withDisabled(value)
            }
    }
}