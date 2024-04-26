package hub.nebula.pangea.command.vanilla.economy.local

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.*
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyLocalPayCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val user: User = context.option("user")!!
        val amount: Long = context.option("amount")!!

        val author = newSuspendedTransaction {
            context.pangeaGuild!!.getMember(context.user.idLong) ?: context.pangeaGuild.registerMember(context.user.idLong)
        }
        val target = newSuspendedTransaction {
            context.pangeaGuild!!.getMember(user.idLong) ?: context.pangeaGuild.registerMember(user.idLong)
        }

        if (context.guild == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.guildOnly"]
                )
            }
            return
        }

        if (context.pangeaGuild?.localEconomy == false) {
            context.reply(true) {
                pretty(
                    context.locale["commands.modules.localEconomy.disabled"]
                )
            }
            return
        }

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

        if (author.currency < amount) {
            context.reply(true) {
                pretty(
                    context.locale["$LOCALE_PREFIX.pay.insufficientFunds", context.pangeaGuild?.currencyNamePlural.toString()]
                )
            }
            return
        }

        context.reply {
            pretty(
                context.locale["$LOCALE_PREFIX.pay.confirmation", context.user.asMention, amount.toString(), if (amount == 1L) context.pangeaGuild?.currencyName.toString() else context.pangeaGuild?.currencyNamePlural.toString(), user.asMention]
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
                            author.currency -= amount
                            target.currency += amount

                            insertTransaction(
                                context.user.idLong,
                                user.idLong,
                                amount,
                                TransactionReason.PAYMENT,
                                "local"
                            )

                            context.pangeaGuild!!.updateMember(author)
                            context.pangeaGuild.updateMember(target)
                        }

                        val text = StringBuilder().apply {
                            appendLine(
                                prettyStr(
                                    it.locale["$LOCALE_PREFIX.pay.successfullyTransferred", user.asMention, amount.toString(), if (amount == 1L) context.pangeaGuild?.currencyName.toString() else context.pangeaGuild?.currencyNamePlural.toString(), context.user.asMention]
                                )
                            )

                            appendLine(
                                prettyStr(
                                    it.locale["$LOCALE_PREFIX.pay.currentBalance", context.pangeaProfile.currency.toString(), if (context.pangeaProfile.currency == 1L) context.pangeaGuild?.currencyName.toString() else context.pangeaGuild?.currencyNamePlural.toString()]
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
                                Button.of(ButtonStyle.DANGER, "-", it.locale["commands.buttons.paymentDeclined"]).asDisabled()
                            )
                        }

                        it.reply {
                            pretty(
                                it.locale["$LOCALE_PREFIX.pay.userDeclined", user.asMention]
                            )
                        }
                    }
            )
        }
    }
}