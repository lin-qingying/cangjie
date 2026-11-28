package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageFeatureSupportStatus
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticArgument
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticLabel
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpImplementationKind
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpRelatedInformationMessage
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpAnnotationPresentation
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpSide
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.cfir.CfirStringLiteralValueNormalizer
import org.cangnova.cangjie.featureSupportStatus
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.cjmpUnaryMinusOperand
import org.cangnova.cangjie.cfir.patterns.primaryBindingNameOrNull
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.diagnostics.rendering.ContextDependentRenderer
import org.cangnova.cangjie.cfir.diagnostics.rendering.Renderer
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.render.renderNativeTypeString
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.name.Name

/**
 * CFIR 诊断参数渲染器集合。
 */
object CfirDiagnosticRenderers {
    /** 正常存储类型 note 专用，不改主诊断与调试输出的既有 renderer。 */
    fun renderStoredType(type: ConeCangJieType): String = renderNativeTypeString(type)

    /** 先按实际类型排除 child，再稳定渲染；同名文本的不同类型不被去重。 */
    fun renderStoredTypeConflictSet(types: List<ConeCangJieType>, child: ConeCangJieType,
        equalTypes: (ConeCangJieType, ConeCangJieType) -> Boolean): String =
        types.filterNot { equalTypes(it, child) }.map(::renderStoredType).sorted()
            .joinToString("', '", prefix = "'", postfix = "'")

    /**
     * 按 Kotlin `LanguageFeatureMessageRenderer` 的职责渲染完整版本原因。
     *
     * 诊断载荷必须保留 settings：仅渲染 feature 名称会把"语言版本过低、
     * 显式关闭、实验特性未开启"全部错误地压成同一条消息。
    */
    val LANGUAGE_FEATURE_SUPPORT = Renderer<Pair<LanguageFeature, LanguageVersionSettings>> { (feature, settings) ->
        val sinceVersion = feature.sinceVersion
        val supportStatus = settings.featureSupportStatus(feature)
        val reason = when {
            feature.testOnly -> "unsupported."
            sinceVersion == null ->
                "experimental and is not enabled by the current language settings; no stability guarantees are provided."
            supportStatus == LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION ->
                "only available since language version ${sinceVersion.versionString}"
            supportStatus == LanguageFeatureSupportStatus.DISABLED -> "disabled."
            else -> "not supported by the current language settings"
        }
        buildString {
            append("The feature \"")
            append(feature.presentableName)
            append("\" is ")
            append(reason)
            feature.hintUrl?.let { append(" (see: ").append(it).append(')') }
        }
    }


    /**
     * 渲染单个 cone 类型。
     */
    val RENDER_TYPE = ContextDependentRenderer { type: ConeCangJieType, _ ->
        type.toString()
    }

    /**
     * 渲染 cone 类型集合。
     */
    val RENDER_TYPE_LIST = ContextDependentRenderer { types: Collection<ConeCangJieType>, context ->
        types.joinToString(", ") { RENDER_TYPE.render(it, context) }
    }

    /**
     * 渲染 CFIR 符号的声明名。
     */
    val DECLARATION_NAME = Renderer { symbol: CfirBasedSymbol<*> ->
        when (symbol) {
            is CfirCallableSymbol<*> -> symbol.name.asString()
            is CfirClassLikeSymbol<*> -> symbol.classId.shortClassName.asString()
            is CfirTypeParameterSymbol -> symbol.name.asString()
            else -> return@Renderer "???"
        }
    }

    /**
     * 渲染 CFIR 符号声明名集合。
     */
    val DECLARATION_NAME_LIST = ContextDependentRenderer { symbols: Collection<CfirBasedSymbol<*>>, context ->
        symbols.joinToString(", ") { DECLARATION_NAME.render(it, context) }
    }

    /** 仅在最终诊断渲染阶段把结构化 CJMP 语义实参转换为消息文本。 */
    val CJMP_DIAGNOSTIC_ARGUMENT = Renderer<CfirCjmpDiagnosticArgument> { argument ->
        when (argument) {
            is CfirCjmpDiagnosticArgument.DeclarationKind -> argument.kind.renderText()
            is CfirCjmpDiagnosticArgument.DeclarationName -> argument.declaration.renderCjmpName()
            is CfirCjmpDiagnosticArgument.VariableKind -> if (argument.isVar) "var" else "let"
            is CfirCjmpDiagnosticArgument.Side -> when (argument.side) {
                CfirCjmpSide.COMMON -> "common"
                CfirCjmpSide.SPECIFIC -> "specific"
            }
            is CfirCjmpDiagnosticArgument.Name -> argument.name.asString()
            is CfirCjmpDiagnosticArgument.ContainerName -> argument.name?.asString() ?: ""
            is CfirCjmpDiagnosticArgument.NotMatchedDeclaration ->
                argument.declaration.renderNotMatchedInfo(argument.outerDeclaration)
            is CfirCjmpDiagnosticArgument.ImplementationKind -> argument.kind.renderText()
            is CfirCjmpDiagnosticArgument.ExtendedType -> argument.declaration.renderDuplicateExtendedType()
            is CfirCjmpDiagnosticArgument.Label -> argument.label.renderText()
        }
    }

