/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.codeinsight.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationResolutionStatus
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaCallableMemberCall
import org.cangnova.cangjie.analysis.api.resolution.CaEnumConstructorCall
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionValueCall
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionCall
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.resolution.symbol
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjMacroAttr
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.psi.CjTypePattern
import org.cangnova.cangjie.psi.CjStruct
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.psiUtil.getAssignmentByLHS
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.utils.exceptions.CangJieIllegalArgumentExceptionWithAttachments

/** Semantic categories shared by the IDE highlighter and LSP token adapter. */
enum class CangJieSemanticHighlightingKind {
    CLASS,
    ABSTRACT_CLASS,
    INTERFACE,
    STRUCT,
    ENUM,
    /** 普通用户类型引用的配色类别；IDE 配色页显示为“用户类型引用”。 */
    TYPE_REFERENCE,
    /** 仓颉 primitive 类型引用，与用户定义类型引用分开配置。 */
    BASIC_TYPE_REFERENCE,
    TYPE_ALIAS,
    TYPE_PARAMETER,
    LOCAL_VARIABLE,
    MUTABLE_VARIABLE,
    PACKAGE_VARIABLE,
    STATIC_VARIABLE,
    INSTANCE_VARIABLE,
    PARAMETER,
    STATIC_PROPERTY,
    INSTANCE_PROPERTY,
    INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
    STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION,
    EXTENSION_PROPERTY,
    MUTABLE_PROPERTY,
    ENUM_CONSTRUCTOR,
    FUNCTION_CALL,
    PACKAGE_FUNCTION_CALL,
    INSTANCE_FUNCTION_CALL,
    STATIC_FUNCTION_CALL,
    EXTENSION_FUNCTION_CALL,
    CONSTRUCTOR_CALL,
    CLASS_CONSTRUCTOR_CALL,
    STRUCT_CONSTRUCTOR_CALL,
    VARIABLE_AS_FUNCTION_CALL,
    MACRO_DECLARATION,
    SAFE_ACCESS,
    MACRO_ESCAPE,
    NAMED_ARGUMENT,
    ANNOTATION,
    ANNOTATION_ATTRIBUTE_NAME,
    MACRO_CALL,
}

/**
 * 解析后的单个符号类别及其叠加属性。
 *
 * [additionalKinds] 用于表达可变变量、可写属性等叠加着色；LSP 可将同一信息投影到
 * token type 和 modifiers，IDE 则为各 kind 创建相应的 HighlightInfo。
 */
data class CangJieSemanticHighlighting(
    val kind: CangJieSemanticHighlightingKind,
    val additionalKinds: Set<CangJieSemanticHighlightingKind> = emptySet(),
    val isMutable: Boolean? = null,
    val isStatic: Boolean = false,
)

/**
 * 仓颉 Analysis API 符号到共享语义高亮类别的唯一映射。
 *
 * IDE 和 LSP 只负责把分类结果写入各自的高亮载体，不再分别按 PSI 外形猜测引用类型。
 */
object CangJieSemanticHighlightingRules {
    /** 只暴露 Analysis API 判定成功且唯一的符号，拒绝失败尝试中的候选符号。 */
    fun resolveSuccessfulSymbol(session: CaSession, element: CjElement): CaSymbol? {
        val resolutionElement = (element as? CjSimpleNameExpression)?.getAssignmentByLHS() ?: element
        val attempt = with(session) { resolutionElement.tryResolveSymbols() }
        val success = attempt as? CaSymbolResolutionSuccess ?: return null
        return success.symbols.singleOrNull()
    }

    /** 仓颉 primitive token 使用独立于用户类型引用的配色类别。 */
    fun classifyBasicType(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.BASIC_TYPE_REFERENCE)

    /** 用户类型引用按引用类别着色；primitive 引用和类型参数保留各自的独立类别。 */
    fun classifyClassifier(symbol: CaClassifierSymbol): CangJieSemanticHighlighting? = when (symbol) {
        is CaClassSymbol -> when (symbol.classKindOrNull) {
            null -> null
            CaClassKind.PRIMITIVE -> classifyBasicType()
            else -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.TYPE_REFERENCE)
        }

