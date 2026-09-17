package org.cangnova.cangjie.cfir.serialization.cjd

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirAnnotationArgumentMappingImpl
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 已选中二进制库的名字查询边界。只返回 metadata 已确认的类身份，不触发声明物化或 Sema。
 * 同名候选必须全部返回；组织名不应被当作包路径的一部分。
 */
interface CjdAnnotationResolutionContext {
    fun findClassIds(fqName: FqName, organizationName: String?): List<ClassId>

    /**
     * SDK 允许隐式使用的系统注解全名。默认关闭；调用方须由选中库的平台身份显式启用。
     * 仅注册表中的 SYSTEM_MACRO 可生效，且显式导入/同包声明的遮蔽优先。
     */
    val implicitSystemAnnotations: Set<FqName> get() = emptySet()
}

enum class CjdAnnotationDiagnosticKind { UNKNOWN_ANNOTATION, AMBIGUOUS_ANNOTATION, UNSUPPORTED_EXPRESSION, DUPLICATE_ARGUMENT }

/** rawText/range/sourceId 同时用于结构化诊断与错误节点，不能只留下可读消息。 */
data class CjdAnnotationConversionDiagnostic(
    val kind: CjdAnnotationDiagnosticKind,
    val sourceId: String,
    val range: CjdSourceRange,
    val rawText: String,
    override val reason: String,
) : ConeDiagnostic

data class CjdAnnotationConversionResult(
    val annotations: List<CfirAnnotationCall>,
    val diagnostics: List<CjdAnnotationConversionDiagnostic>,
)

/** 只构造注解子树；不注册声明、不执行宏、不运行解析 phase。每次调用返回独立可变 CFIR 节点。 */
interface CjdAnnotationConverter {
    fun convert(
        annotations: List<CjdAnnotation>,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        context: CjdAnnotationContext,
        resolutionContext: CjdAnnotationResolutionContext,
        sourceId: String,
    ): CjdAnnotationConversionResult

    companion object {
        fun create(): CjdAnnotationConverter = SyntaxCjdAnnotationConverter()
    }
}