    /** 在 renderer 层生成 CJMP related-information 文案及注解表达式文本。 */
    val CJMP_RELATED_INFORMATION = Renderer<CfirCjmpRelatedInformationMessage> { message ->
        when (message) {
            CfirCjmpRelatedInformationMessage.DeprecatedAnnotationInheritedFromCommon ->
                "specific declarations implicitly inherit deprecation from the common declarations"
            is CfirCjmpRelatedInformationMessage.MismatchedAnnotation ->
                "The mismatched annotation is ${message.annotation.renderCjmpAnnotation()}"
            is CfirCjmpRelatedInformationMessage.MismatchedAnnotationAttribute -> {
                val annotationText = message.annotation?.renderCjmpAnnotation()
                    ?: "@${requireNotNull(BuiltInAnnotationRegistry.findLanguageBuiltIn(message.kind)?.sourceName)}"
                "The mismatched annotation is $annotationText"
            }
            CfirCjmpRelatedInformationMessage.CorrespondingCommonDeclaration ->
                "The corresponding common declaration"
            CfirCjmpRelatedInformationMessage.AnnotationCorrectionAdvice ->
                "Either remove the specific annotation or add it to the common declaration"
        }
    }
}

private fun CfirCjmpAnnotationPresentation.renderCjmpAnnotation(): String {
    val call = annotation as? CfirAnnotationCall
    val explicitArguments = call?.let(CfirAnnotationCall::explicitCjmpDiagnosticArguments).orEmpty()
    return "@$sourceName" + explicitArguments.takeIf { it.isNotEmpty() }
        ?.joinToString(prefix = "(", postfix = ")", separator = ", ").orEmpty()
}

private fun CfirAnnotationCall.explicitCjmpDiagnosticArguments(): List<String> =
    argumentList.arguments.map { argument ->
        val namedArgument = argument as? CfirNamedArgumentExpression
        val expression = namedArgument?.expression ?: argument
        expression.cjmpArgumentDiagnosticText(
            explicitName = namedArgument?.argumentName,
            sourceArgumentText = argument.source?.text?.toString(),
        )
    }

private fun CfirExpression.cjmpArgumentDiagnosticText(
    explicitName: Name?,
    sourceArgumentText: String?,
): String {
    val sourceExpressionText = source?.text?.toString()
    val literal = this as? CfirLiteralExpression
    if (literal == null) {
        sourceArgumentText?.let { return it }
        sourceExpressionText?.let { return it }
        val unaryMinusOperand = (this as? CfirFunctionCall)?.cjmpUnaryMinusOperand()
        if (unaryMinusOperand is CfirLiteralExpression) {
            val nameAndColon = explicitName?.let { "${it.asString()}: " }.orEmpty()
            return "$nameAndColon-${unaryMinusOperand.renderCjmpLiteral()}"
        }
        error("A non-literal CJMP annotation argument must retain its source text")
    }

    val nameAndColon = if (explicitName == null) {
        ""
    } else {
        val sourcePrefix = if (sourceArgumentText != null && sourceExpressionText != null &&
            sourceArgumentText.endsWith(sourceExpressionText)
        ) {
            sourceArgumentText.dropLast(sourceExpressionText.length)
        } else {
            null
        }
        sourcePrefix ?: "${explicitName.asString()}:"
    }
    return nameAndColon + literal.renderCjmpLiteral()
}

private fun CfirLiteralExpression.renderCjmpLiteral(): String = when (kind) {
    org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.STRING -> {
        val value = source?.text?.toString()?.let(CfirStringLiteralValueNormalizer::normalizeSourceLiteral)
            ?: value as? String
            ?: error("A string CJMP annotation argument must retain its normalized value")
        "\"$value\""
    }

    org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.RUNE -> {
        source?.text?.toString()?.let(CfirStringLiteralValueNormalizer::normalizeRuneSourceLiteral)
            ?: value?.toString()
            ?: error("A rune CJMP annotation argument must retain its normalized value")
    }

    org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.BOOLEAN -> value.toString()

    org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.BYTE,
    org.cangnova.cangjie.cfir.expressions.CfirLiteralKind.UNIT,
    -> source?.text?.toString() ?: value?.toString().orEmpty()

    else -> value?.toString() ?: source?.text?.toString().orEmpty()
}

