package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.Profile
import hub.nebula.pangea.utils.*
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyBalanceCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val userId: Long = context.option<User>("user")?.idLong ?: context.user.idLong

        val profile = newSuspendedTransaction {
            Profile.findOrCreate(userId)
        }

        val currencyRanking = newSuspendedTransaction {
            Profile.all().sortedByDescending { it.currency }
                .toList()
        }

        val userRank = currencyRanking.indexOfFirst { it.id == profile.id } + 1

        val currencyName = if (profile.currency == 1L) {
            "Stardust"
        } else {
            "Stardusts"
        }

        val text = StringBuilder().apply {
            if (profile.userId != context.user.idLong) {
                appendLine(
                    prettyStr(
                        context.locale["$LOCALE_PREFIX.balance.otherBalanceText", "<@${profile.userId}>", profile.currency.toString(), currencyName]
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
                        context.locale["$LOCALE_PREFIX.balance.balanceText", profile.currency.toString(), currencyName]
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