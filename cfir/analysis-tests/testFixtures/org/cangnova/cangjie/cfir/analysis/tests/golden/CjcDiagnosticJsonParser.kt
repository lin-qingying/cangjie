package org.cangnova.cangjie.cfir.analysis.tests.golden

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 解析 cjc --diagnostic-format json 输出的 JSON 诊断格式。
 *
 * cjc 输出示例：
 * ```json
 * {
 *   "Diags": [{
 *     "DiagKind": "sema_throw_expr_with_wrong_type",
 *     "Severity": "error",
 *     "Message": "the object thrown must derive from ...",
 *     "Location": { "File": "...", "Line": 6, "Column": 5 },
 *     "MainHint": { "Range": { "Begin": {"Line":6,"Column":5}, "End": {"Line":6,"Column":6} } }
 *   }],
 *   "Num": { "Errors": 1, "PrintedErrors": 1, "Warnnings": 0, "PrintedWarnnings": 0 }
 * }
 * ```
 */
object CjcDiagnosticJsonParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(jsonText: String): CjcDiagnosticOutput {
        return json.decodeFromString<CjcDiagnosticOutput>(jsonText)
    }

    fun parseLenient(jsonText: String): CjcDiagnosticOutput {
        val root = json.parseToJsonElement(jsonText).jsonObject
        val diags = root["Diags"]?.jsonArray.orEmpty().map { element ->
            val obj = element.jsonObject
            CjcDiag(
                diagKind = obj.string("DiagKind"),
                severity = obj.string("Severity"),
                message = obj.string("Message"),
                location = obj.objectOrNull("Location")?.toLocation(),
                mainHint = obj.objectOrNull("MainHint")?.toMainHint(),
            )
        }
        return CjcDiagnosticOutput(diags = diags)
    }

    private fun JsonObject.toLocation(): CjcLocation {
        return CjcLocation(
            file = string("File"),
            line = int("Line"),
            column = int("Column"),
        )
    }

    private fun JsonObject.toMainHint(): CjcMainHint {
        return CjcMainHint(
            range = objectOrNull("Range")?.toRange(),
        )
    }

    private fun JsonObject.toRange(): CjcRange {
        return CjcRange(
            begin = objectOrNull("Begin")?.toPosition(),
            end = objectOrNull("End")?.toPosition(),
        )
    }

    private fun JsonObject.toPosition(): CjcPosition {
        return CjcPosition(
            line = int("Line"),
            column = int("Column"),
        )
    }

    private fun JsonObject.string(name: String): String =
        this[name]?.jsonPrimitive?.content ?: ""

    private fun JsonObject.int(name: String): Int =
        runCatching { this[name]?.jsonPrimitive?.int ?: 0 }.getOrDefault(0)

    private fun JsonObject.objectOrNull(name: String): JsonObject? =
        (this[name] as? JsonObject)
}

@Serializable
data class CjcDiagnosticOutput(
    @SerialName("Diags") val diags: List<CjcDiag> = emptyList(),
    @SerialName("Num") val num: CjcDiagNum? = null,
)

@Serializable
data class CjcDiag(
    @SerialName("DiagKind") val diagKind: String = "",
    @SerialName("Severity") val severity: String = "",
    @SerialName("Message") val message: String = "",
    @SerialName("Location") val location: CjcLocation? = null,
    @SerialName("MainHint") val mainHint: CjcMainHint? = null,
)

@Serializable
data class CjcLocation(
    @SerialName("File") val file: String = "",
    @SerialName("Line") val line: Int = 0,
    @SerialName("Column") val column: Int = 0,
)

@Serializable
data class CjcMainHint(
    @SerialName("Range") val range: CjcRange? = null,
)

@Serializable
data class CjcRange(
    @SerialName("Begin") val begin: CjcPosition? = null,
    @SerialName("End") val end: CjcPosition? = null,
)

@Serializable
data class CjcPosition(
    @SerialName("Line") val line: Int = 0,
    @SerialName("Column") val column: Int = 0,
)

@Serializable
data class CjcDiagNum(
    @SerialName("Errors") val errors: Int = 0,
    @SerialName("PrintedErrors") val printedErrors: Int = 0,
    @SerialName("Warnnings") val warnings: Int = 0,
    @SerialName("PrintedWarnnings") val printedWarnings: Int = 0,
)
