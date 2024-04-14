package hub.nebula.pangea.utils

import kotlinx.serialization.json.Json

object GeneralUtils {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
}