private fun CfirCjmpDiagnosticLabel.renderText(): String = when (this) {
    CfirCjmpDiagnosticLabel.CLASS -> "class"
    CfirCjmpDiagnosticLabel.STRUCT -> "struct"
    CfirCjmpDiagnosticLabel.INTERFACE -> "interface"
    CfirCjmpDiagnosticLabel.ENUM -> "enum"
    CfirCjmpDiagnosticLabel.EXTEND -> "extend"
    CfirCjmpDiagnosticLabel.PROPERTY -> "property"
    CfirCjmpDiagnosticLabel.FUNCTION -> "function"
    CfirCjmpDiagnosticLabel.VARIABLE -> "variable"
    CfirCjmpDiagnosticLabel.TYPE_ALIAS -> "type alias"
    CfirCjmpDiagnosticLabel.FIELD -> "field"
    CfirCjmpDiagnosticLabel.LET -> "let"
    CfirCjmpDiagnosticLabel.VAR -> "var"
    CfirCjmpDiagnosticLabel.OPEN_ABSTRACT -> "open/abstract"
}

private fun CfirCjmpImplementationKind.renderText(): String = when (this) {
    is CfirCjmpImplementationKind.Declaration -> kind.renderText()
    is CfirCjmpImplementationKind.Property -> "property ${name.asString()}"
    is CfirCjmpImplementationKind.EnumConstructor -> "enum '${enumName.asString()}' constructor"
}

private fun CfirDeclaration.renderNotMatchedInfo(outerDeclaration: CfirDeclaration?): String = when (this) {
    is CfirConstructor -> "constructor"
    is CfirEnumConstructor -> {
        val enumName = (outerDeclaration as? CfirEnum)?.name
            ?: checkNotNull(symbol.callableId.classId?.shortClassName) {
                "An enum constructor in NOT_MATCHED must retain its enum owner"
            }
        "enum '${enumName.asString()}' constructor '${name.asString()}'"
    }
    is CfirPatternVariable -> "variable with pattern '${renderCjmpName()}'"
    is CfirExtend -> {
        val typeRef = extendedTypeRef
        val typeText = when (typeRef) {
            is CfirErrorTypeRef -> checkNotNull(typeRef.source.text?.toString()) {
                "An unresolved NOT_MATCHED extend must retain its source type text"
            }
            is CfirResolvedTypeRef -> typeRef.coneType.toString()
            else -> checkNotNull(typeRef.source.text?.toString()) {
                "An unresolved NOT_MATCHED extend must retain its source type text"
            }
        }
        "extend '$typeText'"
    }
    else -> "${renderCjmpKind().renderText()} '${renderCjmpName()}'"
}

private fun CfirDeclaration.renderCjmpKind(): CfirCjmpDiagnosticLabel = when (this) {
    is CfirClass -> CfirCjmpDiagnosticLabel.CLASS
    is CfirStruct -> CfirCjmpDiagnosticLabel.STRUCT
    is CfirInterface -> CfirCjmpDiagnosticLabel.INTERFACE
    is CfirEnum -> CfirCjmpDiagnosticLabel.ENUM
    is CfirExtend -> CfirCjmpDiagnosticLabel.EXTEND
    is CfirProperty -> CfirCjmpDiagnosticLabel.PROPERTY
    is CfirFunction -> CfirCjmpDiagnosticLabel.FUNCTION
    is CfirEnumConstructor ->
        if (valueParameters.isEmpty()) CfirCjmpDiagnosticLabel.VARIABLE else CfirCjmpDiagnosticLabel.FUNCTION
    is CfirVariable -> CfirCjmpDiagnosticLabel.VARIABLE
    is CfirTypeAlias -> CfirCjmpDiagnosticLabel.TYPE_ALIAS
    else -> error("No CJMP diagnostic kind is defined for ${this::class.qualifiedName}")
}

private fun CfirDeclaration.renderCjmpName(): String = when (this) {
    is CfirClassLikeDeclaration -> name.asString()
    is CfirEnumConstructor -> name.asString()
    is CfirExtend -> (extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.let { type ->
        val classLikeType = type as? org.cangnova.cangjie.cfir.types.ConeClassLikeType
        classLikeType?.classId?.shortClassName?.asString() ?: type.toString()
    } ?: "<extend>"
    is CfirPatternVariable -> pattern.primaryBindingNameOrNull()?.asString() ?: symbol.name.asString()
    is CfirCallableDeclaration -> symbol.name.asString()
    else -> error("No CJMP diagnostic name is defined for ${this::class.qualifiedName}")
}

private fun CfirExtend.renderDuplicateExtendedType(): String = checkNotNull(extendedTypeRef.source.text?.toString()) {
    "A duplicate specific extension must retain its source type text"
}
