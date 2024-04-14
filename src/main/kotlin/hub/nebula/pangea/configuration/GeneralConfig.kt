package hub.nebula.pangea.configuration

import kotlinx.serialization.Serializable

@Serializable
data class GeneralConfig(
    val pangea: PangeaConfig,
    val galaxy: GalaxyConfig
) {
    @Serializable
    data class PangeaConfig(
        val token: String,
        val applicationId: Long,
        val clientSecret: String,
        val activities: List<PangeaActivity>
    ) {
        @Serializable
        data class PangeaActivity(
            val type: Int,
            val name: String
        )
    }

    @Serializable
    data class GalaxyConfig(
        val host: String,
        val port: Int,
        val password: String,
        val user: String,
        val database: String
    )
}
