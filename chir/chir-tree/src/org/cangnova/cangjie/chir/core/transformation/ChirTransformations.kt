package org.cangnova.cangjie.chir.core.transformation

import org.cangnova.cangjie.chir.core.analysis.ChirReachabilityAnalysis
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.pipeline.ChirPassMetadata

/**
 * CHIR transformation 执行结果。
 */
data class ChirTransformationResult(
    /**
     * rewrite session 返回的结果。
     */
    val rewriteResult: ChirRewriteResult,

    /**
     * transformation 是否实际改变了包内容。
     */
    val changed: Boolean,
)

/**
 * CHIR transformation 接口。
 */
interface ChirTransformation {
    /**
     * transformation 对应的 pass 元数据。
     */
    val metadata: ChirPassMetadata

    /**
     * 在 [session] 上应用 transformation。
     */
    fun apply(session: ChirRewriteSession): ChirTransformationResult
}

/**
 * 删除函数中不可达基本块的 transformation。
 */
class RemoveUnreachableBlocksTransformation : ChirTransformation {
    /**
     * 删除不可达基本块 pass 的元数据。
     */
    override val metadata: ChirPassMetadata = ChirPassMetadata(
        name = "remove-unreachable-blocks",
        reads = setOf(org.cangnova.cangjie.chir.core.pipeline.ChirDataDomain.CONTROL_FLOW),
        writes = setOf(org.cangnova.cangjie.chir.core.pipeline.ChirDataDomain.CONTROL_FLOW),
    )

    /**
     * 对 session 当前包执行不可达基本块删除。
     */
    override fun apply(session: ChirRewriteSession): ChirTransformationResult {
        val before = session.snapshot()
        val result = session.apply { pkg ->
            pkg.copy(
                modules = pkg.modules.map { module ->
                    module.copy(
                        declarations = module.declarations.map { declaration ->
                            rewriteDeclaration(declaration)
                        },
                    )
                },
            )
        }
        val changed = result is ChirRewriteResult.Success && result.rewritten != before
        return ChirTransformationResult(result, changed)
    }

    /**
     * 重写单个声明中的函数基本块列表。
     */
    private fun rewriteDeclaration(declaration: ChirDeclaration): ChirDeclaration {
        val function = declaration as? DefaultChirFunctionDeclaration ?: return declaration
        val reachable = ChirReachabilityAnalysis.reachableBlocks(function)
        return function.copy(blocks = function.blocks.filter { it.semanticId in reachable })
    }
}
