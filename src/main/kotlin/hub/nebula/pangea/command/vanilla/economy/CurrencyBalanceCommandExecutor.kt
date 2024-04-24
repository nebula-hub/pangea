package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.User
import hub.nebula.pangea.utils.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyBalanceCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val userId = context.getOption("user")?.asUser?.idLong ?: context.user.idLong

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
            if (user.userId != context.user.idLong) {
                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.otherBalanceText", "<@${user.userId}>", user.currency.toString(), currencyName]
                    )
                )

                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.otherRankingText", userRank.toString(), "Stardusts"]
                    )
                )
            } else {
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
        }

        context.reply {
            content = text.toString()
        }
    }
}