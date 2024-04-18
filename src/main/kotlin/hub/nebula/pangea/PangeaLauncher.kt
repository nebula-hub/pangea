package hub.nebula.pangea

import hub.nebula.pangea.configuration.GeneralConfig
import hub.nebula.pangea.database.DatabaseService
import hub.nebula.pangea.utils.GeneralUtils.hocon
import hub.nebula.pangea.utils.GeneralUtils.decodeFromConfig
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.File
import java.io.FileOutputStream
import kotlin.reflect.jvm.jvmName
import kotlin.system.exitProcess

object PangeaLauncher {
    private val logger = KotlinLogging.logger(this::class.jvmName)

    @JvmStatic
    fun main(args: Array<String>) {
        val configurationFile = File("./pangea.conf")

        if (!configurationFile.exists()) {
            copyFromJar("./configuration/pangea.conf", "pangea.conf")
            exitProcess(0)
        }

        handleLocalizationFromResources()

        val config: GeneralConfig = hocon.decodeFromConfig(configurationFile)

        DatabaseService.connect(config.galaxy)

        runBlocking {
            PangeaInstance(config.pangea).start()
        }
    }

    private fun handleLocalizationFromResources() {
        val localeDir = File("./localization")

        if (localeDir.exists()) {
            val ptBrDir = File("./localization/pt-br")
            val enUsDir = File("./localization/en-us")

            if (ptBrDir.exists() && enUsDir.exists()) {
                val ptBrFiles = ptBrDir.listFiles()

                if (ptBrFiles == null) {
                    logger.info { "No files found in pt-br directory." }
                    return
                }

                val enUsFiles = enUsDir.listFiles()

                if (enUsFiles == null) {
                    logger.info { "No files found in en-us directory." }
                    return
                }

                val internalPtBrFile = File("./resources/localization/pt-br/general.yml")
                val internalEnUsFile = File("./resources/localization/en-us/general.yml")

                createNewFileToPath("./localization/pt-br", "general.yml", internalPtBrFile.readBytes())
                createNewFileToPath("./localization/en-us", "general.yml", internalEnUsFile.readBytes())
            }
            exitProcess(0)
        } else {
            logger.info { "Localization directory not found, creating now the language directories" }
            localeDir.mkdir()
            File("./localization/pt-br").mkdir()
            File("./localization/en-us").mkdir()

            createNewFileToPath("./localization/pt-br", "general.yml", getResourceAsByteArray("/localization/pt-br/general.yml"))
            createNewFileToPath("./localization/en-us", "general.yml", getResourceAsByteArray("/localization/en-us/general.yml"))

            exitProcess(0)
        }
    }

    private fun copyFromJar(pathToFile: String, absoluteName: String) {
        val inputStream = javaClass.getResourceAsStream(pathToFile)
        val outputStream = FileOutputStream(absoluteName)
        inputStream.use { input ->
            outputStream.use { output ->
                input?.copyTo(output) ?: throw IllegalStateException("Resource not found: $absoluteName")
            }
        }

        logger.info { "Copied $absoluteName from JAR." }
        logger.info { "Please fill in the configuration file and restart the bot."}
    }

    private fun createNewFileToPath(path: String, fileName: String, bytes: ByteArray): Boolean {
        val newPathFile = File("$path/$fileName")

        if (!newPathFile.exists()) {
            newPathFile.createNewFile()
            newPathFile.writeBytes(bytes)
            return true
        } else {
            logger.info { "File already exists, overwriting..." }
            newPathFile.writeBytes(bytes)
            return false
        }
    }

    private fun getResourceAsByteArray(path: String): ByteArray {
        return javaClass.getResourceAsStream(path)?.readBytes()
            ?: throw IllegalStateException("Resource not found: $path")
    }
}