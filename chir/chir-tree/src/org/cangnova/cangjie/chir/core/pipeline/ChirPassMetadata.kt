package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR pass 读写的数据域。
 */
enum class ChirDataDomain {
    DECLARATION,
    CONTROL_FLOW,
    EXPRESSION,
    TYPE,
    VALUE,
    SYMBOL,
    METADATA,
}

/**
 * CHIR pass 可能产生的外部副作用。
 */
enum class ChirPassSideEffect {
    DIAGNOSTICS,
    SERIALIZATION,
    LOGGING,
    METRICS,
}

/**
 * CHIR pipeline pass 元数据。
 */
data class ChirPassMetadata(
    /**
     * pass 唯一名称。
     */
    val name: String,

    /**
     * 当前 pass 依赖的其他 pass 名称集合。
     */
    val dependsOn: Set<String> = emptySet(),

    /**
     * 当前 pass 读取的数据域集合。
     */
    val reads: Set<ChirDataDomain> = emptySet(),

    /**
     * 当前 pass 写入的数据域集合。
     */
    val writes: Set<ChirDataDomain> = emptySet(),

    /**
     * 当前 pass 显式失效的数据域集合。
     */
    val invalidates: Set<ChirDataDomain> = emptySet(),

    /**
     * 当前 pass 的副作用集合。
     */
    val sideEffects: Set<ChirPassSideEffect> = emptySet(),

    /**
     * 当前 pass 的可读说明。
     */
    val description: String? = null,
) {
    init {
        require(name.isNotBlank()) { "pass name must not be blank" }
    }

    /**
     * 实际需要失效的分析域，包含显式失效域和写入域。
     */
    val effectiveInvalidates: Set<ChirDataDomain>
        get() = invalidates + writes
}

/**
 * CHIR pass 执行输出。
 */
data class ChirPassExecutionOutput(
    /**
     * 当前 pass 触达的节点集合。
     */
    val touchedNodes: Set<ChirSemanticId> = emptySet(),

    /**
     * 当前 pass 的执行摘要。
     */
    val summary: String? = null,
)

/**
 * CHIR pipeline pass 接口。
 */
interface ChirPipelinePass {
    /**
     * 当前 pass 元数据。
     */
    val metadata: ChirPassMetadata

    /**
     * 执行当前 pass。
     */
    fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput
}
