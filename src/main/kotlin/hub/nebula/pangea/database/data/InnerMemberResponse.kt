package hub.nebula.pangea.database.data

import kotlinx.serialization.Serializable

@Serializable
data class InnerMemberResponse(
    val id: Long,
    var currency: Long,
    var lastDaily: Long? = null
)
