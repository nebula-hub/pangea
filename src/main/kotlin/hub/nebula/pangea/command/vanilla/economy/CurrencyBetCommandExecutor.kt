package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.Profile
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.*
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyBetCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val amount: Long = context.option("amount")!!
        val target: net.dv8tion.jda.api.entities.User = context.option("user")!!

        newSuspendedTransaction {
            val author = context.pangeaProfile

            context.defer()

            val targetDb = Profile.findOrCreate(target.idLong)

            if (author.currency < amount) {
                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.bet.youDontHaveSufficientCurrency", "Stardusts"]
                    )
                }
                return@newSuspendedTransaction
            }

            if (targetDb.currency < amount) {
                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.bet.targetDontHaveSufficientCurrency", target.asMention, "Stardusts"]
                    )
                }
                return@newSuspendedTransaction
            }

            val text = StringBuilder().apply {
                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.bet.userWantsToBetWithYou", context.user.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts", target.asMention]
                    )
                )

                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.bet.howItWorks", target.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts"]
                    )
                )
            }

            context.reply {
                content = text.toString()

                actionRow(
                    context.pangea.interactionManager
                        .createButtonForUser(target, ButtonStyle.SUCCESS, context.locale["commands.buttons.accept"]) {
                            if (author.currency < amount) {
                                it.edit {
                                    actionRow(
                                        Button.of(ButtonStyle.DANGER, "-", context.locale["commands.buttons.notEnoughCurrency"]).asDisabled()
                                    )
                                }

                                it.reply {
                                    pretty(
                                        context.locale["$LOCALE_PREFIX.bet.youDontHaveSufficientCurrency", "Stardusts"]
                                    )
                                }
                            } else if (targetDb.currency < amount) {
                                it.edit {
                                    actionRow(
                                        Button.of(ButtonStyle.DANGER, "-", context.locale["commands.buttons.notEnoughCurrency"]).asDisabled()
                                    )
                                }

                                it.reply {
                                    pretty(
                                        context.locale["$LOCALE_PREFIX.bet.targetDontHaveSufficientCurrency", target.asMention, "Stardusts"]
                                    )
                                }
                            } else {
                                val authorDice = context.random.nextInt(1, 6)
                                val targetDice = context.random.nextInt(1, 6)

                                val text = StringBuilder().apply {
                                    appendLine(
                                        prettyStr(
                                            context.locale["$LOCALE_PREFIX.bet.authorDice", authorDice.toString()]
                                        )
                                    )
                                    appendLine(
                                        prettyStr(
                                            context.locale["$LOCALE_PREFIX.bet.targetDice", target.asMention, targetDice.toString()]
                                        )
                                    )
                                }

                                it.edit {
                                    actionRow(
                                        Button.of(ButtonStyle.SUCCESS, "-", context.locale["commands.buttons.bettedSuccessfully"]).asDisabled()
                                    )
                                }

                                if (authorDice > targetDice) {
                                    newSuspendedTransaction {
                                        author.currency += amount
                                        targetDb.currency -= amount

                                        insertTransaction(
                                            target.idLong,
                                            context.user.idLong,
                                            amount,
                                            TransactionReason.BET,
                                            "global"
                                        )
                                    }

                                    text.appendLine(
                                        prettyStr(
                                            context.locale["$LOCALE_PREFIX.bet.successfullyWon", context.user.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts", target.asMention]
                                        )
                                    )

                                    it.reply {
                                        content = text.toString()
                                    }
                                } else if (authorDice < targetDice) {
                                    newSuspendedTransaction {
                                        author.currency -= amount
                                        targetDb.currency += amount

                                        insertTransaction(
                                            context.user.idLong,
                                            target.idLong,
                                            amount,
                                            TransactionReason.BET,
                                            "global"
                                        )
                                    }

                                    text.appendLine(
                                        prettyStr(
                                            context.locale["$LOCALE_PREFIX.bet.successfullyWon", target.asMention, amount.toString(), if (amount == 1L) "Stardust" else "Stardusts", context.user.asMention]
                                        )
                                    )

                                    it.reply {
                                        content = text.toString()
                                    }
                                } else {
                                    text.appendLine(
                                        prettyStr(
                                            context.locale["$LOCALE_PREFIX.bet.tie"]
                                        )
                                    )

                                    it.reply {
                                        content = text.toString()
                                    }
                                }
                            }
                        },
                    context.pangea.interactionManager
                        .createButtonForUser(target, ButtonStyle.DANGER, context.locale["commands.buttons.decline"]) {
                            it.edit {
                                actionRow(
                                    Button.of(ButtonStyle.DANGER, "-", context.locale["commands.buttons.betDeclined"]).asDisabled()
                                )
                            }

                            it.reply {
                                pretty(
                                    context.locale["$LOCALE_PREFIX.bet.declined", target.asMention]
                                )
                            }
                        }
                )
            }
        }
    }
}