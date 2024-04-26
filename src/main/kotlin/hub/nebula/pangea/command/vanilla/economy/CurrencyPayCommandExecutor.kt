package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.insertPayment
import hub.nebula.pangea.utils.pangeaUser
import hub.nebula.pangea.utils.pretty
import hub.nebula.pangea.utils.prettyStr
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyPayCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val user: User = context.option("user")!!
        val amount: Long = context.option("amount")!!

        if (amount <= 0) {
            context.reply(true) {
                pretty(
                    context.locale["$LOCALE_PREFIX.pay.invalidAmount"]
                )
            }
            return
        }

        if (context.user.idLong == user.idLong) {
            context.reply(true) {
                pretty(
                    context.locale["$LOCALE_PREFIX.pay.selfTransfer"]
                )
            }
            return
        }

        if (context.pangeaProfile.currency < amount) {
            context.reply(true) {
                pretty(
                    context.locale["$LOCALE_PREFIX.pay.insufficientFunds", "Stardusts"]
                )
            }
            return
        }

        context.reply {
            pretty(
                context.locale["$LOCALE_PREFIX.pay.confirmation", context.user.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts", user.asMention]
            )

            actionRow(
                context.pangea.interactionManager
                    .createButtonForUser(user, ButtonStyle.SUCCESS, context.locale["commands.buttons.accept"]) {
                        it.edit {
                            actionRow(
                                Button.of(ButtonStyle.SUCCESS, "-", it.locale["commands.buttons.paymentAccepted"]).asDisabled()
                            )
                        }

                        newSuspendedTransaction {
                            insertPayment(
                                context.user.idLong,
                                user.idLong,
                                amount,
                                TransactionReason.PAYMENT
                            )
                        }

                        val text = StringBuilder().apply {
                            appendLine(
                                prettyStr(
                                    it.locale["$LOCALE_PREFIX.pay.successfullyTransferred", user.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts", context.user.asMention]
                                )
                            )

                            appendLine(
                                prettyStr(
                                    it.locale["$LOCALE_PREFIX.pay.currentBalance", user.pangeaUser().currency.toString(), if (user.pangeaUser().currency == 1L) "Stardust" else "Stardusts"]
                                )
                            )
                        }

                        it.reply {
                            content = text.toString()
                        }
                    },
                context.pangea.interactionManager
                    .createButtonForUser(user, ButtonStyle.DANGER, context.locale["commands.buttons.decline"]) {
                        it.edit {
                            actionRow(
                                Button.of(ButtonStyle.DANGER, "-", it.locale["$LOCALE_PREFIX.pay.paymentDeclined"]).asDisabled()
                            )
                        }

                        it.reply {
                            pretty(
                                it.locale["$LOCALE_PREFIX.pay.userDeclined", context.user.asMention]
                            )
                        }
                    }
            )
        }
    }
}