package org.cangnova.cangjie.chir.core.transformation

import org.cangnova.cangjie.chir.core.checker.ChirValidationReport
import org.cangnova.cangjie.chir.core.checker.ChirValidator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage

sealed interface ChirRewriteResult {
    data class Success(val rewritten: ChirPackage) : ChirRewriteResult
    data class Rejected(val report: ChirValidationReport) : ChirRewriteResult
}

class ChirRewriteSession(
    initial: ChirPackage,
    private val context: ChirContext? = null,
    private val validator: ChirValidator = DefaultChirValidator(),
) {
    private var current: ChirPackage = initial

    fun snapshot(): ChirPackage = current

    fun apply(mutator: (ChirPackage) -> ChirPackage): ChirRewriteResult {
        val candidate = mutator(current)
        val report = validator.validatePackage(candidate, context)
        if (report.hasErrors) {
            return ChirRewriteResult.Rejected(report)
        }
        current = candidate
        return ChirRewriteResult.Success(candidate)
    }

    fun replaceExpression(
        functionId: ChirSemanticId,
        blockId: ChirSemanticId,
        expressionId: ChirSemanticId,
        replacement: ChirExpression,
    ): ChirRewriteResult {
        return apply { pkg ->
            pkg.copy(
                modules = pkg.modules.map { module ->
                    module.copy(
                        declarations = module.declarations.map { declaration ->
                            rewriteDeclarationExpression(declaration, functionId, blockId, expressionId, replacement)
                        },
                    )
                },
            )
        }
    }

    private fun rewriteDeclarationExpression(
        declaration: ChirDeclaration,
        functionId: ChirSemanticId,
        blockId: ChirSemanticId,
        expressionId: ChirSemanticId,
        replacement: ChirExpression,
    ): ChirDeclaration {
        val function = declaration as? DefaultChirFunctionDeclaration ?: return declaration
        if (function.semanticId != functionId) return declaration

        return function.copy(
            blocks = function.blocks.map { block ->
                if (block.semanticId != blockId) return@map block
                block.copy(
                    expressions = block.expressions.map { expression ->
                        if (expression.semanticId == expressionId) replacement else expression
                    },
                )
            },
        )
    }
}
