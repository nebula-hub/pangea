package hub.nebula.pangea.command.vanilla.economy.local

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.utils.getMember
import hub.nebula.pangea.utils.pretty
import hub.nebula.pangea.utils.registerMember
import hub.nebula.pangea.utils.updateMember
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyLocalDailyCommandExecutor : PangeaSlashCommandExecutor() {
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

        context.defer()

        val dailyValue = context.random.nextLong(guild.currencyDailyMin, guild.currencyDailyMax)

        newSuspendedTransaction {
            val member = guild.getMember(context.member!!.idLong) ?: guild.registerMember(context.member.idLong)

            if (member.lastDaily == null) {
                member.currency += dailyValue
                member.lastDaily = System.currentTimeMillis()

                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.daily.claimed", "Local", dailyValue.toString(), guild.currencyNamePlural]
                    )
                }

                guild.updateMember(member)
            } else {
                if (System.currentTimeMillis() - member.lastDaily!! < 86400000) {

                    context.reply {
                        pretty(
                            context.locale["$LOCALE_PREFIX.daily.alreadyClaimed", "Local", ((member.lastDaily!! + 86400000) / 1000).toString()]
                        )
                    }
                    return@newSuspendedTransaction
                }

                member.currency += dailyValue
                member.lastDaily = System.currentTimeMillis()

                context.reply {
                    pretty(
                        context.locale["$LOCALE_PREFIX.daily.claimed", "Local", dailyValue.toString(), guild.currencyNamePlural]
                    )
                }

                guild.updateMember(member)
            }
        }
    }
}