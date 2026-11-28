package org.cangnova.cangjie.analysis.api.cfir.components.resolver

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CJ_DIAGNOSTIC_CONVERTER
import org.cangnova.cangjie.analysis.api.cfir.references.getCaCandidateSymbols
import org.cangnova.cangjie.analysis.api.cfir.references.toCaTargetSymbol
import org.cangnova.cangjie.analysis.api.cfir.references.toCaTargetSymbolOrNull
import org.cangnova.cangjie.analysis.api.cfir.references.toCaTargetSymbols
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.diagnostics.CaSeverity
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSymbolResolutionError
import org.cangnova.cangjie.analysis.api.impl.base.resolution.CaBaseSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaCall
import org.cangnova.cangjie.analysis.api.resolution.CaErrorCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaFunctionCall
import org.cangnova.cangjie.analysis.api.resolution.CaSuccessCallInfo
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionAttempt
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.toCfirDiagnostics
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCallElement
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjNameReferenceExpression
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.psi.CjValueArgumentList
import org.cangnova.cangjie.psi.CjValueArgumentName
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 将 Analysis API 元素解析为符号结果；命名实参标签通过调用的真实参数映射解析。
 */
internal fun CaCfirSession.resolveSymbol(element: CjElement): CaSymbolResolutionAttempt? {
    if (element is CjNameReferenceExpression && element.parent is CjValueArgumentName) {
        return resolveValueArgumentName(element)
    }

    val cfir = element.getOrBuildCfir(resolutionFacade) ?: return null
    return cfir.toCaSymbolResolutionAttempt(this)
}

/**
 * 按拥有该标签的调用取得参数映射，避免把参数名当作普通表达式引用解析。
 */
private fun CaCfirSession.resolveValueArgumentName(expression: CjNameReferenceExpression): CaSymbolResolutionAttempt? {
    val valueArgumentName = expression.parent as? CjValueArgumentName ?: return null
    val valueArgument = valueArgumentName.parent as? CjValueArgument ?: return null
    val argumentList = valueArgument.parent as? CjValueArgumentList ?: return null
    val callElement = argumentList.parent as? CjCallElement ?: return null

    return when (callElement) {
        is CjCallExpression -> {
            val argumentExpression = valueArgument.getArgumentExpression() ?: return null
            val callInfo = with(this) { callElement.resolveToCall() } ?: return null
            when (callInfo) {
                is CaSuccessCallInfo -> callInfo.call.parameterForArgument(argumentExpression)
                    ?.let(::CaBaseSymbolResolutionSuccess)

                is CaErrorCallInfo -> {
                    val candidateParameters = callInfo.candidateCalls
                        .mapNotNull { candidate -> candidate.parameterForArgument(argumentExpression) }
                        .distinctBy { parameter -> parameter.psi ?: parameter }
                    if (candidateParameters.isEmpty()) {
                        null
                    } else {
                        CaBaseSymbolResolutionError(callInfo.diagnostic, candidateParameters)
                    }
                }
            }
        }

        is CjAnnotation -> resolveAnnotationValueArgument(callElement, valueArgument, valueArgumentName)
        else -> null
    }
}

/**
 * 自定义注解沿用其已发布的 CFIR 参数视图，目标必须来自当前源码实参对应的真实形参。
 */
private fun CaCfirSession.resolveAnnotationValueArgument(
    annotation: CjAnnotation,
    valueArgument: CjValueArgument,
    valueArgumentName: CjValueArgumentName,
): CaSymbolResolutionAttempt? {
    val annotationCall = resolveAnnotationCall(annotation) ?: return null
    val argument = annotationCall.argumentView?.entries?.singleOrNull { entry ->
        entry.source?.psi === valueArgument.asElement()
    } ?: return null
    if (argument.explicitName != valueArgumentName.asName) return null

    val parameter = argument.resolvedParameter?.takeIf { it.isNamed } ?: return null
    val symbol = cfirSymbolBuilder.buildSymbol(parameter.symbol) as? CaValueParameterSymbol ?: return null
    return CaBaseSymbolResolutionSuccess(symbol)
}

/**
 * 只返回真实命名形参。内置指针与 C 函数构造器的错误恢复映射可能指向合成非命名形参。
 */
