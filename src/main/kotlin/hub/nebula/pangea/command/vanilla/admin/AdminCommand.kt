package hub.nebula.pangea.command.vanilla.admin

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaCommandContext
import hub.nebula.pangea.command.PangeaSlashCommandDeclarationWrapper
import hub.nebula.pangea.command.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.util.concurrent.TimeUnit

class AdminCommand : PangeaSlashCommandDeclarationWrapper {
    override fun create() = command(
        "admin",
        "admin.description"
    ) {
        addPermission(
            Permission.KICK_MEMBERS,
            Permission.BAN_MEMBERS,
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_SERVER,
            Permission.MESSAGE_MANAGE
        )

        subCommand(
            "ban",
            "admin.ban.description"
        ) {
            options.apply {
                add(
                    OptionData(
                        OptionType.USER,
                        "member",
                        "admin.ban.member.description",
                        true
                    )
                )

                add(
                    OptionData(
                        OptionType.STRING,
                        "reason",
                        "admin.ban.reason.description",
                        false
                    )
                )
            }

            executor = AdminBanCommandExecutor()
        }

        subCommand(
            "checkban",
            "admin.checkban.description"
        ) {
            options.apply {
                add(
                    OptionData(
                        OptionType.STRING,
                        "member_id",
                        "admin.checkban.member_id.description",
                        true
                    )
                )
            }

            executor = AdminCheckBanCommandExecutor()
        }
    }

    inner class AdminBanCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            if (context.guild == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.guildOnly"]
                    )
                }
                return
            }

            if (context.member?.permissions?.any { it == Permission.BAN_MEMBERS } == false) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.noPermission", Permission.BAN_MEMBERS.toString()]
                    )
                }
                return
            }

            val member = context.getOption("member")!!.asMember

            if (member == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.invalidUser", context.getOption("member")?.asString ?: "null"]
                    )
                }
                return
            }

            val reason = context.getOption("reason")?.asString
            val prettyReason = if (reason == null) {
                context.locale["commands.command.admin.ban.noBanReason", context.user.name]
            } else {
                context.locale["commands.command.admin.ban.banReason", context.user.name, reason]
            }

            context.defer(false)

            try {
                member.ban(
                    0,
                    TimeUnit.DAYS
                ).reason(
                    prettyReason
                ).await()

                context.reply {
                    pretty(
                        context.locale["commands.command.admin.successfullyPunished"]
                    )
                }
            } catch (e: Exception) {
                when (e) {
                    is InsufficientPermissionException -> {
                        context.reply(true) {
                            pretty(
                                context.locale["commands.noPermission", Permission.BAN_MEMBERS.toString()]
                            )
                        }
                        return
                    }

                    is HierarchyException -> {
                        context.reply(true) {
                            pretty(
                                context.locale["commands.hierarchyError"]
                            )
                        }
                        return
                    }
                }
            }
        }
    }

    inner class AdminCheckBanCommandExecutor : PangeaSlashCommandExecutor() {
        override suspend fun execute(context: PangeaCommandContext) {
            if (context.guild == null) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.guildOnly"]
                    )
                }
                return
            }

            if (context.member?.permissions?.any { it == Permission.BAN_MEMBERS } == false) {
                context.reply(true) {
                    pretty(
                        context.locale["commands.noPermission", Permission.BAN_MEMBERS.toString()]
                    )
                }
                return
            }

            val queryBan = context.guild.retrieveBan(
                UserSnowflake.fromId(context.getOption("member_id")!!.asLong)
            ).await()

            if (queryBan == null) {
                context.reply {
                    pretty(
                        context.locale["commands.command.admin.checkban.notBanned"]
                    )
                }
            } else {
                context.sendEmbed {
                    title = context.locale["commands.command.admin.checkban.userBanned"]

                    field {
                        name = context.locale["commands.command.admin.checkban.name"]
                        value = "`${queryBan.user.name}`"
                    }

                    field {
                        name = "ID"
                        value = "`${queryBan.user.id}`"
                    }

                    field {
                        name = context.locale["commands.command.admin.checkban.reason"]
                        value = "`${queryBan.reason ?: context.locale["commands.command.admin.checkban.noReason"]}`"
                    }

                    color = Constants.DEFAULT_COLOR
                }
            }
        }
    }
}