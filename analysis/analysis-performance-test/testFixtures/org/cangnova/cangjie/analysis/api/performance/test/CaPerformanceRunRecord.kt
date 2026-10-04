package org.cangnova.cangjie.analysis.api.performance.test

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * 一个测试用例在一次运行中留下的测量记录。
 *
 * 记录的是**增量**而不是绝对值：SDK 的指标按 JVM 累积，绝对值会把同一次运行里其他用例的
 * 数据算进来。墙钟同理，只覆盖用例执行本身。
 *
 * @property testClass 用例所属测试类
 * @property testMethod 用例方法名
 * @property wallNanos 用例执行的墙钟耗时（纳秒）
 * @property delta 该用例触发的指标增量
 */
data class CaPerformanceCaseRecord(
    val testClass: String,
    val testMethod: String,
    val wallNanos: Long,
    val delta: CaPerformanceDelta,
) {
    /**
     * 用例的稳定标识：跨运行对齐靠它，不含运行期变化的部分。
     */
    val caseId: String
        get() = "$testClass#$testMethod"

    /**
     * 编码成 JSON 对象。
     */
    fun toJson(): JsonObject = buildJsonObject {
        put("testClass", testClass)
        put("testMethod", testMethod)
        put("wallNanos", wallNanos)
        put("counters", countersJson(delta.counters))
        put("durationNanosSum", sumsJson(delta.durationNanosSum))
        put("histogramCounts", countsJson(delta.histogramCounts))
    }

    /**
     * 从 JSON 对象解码；字段缺失或类型不符时返回 null 而不是抛错——
     * 历史文件是上一轮甚至别的分支写下的，一条坏行不该让整次运行失败。
     */
    companion object {
        fun fromJson(json: JsonObject): CaPerformanceCaseRecord? {
            val testClass = json["testClass"]?.jsonPrimitive?.contentOrNull() ?: return null
            val testMethod = json["testMethod"]?.jsonPrimitive?.contentOrNull() ?: return null
            val wallNanos = json["wallNanos"]?.jsonPrimitive?.longOrNull ?: return null
            return CaPerformanceCaseRecord(
                testClass = testClass,
                testMethod = testMethod,
                wallNanos = wallNanos,
                delta = CaPerformanceDelta(
                    counters = json.longMap("counters"),
                    durationNanosSum = json.doubleMap("durationNanosSum"),
                    histogramCounts = json.longMap("histogramCounts"),
                ),
            )
        }

        private fun countersJson(values: Map<String, Long>): JsonObject =
            buildJsonObject { values.forEach { (name, value) -> put(name, value) } }

        private fun countsJson(values: Map<String, Long>): JsonObject =
            buildJsonObject { values.forEach { (name, value) -> put(name, value) } }

        private fun sumsJson(values: Map<String, Double>): JsonObject =
            buildJsonObject { values.forEach { (name, value) -> put(name, value) } }
    }
}

/**
 * 一次完整运行的测量记录，作为基线对比的输入单元。
 *
 * @property runId 本次运行的标识（时间戳派生，同一模块内唯一）
 * @property timestampMillis 本次运行的起始时间
 * @property gitCommit 测试进程能读到的 git commit；读不到时为 null
 * @property cases 本次运行的全部用例记录
 */
data class CaPerformanceRunRecord(
    val runId: String,
    val timestampMillis: Long,
    val gitCommit: String?,
    val cases: List<CaPerformanceCaseRecord>,
) {
    /**
     * 按 [caseId] 索引的用例记录，便于跨运行对齐。
     */
    val casesById: Map<String, CaPerformanceCaseRecord> by lazy { cases.associateBy { it.caseId } }

    /**
     * 编码成 JSONL 的一行。
     */
    fun toJsonLine(): String = Json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("runId", runId)
            put("timestampMillis", timestampMillis)
            put("gitCommit", gitCommit ?: "")
            put("cases", buildJsonArray { cases.forEach { add(it.toJson()) } })
        },
    )

    companion object {
        /**
         * 解析一行 JSONL；行格式不对或字段缺失时返回 null。
         */
        fun parse(line: String): CaPerformanceRunRecord? {
            val json = runCatching { Json.parseToJsonElement(line).jsonObject }.getOrNull() ?: return null
            val runId = json["runId"]?.jsonPrimitive?.contentOrNull() ?: return null
            val timestamp = json["timestampMillis"]?.jsonPrimitive?.longOrNull ?: return null
            val commit = json["gitCommit"]?.jsonPrimitive?.contentOrNull()?.takeIf { it.isNotBlank() }
            val cases = (json["cases"] as? JsonArray)?.mapNotNull { element ->
                (element as? JsonObject)?.let { CaPerformanceCaseRecord.fromJson(it) }
            } ?: return null
            return CaPerformanceRunRecord(runId, timestamp, commit, cases)
        }
    }
}

/**
 * 读取字符串型 JSON 字段；非字符串或缺失时返回 null。
 */
private fun kotlinx.serialization.json.JsonElement.contentOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content

/**
 * 读取对象里的整数键值对。
 */
private fun JsonObject.longMap(key: String): Map<String, Long> {
    val obj = this[key] as? JsonObject ?: return emptyMap()
    return obj.mapNotNull { (name, element) ->
        (element as? JsonPrimitive)?.longOrNull?.let { name to it }
    }.toMap()
}

/**
 * 读取对象里的浮点键值对。
 */
private fun JsonObject.doubleMap(key: String): Map<String, Double> {
    val obj = this[key] as? JsonObject ?: return emptyMap()
    return obj.mapNotNull { (name, element) ->
        (element as? JsonPrimitive)?.doubleOrNull?.let { name to it }
    }.toMap()
}