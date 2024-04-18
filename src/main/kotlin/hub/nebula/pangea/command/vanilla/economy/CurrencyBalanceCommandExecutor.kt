package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.User
import hub.nebula.pangea.utils.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyBalanceCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val gateway = context.getOption("gateway")?.asString

        showBalance(
            context,
            context.getOption("user")?.asUser?.idLong ?: context.user.idLong,
            gateway ?: "global-economy"
        )
    }

    private suspend fun showBalance(context: PangeaInteractionContext, userId: Long, gateway: String) {
        when (gateway) {
            "global-economy" -> {
                val user = newSuspendedTransaction {
                    User.findOrCreate(userId)
                }

                val currencyRanking = newSuspendedTransaction {
                    User.all().sortedByDescending { it.currency }
                        .toList()
                }

                val userRank = currencyRanking.indexOfFirst { it.id == user.id } + 1

                val currencyName = if (user.currency == 1L) {
                    "Stardust"
                } else {
                    "Stardusts"
                }

                val text = StringBuilder().apply {
                    appendLine(
                        prettyStr(
                            context.locale["$LOCALE_PREFIX.balance.balanceText", user.currency.toString(), currencyName]
                        )
                    )

                    appendLine(
                        prettyStr(
                            context.locale["$LOCALE_PREFIX.balance.rankingText", userRank.toString(), "Stardusts"]
                        )
                    )
                }

                context.reply {
                    content = text.toString()
                }
            }
            "local-economy" -> {
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
                            context.locale["$LOCALE_PREFIX.balance.localEconomyDisabled"]
                        )
                    }
                    return
                }

                context.defer()

                val member = newSuspendedTransaction {
                    guild.getMember(userId)
                } ?: newSuspendedTransaction {
                    guild.registerMember(userId)
                }

                val currencyName = if (member.currency == 1L) {
                    guild.currencyName
                } else {
                    guild.currencyNamePlural
                }

                val userRank = newSuspendedTransaction {
                    guild.retrieveAllMembers()
                }.sortedByDescending { it.currency }.indexOfFirst { it.id == member.id } + 1

                val text = StringBuilder().apply {
                    if (userId != context.member!!.idLong) {
                        appendLine(
                            prettyStr(
                                context.locale["$LOCALE_PREFIX.balance.otherBalanceText", "<@$userId>", member.currency.toString(), currencyName]
                            )
                        )
                    } else {
                        appendLine(
                            prettyStr(
                                context.locale["$LOCALE_PREFIX.balance.balanceText", member.currency.toString(), currencyName]
                            )
                        )
                    }
                    appendLine(
                        prettyStr(
                            context.locale["$LOCALE_PREFIX.balance.rankingText", userRank.toString(), guild.currencyNamePlural]
                        )
                    )
                }

                context.reply {
                    content = text.toString()
                }
            }
        }
    }
}