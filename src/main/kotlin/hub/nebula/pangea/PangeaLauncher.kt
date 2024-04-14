package hub.nebula.pangea

import hub.nebula.pangea.configuration.GeneralConfig
import hub.nebula.pangea.utils.GeneralUtils
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

object PangeaLauncher {
    private val logger = mu.KotlinLogging.logger(this::class.jvmName)

    @JvmStatic
    fun main(args: Array<String>) {
        copyFromJar("pangea.conf")
        // TODO: copyFromJar("interactions.conf")

        val config: GeneralConfig = loadConfig("pangea.conf")

        runBlocking {
            PangeaInstance(config.pangea).start()
        }
    }

    private fun loadConfig(fileName: String): GeneralConfig {
        val file = File("./$fileName")

        return GeneralUtils.json.decodeFromString(file.readBytes().toString(Charsets.UTF_8))
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
        exitProcess(0)
    }
}