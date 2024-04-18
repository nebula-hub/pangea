package hub.nebula.pangea.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Users : LongIdTable() {
    var currency = long("currency").default(0)
    var lastDaily = long("last_daily").nullable().default(null)
    var banned = bool("banned").default(false)
    var ban_reason = text("ban_reason").nullable().default(null)
    var premium = bool("premium").default(false)
    var premium_until = long("premium_until").nullable().default(null)
}