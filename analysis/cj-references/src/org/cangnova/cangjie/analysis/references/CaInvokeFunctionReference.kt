package org.cangnova.cangjie.analysis.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.MultiRangeReference
import org.cangnova.cangjie.idea.references.CjSimpleReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 只为“隐式 invoke”调用暴露 parent-level reference。
 *
 * 普通直接调用仍由 callee 的 simple-name 引用负责导航；
 * 只有当 Analysis API 明确把当前调用标记为 `isImplicitInvoke` 时，
 * 括号或 trailing lambda 才属于独立语义位点。
 */
internal class CaInvokeFunctionReference(
    expression: CjCallExpression,
) : CjSimpleReference<CjCallExpression>(expression), MultiRangeReference {
    override val resolvesByNames: Collection<Name>
        get() = listOf(OperatorNameConventions.INVOKE)

    override fun getRangeInElement(): TextRange = element.textRange.shiftRight(-element.textOffset)

    override fun getRanges(): List<TextRange> {
        val ranges = mutableListOf<TextRange>()

        expression.valueArgumentList?.let { valueArgumentList ->
            val leftParenthesis = valueArgumentList.leftParenthesis
            val rightParenthesis = valueArgumentList.rightParenthesis
            if (leftParenthesis != null && rightParenthesis != null) {
                ranges += leftParenthesis.textRange.shiftRight(-expression.textOffset)
                ranges += rightParenthesis.textRange.shiftRight(-expression.textOffset)
            } else {
                ranges += valueArgumentList.textRange.shiftRight(-expression.textOffset)
            }
        }

        expression.lambdaArguments.forEach { lambdaArgument ->
            val lambdaExpression = lambdaArgument.getLambdaExpression() ?: return@forEach
            ranges += lambdaExpression.leftCurlyBrace.textRange.shiftRight(-expression.textOffset)
            lambdaExpression.rightCurlyBrace?.let { rightCurlyBrace ->
                ranges += rightCurlyBrace.textRange.shiftRight(-expression.textOffset)
            }
        }

        return ranges
    }

    override fun resolveTargetElements() =
        element.resolveCallTargetPsis { call ->
            call.calleeName == OperatorNameConventions.INVOKE
        }

    class Provider : CangJiePsiReferenceProviderContributor<CjCallExpression> {
        override val elementClass: Class<CjCallExpression>
            get() = CjCallExpression::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjCallExpression>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { expression ->
                val shouldExposeReference = expression.resolveCallTargetPsis { call ->
                    call.calleeName == OperatorNameConventions.INVOKE
                }.isNotEmpty()
                if (shouldExposeReference) {
                    listOf(CaInvokeFunctionReference(expression))
                } else {
                    emptyList()
                }
            }
    }
}
