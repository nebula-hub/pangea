package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.User
import hub.nebula.pangea.utils.pretty
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyDailyCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val user = newSuspendedTransaction {
            User.findOrCreate(context.user.idLong)
        }

        val dailyValue = context.random.nextLong(1000, 3000)

        if (user.lastDaily == null) {
            newSuspendedTransaction {
                user.currency += dailyValue
                user.lastDaily = System.currentTimeMillis()
            }

            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.daily.claimed", "Global", dailyValue.toString(), "Stardusts"]
                )
            }
        } else {
            if (System.currentTimeMillis() - user.lastDaily!! < 86400000) {
                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.daily.alreadyClaimed", "Global", ((user.lastDaily!!+ 86400000) / 1000).toString()]
                    )
                }
                return
            }

            newSuspendedTransaction {
                user.currency += dailyValue
                user.lastDaily = System.currentTimeMillis()
            }

            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.daily.claimed", "Global", dailyValue.toString(), "Stardusts"]
                )
            }
        }
    }
}