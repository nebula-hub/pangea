package hub.nebula.pangea.command.vanilla.admin

import dev.minn.jda.ktx.coroutines.await
import hub.nebula.pangea.command.PangeaInteractionContext
import hub.nebula.pangea.command.structure.PangeaSlashCommandExecutor
import hub.nebula.pangea.utils.Constants
import hub.nebula.pangea.utils.pretty
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.UserSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle

class AdminCheckBanCommandExecutor : PangeaSlashCommandExecutor() {
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

        val targetId: Long = context.option("member_id")!!

        val queryBan = try {
            context.guild.retrieveBan(
                UserSnowflake.fromId(targetId)
            ).await()
        } catch (e: Exception) {
            null
        }

        if (queryBan == null) {
            context.reply(true) {
                pretty(
                    context.locale["commands.command.admin.checkban.notBanned"]
                )
            }
        } else {
            context.reply {
                embed {
                    title = context.locale["commands.command.admin.checkban.userBanned"]
                    color = Constants.DEFAULT_COLOR
                    thumbnail = queryBan.user.effectiveAvatarUrl

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
                }

                actionRow(
                    context.pangea.interactionManager
                        .createButtonForUser(
                            context.user,
                            ButtonStyle.DANGER,
                            context.locale["commands.buttons.unban"],
                        ) {
                            context.guild.unban(
                                UserSnowflake.fromId(queryBan.user.idLong)
                            ).await()

                            it.reply {
                                pretty(
                                    it.locale["commands.command.admin.checkban.successfullyUnban"]
                                )
                            }
                        }
                )
            }
        }
    }
}