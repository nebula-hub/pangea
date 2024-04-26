package hub.nebula.pangea.database.dao

import hub.nebula.pangea.database.table.Transactions
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class Transaction(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<Transaction>(Transactions)

    var sender by Transactions.sender
    var receiver by Transactions.receiver
    var amount by Transactions.amount
    var reason by Transactions.reason
    var gateway by Transactions.gateway
}