private class SyntaxCjdAnnotationConverter : CjdAnnotationConverter {
    override fun convert(
        annotations: List<CjdAnnotation>,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        context: CjdAnnotationContext,
        resolutionContext: CjdAnnotationResolutionContext,
        sourceId: String,
    ): CjdAnnotationConversionResult {
        val diagnostics = mutableListOf<CjdAnnotationConversionDiagnostic>()
        val moduleName = BuiltInAnnotationRegistry.sourceModuleName(FqName(context.packageFqName), context.organizationName?.let(Name::identifier))
        val calls = annotations.map { syntax ->
            val builtin = BuiltInAnnotationRegistry.resolveLanguageBuiltIn(syntax.name, syntax.forcedCustom, moduleName)
            val candidates = if (builtin == null) resolve(syntax.name, context, resolutionContext) else emptyList()
            val classId = candidates.singleOrNull()
            val system = classId?.asSingleFqName()?.let(BuiltInAnnotationRegistry::findSystemAnnotation)
            val descriptor: AnnotationDescriptor? = builtin ?: system
            val unresolved = builtin == null && classId == null
            val identityDiagnostic = if (unresolved) CjdAnnotationConversionDiagnostic(
                if (candidates.size > 1) CjdAnnotationDiagnosticKind.AMBIGUOUS_ANNOTATION else CjdAnnotationDiagnosticKind.UNKNOWN_ANNOTATION,
                sourceId, syntax.range, syntax.rawText,
                "Cannot uniquely resolve sidecar annotation '${syntax.name}': $candidates",
            ).also(diagnostics::add) else null
            val source = cjdAnnotationSource(syntax.rawText, syntax.range)
            val cone = classId?.let { ConeClassLikeType(ConeClassLikeLookupTagImpl(it), emptyList()) }
            val expressionConverter = CjdAnnotationExpressionConverter(sourceId, diagnostics)
            val arguments = syntax.arguments.map { argument ->
                val expression = expressionConverter.convert(argument.expression)
                argument.name?.let { explicitName ->
                    buildNamedArgumentExpression {
                        this.source = cjdAnnotationSource(argument.rawText, argument.range)
                        this.expression = expression
                        argumentName = Name.identifier(explicitName)
                    }
                } ?: expression
            }
            val map = linkedMapOf<Name, CfirExpression>()
            val seen = mutableSetOf<Name>()
            val view = arguments.mapIndexed { index, argument ->
                val named = argument as? CfirNamedArgumentExpression
                val expression = named?.expression ?: argument
                val parameterName = named?.argumentName ?: descriptor?.argumentSchema?.positionalParameter?.name?.let(Name::identifier)
                val duplicate = parameterName != null && !seen.add(parameterName)
                if (parameterName != null && !duplicate) map[parameterName] = expression
                if (duplicate) diagnostics += CjdAnnotationConversionDiagnostic(
                    CjdAnnotationDiagnosticKind.DUPLICATE_ARGUMENT, sourceId, syntax.arguments[index].range,
                    syntax.arguments[index].rawText, "Duplicate annotation argument '$parameterName'",
                )
                CfirAnnotationArgumentViewEntry(index, named?.argumentName, argument, null,
                    when {
                        duplicate -> CfirAnnotationArgumentStatus.DUPLICATE
                        expression is CfirErrorExpression -> CfirAnnotationArgumentStatus.ERROR
                        descriptor != null && parameterName != null -> CfirAnnotationArgumentStatus.RESOLVED
                        else -> CfirAnnotationArgumentStatus.UNMAPPED
                    }, false, expression as? CfirLiteralExpression, argument.source)
            }
            buildAnnotationCall {
                this.source = source
                typeRef = when {
                    cone != null -> buildResolvedTypeRef { coneType = cone; customRenderer = false; this.source = source }
                    identityDiagnostic != null -> buildErrorTypeRef { diagnostic = identityDiagnostic; this.source = source }
                    else -> buildImplicitTypeRef { customRenderer = false; this.source = source }
                }
                coneTypeOrNull = cone
                annotationSourceName = syntax.name
                annotationClassId = classId
                annotationKind = builtin?.kind
                forcedCustom = syntax.forcedCustom
                isCompileTimeVisible = syntax.forcedCustom
                sourceModuleName = moduleName
                annotationOrigin = descriptor?.origin ?: CangjieAnnotationOrigin.CUSTOM
                annotationIdentity = when {
                    builtin != null -> CangjieAnnotationIdentity.LanguageBuiltIn(builtin.kind, builtin.sourceName)
                    system != null -> CangjieAnnotationIdentity.SystemMacro(system.classFqName, system.sourceName)
                    classId != null -> CangjieAnnotationIdentity.Custom(classId.asSingleFqName())
                    else -> CangjieAnnotationIdentity.Unknown
                }
                argumentList = buildArgumentList { this.arguments.addAll(arguments) }
                argumentMapping = CfirAnnotationArgumentMappingImpl(source, map)
                argumentView = CfirAnnotationArgumentView(view)
                calleeReference = buildNamedReference { name = Name.identifierIfValid(syntax.name.substringAfterLast('.')) ?: Name.ERROR_NAME }
                this.containingDeclarationSymbol = containingDeclarationSymbol
                annotationResolveState = when {
                    unresolved || view.any { it.status == CfirAnnotationArgumentStatus.ERROR || it.status == CfirAnnotationArgumentStatus.DUPLICATE } -> CfirAnnotationResolveState.ERROR
                    descriptor != null -> CfirAnnotationResolveState.SEMANTIC_RESOLVED
                    else -> CfirAnnotationResolveState.TYPE_RESOLVED
                }
            }
        }
        return CjdAnnotationConversionResult(calls, diagnostics.toList())
    }

    private fun resolve(name: String, context: CjdAnnotationContext, resolution: CjdAnnotationResolutionContext): List<ClassId> {
        fun query(fqName: String, organization: String? = null) = resolution.findClassIds(FqName(fqName), organization)
        val first = name.substringBefore('.')
        val suffix = name.removePrefix(first)
        val explicit = context.imports.filter { !it.isAllUnder && (it.alias ?: it.fqName.substringAfterLast('.')) == first }
        // 未解析的显式导入同样遮蔽隐式 SDK 名称，不能在查询失败后回退。
        if (explicit.isNotEmpty()) return explicit.flatMap { query(it.fqName + suffix, it.organizationName) }.distinct()
        val local = query(listOf(context.packageFqName, name).filter(String::isNotEmpty).joinToString("."), context.organizationName)
        if (local.isNotEmpty() || first in context.declaredNames) return local.distinct()
        if ('.' in name) return query(name).distinct()
        val stars = context.imports.filter { it.isAllUnder }.flatMap { query("${it.fqName}.$name", it.organizationName) }.distinct()
        if (stars.isNotEmpty()) return stars
        // 这里是平台明确授权的预导入元数据，不是依据短名虚构任意 ClassId。
        return resolution.implicitSystemAnnotations.mapNotNull { fqName ->
            BuiltInAnnotationRegistry.findSystemAnnotation(fqName)
                ?.takeIf { it.sourceName == name && it.origin == CangjieAnnotationOrigin.SYSTEM_MACRO }
                ?.let { ClassId.topLevel(fqName) }
        }.distinct()
    }
}
