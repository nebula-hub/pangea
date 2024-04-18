package hub.nebula.pangea.command.vanilla.economy.declaration

import dev.minn.jda.ktx.interactions.commands.choice
import hub.nebula.pangea.command.structure.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.vanilla.economy.*
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
        subCommand(
            "balance",
            "Check your balance",
            this@command.name
        ) {
            addOption(
                OptionData(
                    OptionType.USER,
                    "user",
                    "The user to check the balance",
                    false
                ),
                OptionData(
                    OptionType.STRING,
                    "gateway",
                    "The gateway to check the balance",
                    false
                ).apply {
                    choice("Local", "local-economy")
                    choice("Global", "global-economy")
                },
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = CurrencyBalanceCommandExecutor()
        }

        subCommand(
            "daily",
            "Claim your daily reward",
            this@command.name
        ) {
            addOption(
                OptionData(
                    OptionType.STRING,
                    "gateway",
                    "The gateway to claim the reward",
                    false
                ).apply {
                    choice("Local", "local-economy")
                    choice("Global", "global-economy")
                },
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = CurrencyDailyCommandExecutor()
        }

        subCommand(
            "rank",
            "Check currency rank",
            this@command.name
        ) {
            addOption(
                OptionData(
                    OptionType.STRING,
                    "gateway",
                    "The gateway to check the rank",
                    false
                ).apply {
                    choice("Local", "local-economy")
                    choice("Global", "global-economy")
                },
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = CurrencyRankCommandExecutor()
        }

        subCommand(
            "bet",
            "Bet your currency",
            this@command.name
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
                OptionData(
                    OptionType.STRING,
                    "gateway",
                    "The gateway to bet the currency",
                    false
                ).apply {
                    choice("Local", "local-economy")
                    choice("Global", "global-economy")
                },

                isSubcommand = true,
                baseName = this@command.name
            )

            executor = CurrencyBetCommandExecutor()
        }

        subCommand(
            "slots",
            "Play the slots",
            this@command.name
        ) {
            addOption(
                OptionData(
                    OptionType.INTEGER,
                    "amount",
                    "The amount to bet",
                    true
                ),
                OptionData(
                    OptionType.STRING,
                    "gateway",
                    "The gateway to play the slots",
                    false
                ).apply {
                    choice("Local", "local-economy")
                    choice("Global", "global-economy")
                },
                isSubcommand = true,
                baseName = this@command.name
            )

            executor = CurrencySlotsCommandExecutor()
        }
    }
}