private fun CaCall.parameterForArgument(argumentExpression: CjExpression): CaValueParameterSymbol? =
    (this as? CaFunctionCall<*>)
        ?.valueArgumentMapping
        ?.get(argumentExpression)
        ?.symbol
        ?.takeIf { it.isNamed }

/**
 * 将 CFIR 元素投影为 Analysis API 符号结果。
 */
private fun CfirElement.toCaSymbolResolutionAttempt(analysisSession: CaCfirSession): CaSymbolResolutionAttempt? = when (this) {
    is CfirDiagnosticHolder -> toCaSymbolResolutionError(analysisSession)
    is org.cangnova.cangjie.cfir.declarations.CfirDeclaration ->
        analysisSession.cfirSymbolBuilder.buildSymbol(symbol).toSymbolResolutionSuccessOrNull()
    is org.cangnova.cangjie.cfir.patterns.CfirEnumPattern ->
        constructorReference?.toCaSymbolResolutionAttempt(analysisSession)
    is CfirAssignment -> lValue.toCaSymbolResolutionAttempt(analysisSession)
    is CfirResolvable -> calleeReference.toCaSymbolResolutionAttempt(analysisSession)
    is CfirReference -> toCaTargetSymbols(analysisSession.cfirSymbolBuilder).toSymbolResolutionSuccessOrNull()
    is CfirResolvedTypeRef -> toCaTargetSymbol(analysisSession, analysisSession.cfirSymbolBuilder)
        ?.toSymbolResolutionSuccessOrNull()

    else -> null
}

/**
 * 将携带诊断的 CFIR 节点投影为失败尝试，并保留实际候选符号。
 */
private fun CfirDiagnosticHolder.toCaSymbolResolutionError(
    analysisSession: CaCfirSession,
): CaSymbolResolutionAttempt {
    val cfirDiagnostic = diagnostic.toCfirDiagnostics(
        session = analysisSession.cfirSession,
        source = source,
        callOrAssignmentSource = null,
    ).firstOrNull()
    val caDiagnostic = cfirDiagnostic?.let { CJ_DIAGNOSTIC_CONVERTER.convert(analysisSession, it) }
        ?: CaCfirSymbolResolutionDiagnostic(diagnostic, analysisSession.token)
    val candidates = diagnostic.getCaCandidateSymbols()
        .mapNotNull { it.toCaTargetSymbolOrNull(analysisSession.cfirSymbolBuilder) }
    return CaBaseSymbolResolutionError(caDiagnostic, candidates)
}

/** 非空符号集合投影为成功尝试。 */
private fun Collection<CaSymbol>.toSymbolResolutionSuccessOrNull(): CaSymbolResolutionSuccess? =
    takeIf { it.isNotEmpty() }?.toList()?.let { symbols ->
        CaBaseSymbolResolutionSuccess(backingSymbols = symbols, token = symbols.first().token)
    }

/** 单个符号投影为成功尝试。 */
private fun CaSymbol.toSymbolResolutionSuccessOrNull(): CaSymbolResolutionSuccess =
    CaBaseSymbolResolutionSuccess(this)

/** CFIR 符号解析没有可映射的前端诊断时使用稳定 Analysis API 诊断。 */
private class CaCfirSymbolResolutionDiagnostic(
    private val coneDiagnostic: ConeDiagnostic,
    override val token: CaLifetimeToken,
) : CaDiagnostic {
    override val diagnosticClass: kotlin.reflect.KClass<*> get() = CaDiagnostic::class
    override val factoryName: String get() = "SYMBOL_RESOLUTION_ERROR"
    override val severity: CaSeverity get() = CaSeverity.ERROR
    override val defaultMessage: String get() = withValidityAssertion { coneDiagnostic.reason }
}

/**
 * 从声明的共享 annotation container 中恢复与 PSI 注解对应的 CFIR 调用。
 */
internal fun CaCfirSession.resolveAnnotationCall(annotation: CjAnnotation): CfirAnnotationCall? {
    val declaration = annotation.getStrictParentOfType<CjDeclaration>() ?: return null
    val annotationContainer = declaration.getOrBuildCfir(resolutionFacade) as? CfirAnnotationContainer ?: return null
    return annotationContainer.annotations
        .filterIsInstance<CfirAnnotationCall>()
        .firstOrNull { it.psi === annotation }
}
