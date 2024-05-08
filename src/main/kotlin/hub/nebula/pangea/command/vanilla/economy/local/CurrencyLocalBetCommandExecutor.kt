package hub.nebula.pangea.command.vanilla.economy.local

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.*
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyLocalBetCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.guild == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.guildOnly"]
                )
            }
            return
        }

        val guild = context.pangeaGuild!!

        if (!guild.localEconomy) {
            context.reply(true) {
                pretty(
                    context.locale["commands.modules.localEconomy.disabled"]
                )
            }
            return
        }

        val target: User = context.option("user")!!
        val amount: Long = context.option("amount")!!

        context.defer()

        val author = newSuspendedTransaction {
            guild.getMember(context.member!!.idLong)
        } ?: newSuspendedTransaction {
            guild.registerMember(context.member!!.idLong)
        }

        val targetDb = newSuspendedTransaction {
            guild.getMember(target.idLong)
        } ?: newSuspendedTransaction {
            guild.registerMember(target.idLong)
        }

        if (author.currency < amount) {
            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.bet.youDontHaveSufficientCurrency", guild.currencyNamePlural]
                )
            }
            return
        }

        if (targetDb.currency < amount) {
            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.bet.targetDontHaveSufficientCurrency", target.asMention, guild.currencyNamePlural]
                )
            }
            return
        }

        val text = StringBuilder().apply {
            appendLine(
                prettyStr(
                    context.locale["$LOCALE_PREFIX.bet.userWantsToBetWithYou", context.user.asMention, amount.toString(), guild.currencyNamePlural, target.asMention]
                )
            )

            appendLine(
                prettyStr(
                    context.locale["$LOCALE_PREFIX.bet.howItWorks", target.asMention, amount.toString(), if (amount == 1L) guild.currencyName else guild.currencyNamePlural]
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
                                    Button.of(
                                        ButtonStyle.DANGER,
                                        "-",
                                        context.locale["commands.buttons.notEnoughCurrency"]
                                    ).asDisabled()
                                )
                            }

                            it.reply {
                                pretty(
                                    context.locale["$LOCALE_PREFIX.bet.youDontHaveSufficientCurrency", guild.currencyNamePlural]
                                )
                            }
                        } else if (targetDb.currency < amount) {
                            it.edit {
                                actionRow(
                                    Button.of(
                                        ButtonStyle.DANGER,
                                        "-",
                                        context.locale["commands.buttons.notEnoughCurrency"]
                                    ).asDisabled()
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
                                    Button.of(
                                        ButtonStyle.SUCCESS,
                                        "-",
                                        context.locale["commands.buttons.bettedSuccessfully"]
                                    ).asDisabled()
                                )
                            }

                            if (authorDice > targetDice) {
                                author.currency += amount
                                targetDb.currency -= amount

                                text.appendLine(
                                    prettyStr(
                                        context.locale["$LOCALE_PREFIX.bet.successfullyWon", context.user.asMention, amount.toString(), guild.currencyNamePlural, target.asMention]
                                    )
                                )

                                newSuspendedTransaction {
                                    guild.updateMember(author)
                                    guild.updateMember(targetDb)
                                }

                                it.reply {
                                    content = text.toString()
                                }
                            } else if (authorDice < targetDice) {
                                author.currency -= amount
                                targetDb.currency += amount

                                text.appendLine(
                                    prettyStr(
                                        context.locale["$LOCALE_PREFIX.bet.successfullyWon", target.asMention, amount.toString(), guild.currencyNamePlural, context.user.asMention]
                                    )
                                )

                                newSuspendedTransaction {
                                    guild.updateMember(author)
                                    guild.updateMember(targetDb)
                                }

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