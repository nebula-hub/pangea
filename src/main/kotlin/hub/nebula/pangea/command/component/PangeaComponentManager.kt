package hub.nebula.pangea.command.component

import com.github.benmanes.caffeine.cache.Caffeine
import hub.nebula.pangea.utils.pretty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class PangeaComponentManager {
    companion object {
        val delay = 15.minutes
    }
    // Loritta's version is more beaultiful
    val buttonCallbacks = Caffeine
        .newBuilder()
        .expireAfterWrite(delay.toJavaDuration())
        .build<UUID, suspend (PangeaButtonContext) -> Unit>()
        .asMap()

    fun createButtonForUser(
        targetUser: User,
        style: ButtonStyle,
        label: String = "",
        builder: (ButtonBuilder).() -> (Unit) = {},
        callback: suspend (PangeaButtonContext) -> (Unit)
    ) = createButton(targetUser.idLong, style, label, builder, callback)

    fun createButton(
        targetUserId: Long,
        style: ButtonStyle,
        label: String = "",
        builder: (ButtonBuilder).() -> (Unit) = {},
        callback: suspend (PangeaButtonContext) -> (Unit)
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
        callback: suspend (PangeaButtonContext) -> (Unit)
    ): Button {
        val buttonId = UUID.randomUUID()
        buttonCallbacks[buttonId] = callback

        return Button.of(
            style,
            PangeaComponentId(buttonId).toString(),
            label
        ).let {
            ButtonBuilder(it).apply(builder).button
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