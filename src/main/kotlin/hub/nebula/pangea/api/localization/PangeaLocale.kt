package hub.nebula.pangea.api.localization

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class PangeaLocale(val locale: String) {
    private val mapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    companion object {
        const val PATH = "./localization"
    }

    operator fun get(key: String, vararg placeholder: String): String {
        val file = File("$PATH/${locale}/general.yml")
        val tree = mapper.readTree(file)

        val keyList = key.split(".")
        var current = tree

        for (k in keyList) {
            current = current.get(k)
            if (current == null) {
                return "!!{${key}}!!"
            }
        }

        var result = current.asText()

        placeholder.forEachIndexed { index, s ->
            result = result.replace("{${index}}", s)
        }

        return result
    }
}