package hub.nebula.pangea.command.vanilla.admin

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import java.util.concurrent.TimeUnit

class AdminBanCommandExecutor : PangeaSlashCommandExecutor() {
    override suspend fun execute(context: PangeaInteractionContext) {
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

        val target: User = context.option("member")!!
        val member = context.guild.getMemberById(target.idLong)

        if (member == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.invalidUser"]
                )
            }
            return
        }

        val reason: String? = context.option("reason")
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