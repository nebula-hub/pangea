package hub.nebula.pangea.database.dao

import hub.nebula.pangea.database.data.InnerMemberResponse
import hub.nebula.pangea.database.table.Guilds
import hub.nebula.pangea.utils.GeneralUtils.json
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Guild(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Guild>(Guilds) {
        fun getOrInsert(id: Long) = Guild.findById(id) ?: Guild.new(id) {}
    }

    val guildId = this.id.value
    var currencyName by Guilds.currency_name
    var currencyNamePlural by Guilds.currency_name_plural
    var currencyDailyMin by Guilds.currency_daily_min
    var currencyDailyMax by Guilds.currency_daily_max
    var dj by Guilds.dj
    var localEconomy by Guilds.local_economy
    var members by Guilds.members
    var prettyBans by Guilds.pretty_bans
    var prettyBansReasons by Guilds.pretty_bans_reasons
    var welcomer by Guilds.welcomer
    var welcomerChannel by Guilds.welcomer_channel
    var welcomerMessage by Guilds.welcomer_message
    var autorole by Guilds.autorole
    var autoroleRolesIds by Guilds.autorole_roles_ids
    var eventLogger by Guilds.event_logger
    var eventLoggerChannel by Guilds.event_logger_channel
    var eventLoggerLog by Guilds.event_logger_log
    var uno by Guilds.uno
}