        is CaTypeAliasSymbol -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.TYPE_REFERENCE)
        is CaTypeParameterSymbol -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.TYPE_PARAMETER)
        else -> null
    }

    /**
     * 将引用解析到的符号映射为语义类别；不唯一或未解析的引用由调用方保持无分类状态。
     */
    fun classifyReference(symbol: CaSymbol): CangJieSemanticHighlighting? = when (symbol) {
        is CaClassifierSymbol -> classifyClassifier(symbol)
        // 模式叶子保留独立符号族，位置与可变性直接读取当前 Analysis 符号。
        is CaPatternBindingSymbol -> classifyVariable(
            kind = if (symbol.location == CaSymbolLocation.TOP_LEVEL) {
                CangJieSemanticHighlightingKind.PACKAGE_VARIABLE
            } else {
                CangJieSemanticHighlightingKind.LOCAL_VARIABLE
            },
            isMutable = !symbol.isLet,
        )
        is CaLocalVariableSymbol -> classifyVariable(
            kind = CangJieSemanticHighlightingKind.LOCAL_VARIABLE,
            isMutable = symbol.psi.localPatternVariable()?.isVar ?: !symbol.isLet,
        )

        is CaParameterSymbol -> classifyParameter(symbol)
        is CaFieldSymbol -> classifyVariable(
            kind = when {
                symbol.location == CaSymbolLocation.TOP_LEVEL -> CangJieSemanticHighlightingKind.PACKAGE_VARIABLE
                symbol.isStatic -> CangJieSemanticHighlightingKind.STATIC_VARIABLE
                else -> CangJieSemanticHighlightingKind.INSTANCE_VARIABLE
            },
            isMutable = !symbol.isLet,
            isStatic = symbol.isStatic,
        )

        is CaPropertySymbol -> classifyProperty(symbol)
        is CaEnumConstructorSymbol -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ENUM_CONSTRUCTOR)
        else -> null
    }

    /** 函数值调用仍然引用一个变量、参数、字段或属性；这些符号保留自己的引用类别。 */
    fun isValueReferenceSymbol(symbol: CaSymbol): Boolean =
        symbol is CaPatternBindingSymbol ||
            symbol is CaLocalVariableSymbol ||
            symbol is CaParameterSymbol ||
            symbol is CaFieldSymbol ||
            symbol is CaPropertySymbol

    /** Classify a successfully resolved call. */
    @OptIn(CaExperimentalApi::class)
    fun classifyCall(session: CaSession, call: CaCall): CangJieSemanticHighlighting? = when (call) {
        is CaEnumConstructorCall<*> -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ENUM_CONSTRUCTOR)
        is CaFunctionValueCall -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.VARIABLE_AS_FUNCTION_CALL)
        is CaFunctionCall<*> -> when (val symbol = call.symbol) {
            is CaMacroSymbol -> null
            is CaConstructorSymbol -> classifyConstructor(session, symbol)
            else -> classifyFunction(symbol)
        }

        else -> null
    }

    /** 按唯一成功解析到的函数符号分类运算符引用。 */
    fun classifyFunction(symbol: CaFunctionSymbol): CangJieSemanticHighlighting? = when {
        symbol is CaMacroSymbol -> null
        symbol is CaConstructorSymbol -> CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.CONSTRUCTOR_CALL)
        symbol.location == CaSymbolLocation.EXTEND -> CangJieSemanticHighlighting(
            kind = CangJieSemanticHighlightingKind.EXTENSION_FUNCTION_CALL,
            isStatic = symbol.isStatic,
        )

        symbol.location == CaSymbolLocation.TOP_LEVEL -> CangJieSemanticHighlighting(
            kind = CangJieSemanticHighlightingKind.PACKAGE_FUNCTION_CALL,
            isStatic = symbol.isStatic,
        )

        symbol.location == CaSymbolLocation.CLASS -> CangJieSemanticHighlighting(
            kind = if (symbol.isStatic) {
                CangJieSemanticHighlightingKind.STATIC_FUNCTION_CALL
            } else {
                CangJieSemanticHighlightingKind.INSTANCE_FUNCTION_CALL
            },
            isStatic = symbol.isStatic,
        )

        else -> CangJieSemanticHighlighting(
            kind = CangJieSemanticHighlightingKind.FUNCTION_CALL,
            isStatic = symbol.isStatic,
        )
    }

    /** 将已解析的构造器符号映射为 class 或 struct 的构造调用类别。 */
    fun classifyConstructor(session: CaSession, symbol: CaConstructorSymbol): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(
            kind = when (constructedTypeKind(session, symbol)) {
                CaClassKind.ENUM -> CangJieSemanticHighlightingKind.ENUM_CONSTRUCTOR
                CaClassKind.STRUCT -> CangJieSemanticHighlightingKind.STRUCT_CONSTRUCTOR_CALL
                CaClassKind.CLASS -> CangJieSemanticHighlightingKind.CLASS_CONSTRUCTOR_CALL
                else -> CangJieSemanticHighlightingKind.CONSTRUCTOR_CALL
            },
        )

    /** 创建纯语法的命名实参类别，供 IDE 解析前 pass 与 LSP 共用。 */
    fun namedArgument(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.NAMED_ARGUMENT)

    /** 已由注解解析器确认的注解调用类别。 */
    fun annotationCall(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ANNOTATION)

    /** 只为 Analysis API 注解列表中唯一回指到该 PSI、且具有已解析目标的注解创建语义类别。 */
    fun classifyAnnotationCall(session: CaSession, annotation: CjAnnotation): CangJieSemanticHighlighting? {
        if (resolvedSourceAnnotation(session, annotation) == null) return null
        return CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ANNOTATION)
    }

    /** 只为 Analysis API 已绑定到注解实参的唯一参数标签创建语义类别。 */
    fun classifyAnnotationAttributeName(
        session: CaSession,
        argumentName: CjValueArgumentName,
    ): CangJieSemanticHighlighting? {
        val annotationPsi = argumentName.getStrictParentOfType<CjAnnotation>() ?: return null
        val annotation = resolvedSourceAnnotation(session, annotationPsi) ?: return null
        if (annotation.resolutionStatus !in setOf(
                CaAnnotationResolutionStatus.ARGUMENTS_RESOLVED,
                CaAnnotationResolutionStatus.RESOLVED,
            )
        ) {
            return null
        }

        val valueArgument = argumentName.parent as? CjValueArgument ?: return null
        if (valueArgument.getArgumentName() !== argumentName) return null
        val argumentExpression = valueArgument.getArgumentExpression() ?: return null
        val name = argumentName.asName
        if (annotationPsi.valueArguments.count { sourceArgument ->
                (sourceArgument as? CjValueArgument)?.getArgumentName()?.asName == name
            } != 1
        ) {
            return null
        }
        if (annotation.arguments.singleOrNull { resolvedArgument ->
                resolvedArgument.name == name && resolvedArgument.expression.sourcePsi === argumentExpression
            } == null
        ) {
            return null
        }

        return CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ANNOTATION_ATTRIBUTE_NAME)
    }

    /** 创建已解析注解属性名类别，供注解 PSI 与宏属性 token 共用。 */
    fun annotationAttributeName(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ANNOTATION_ATTRIBUTE_NAME)

    /** 已由宏构造解析器唯一确认的宏调用类别。 */
    fun macroCall(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.MACRO_CALL)

    /** 宏声明名在 IDE 与 LSP 中共享声明类别。 */
    fun macroDeclaration(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.MACRO_DECLARATION)

    /** 宏 Tokens 输入中的定界符转义是语法类别，不依赖宏名称是否解析成功。 */
    fun macroEscape(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.MACRO_ESCAPE)

    /** Option 可选链问号由解析器创建的 OPTIONAL_EXPRESSION 节点及其后缀 token 确认。 */
    fun safeAccess(): CangJieSemanticHighlighting =
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.SAFE_ACCESS)

    /** 按解析器的 quest 后缀规则识别安全访问，不把 Option 类型前缀问号当作访问符。 */
    fun isSafeAccessToken(element: PsiElement): Boolean {
        val token = element.node
        if (token.elementType != CjTokens.QUEST) return false
        val optionalExpression = token.treeParent ?: return false
        if (optionalExpression.elementType != CjNodeTypes.OPTIONAL_EXPRESSION) return false

        var suffix = optionalExpression.treeNext
        while (suffix != null && CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(suffix.elementType)) {
            suffix = suffix.treeNext
        }
        return isSafeAccessQuestionMark(token.elementType, suffix?.elementType)
    }

    /** 共享解析器对 `?` 后缀的 token 规则，供 AST 与 LSP lexer 路径共同使用。 */
    fun isSafeAccessQuestionMark(questionTokenType: IElementType, suffixTokenType: IElementType?): Boolean =
        questionTokenType == CjTokens.QUEST && suffixTokenType != null && suffixTokenType in SAFE_ACCESS_SUFFIX_TOKENS

    /** 只从宏输入的 token 容器返回词法器识别出的转义 token。 */
    fun macroEscapeTokenElements(expression: CjMacroExpression): List<PsiElement> =
        listOfNotNull(expression.input?.quoteTokens, expression.attr?.quoteTokens)
            .flatMap { tokenContainer ->
                PsiTreeUtil.collectElements(tokenContainer) { element ->
                    element.node.elementType in MACRO_ESCAPE_TOKENS
                }.toList()
            }

    /** 顶层注解宏包装从其声明符号的解析结果恢复唯一注解调用。 */
    fun classifyMacroAnnotationCall(
        session: CaSession,
        expression: CjMacroExpression,
    ): CangJieSemanticHighlighting? = resolvedMacroAnnotation(session, expression)?.let {
        CangJieSemanticHighlighting(CangJieSemanticHighlightingKind.ANNOTATION)
    }

    /** 顶层注解宏包装中的具名属性，仅返回 Analysis API 已绑定到形参的属性名 token。 */
    fun macroAnnotationAttributeNameElements(
        session: CaSession,
        expression: CjMacroExpression,
    ): List<PsiElement> {
        val annotation = resolvedMacroAnnotation(session, expression) ?: return emptyList()
        val parameterNames = annotation.arguments.mapTo(linkedSetOf()) { argument -> argument.name.asString() }
        return expression.macroAttributeNameElements(parameterNames)
    }

    /** 内建注解宏调用的命名属性集合来自官方 annotation schema。 */
    fun builtinMacroAnnotationAttributeNameElements(expression: CjMacroExpression): List<PsiElement> {
        val file = expression.containingFile as? CjFile ?: return emptyList()
        val name = expression.shortName?.asString() ?: return emptyList()
        val moduleName = BuiltInAnnotationRegistry.sourceModuleName(
            file.packageFqName,
            file.packageDirective?.organizationName,
        )
        val descriptor = BuiltInAnnotationRegistry.resolveLanguageBuiltIn(
            sourceName = name,
            forcedCustom = false,
            moduleName = moduleName,
        ) ?: return emptyList()
        return expression.macroAttributeNameElements(
            descriptor.argumentSchema.parameters.mapTo(linkedSetOf()) { parameter -> parameter.name },
        )
    }

    private fun CjMacroExpression.macroAttributeNameElements(parameterNames: Set<String>): List<PsiElement> {
        if (parameterNames.isEmpty()) return emptyList()
        val tokens = attr?.tokens.orEmpty()
        val matchesByName = linkedMapOf<String, MutableList<PsiElement>>()
        tokens.forEachIndexed { index, token ->
            if (token.node?.elementType != CjTokens.IDENTIFIER) return@forEachIndexed
            val nextToken = tokens.asSequence()
                .drop(index + 1)
                .firstOrNull { candidate ->
                    candidate.node?.elementType?.let { type ->
                        !CjTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(type)
                    } == true
                }
            if (nextToken?.node?.elementType != CjTokens.COLON) return@forEachIndexed
            val name = CjPsiUtil.unquoteIdentifier(token.text)
            if (name in parameterNames) matchesByName.getOrPut(name, ::mutableListOf) += token
        }

        return matchesByName.values.filter { matches -> matches.size == 1 }.flatten()
    }

    private fun resolvedMacroAnnotation(session: CaSession, expression: CjMacroExpression): CaAnnotation? {
        val declaration = expression.unwrappedDeclaration as? CjDeclaration ?: return null
        val shortName = expression.shortName ?: return null
        val annotations = declarationAnnotationsOrNull(session, declaration) ?: return null
        val matchingAnnotations = annotations.filter { annotation -> annotation.shortName == shortName }
        return matchingAnnotations.singleOrNull()?.takeIf { annotation ->
            annotation.hasResolvedTarget() &&
                (annotation.builtInKind != null ||
                    ((expression.containingFile as? CjFile)
                        ?.hasUnambiguousAnnotationTarget(session, shortName, annotation) == true))
        }
    }

    /** 按来源 PSI 从声明 Analysis API 注解列表中取得唯一已解析注解调用。 */
    private fun resolvedSourceAnnotation(session: CaSession, annotation: CjAnnotation): CaAnnotation? {
        val declaration = annotation.getStrictParentOfType<CjDeclaration>() ?: return null
        val annotations = declarationAnnotationsOrNull(session, declaration) ?: return null
        return annotations.singleOrNull { resolvedAnnotation -> resolvedAnnotation.psi === annotation }
            ?.takeIf { resolvedAnnotation ->
                resolvedAnnotation.hasResolvedTarget() &&
                    resolvedAnnotation.hasUniqueSourceType(session, annotation) &&
                    (resolvedAnnotation.builtInKind != null ||
                        ((annotation.containingFile as? CjFile)
                            ?.hasUnambiguousAnnotationTarget(session, annotation.shortName, resolvedAnnotation) == true))
            }
    }

    /** Analysis API 返回单个注解候选时，再核对源码可见导入名没有对应多个 class。 */
    private fun CjFile.hasUnambiguousAnnotationTarget(
        session: CaSession,
        name: org.cangnova.cangjie.name.Name?,
        annotation: CaAnnotation,
    ): Boolean {
        val shortName = name ?: return false
        val samePackageTargets = with(session) { getTopLevelClassLikeSymbols(packageFqName, shortName) }
            .distinctBy { symbol -> symbol.classId }
        if (samePackageTargets.isNotEmpty()) {
            return samePackageTargets.singleOrNull()?.classId == annotation.classId
        }

        val explicitTargets = importDirectivesItem.asSequence()
            .filter { import -> !import.isAllUnder && import.importedName == shortName }
            .mapNotNull { import -> import.importedFqName }
            .flatMap { importedTarget ->
                with(session) {
                    getTopLevelClassLikeSymbols(importedTarget.parent(), importedTarget.shortName())
                }.asSequence()
            }

        val wildcardTargets = importDirectivesItem.asSequence()
            .filter { import -> import.isAllUnder }
            .mapNotNull { import -> import.importedFqName }
            .flatMap { packageName ->
                with(session) { getTopLevelClassLikeSymbols(packageName, shortName) }.asSequence()
            }
        val visibleTargets = (explicitTargets + wildcardTargets)
            .distinctBy { symbol -> symbol.classId }
            .toList()
        if (visibleTargets.isEmpty()) return true
        val visibleTarget = visibleTargets.singleOrNull() ?: return false
        return annotation.classId == visibleTarget.classId
    }

    /** 自定义注解还需由 Analysis API 的类型引用唯一解析到与注解调用相同的 class。 */
    private fun CaAnnotation.hasUniqueSourceType(session: CaSession, annotation: CjAnnotation): Boolean {
        if (builtInKind != null) return true
        val typeReference = annotation.calleeExpression?.typeReference ?: return false
        val classifier = resolveSuccessfulSymbol(session, typeReference) as? CaClassSymbol ?: return false
        return constructorSymbol != null && classId == classifier.classId
    }

    /** 未映射到当前 CFIR 的宏包装声明没有成功注解解析结果；保留其它 API 错误。 */
    private fun declarationAnnotationsOrNull(session: CaSession, declaration: CjDeclaration): List<CaAnnotation>? = try {
        with(session) { declaration.symbol.annotations }
    } catch (failure: CangJieIllegalArgumentExceptionWithAttachments) {
        if (failure.message?.startsWith(MISSING_CFIR_DECLARATION_PREFIX) == true) null else throw failure
    }

    private const val MISSING_CFIR_DECLARATION_PREFIX = "CfirDeclaration was not found for declaration "

    private val MACRO_ESCAPE_TOKENS = setOf(
        CjTokens.ESCAPE_LPAR,
        CjTokens.ESCAPE_RPAR,
        CjTokens.ESCAPE_LBRACKET,
        CjTokens.ESCAPE_RBRACKET,
        CjTokens.ESCAPE_AT,
    )

    private val SAFE_ACCESS_SUFFIX_TOKENS = setOf(
        CjTokens.DOT,
        CjTokens.LBRACKET,
        CjTokens.LPAR,
        CjTokens.LBRACE,
    )

    private fun CaAnnotation.hasResolvedTarget(): Boolean =
        resolutionStatus in setOf(
            CaAnnotationResolutionStatus.TYPE_RESOLVED,
            CaAnnotationResolutionStatus.ARGUMENTS_RESOLVED,
            CaAnnotationResolutionStatus.RESOLVED,
        ) && (classId != null || builtInKind != null) && (constructorSymbol != null || builtInKind != null)

    private fun classifyParameter(symbol: CaParameterSymbol): CangJieSemanticHighlighting {
        val isPropertyParameter = (symbol.psi as? CjParameter)?.letOrVarKeyword != null
        if (!isPropertyParameter) {
            return CangJieSemanticHighlighting(
                kind = CangJieSemanticHighlightingKind.PARAMETER,
                isMutable = null,
            )
        }

        val mutable = !symbol.isLet
        return CangJieSemanticHighlighting(
            kind = CangJieSemanticHighlightingKind.INSTANCE_PROPERTY,
            additionalKinds = if (mutable) setOf(CangJieSemanticHighlightingKind.MUTABLE_PROPERTY) else emptySet(),
            isMutable = mutable,
        )
    }

    private fun classifyProperty(symbol: CaPropertySymbol): CangJieSemanticHighlighting? {
        if (symbol.location == CaSymbolLocation.LOCAL) {
            val patternVariable = symbol.psi.localPatternVariable()
            val mutable = patternVariable?.isVar ?: (symbol.setter != null || !symbol.isLet)
            return CangJieSemanticHighlighting(
                kind = CangJieSemanticHighlightingKind.LOCAL_VARIABLE,
                additionalKinds = if (mutable) setOf(CangJieSemanticHighlightingKind.MUTABLE_VARIABLE) else emptySet(),
                isMutable = mutable,
            )
        }
        if (symbol.location == CaSymbolLocation.TOP_LEVEL) return null

        val hasCustomAccessor = symbol.getter?.isDefault == false || symbol.setter?.isDefault == false
        val kind = when {
            symbol.location == CaSymbolLocation.EXTEND -> CangJieSemanticHighlightingKind.EXTENSION_PROPERTY
            symbol.isStatic -> if (hasCustomAccessor) {
                CangJieSemanticHighlightingKind.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION
            } else {
                CangJieSemanticHighlightingKind.STATIC_PROPERTY
            }
            hasCustomAccessor -> CangJieSemanticHighlightingKind.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
            else -> CangJieSemanticHighlightingKind.INSTANCE_PROPERTY
        }
        val mutable = symbol.setter != null
        return CangJieSemanticHighlighting(
            kind = kind,
            additionalKinds = if (mutable) setOf(CangJieSemanticHighlightingKind.MUTABLE_PROPERTY) else emptySet(),
            isMutable = mutable,
            isStatic = symbol.isStatic,
        )
    }

    private fun classifyVariable(
        kind: CangJieSemanticHighlightingKind,
        isMutable: Boolean,
        isStatic: Boolean = false,
    ): CangJieSemanticHighlighting = CangJieSemanticHighlighting(
        kind = kind,
        additionalKinds = if (isMutable) setOf(CangJieSemanticHighlightingKind.MUTABLE_VARIABLE) else emptySet(),
        isMutable = isMutable,
        isStatic = isStatic,
    )

    private fun PsiElement?.localPatternVariable(): CjPatternVariable? = when (this) {
        is CjPatternVariable -> this
        is CjBindingPattern -> variable
        is CjTypePattern -> variable
        else -> null
    }

    private fun constructedTypeKind(session: CaSession, symbol: CaConstructorSymbol): CaClassKind? {
        val containingClassKind = with(session) {
            (symbol.containingDeclaration as? CaClassSymbol)?.classKindOrNull
        }
        if (containingClassKind != null) return containingClassKind
        val constructor = symbol.psi as? CjConstructor<*> ?: return null
        return when (constructor.getContainingTypeStatement()) {
            is CjClass -> CaClassKind.CLASS
            is CjStruct -> CaClassKind.STRUCT
            else -> null
        }
    }
}

