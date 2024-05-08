package hub.nebula.pangea.api.modules

import hub.nebula.pangea.database.dao.Guild
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.reflect.jvm.jvmName

class AutoRoleModule {
    private val logger = KotlinLogging.logger(this::class.jvmName)

    suspend fun run(event: GuildMemberJoinEvent) {
        val guild = event.guild

        val rGuild = newSuspendedTransaction {
            Guild.getOrInsert(guild.idLong)
        }

        if (rGuild.autorole) {
            logger.info { "AutoRole is enabled, checking for roles and adding them to the new member." }

            if (rGuild.autoroleRolesIds.isEmpty()) {
                logger.info { "No roles found in the autorole list." }
                return
            }

            // mapping all the non-null id roles into jda roles
            val roles = rGuild.autoroleRolesIds.mapNotNull { guild.getRoleById(it) }

            if (roles.isEmpty()) {
                logger.info { "No roles found in the autorole list." }
                return
            }

            val hasPermission = guild.selfMember.hasPermission(Permission.MANAGE_ROLES)

            if (hasPermission) {
                roles.forEach {
                    guild.addRoleToMember(event.member, it).queue()
                }
            } else {
                logger.info { "The bot does not have the MANAGE_ROLES permission." }
            }
        }
    }
}