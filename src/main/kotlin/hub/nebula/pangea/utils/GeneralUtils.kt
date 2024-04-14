package hub.nebula.pangea.utils

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.hocon.Hocon
import kotlinx.serialization.hocon.decodeFromConfig
import kotlinx.serialization.json.Json
import java.io.File

object GeneralUtils {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    val hocon = Hocon {  }

    inline fun <reified T> Hocon.decodeFromConfig(configFile: File): T {
        return hocon.decodeFromConfig(
            ConfigFactory.parseFile(configFile).resolve()
        )
    }
}