package org.cangnova.cangjie.cfir.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBinaryExpressionWithTypeRHS
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjConstructorCalleeExpression
import org.cangnova.cangjie.psi.CjDotQualifiedExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjForExpression
import org.cangnova.cangjie.psi.CjFunction
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMainFunction
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjImportPathOwner
import org.cangnova.cangjie.psi.CjImportItem
import org.cangnova.cangjie.psi.CjMatchExpression
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjOperationExpression
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjParenthesizedExpression
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.CjQualifiedExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjThrowExpression
import org.cangnova.cangjie.psi.CjTypeArgumentList
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.CjUnaryExpression
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentList
import org.cangnova.cangjie.psi.getTypeArgumentList

/**
 * PSI 前端使用的诊断定位策略集合。
 */
object PositioningStrategies {
    /** 与 Kotlin DeclarationHeader 对位，声明名有效性独立于签名范围选择。 */
    open class DeclarationHeader<T : PsiElement>(private val checkValidity: Boolean) : PositioningStrategy<T>() {
        override fun isValid(element: T): Boolean {
            if (checkValidity && element is CjNamedDeclaration && element !is CjSecondaryConstructor && element !is CjFunction) {
                if (element.nameIdentifier == null) return false
            }
            return super.isValid(element)
        }
    }

    /**
     * 默认定位策略，直接标记 PSI 元素文本范围。
     */
    val DEFAULT: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {}
    /**
     * 标记声明的实际名称标识符。
     */
    val ACTUAL_DECLARATION_NAME: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun markOrNull(element: PsiElement): List<TextRange>? {
            val nameIdentifier = when (element) {
                is CjConstructor<*> -> element.getIdentifyingElement() ?: element.nameIdentifier
                is CjExtend -> (element.typeParameterList ?: element.receiverTypeReceiver)?.firstLeaf()
                is CjPropertyAccessor -> element.node.findChildByType(CjTokens.GET_KEYWORD)?.psi
                    ?: element.node.findChildByType(CjTokens.SET_KEYWORD)?.psi
                is CjNamedDeclaration -> element.nameIdentifier
                else -> null
            } ?: return null
            return markElement(nameIdentifier)
        }

        override fun mark(element: PsiElement): List<TextRange> = markOrNull(element) ?: super.mark(element)
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
    val DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK: PositioningStrategy<CjDeclaration> = DeclarationSignature(true)

    val DECLARATION_SIGNATURE: PositioningStrategy<CjDeclaration> = DeclarationSignature(false)

    /**
     * 保留 Kotlin DeclarationSignature 的声明分派与 helper 所属。
     * 仓颉主构造器写在类型 body 内，类型头不包含 Kotlin 的主构造器参数；
     * lambda 没有 Kotlin 接收者文法，静态初始化器由真实构造器节点承担。
     */
    private class DeclarationSignature(checkValidity: Boolean) : DeclarationHeader<CjDeclaration>(checkValidity) {
        override fun mark(element: CjDeclaration): List<TextRange> {
            when (element) {
                is CjConstructor<*> -> {
                    val keyword = if (element is CjPrimaryConstructor) element.getInitKeyword() else element.getConstructorKeyword()
                    val begin = keyword ?: element.valueParameterList ?: return markElement(element)
                    val end = element.valueParameterList ?: keyword ?: return markElement(element)
                    return markRange(begin, end)
                }
                is CjFunction -> {
                    val end = element.typeReference ?: element.valueParameterList ?: element.nameIdentifier ?: element
                    val start = if (element is CjFunctionLiteral) element.valueParameterList ?: element else element
                    return markRange(start, end)
                }
                is CjProperty -> return markRange(element, element.typeReference ?: element.nameIdentifier ?: element)
                is CjPropertyAccessor -> {
                    val end = element.returnTypeReference ?: element.parameterList ?: element.namePlaceholder
                    return markRange(element, end)
                }
                // extend 没有名义声明标识符，也不是 Kotlin class 分支的对应项。
                is CjExtend -> return super.mark(element)
                is CjTypeStatement -> return markElement(element.nameIdentifier ?: element)
            }
            return super.mark(element)
        }
    }

