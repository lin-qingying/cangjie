package org.cangnova.cangjie.cfir.analysis.tests.golden

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(jsonText: String): CjcDiagnosticOutput {
        return json.decodeFromString<CjcDiagnosticOutput>(jsonText)
    }
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
