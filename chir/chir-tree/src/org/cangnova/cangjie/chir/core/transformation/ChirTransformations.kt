package org.cangnova.cangjie.chir.core.transformation

import org.cangnova.cangjie.chir.core.analysis.ChirReachabilityAnalysis
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.pipeline.ChirPassMetadata

data class ChirTransformationResult(
    val rewriteResult: ChirRewriteResult,
    val changed: Boolean,
)

interface ChirTransformation {
    val metadata: ChirPassMetadata

    fun apply(session: ChirRewriteSession): ChirTransformationResult
}

class RemoveUnreachableBlocksTransformation : ChirTransformation {
    override val metadata: ChirPassMetadata = ChirPassMetadata(
        name = "remove-unreachable-blocks",
        reads = setOf(org.cangnova.cangjie.chir.core.pipeline.ChirDataDomain.CONTROL_FLOW),
        writes = setOf(org.cangnova.cangjie.chir.core.pipeline.ChirDataDomain.CONTROL_FLOW),
    )

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

    private fun rewriteDeclaration(declaration: ChirDeclaration): ChirDeclaration {
        val function = declaration as? DefaultChirFunctionDeclaration ?: return declaration
        val reachable = ChirReachabilityAnalysis.reachableBlocks(function)
        return function.copy(blocks = function.blocks.filter { it.semanticId in reachable })
    }
}
