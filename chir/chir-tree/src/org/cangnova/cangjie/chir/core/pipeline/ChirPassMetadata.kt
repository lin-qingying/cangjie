package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

enum class ChirDataDomain {
    DECLARATION,
    CONTROL_FLOW,
    EXPRESSION,
    TYPE,
    VALUE,
    SYMBOL,
    METADATA,
}

enum class ChirPassSideEffect {
    DIAGNOSTICS,
    SERIALIZATION,
    LOGGING,
    METRICS,
}

data class ChirPassMetadata(
    val name: String,
    val dependsOn: Set<String> = emptySet(),
    val reads: Set<ChirDataDomain> = emptySet(),
    val writes: Set<ChirDataDomain> = emptySet(),
    val invalidates: Set<ChirDataDomain> = emptySet(),
    val sideEffects: Set<ChirPassSideEffect> = emptySet(),
    val description: String? = null,
) {
    init {
        require(name.isNotBlank()) { "pass name must not be blank" }
    }

    val effectiveInvalidates: Set<ChirDataDomain>
        get() = invalidates + writes
}

data class ChirPassExecutionOutput(
    val touchedNodes: Set<ChirSemanticId> = emptySet(),
    val summary: String? = null,
)

interface ChirPipelinePass {
    val metadata: ChirPassMetadata

    fun execute(cache: ChirAnalysisCache): ChirPassExecutionOutput
}
