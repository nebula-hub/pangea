package hub.nebula.pangea.utils

import hub.nebula.pangea.database.dao.Guild
import hub.nebula.pangea.database.dao.Transaction
import hub.nebula.pangea.database.data.InnerMemberResponse
import hub.nebula.pangea.database.table.TransactionReason
import hub.nebula.pangea.utils.GeneralUtils.json
import net.dv8tion.jda.api.entities.User
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun Guild.registerMember(memberId: Long): InnerMemberResponse {
    return newSuspendedTransaction {
        val member = this@registerMember.members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }.firstOrNull { it.id == memberId }

        if (member == null) {
            this@registerMember.members = members.toMutableList().apply {
                add(json.encodeToString(InnerMemberResponse.serializer(), InnerMemberResponse(memberId, 0)))
            }
        }

        return@newSuspendedTransaction members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }.first { it.id == memberId }
    }
}

suspend fun User.pangeaUser(): hub.nebula.pangea.database.dao.Profile {
    return newSuspendedTransaction {
        hub.nebula.pangea.database.dao.Profile.findOrCreate(this@pangeaUser.idLong)
    }
}

suspend fun insertTransaction(sender: Long, receiver: Long = 0L, amount: Long, reason: TransactionReason, gateway: String) = newSuspendedTransaction {
    exposedLogger.info("Inserting transaction from $sender to $receiver with amount $amount and reason $reason // $gateway")

    Transaction.new {
        this.sender = sender
        this.receiver = receiver
        this.amount = amount
        this.reason = reason
        this.gateway = gateway
    }
}

suspend fun insertPayment(sender: Long, receiver: Long = 0L, amount: Long, reason: TransactionReason) = newSuspendedTransaction {
    exposedLogger.info("Inserting payment from $sender to $receiver with amount $amount and reason $reason // local gateway")

    Transaction.new {
        this.sender = sender
        this.receiver = receiver
        this.amount = amount
        this.reason = reason
        this.gateway = "global"
    }

    val senderProfile = hub.nebula.pangea.database.dao.Profile.findOrCreate(sender)
    val receiverProfile = hub.nebula.pangea.database.dao.Profile.findOrCreate(receiver)

    senderProfile.currency -= amount
    receiverProfile.currency += amount
}

suspend fun Guild.insertLocalPayment(sender: Long, receiver: Long = 0L, amount: Long, reason: TransactionReason) = newSuspendedTransaction {
    exposedLogger.info("Inserting payment from $sender to $receiver with amount $amount and reason $reason // global gateway")

    val senderMember = getMember(sender) ?: throw IllegalArgumentException("Sender not found")
    val receiverMember = getMember(receiver) ?: throw IllegalArgumentException("Receiver not found")

    Transaction.new {
        this.sender = sender
        this.receiver = receiver
        this.amount = amount
        this.reason = reason
        this.gateway = "local"
    }

    updateMember(sender, senderMember.currency - amount)
    updateMember(receiver, receiverMember.currency + amount)
}

suspend fun Guild.getMember(memberId: Long): InnerMemberResponse? {
    return newSuspendedTransaction {
        val members = this@getMember.members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }

        val foundMember = members.find { it.id == memberId }

        if (foundMember != null) {
            return@newSuspendedTransaction foundMember
        } else {
            return@newSuspendedTransaction null
        }
    }
}

suspend fun Guild.updateMember(memberId: Long, newCurrency: Long) {
    return newSuspendedTransaction {
        val members = this@updateMember.members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }

        val member = members.find { it.id == memberId }

        if (member == null)
            throw IllegalArgumentException("Member not found")

        member.currency = newCurrency

        val asMutable = members.toMutableList()

        asMutable.removeIf { it.id == member.id }

        asMutable.add(member)

        this@updateMember.members = asMutable.map { json.encodeToString(InnerMemberResponse.serializer(), it) }
    }
}

suspend fun Guild.updateMember(member: InnerMemberResponse) {
    return newSuspendedTransaction {
        val members = this@updateMember.members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }

        val asMutable = members.toMutableList()

        asMutable.removeIf { it.id == member.id }

        asMutable.add(member)

        this@updateMember.members = asMutable.map { json.encodeToString(InnerMemberResponse.serializer(), it) }
    }
}

suspend fun Guild.retrieveAllMembers(): List<InnerMemberResponse> {
    return newSuspendedTransaction {
        val members = this@retrieveAllMembers.members.map {
            json.decodeFromString(InnerMemberResponse.serializer(), it)
        }

        return@newSuspendedTransaction members
    }
}