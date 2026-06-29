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

/**
 * CHIR rewrite session 的提交结果。
 */
sealed interface ChirRewriteResult {
    /**
     * rewrite 成功并返回改写后的包。
     */
    data class Success(val rewritten: ChirPackage) : ChirRewriteResult

    /**
     * rewrite 后校验失败，改写被拒绝。
     */
    data class Rejected(val report: ChirValidationReport) : ChirRewriteResult
}

/**
 * 带校验的 CHIR rewrite 会话。
 */
class ChirRewriteSession(
    initial: ChirPackage,
    /**
     * 可选上下文，用于 rewrite 后校验跨节点引用。
     */
    private val context: ChirContext? = null,

    /**
     * rewrite 后使用的校验器。
     */
    private val validator: ChirValidator = DefaultChirValidator(),
) {
    /**
     * 当前会话持有的包快照。
     */
    private var current: ChirPackage = initial

    /**
     * 返回当前包快照。
     */
    fun snapshot(): ChirPackage = current

    /**
     * 对当前包应用 [mutator]，校验通过后提交变更。
     */
    fun apply(mutator: (ChirPackage) -> ChirPackage): ChirRewriteResult {
        val candidate = mutator(current)
        val report = validator.validatePackage(candidate, context)
        if (report.hasErrors) {
            return ChirRewriteResult.Rejected(report)
        }
        current = candidate
        return ChirRewriteResult.Success(candidate)
    }

    /**
     * 替换指定函数和基本块内的一条表达式。
     */
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

    /**
     * 在单个声明内执行表达式替换。
     */
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
