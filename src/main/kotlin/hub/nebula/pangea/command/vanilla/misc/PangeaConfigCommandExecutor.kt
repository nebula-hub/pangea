package hub.nebula.pangea.command.vanilla.misc

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.Emojis
import hub.nebula.pangea.utils.edit
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PangeaConfigCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        if (context.guild == null) {
            context.reply {
                content = context.locale["commands.guildOnly"]
            }
            return
        }

        if (!context.member!!.hasPermission(Permission.MANAGE_SERVER)) {
            context.reply {
                content = context.locale["commands.noPermission", Permission.MANAGE_SERVER.toString()]
            }
            return
        }

        context.defer()

        val modules = mapOf(
            "DJ Pangea" to context.pangeaGuild!!.dj,
            "UNO" to context.pangeaGuild.uno,
            "Pretty Bans" to context.pangeaGuild.prettyBans,
            "Local Economy" to context.pangeaGuild.localEconomy,
            "Welcomer" to context.pangeaGuild.welcomer,
            "Event Logger" to context.pangeaGuild.eventLogger
        )

        context.reply {
            embed {
                title = "Pangea Configuration"
                color = Constants.DEFAULT_COLOR

                description = modules.entries.joinToString("\n") { (module, enabled) ->
                    val status = if (enabled) Emojis.custom["enabled"] else Emojis.custom["disabled"]
                    "$status · $module"
                }
            }

            actionRow(
                context.pangea.interactionManager
                    .createButtonForUser(context.user, ButtonStyle.PRIMARY, "DJ Pangea") { button ->
                        val hook = button.deferEdit()
                        val commands = context.jda.retrieveCommands().await()
                        val djCommands = commands.filter { it.fullCommandName.split(" ")[0] == "music" }

                        hook?.edit {
                            embed {
                                title = "DJ Pangea · Configuration"

                                description = if (context.pangeaGuild.dj) {
                                    "DJ Pangea is enabled in this server."
                                } else {
                                    "DJ Pangea is disabled in this server."
                                }

                                color = Constants.DEFAULT_COLOR

                                field {
                                    name = "Commands"
                                    value = djCommands.joinToString("\n") { command ->
                                        command.subcommands.joinToString("\n") { "/${it.fullCommandName}" }
                                    }
                                }
                            }

                            if (button.pangeaGuild!!.dj) {
                                actionRow(
                                    button.pangea.interactionManager
                                        .createButtonForUser(context.user, ButtonStyle.DANGER, "Disable") {
                                            val hook2 = it.deferEdit()

                                            newSuspendedTransaction {
                                                button.pangeaGuild.dj = false
                                            }

                                            hook2?.edit {
                                                embed {
                                                    title = "DJ Pangea · Configuration"
                                                    color = Constants.DEFAULT_COLOR

                                                    description = if (context.pangeaGuild.dj) {
                                                        "DJ Pangea is enabled in this server."
                                                    } else {
                                                        "DJ Pangea is disabled in this server."
                                                    }

                                                    field {
                                                        name = "Commands"
                                                        value = djCommands.joinToString("\n") { command ->
                                                            command.subcommands.joinToString("\n") { "/${it.fullCommandName}" }
                                                        }
                                                    }
                                                }

                                                actionRow(
                                                    Button.of(ButtonStyle.DANGER, "-", "Disabled").asDisabled()
                                                )
                                            }

                                            it.reply(true) {
                                                pretty(
                                                    "DJ Pangea has been disabled in this server."
                                                )
                                            }
                                        }
                                )
                            } else {
                                actionRow(
                                    button.pangea.interactionManager
                                        .createButtonForUser(context.user, ButtonStyle.SUCCESS, "Enable") {
                                            val hook = button.deferEdit()

                                            newSuspendedTransaction {
                                                button.pangeaGuild.dj = true
                                            }

                                            hook?.edit {
                                                embed {
                                                    title = "DJ Pangea · Configuration"

                                                    description = if (context.pangeaGuild.dj) {
                                                        "DJ Pangea is enabled in this server."
                                                    } else {
                                                        "DJ Pangea is disabled in this server."
                                                    }

                                                    color = Constants.DEFAULT_COLOR

                                                    field {
                                                        name = "Commands"
                                                        value = djCommands.joinToString("\n") { command ->
                                                            command.subcommands.joinToString("\n") { "/${it.fullCommandName}" }
                                                        }
                                                    }
                                                }

                                                actionRow(
                                                    Button.of(ButtonStyle.SUCCESS, "-", "Enabled").asDisabled()
                                                )
                                            }

                                            button.reply(true) {
                                                pretty(
                                                    "DJ Pangea has been enabled in this server."
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    },
                context.pangea.interactionManager
                    .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Local Economy") {
                        val hook = it.deferEdit()

                        hook?.edit {
                            embed {
                                title = "Local Economy · Configuration"
                                color = Constants.DEFAULT_COLOR

                                description = if (it.pangeaGuild!!.localEconomy) {
                                    "Local Economy is enabled in this server."
                                } else {
                                    "Local Economy is disabled in this server."
                                }

                                if (it.pangeaGuild.localEconomy) {
                                    field {
                                        name = "General Config"
                                        value = StringBuilder().apply {
                                            appendLine("· **Currency Name**: ${it.pangeaGuild.currencyName}/${it.pangeaGuild.currencyNamePlural}")
                                            appendLine("· **Min Daily Reward**: ${it.pangeaGuild.currencyDailyMin} ${if (it.pangeaGuild.currencyDailyMin == 1L) it.pangeaGuild.currencyName else it.pangeaGuild.currencyNamePlural}")
                                            appendLine("· **Max Daily Reward**: ${it.pangeaGuild.currencyDailyMax} ${if (it.pangeaGuild.currencyDailyMax == 1L) it.pangeaGuild.currencyName else it.pangeaGuild.currencyNamePlural}")
                                        }.toString()
                                    }
                                }
                            }

                            if (it.pangeaGuild!!.localEconomy) {
                                actionRow(
                                    it.pangea.interactionManager
                                        .createButtonForUser(context.user, ButtonStyle.PRIMARY, "Make Changes") {
                                            it.sendModal(
                                                it.pangea.interactionManager
                                                    .createModal("Local Economy Configuration", {
                                                        val currencyName = TextInput.create("currencyName", "Currency Name", TextInputStyle.SHORT)
                                                            .setValue(it.pangeaGuild!!.currencyName)
                                                            .setPlaceholder("Currency Name")
                                                            .setRequired(true)
                                                            .setMinLength(2)
                                                            .setMaxLength(8)
                                                            .build()

                                                        val currencyNamePlural = TextInput.create("currencyNamePlural", "Currency Name (Plural)", TextInputStyle.SHORT)
                                                            .setValue(it.pangeaGuild.currencyNamePlural)
                                                            .setPlaceholder("Currency Name (Plural)")
                                                            .setRequired(true)
                                                            .setMinLength(2)
                                                            .setMaxLength(8)
                                                            .build()

                                                        val minDailyReward = TextInput.create("minDailyReward", "Min Daily Reward", TextInputStyle.SHORT)
                                                            .setValue(it.pangeaGuild.currencyDailyMin.toString())
                                                            .setPlaceholder("Min Daily Reward")
                                                            .setRequired(true)
                                                            .setMinLength(1)
                                                            .setMaxLength(8)
                                                            .build()

                                                        val maxDailyReward = TextInput.create("maxDailyReward", "Max Daily Reward", TextInputStyle.SHORT)
                                                            .setValue(it.pangeaGuild.currencyDailyMax.toString())
                                                            .setPlaceholder("Max Daily Reward")
                                                            .setRequired(true)
                                                            .setMinLength(1)
                                                            .setMaxLength(8)
                                                            .build()

                                                        components = listOf(
                                                            ActionRow.of(
                                                                currencyName
                                                            ),
                                                            ActionRow.of(
                                                                currencyNamePlural
                                                            ),
                                                            ActionRow.of(
                                                                minDailyReward
                                                            ),
                                                            ActionRow.of(
                                                                maxDailyReward
                                                            )
                                                        )

                                                    }) {
                                                        val currencyName = it.getValue("currencyName")!!.asString
                                                        val currencyNamePlural = it.getValue("currencyNamePlural")!!.asString
                                                        val minDailyReward = it.getValue("minDailyReward")!!.asString.toLong()
                                                        val maxDailyReward = it.getValue("maxDailyReward")!!.asString.toLong()

                                                        if (minDailyReward > maxDailyReward) {
                                                            it.reply(true) {
                                                                pretty("The minimum daily reward cannot be greater than the maximum daily reward.")
                                                            }

                                                            return@createModal
                                                        }

                                                        newSuspendedTransaction {
                                                            it.pangeaGuild!!.currencyName = currencyName
                                                            it.pangeaGuild.currencyNamePlural = currencyNamePlural
                                                            it.pangeaGuild.currencyDailyMin = minDailyReward
                                                            it.pangeaGuild.currencyDailyMax = maxDailyReward
                                                        }

                                                        it.edit {
                                                            embed {
                                                                title = "Local Economy · Configuration"
                                                                color = Constants.DEFAULT_COLOR

                                                                description = "Local Economy is enabled in this server."

                                                                field {
                                                                    name = "Current Settings"
                                                                    value = StringBuilder().apply {
                                                                        appendLine("· **Currency Name**: $currencyName/$currencyNamePlural")
                                                                        appendLine("· **Min Daily Reward**: $minDailyReward ${if (minDailyReward == 1L) currencyName else currencyNamePlural}")
                                                                        appendLine("· **Max Daily Reward**: $maxDailyReward ${if (maxDailyReward == 1L) currencyName else currencyNamePlural}")
                                                                    }.toString()
                                                                }
                                                            }

                                                            actionRow(
                                                                Button.of(ButtonStyle.SUCCESS, "-", "Configuration updated").asDisabled()
                                                            )
                                                        }

                                                        it.reply(true) {
                                                            pretty(
                                                                "Local Economy configuration has been updated."
                                                            )
                                                        }
                                                    }
                                            )
                                        },
                                    it.pangea.interactionManager
                                        .createButtonForUser(context.user, ButtonStyle.DANGER, "Disable") {
                                            val hook = it.deferEdit()

                                            newSuspendedTransaction {
                                                it.pangeaGuild!!.localEconomy = false
                                            }

                                            hook?.edit {
                                                embed {
                                                    title = "Local Economy · Configuration"
                                                    color = Constants.DEFAULT_COLOR

                                                    description = "Local Economy is now disabled in this server!"
                                                }

                                                actionRow(
                                                    Button.of(ButtonStyle.DANGER, "-", "Disabled").asDisabled()
                                                )
                                            }

                                            it.reply(true) {
                                                pretty(
                                                    "Local Economy has been disabled in this server."
                                                )
                                            }
                                        }
                                )
                            } else {
                                actionRow(
                                    it.pangea.interactionManager
                                        .createButtonForUser(context.user, ButtonStyle.SUCCESS, "Enable") {
                                            val hook = it.deferEdit()

                                            newSuspendedTransaction {
                                                it.pangeaGuild!!.localEconomy = true
                                            }

                                            hook?.edit {
                                                embed {
                                                    title = "Local Economy · Configuration"
                                                    color = Constants.DEFAULT_COLOR

                                                    description = "Local Economy is now enabled in this server!"
                                                }

                                                actionRow(
                                                    Button.of(ButtonStyle.SUCCESS, "-", "Enabled").asDisabled()
                                                )
                                            }

                                            it.reply(true) {
                                                pretty(
                                                    "Local Economy has been enabled in this server."
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
            )
        }
    }
}