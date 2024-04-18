package hub.nebula.pangea.database.dao

import hub.nebula.pangea.database.table.Songs
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Song(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Song>(Songs)

    var title by Songs.title
    var uri by Songs.uri
    var platform by Songs.platform
    var playCount by Songs.play_count
}