package hub.nebula.pangea.command.vanilla.economy.local

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyLocalBalanceCommandExecutor : PangeaSlashCommandExecutor() {
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
                    context.locale["$LOCALE_PREFIX.balance.localEconomyDisabled"]
                )
            }
            return
        }

        context.defer()

        val userId = context.getOption("user")?.asUser?.idLong ?: context.user.idLong

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

                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.otherRankingText", userRank.toString(), guild.currencyNamePlural]
                    )
                )
            } else {
                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.balanceText", member.currency.toString(), currencyName]
                    )
                )

                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.rankingText", userRank.toString(), guild.currencyNamePlural]
                    )
                )
            }
        }

        context.reply {
            content = text.toString()
        }
    }
}