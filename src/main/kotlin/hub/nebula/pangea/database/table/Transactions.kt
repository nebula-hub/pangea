package hub.nebula.pangea.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Transactions : LongIdTable() {
    var sender = long("sender").index()
    var receiver = long("receiver").nullable()
    var amount = long("amount")
    var reason = enumerationByName("reason", 10, TransactionReason::class)
    var gateway = text("gateway").default("global").index()
}

enum class TransactionReason {
    PAYMENT,
    BET,
    DAILY_REWARD,
    SLOTS
}