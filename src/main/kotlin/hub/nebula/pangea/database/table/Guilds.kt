package hub.nebula.pangea.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Guilds : LongIdTable() {
    // Modules
    var dj = bool("dj").default(false) // dj function
    var local_economy = bool("local_economy").default(false) // local economy function
    var members = array<String>("members").default(emptyList()) // only for local economy
    var currency_name = text("currency_name").default("Nebula") // only for local economy
    var currency_name_plural = text("currency_name_plural").default("Nebulae") // only for local economy
    var currency_daily_min = long("currency_daily_min").default(100) // only for local economy
    var currency_daily_max = long("currency_daily_max").default(1000) // only for local economy
    var pretty_bans = bool("pretty_bans").default(false) // pretty bans function
    var pretty_bans_reasons = array<String>("pretty_bans_reasons").default(emptyList()) // only for pretty bans function
    var welcomer = bool("welcomer").default(false) // welcomer function
    var welcomer_channel = long("welcomer_channel").nullable().default(null) // only for welcomer function
    var welcomer_message = text("welcomer_message").nullable().default(null) // only for welcomer function
    var autorole = bool("autorole").default(false) // autorole function
    var autorole_roles_ids = array<Long>("autorole_roles_ids").default(emptyList()) // only for autorole function
    var event_logger = bool("event_logger").default(false) // event logger function
    var event_logger_channel = long("event_logger_channel").nullable().default(null) // only for event logger function
    var event_logger_log = array<String>("event_logger_log").default(emptyList()) // only for event logger function
    var uno = bool("uno").default(false) // uno function
}