    /** 声明关键字至返回类型的完整签名；函数、属性和访问器共用两入口相同的选择顺序。 */
    val CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            when (element) {
                is CjNamedFunction -> {
                    val start = element.funKeyword ?: element
                    val end = element.typeReference ?: element.valueParameterList ?: element.nameIdentifier ?: element
                    return markRange(start, end)
                }
                is CjMainFunction -> {
                    val start = element.node.findChildByType(CjTokens.MAIN_KEYWORD)?.psi ?: element
                    val end = element.typeReference ?: element.valueParameterList ?: element
                    return markRange(start, end)
                }
                is CjProperty -> {
                    val start = element.node.findChildByType(CjTokens.PROP_KEYWORD)?.psi ?: element
                    val end = element.typeReference ?: element.nameIdentifier ?: element
                    return markRange(start, end)
                }
                is CjPropertyAccessor -> {
                    val end = element.returnTypeReference ?: element.parameterList ?: element.namePlaceholder
                    return markRange(element.namePlaceholder, end)
                }
            }
            return if (element is CjDeclaration) DECLARATION_SIGNATURE_WITH_VALIDITY_CHECK.mark(element) else DEFAULT.mark(element)
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
     * 标记 for-in 表达式中的 for 关键字。
     */
    val FOR_KEYWORD: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val forExpression = element as? CjForExpression ?: return super.mark(element)
            val forKeyword = forExpression.forKeyword ?: return super.mark(element)
            return markElement(forKeyword)
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
                is CjQualifiedExpression -> (element.selectorExpression as? CjCallExpression)
                    ?.valueArgumentList?.arguments.orEmpty()
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
                is CjQualifiedExpression -> (element.selectorExpression as? CjCallExpression)?.valueArgumentList
                is CjValueArgumentList -> element
                else -> null
            } ?: return VALUE_ARGUMENTS.mark(element)
            return markElement(argumentList)
        }
    }

    /** 标记当前调用或引用自身的完整类型实参列表，未提供列表时保留源节点范围。 */
    val TYPE_ARGUMENT_LIST_OR_SELF: PositioningStrategy<PsiElement> = object : PositioningStrategy<PsiElement>() {
        override fun mark(element: PsiElement): List<TextRange> {
            val target = (element as? CjQualifiedExpression)?.selectorExpression ?: element
            val argumentList = when (target) {
                is CjCallExpression -> target.typeArgumentList
                is CjSimpleNameExpression -> target.getTypeArgumentList()
                is CjUserType -> target.typeArgumentList
                is CjTypeArgumentList -> target
                else -> null
            } ?: return super.mark(element)
            return markElement(argumentList)
        }
    }
    /** match 表达式级诊断覆盖完整 match 关键字，与 K2 WHEN_EXPRESSION 定位一致。 */
    val MATCH_KEYWORD: PositioningStrategy<CjMatchExpression> = object : PositioningStrategy<CjMatchExpression>() {
        override fun mark(element: CjMatchExpression): List<TextRange> = markElement(element.matchKeyword)
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
                is CjImportPathOwner -> element.importedReference ?: element
                is CjImportAlias -> element.nameIdentifier ?: element
                else -> element
            }

            while (locateReferencedName && result is CjParenthesizedExpression) {
                result = result.expression ?: break
            }
            if (locateReferencedName && result is CjConstructorCalleeExpression) {
                result = result.constructorReferenceExpression?.referencedNameElement ?: result
            }
            if (locateReferencedName && result is CjSimpleNameExpression) {
                result = result.referencedNameElement
            }
            return super.mark(result)
        }
    }
}

/** 返回语法节点中第一个叶子 token，保留 extend identifier 的真实起始 token。 */
private fun PsiElement.firstLeaf(): PsiElement = firstChild?.firstLeaf() ?: this
