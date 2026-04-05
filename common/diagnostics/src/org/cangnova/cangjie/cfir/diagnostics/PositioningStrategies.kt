package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBinaryExpressionWithTypeRHS
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjImportDirective
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjOperationExpression
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUnaryExpression
import org.cangnova.cangjie.psi.CjUserType

object PositioningStrategies {
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {}
    val ACTUAL_DECLARATION_NAME: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val namedDeclaration = element as? CjNamedDeclaration ?: return super.mark(element)
            val nameIdentifier = namedDeclaration.nameIdentifier ?: return super.mark(element)
            return markElement(nameIdentifier)
        }
    }
    val DECLARATION_START_TO_NAME: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val namedDeclaration = element as? CjNamedDeclaration
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val nameIdentifier = namedDeclaration.nameIdentifier
                ?: return ACTUAL_DECLARATION_NAME.mark(element)

            val declarationStart = listOf(
                CjTokens.CLASS_KEYWORD,
                CjTokens.INTERFACE_KEYWORD,
                CjTokens.STRUCT_KEYWORD,
                CjTokens.ENUM_KEYWORD,
            ).firstNotNullOfOrNull { token ->
                namedDeclaration.node.findChildByType(token)?.psi
            } ?: return ACTUAL_DECLARATION_NAME.mark(element)

            return markRange(declarationStart, nameIdentifier)
        }
    }
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val callable = element as? CjCallableDeclaration
                ?: return ACTUAL_DECLARATION_NAME.mark(element)

            val startElement = (callable as? CjFunction)?.keyword ?: callable.nameIdentifier ?: callable
            val endElement = callable.valueParameterList ?: callable.nameIdentifier ?: startElement
            return markRange(startElement, endElement)
        }
    }
    val IMPORT_LAST_NAME: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjImportItem) {
                val importedReference = element.importedReference
                if (importedReference is CjDotQualifiedExpression) {
                    importedReference.selectorExpression?.let { return super.mark(it) }
                }
                return super.mark(element.importedReference ?: element)
            }
            return super.mark(element)
        }
    }
    val INITIALIZER_EQ: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val eqElement = when (element) {
                is CjPatternVariable -> element.equalsToken
                is CjFieldVariable -> element.equalsToken
                else -> element.node.findChildByType(CjTokens.EQ)?.psi
            }
            return if (eqElement != null) markElement(eqElement) else super.mark(element)
        }
    }
    val VISIBILITY_MODIFIER: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val modifierOwner = element as? CjModifierListOwner
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val modifier = modifierOwner.modifierList?.getModifier(CjTokens.VISIBILITY_MODIFIERS)
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            return markElement(modifier)
        }
    }
    val OVERRIDE_MODIFIER: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val modifierOwner = element as? CjModifierListOwner
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val modifier = modifierOwner.modifierList?.getModifier(CjTokens.OVERRIDE_KEYWORD)
                ?: modifierOwner.modifierList?.getModifier(CjTokens.REDEF_KEYWORD)
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            return markElement(modifier)
        }
    }
    val MUT_MODIFIER: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val modifierOwner = element as? CjModifierListOwner
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val modifier = modifierOwner.modifierList?.getModifier(CjTokens.MUT_KEYWORD)
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            return markElement(modifier)
        }
    }

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
