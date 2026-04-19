package org.cangnova.cangjie.cfir.declarations

/**
 * 对齐 Kotlin FIR 的 property body resolve state。
 *
 * 这些状态描述的不是新的 resolve phase，而是 property 在
 * [CfirResolvePhase.IMPLICIT_TYPES] 与 [CfirResolvePhase.BODY_RESOLVE] 之间
 * 已经完成到哪一段 body 语义。
 */
enum class CfirPropertyBodyResolveState {
    NOTHING_RESOLVED,
    INITIALIZER_RESOLVED,
    INITIALIZER_AND_GETTER_RESOLVED,
    ALL_BODIES_RESOLVED,
}
