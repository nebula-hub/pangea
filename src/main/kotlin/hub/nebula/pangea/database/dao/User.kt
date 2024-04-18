package hub.nebula.pangea.database.dao

import hub.nebula.pangea.database.table.Users
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class User(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<User>(Users) {
        fun findOrCreate(id: Long): User {
            return User.findById(id) ?: User.new(id) {}
        }
    }

    var userId = this.id.value
    var currency by Users.currency
    var lastDaily by Users.lastDaily
    var banned by Users.banned
    var banReason by Users.ban_reason
    var premium by Users.premium
    var premiumUntil by Users.premium_until
}