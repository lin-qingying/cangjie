package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBinaryExpressionWithTypeRHS
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjOperationExpression
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUnaryExpression
import org.cangnova.cangjie.psi.CjUserType

object PositioningStrategies {
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {}

    val OPERATOR: PositioningStrategy<CjExpression> = object : PositioningStrategy<CjExpression>() {
        override fun mark(element: CjExpression) = when (element) {
            is CjBinaryExpression -> markElement(element.operationReference)
            is CjBinaryExpressionWithTypeRHS -> markElement(element.operationReference)
            is CjUnaryExpression -> markElement(element.operationReference)
            else -> super.mark(element)
        }
    }

    val REFERENCE_BY_QUALIFIED: PositioningStrategy<PsiElement> = FindReferencePositioningStrategy(false)
    val REFERENCED_NAME_BY_QUALIFIED: PositioningStrategy<PsiElement> = FindReferencePositioningStrategy(true)

    private class FindReferencePositioningStrategy(
        private val locateReferencedName: Boolean,
    ) : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            var result: PsiElement = when (element) {
                is CjQualifiedExpression -> {
                    when (val selector = element.selectorExpression) {
                        is CjCallExpression -> selector.calleeExpression ?: selector
                        is CjReferenceExpression -> selector
                        else -> element
                    }
                }
                is CjCallExpression -> element.calleeExpression ?: element
                is CjOperationExpression -> element.operationReference
                is CjTypeReference -> {
                    val userType = element.typeElement as? CjUserType
                    userType?.referenceExpression ?: element
                }
                is CjImportDirective -> element.importedReference ?: element
                is CjImportAlias -> element.nameIdentifier ?: element
                else -> element
            }

            while (locateReferencedName && result is CjParenthesizedExpression) {
                result = result.expression ?: break
            }
            return super.mark(result)
        }
    }
}
