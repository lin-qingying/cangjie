package org.cangnova.cangjie.cfir.resolve.dfa.cfg

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * 对位 Kotlin `ControlFlowGraph` 的主图声明。
 *
 * 当前先补齐 low-level API 已经依赖的图标识与图种类主契约，
 * 后续节点/边/builder 继续落到同一主干包层。
 */
class ControlFlowGraph(
    val declaration: CfirDeclaration?,
    val name: String,
    val kind: Kind,
) {
    enum class Kind {
        File,
        Class,
        Constructor,
        Function,
        LocalFunction,
        AnonymousFunction,
        AnonymousFunctionCalledInPlace,
        PropertyInitializer,
        DefaultArgument,
    }
}

@RequiresOptIn
annotation class CfgInternals
