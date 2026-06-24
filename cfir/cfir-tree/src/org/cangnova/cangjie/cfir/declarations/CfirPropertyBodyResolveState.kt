package org.cangnova.cangjie.cfir.declarations

/**
 * 对齐 Kotlin FIR 的 property body resolve state。
 *
 * 这些状态描述的不是新的 resolve phase，而是 property 在
 * [CfirResolvePhase.IMPLICIT_TYPES] 与 [CfirResolvePhase.BODY_RESOLVE] 之间
 * 已经完成到哪一段 body 语义。
 */
enum class CfirPropertyBodyResolveState {
    /**
     * property initializer、getter 和 setter body 均未解析。
     */
    NOTHING_RESOLVED,

    /**
     * initializer 已解析，访问器 body 尚未全部解析。
     */
    INITIALIZER_RESOLVED,

    /**
     * initializer 与 getter 已解析，setter body 尚未解析。
     */
    INITIALIZER_AND_GETTER_RESOLVED,

    /**
     * initializer、getter 和 setter body 均已解析完成。
     */
    ALL_BODIES_RESOLVED,
}
