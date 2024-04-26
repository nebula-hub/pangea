package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.Profile
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.insertTransaction
import hub.nebula.pangea.utils.pretty
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyDailyCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val profile = newSuspendedTransaction {
            Profile.findOrCreate(context.user.idLong)
        }

        val dailyValue = context.random.nextLong(1000, 3000)

        if (profile.lastDaily == null) {
            newSuspendedTransaction {
                profile.currency += dailyValue
                profile.lastDaily = System.currentTimeMillis()

                insertTransaction(
                    0L,
                    context.user.idLong,
                    dailyValue,
                    TransactionReason.DAILY_REWARD,
                    "global"
                )
            }

            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.daily.claimed", "Global", dailyValue.toString(), "Stardusts"]
                )
            }
        } else {
            if (System.currentTimeMillis() - profile.lastDaily!! < 86400000) {
                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.daily.alreadyClaimed", "Global", ((profile.lastDaily!!+ 86400000) / 1000).toString()]
                    )
                }
                return
            }

            newSuspendedTransaction {
                profile.currency += dailyValue
                profile.lastDaily = System.currentTimeMillis()

                insertTransaction(
                    0L,
                    context.user.idLong,
                    dailyValue,
                    TransactionReason.DAILY_REWARD,
                    "global"
                )
            }

            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.daily.claimed", "Global", dailyValue.toString(), "Stardusts"]
                )
            }
        }
    }
}