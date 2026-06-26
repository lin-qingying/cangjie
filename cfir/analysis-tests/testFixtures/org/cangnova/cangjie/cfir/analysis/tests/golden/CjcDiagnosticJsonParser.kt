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

    /**
     * cjc JSON 输出解析器。
     *
     * 解析器允许未知字段和宽松格式，避免官方编译器增加字段时破坏测试工具。
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 使用 kotlinx serialization 解析标准 cjc JSON 诊断输出。
     */
    fun parse(jsonText: String): CjcDiagnosticOutput {
        return json.decodeFromString<CjcDiagnosticOutput>(jsonText)
    }

    /**
     * 宽松解析 cjc JSON 诊断输出。
     *
     * 当官方输出字段类型或 `Num` 结构发生兼容性漂移时，该入口只抽取诊断比对所需字段。
     */
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

    /**
     * 将 JSON 对象转换为 cjc 诊断位置。
     */
    private fun JsonObject.toLocation(): CjcLocation {
        return CjcLocation(
            file = string("File"),
            line = int("Line"),
            column = int("Column"),
        )
    }

    /**
     * 将 JSON 对象转换为主提示区间容器。
     */
    private fun JsonObject.toMainHint(): CjcMainHint {
        return CjcMainHint(
            range = objectOrNull("Range")?.toRange(),
        )
    }

    /**
     * 将 JSON 对象转换为诊断范围。
     */
    private fun JsonObject.toRange(): CjcRange {
        return CjcRange(
            begin = objectOrNull("Begin")?.toPosition(),
            end = objectOrNull("End")?.toPosition(),
        )
    }

    /**
     * 将 JSON 对象转换为一基行列位置。
     */
    private fun JsonObject.toPosition(): CjcPosition {
        return CjcPosition(
            line = int("Line"),
            column = int("Column"),
        )
    }

    /**
     * 读取字符串字段，缺失时返回空串以便宽松解析继续进行。
     */
    private fun JsonObject.string(name: String): String =
        this[name]?.jsonPrimitive?.content ?: ""

    /**
     * 读取整数字段，缺失或格式错误时返回 0。
     */
    private fun JsonObject.int(name: String): Int =
        runCatching { this[name]?.jsonPrimitive?.int ?: 0 }.getOrDefault(0)

    /**
     * 读取对象字段，字段不存在或非对象时返回 null。
     */
    private fun JsonObject.objectOrNull(name: String): JsonObject? =
        (this[name] as? JsonObject)
}

/**
 * cjc JSON 诊断输出的顶层结构。
 *
 * @property diags 诊断条目列表。
 * @property num 官方输出中的诊断计数信息。
 */
@Serializable
data class CjcDiagnosticOutput(
    @SerialName("Diags") val diags: List<CjcDiag> = emptyList(),
    @SerialName("Num") val num: CjcDiagNum? = null,
)

/**
 * cjc 输出中的单条诊断。
 *
 * @property diagKind 官方诊断定义名。
 * @property severity 诊断严重级别。
 * @property message 官方渲染后的诊断消息。
 * @property location 诊断主位置。
 * @property mainHint 官方主提示范围。
 */
@Serializable
data class CjcDiag(
    @SerialName("DiagKind") val diagKind: String = "",
    @SerialName("Severity") val severity: String = "",
    @SerialName("Message") val message: String = "",
    @SerialName("Location") val location: CjcLocation? = null,
    @SerialName("MainHint") val mainHint: CjcMainHint? = null,
)

/**
 * cjc 诊断的文件位置。
 *
 * @property file 源文件路径。
 * @property line 一基行号。
 * @property column 一基列号。
 */
@Serializable
data class CjcLocation(
    @SerialName("File") val file: String = "",
    @SerialName("Line") val line: Int = 0,
    @SerialName("Column") val column: Int = 0,
)

/**
 * cjc 主提示信息。
 *
 * @property range 诊断主提示覆盖范围。
 */
@Serializable
data class CjcMainHint(
    @SerialName("Range") val range: CjcRange? = null,
)

/**
 * cjc 诊断范围。
 *
 * @property begin 起始位置。
 * @property end 结束位置。
 */
@Serializable
data class CjcRange(
    @SerialName("Begin") val begin: CjcPosition? = null,
    @SerialName("End") val end: CjcPosition? = null,
)

/**
 * cjc 一基行列位置。
 *
 * @property line 一基行号。
 * @property column 一基列号。
 */
@Serializable
data class CjcPosition(
    @SerialName("Line") val line: Int = 0,
    @SerialName("Column") val column: Int = 0,
)

/**
 * cjc 诊断计数信息。
 *
 * @property errors 错误总数。
 * @property printedErrors 已打印错误数。
 * @property warnings 警告总数；字段名保持官方输出中的拼写。
 * @property printedWarnings 已打印警告数；字段名保持官方输出中的拼写。
 */
@Serializable
data class CjcDiagNum(
    @SerialName("Errors") val errors: Int = 0,
    @SerialName("PrintedErrors") val printedErrors: Int = 0,
    @SerialName("Warnnings") val warnings: Int = 0,
    @SerialName("PrintedWarnnings") val printedWarnings: Int = 0,
)
