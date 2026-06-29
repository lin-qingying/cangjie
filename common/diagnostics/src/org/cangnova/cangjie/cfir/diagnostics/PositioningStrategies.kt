package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBinaryExpressionWithTypeRHS
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstructor
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
import org.cangnova.cangjie.psi.CjThrowExpression
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjUnaryExpression
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentList

/**
 * PSI 前端使用的诊断定位策略集合。
 */
object PositioningStrategies {
    /**
     * 默认定位策略，直接标记 PSI 元素文本范围。
     */
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {}
    /**
     * 标记声明的实际名称标识符。
     */
    val ACTUAL_DECLARATION_NAME: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjConstructor<*>) {
                element.getIdentifyingElement()?.let { return markElement(it) }
            }
            val namedDeclaration = element as? CjNamedDeclaration ?: return super.mark(element)
            val nameIdentifier = namedDeclaration.nameIdentifier ?: return super.mark(element)
            return markElement(nameIdentifier)
        }
    }
    /**
     * 标记声明起始关键字到名称标识符的范围。
     */
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
    /**
     * 标记可调用声明签名中不含修饰符的主体范围。
     */
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val callable = element as? CjCallableDeclaration
                ?: return ACTUAL_DECLARATION_NAME.mark(element)

            val startElement = (callable as? CjFunction)?.keyword ?: callable.nameIdentifier ?: callable
            val endElement = callable.valueParameterList ?: callable.nameIdentifier ?: startElement
            return markRange(startElement, endElement)
        }
    }
    /**
     * 标记 import 路径最后一个被引用名称。
     */
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
    /**
     * 标记 import alias 的别名标识符。
     */
    val IMPORT_ALIAS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            if (element is CjImportItem) {
                val aliasIdentifier = element.alias?.nameIdentifier
                if (aliasIdentifier != null) return markElement(aliasIdentifier)
                return IMPORT_LAST_NAME.mark(element)
            }
            return super.mark(element)
        }
    }
    /**
     * 标记初始化器中的等号 token。
     */
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
    /**
     * 标记声明上的可见性修饰符。
     */
    val VISIBILITY_MODIFIER: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val modifierOwner = element as? CjModifierListOwner
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val modifier = modifierOwner.modifierList?.getModifier(CjTokens.VISIBILITY_MODIFIERS)
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            return markElement(modifier)
        }
    }
    /**
     * 标记 override 或 redef 修饰符。
     */
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
    /**
     * 标记 mut 修饰符。
     */
    val MUT_MODIFIER: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val modifierOwner = element as? CjModifierListOwner
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            val modifier = modifierOwner.modifierList?.getModifier(CjTokens.MUT_KEYWORD)
                ?: return ACTUAL_DECLARATION_NAME.mark(element)
            return markElement(modifier)
        }
    }
    /**
     * 标记 throw 表达式中的 throw 关键字。
     */
    val THROW_KEYWORD: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val throwExpression = element as? CjThrowExpression ?: return super.mark(element)
            val throwKeyword = throwExpression.node.findChildByType(CjTokens.THROW_KEYWORD)?.psi
                ?: return super.mark(element)
            return markElement(throwKeyword)
        }
    }
    /**
     * 标记数组字面量左中括号。
     */
    val ARRAY_LITERAL_LEFT_BRACKET: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val arrayLiteral = element as? CjCollectionLiteralExpression ?: return super.mark(element)
            return arrayLiteral.leftBracket?.let(::markElement) ?: super.mark(element)
        }
    }

    /**
     * 标记表达式中的操作符引用。
     */
    val OPERATOR: PositioningStrategy<CjExpression> = object : PositioningStrategy<CjExpression>() {
        override fun mark(element: CjExpression) = when (element) {
            is CjBinaryExpression -> markElement(element.operationReference)
            is CjBinaryExpressionWithTypeRHS -> markElement(element.operationReference)
            is CjUnaryExpression -> markElement(element.operationReference)
            else -> super.mark(element)
        }
    }

    /**
     * 标记具名实参的参数名。
     */
    val NAME_OF_NAMED_ARGUMENT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val valueArgument = element as? CjValueArgument ?: return super.mark(element)
            val argumentName = valueArgument.getArgumentName()?.referenceExpression ?: return super.mark(element)
            return markElement(argumentName)
        }
    }

    /**
     * 标记调用表达式中的实参范围。
     */
    val VALUE_ARGUMENTS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val arguments = when (element) {
                is CjCallExpression -> element.valueArgumentList?.arguments.orEmpty()
                is CjValueArgumentList -> element.arguments
                else -> emptyList()
            }
            if (arguments.isEmpty()) return super.mark(element)
            return markRange(arguments.first(), arguments.last())
        }
    }

    /**
     * 标记调用表达式的实参列表节点。
     */
    val VALUE_ARGUMENTS_LIST: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val argumentList = when (element) {
                is CjCallExpression -> element.valueArgumentList
                is CjValueArgumentList -> element
                else -> null
            } ?: return VALUE_ARGUMENTS.mark(element)
            return markElement(argumentList)
        }
    }

    /**
     * 标记限定表达式中的引用表达式。
     */
    val REFERENCE_BY_QUALIFIED: PositioningStrategy<PsiElement> = FindReferencePositioningStrategy(false)
    /**
     * 标记限定表达式中最终被引用的名称。
     */
    val REFERENCED_NAME_BY_QUALIFIED: PositioningStrategy<PsiElement> = FindReferencePositioningStrategy(true)

    /**
     * 在不同 PSI 表达式形状中寻找应标记引用节点的策略。
     */
    private class FindReferencePositioningStrategy(
        /**
         * 是否跳过括号并定位到最终被引用名称。
         */
        private val locateReferencedName: Boolean,
    ) : PositioningStrategy<PsiElement>() {
        /**
         * 根据 PSI 类型选择引用节点并返回其标记范围。
         */
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
