package hub.nebula.pangea

import hub.nebula.pangea.configuration.GeneralConfig
import hub.nebula.pangea.utils.GeneralUtils.hocon
import hub.nebula.pangea.utils.GeneralUtils.decodeFromConfig
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

object PangeaLauncher {
    private val logger = mu.KotlinLogging.logger(this::class.jvmName)

    @JvmStatic
    fun main(args: Array<String>) {
        val configurationFile = File("./pangea.conf")

        if (!configurationFile.exists()) {
            copyFromJar("pangea.conf")
            exitProcess(0)
        }

        val config: GeneralConfig = hocon.decodeFromConfig(configurationFile)

        runBlocking {
            PangeaInstance(config.pangea).start()
        }
    }

    private fun copyFromJar(fileName: String) {
        val inputStream = javaClass.getResourceAsStream("/configuration/$fileName")
        val outputStream = FileOutputStream(fileName)
        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output) ?: throw IllegalStateException("Resource not found: $fileName")
            }
        }

        logger.info { "Copied $fileName from JAR." }
        logger.info { "Please fill in the configuration file and restart the bot."}
    }
}