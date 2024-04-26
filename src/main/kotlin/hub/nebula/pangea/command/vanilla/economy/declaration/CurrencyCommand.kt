package hub.nebula.pangea.command.vanilla.economy.declaration

import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.economy.*
import hub.nebula.pangea.command.vanilla.economy.local.*
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class CurrencyCommand : PangeaSlashCommandDeclarationWrapper {
    companion object {
        const val LOCALE_PREFIX = "commands.command.currency"
    }

    override fun create() = command(
        "currency",
        "Manage your currency"
    ) {
        subCommandGroup("local", "Local economy", this@command.name) {
            subCommand(
                "balance",
                "Check your balance",
            ) {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "The user to check the balance",
                        false
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyLocalBalanceCommandExecutor()
            }

            subCommand(
                "daily",
                "Claim your daily reward"
            ) {
                executor = CurrencyLocalDailyCommandExecutor()
            }

            subCommand(
                "rank",
                "Check currency rank"
            ) {
                executor = CurrencyLocalRankCommandExecutor()
            }

            subCommand(
                "bet",
                "Bet your currency"
            ) {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "The user to bet against",
                        true
                    ),
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "The amount to bet",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyLocalBetCommandExecutor()
            }

            subCommand("slots", "Play the slots") {
                addOption(
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "The amount to bet",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )
                executor = CurrencyLocalSlotsCommandExecutor()
            }

            subCommand("pay", "pay another user") {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "the user you wanna pay",
                        true
                    ),
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "the amount you wanna pay",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyLocalPayCommandExecutor()
            }
        }
        subCommandGroup("global", "Global economy", this@command.name) {
            subCommand("balance", "Check your balance") {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "The user to check the balance",
                        false
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyBalanceCommandExecutor()
            }

            subCommand("rank", "Check currency rank") {
                executor = CurrencyRankCommandExecutor()
            }

            subCommand("bet", "Bet your currency") {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "The user to bet against",
                        true
                    ),
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "The amount to bet",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyBetCommandExecutor()
            }

            subCommand("slots", "Play the slots") {
                addOption(
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "The amount to bet",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencySlotsCommandExecutor()
            }

            subCommand("daily", "Claim your daily reward") {
                executor = CurrencyDailyCommandExecutor()
            }

            subCommand("pay", "pay another user") {
                addOption(
                    OptionData(
                        OptionType.USER,
                        "user",
                        "the user you wanna pay",
                        true
                    ),
                    OptionData(
                        OptionType.INTEGER,
                        "amount",
                        "the amount you wanna pay",
                        true
                    ),
                    isSubcommand = true,
                    baseName = this@command.name
                )

                executor = CurrencyPayCommandExecutor()
            }
        }
    }
}