package hub.nebula.pangea.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Songs : LongIdTable() {
    var title = text("title")
    var uri = text("uri")
    var platform = text("platform")
    var play_count = long("play_count")
}