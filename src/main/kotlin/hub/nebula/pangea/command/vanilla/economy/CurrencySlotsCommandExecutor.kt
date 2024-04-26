package hub.nebula.pangea.command.vanilla.economy

import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencySlotsCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val amount: Long = context.option("amount")!!

        context.defer()

        val author = newSuspendedTransaction {
            context.pangeaProfile
        }

        if (author.currency < amount) {
            context.reply {
                pretty(
                    context.locale["$LOCALE_PREFIX.slots.notEnoughCurrency", "Stardusts"]
                )
            }
            return
        }

        val slots = arrayOf(
            arrayOf("<:lori_heart:1233526391967055923>", "<:kurama_heart:1233526638923354123>", "<:jerbs:1233526756003020901>"),
            arrayOf("<:chino_happy:1233526997624553495>", "<:WAAAH:1233527509338030193>", "<:oopsie:1230743085609123892>"),
            arrayOf("<:gatoburro:1233528466608226404>", "<:MONKA:1233527910783127572>", "<:Otag:1231448079891431484>")
        )

        val slot1 = slots[context.random.nextInt(0, 3)]
        val slot2 = slots[context.random.nextInt(0, 3)]
        val slot3 = slots[context.random.nextInt(0, 3)]

        val text = StringBuilder().apply {
            appendLine(
                slot1[0] + slot2[0] + slot3[0]
            )
            appendLine(
                slot1[1] + slot2[1] + slot3[1]
            )
            appendLine(
                slot1[2] + slot2[2] + slot3[2]
            )
        }

        val winnedAmount: Long
        val winMessage: String

        val multiplier: Int = when {
            // All rows are equal
            slot1[0] == slot2[0] && slot2[0] == slot3[0] && slot1[1] == slot2[1] && slot2[1] == slot3[1] && slot1[2] == slot2[2] && slot2[2] == slot3[2] -> 4
            // Two rows are equal
            (slot1[0] == slot2[0] && slot2[0] == slot3[0] && slot1[1] == slot2[1] && slot2[1] == slot3[1]) || (slot1[0] == slot2[0] && slot2[0] == slot3[0] && slot1[2] == slot2[2] && slot2[2] == slot3[2]) || (slot1[1] == slot2[1] && slot2[1] == slot3[1] && slot1[2] == slot2[2] && slot2[2] == slot3[2]) -> 3
            // One row is equal
            slot1[0] == slot2[0] && slot2[0] == slot3[0] || slot1[1] == slot2[1] && slot2[1] == slot3[1] || slot1[2] == slot2[2] && slot2[2] == slot3[2] -> 2
            // No rows are equal
            else -> 0
        }

        if (multiplier > 0) {
            winnedAmount = amount * multiplier
            winMessage = context.locale["$LOCALE_PREFIX.slots.youWon", winnedAmount.toString(), if (winnedAmount == 1L) "Stardust" else "Stardusts"]
        } else {
            winnedAmount = -amount
            winMessage = context.locale["$LOCALE_PREFIX.slots.youLose", amount.toString(), if (amount == 1L) "Stardust" else "Stardusts"]
        }

        newSuspendedTransaction {
            author.currency += winnedAmount

            if (multiplier > 0) {
                insertTransaction(
                    0L,
                    context.user.idLong,
                    winnedAmount,
                    TransactionReason.SLOTS,
                    "global"
                )
            } else {
                insertTransaction(
                    context.user.idLong,
                    0L,
                    winnedAmount.toString().split("-")[1].toLong(),
                    TransactionReason.SLOTS,
                    "global"
                )
            }
        }

        context.reply {
            embed {
                title = "Slots"
                description = text.toString()
                color = Constants.DEFAULT_COLOR

                field {
                    name = context.locale["$LOCALE_PREFIX.slots.result"]
                    value = winMessage
                }
            }
        }
    }
}