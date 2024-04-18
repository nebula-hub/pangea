package hub.nebula.pangea.command.vanilla.economy

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.command.vanilla.economy.declaration.CurrencyCommand.Companion.LOCALE_PREFIX
import hub.nebula.pangea.database.dao.User
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.pretty
import hub.nebula.pangea.utils.retrieveAllMembers
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class CurrencyRankCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
        val gateway = context.getOption("gateway")?.asString

        showRanking(context, gateway ?: "global-economy")
    }

    private suspend fun showRanking(context: PangeaInteractionContext, gateway: String) {
        when (gateway) {
            "global-economy" -> {
                val currencyRanking = newSuspendedTransaction {
                    User.all().sortedByDescending { it.currency }
                        .mapIndexed { index, user ->
                            val u = context.jda.retrieveUserById(user.userId).await()

                            "${index + 1}. ${u.name} - ${user.currency} ${if (user.currency == 1L) "Stardust" else "Stardusts"}"
                        }
                        .toList()
                }

                var currentPage = 0
                val pages = currencyRanking.chunked(10)

                context.reply {
                    embed {
                        title = context.locale["$LOCALE_PREFIX.rank.embedTitle", "Stardusts", "Global"]
                        description = pages[currentPage].joinToString("\n")
                        color = Constants.DEFAULT_COLOR
                        thumbnail = context.jda.selfUser.effectiveAvatarUrl
                        footer {
                            name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                        }
                    }

                    actionRow(
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.rank.previous"]) {
                                if (currentPage == 0) {
                                    it.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.rank.alreadyAtFirstPage"]
                                        )
                                    }
                                } else {
                                    currentPage--

                                    it.edit {
                                        embed {
                                            title = context.locale["$LOCALE_PREFIX.rank.embedTitle", "Stardusts", "Global"]
                                            description = pages[currentPage].joinToString("\n")
                                            color = Constants.DEFAULT_COLOR
                                            thumbnail = context.jda.selfUser.effectiveAvatarUrl
                                            footer {
                                                name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                                            }
                                        }
                                    }
                                }
                            },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.rank.next"]) {
                                if (currentPage == pages.size - 1) {
                                    it.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.rank.alreadyAtLastPage"]
                                        )
                                    }
                                } else {
                                    currentPage++

                                    it.edit {
                                        embed {
                                            title = context.locale["$LOCALE_PREFIX.rank.embedTitle", "Stardusts", "Global"]
                                            description = pages[currentPage].joinToString("\n")
                                            color = Constants.DEFAULT_COLOR
                                            thumbnail = context.jda.selfUser.effectiveAvatarUrl
                                            footer {
                                                name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }
            }
            "local-economy" -> {
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

                val members = newSuspendedTransaction {
                    guild.retrieveAllMembers()
                        .sortedByDescending { it.currency }
                        .mapIndexed {index, member ->
                            val u = context.jda.retrieveUserById(member.id).await()

                            "${index + 1}. ${u.name} - ${member.currency} ${if (member.currency == 1L) context.pangeaGuild.currencyName else context.pangeaGuild.currencyNamePlural}"
                        }
                }

                var currentPage = 0
                val pages = members.chunked(10)

                context.reply {
                    embed {
                        title = context.locale["$LOCALE_PREFIX.rank.embedTitle", context.pangeaGuild.currencyNamePlural, "Local"]
                        description = pages[currentPage].joinToString("\n")
                        color = Constants.DEFAULT_COLOR
                        thumbnail = context.jda.selfUser.effectiveAvatarUrl
                        footer {
                            name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                        }
                    }

                    actionRow(
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.rank.previous"]) {
                                if (currentPage == 0) {
                                    it.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.rank.alreadyAtFirstPage"]
                                        )
                                    }
                                } else {
                                    currentPage--

                                    it.edit {
                                        embed {
                                            title = context.locale["$LOCALE_PREFIX.rank.embedTitle", context.pangeaGuild.currencyNamePlural, "Local"]
                                            description = pages[currentPage].joinToString("\n")
                                            color = Constants.DEFAULT_COLOR
                                            thumbnail = context.jda.selfUser.effectiveAvatarUrl
                                            footer {
                                                name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                                            }
                                        }
                                    }
                                }
                            },
                        context.pangea.interactionManager
                            .createButtonForUser(context.user, ButtonStyle.PRIMARY, context.locale["$LOCALE_PREFIX.rank.next"]) {
                                if (currentPage == pages.size - 1) {
                                    it.reply(true) {
                                        pretty(
                                            context.locale["$LOCALE_PREFIX.rank.alreadyAtLastPage"]
                                        )
                                    }
                                } else {
                                    currentPage++

                                    it.edit {
                                        embed {
                                            title = context.locale["$LOCALE_PREFIX.rank.embedTitle", context.pangeaGuild.currencyNamePlural, "Local"]
                                            description = pages[currentPage].joinToString("\n")
                                            color = Constants.DEFAULT_COLOR
                                            thumbnail = context.jda.selfUser.effectiveAvatarUrl
                                            footer {
                                                name = context.locale["$LOCALE_PREFIX.rank.embedFooter", (currentPage + 1).toString(), pages.size.toString()]
                                            }
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }
    }
}