/** 将共享语义类别投影到 IntelliJ 的 HighlightInfoType。 */
fun CangJieSemanticHighlightingKind.toHighlightInfoType(): HighlightInfoType = when (this) {
    CangJieSemanticHighlightingKind.CLASS -> CangJieHighlightInfoTypeSemanticNames.CLASS
    CangJieSemanticHighlightingKind.ABSTRACT_CLASS -> CangJieHighlightInfoTypeSemanticNames.ABSTRACT_CLASS
    CangJieSemanticHighlightingKind.INTERFACE -> CangJieHighlightInfoTypeSemanticNames.INTERFACE
    CangJieSemanticHighlightingKind.STRUCT -> CangJieHighlightInfoTypeSemanticNames.STRUCT
    CangJieSemanticHighlightingKind.ENUM -> CangJieHighlightInfoTypeSemanticNames.ENUM
    CangJieSemanticHighlightingKind.TYPE_REFERENCE -> CangJieHighlightInfoTypeSemanticNames.TYPE_REFERENCE
    CangJieSemanticHighlightingKind.BASIC_TYPE_REFERENCE -> CangJieHighlightInfoTypeSemanticNames.BASIC_TYPE_REFERENCE
    CangJieSemanticHighlightingKind.TYPE_ALIAS -> CangJieHighlightInfoTypeSemanticNames.TYPE_ALIAS
    CangJieSemanticHighlightingKind.TYPE_PARAMETER -> CangJieHighlightInfoTypeSemanticNames.TYPE_PARAMETER
    CangJieSemanticHighlightingKind.LOCAL_VARIABLE -> CangJieHighlightInfoTypeSemanticNames.LOCAL_VARIABLE
    CangJieSemanticHighlightingKind.MUTABLE_VARIABLE -> CangJieHighlightInfoTypeSemanticNames.MUTABLE_VARIABLE
    CangJieSemanticHighlightingKind.PACKAGE_VARIABLE -> CangJieHighlightInfoTypeSemanticNames.PACKAGE_VARIABLE
    CangJieSemanticHighlightingKind.STATIC_VARIABLE -> CangJieHighlightInfoTypeSemanticNames.STATIC_VARIABLE
    CangJieSemanticHighlightingKind.INSTANCE_VARIABLE -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_VARIABLE
    CangJieSemanticHighlightingKind.PARAMETER -> CangJieHighlightInfoTypeSemanticNames.PARAMETER
    CangJieSemanticHighlightingKind.STATIC_PROPERTY -> CangJieHighlightInfoTypeSemanticNames.STATIC_PROPERTY
    CangJieSemanticHighlightingKind.INSTANCE_PROPERTY -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY
    CangJieSemanticHighlightingKind.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION ->
        CangJieHighlightInfoTypeSemanticNames.INSTANCE_PROPERTY_CUSTOM_PROPERTY_DECLARATION
    CangJieSemanticHighlightingKind.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION ->
        CangJieHighlightInfoTypeSemanticNames.STATIC_PROPERTY_CUSTOM_PROPERTY_DECLARATION

    CangJieSemanticHighlightingKind.EXTENSION_PROPERTY -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_PROPERTY
    CangJieSemanticHighlightingKind.MUTABLE_PROPERTY -> CangJieHighlightInfoTypeSemanticNames.MUTABLE_PROPERTY
    CangJieSemanticHighlightingKind.ENUM_CONSTRUCTOR -> CangJieHighlightInfoTypeSemanticNames.ENUM_CONSTRUCTOR
    CangJieSemanticHighlightingKind.FUNCTION_CALL -> CangJieHighlightInfoTypeSemanticNames.FUNCTION_CALL
    CangJieSemanticHighlightingKind.INSTANCE_FUNCTION_CALL -> CangJieHighlightInfoTypeSemanticNames.INSTANCE_FUNCTION_CALL
    CangJieSemanticHighlightingKind.STATIC_FUNCTION_CALL -> CangJieHighlightInfoTypeSemanticNames.STATIC_FUNCTION_CALL
    CangJieSemanticHighlightingKind.PACKAGE_FUNCTION_CALL -> CangJieHighlightInfoTypeSemanticNames.PACKAGE_FUNCTION_CALL
    CangJieSemanticHighlightingKind.EXTENSION_FUNCTION_CALL -> CangJieHighlightInfoTypeSemanticNames.EXTENSION_FUNCTION_CALL
    CangJieSemanticHighlightingKind.CONSTRUCTOR_CALL,
    -> CangJieHighlightInfoTypeSemanticNames.CONSTRUCTOR_CALL
    CangJieSemanticHighlightingKind.CLASS_CONSTRUCTOR_CALL -> CangJieHighlightInfoTypeSemanticNames.CLASS_CONSTRUCTOR_CALL
    CangJieSemanticHighlightingKind.STRUCT_CONSTRUCTOR_CALL -> CangJieHighlightInfoTypeSemanticNames.STRUCT_CONSTRUCTOR_CALL

    CangJieSemanticHighlightingKind.VARIABLE_AS_FUNCTION_CALL ->
        CangJieHighlightInfoTypeSemanticNames.VARIABLE_AS_FUNCTION_CALL

    CangJieSemanticHighlightingKind.NAMED_ARGUMENT -> CangJieHighlightInfoTypeSemanticNames.NAMED_ARGUMENT
    CangJieSemanticHighlightingKind.ANNOTATION -> CangJieHighlightInfoTypeSemanticNames.ANNOTATION
    CangJieSemanticHighlightingKind.ANNOTATION_ATTRIBUTE_NAME ->
        CangJieHighlightInfoTypeSemanticNames.ANNOTATION_ATTRIBUTE_NAME_ATTRIBUTES
    CangJieSemanticHighlightingKind.MACRO_CALL -> CangJieHighlightInfoTypeSemanticNames.MACRO_CALL
    CangJieSemanticHighlightingKind.MACRO_DECLARATION -> CangJieHighlightInfoTypeSemanticNames.MACRO_DECLARATION
    CangJieSemanticHighlightingKind.MACRO_ESCAPE -> CangJieHighlightInfoTypeSemanticNames.MACRO_ESCAPE
    CangJieSemanticHighlightingKind.SAFE_ACCESS -> CangJieHighlightInfoTypeSemanticNames.SAFE_ACCESS
}
