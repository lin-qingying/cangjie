package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory2
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory3
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.expressions.CfirLambdaExpression
import org.cangnova.cangjie.cfir.resovle.calls.TypeVariableReplacement
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeTypeContext

import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjSourceElement

fun ConeDiagnostic.toCfirDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    valueParameter: CfirValueParameter? = null,
): List<CjDiagnostic> {
    return when (this) {
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, source, callOrAssignmentSource)
        is ConeAmbiguityError -> mapConeAmbiguityError(source, callOrAssignmentSource, session)
        else -> listOfNotNull(mapOtherDiagnostic(source, valueParameter, callOrAssignmentSource, session))
    }
}

// region mapInapplicableCandidateError — 对齐 K2 coneDiagnosticToFirDiagnostic.kt

private fun ConeInapplicableCandidateError.mapInapplicableCandidateError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    val genericDiagnostic = source?.let {
        CfirErrors.UNRESOLVED_REFERENCE.on(it, candidateSymbol.debugName, null, session)
    }

    val diagnostics = candidate.diagnostics.filter { !it.isSuccess }.mapNotNull { rootCause ->
        when (rootCause) {
            is ArgumentTypeMismatch -> {
                val expectedType = rootCause.expectedType.substituteTypeVariableTypes(candidate)
                val actualType = if (rootCause.argument is CfirLambdaExpression && rootCause.argument.coneTypeOrNull?.isError == false) {
                    rootCause.argument.coneTypeOrNull!!
                } else {
                    rootCause.actualType.substituteTypeVariableTypes(candidate)
                }
                argumentTypeMismatch(
                    source = rootCause.argument.source ?: source,
                    expectedType = expectedType,
                    actualType = actualType,
                    isMismatchDueToNullability = rootCause.isMismatchDueToNullability,
                    anonymousFunction = rootCause.anonymousFunctionIfReturnExpression,
                    session = session,
                )
            }
            else -> genericDiagnostic
        }
    }

    if (diagnostics.isNotEmpty()) return diagnostics

    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session))
}

// endregion

// region argumentTypeMismatch — 对齐 K2 第 752-791 行

/**
 * 处理参数类型不匹配。
 * - 若存在 anonymousFunction（lambda 返回表达式），报告 RETURN_TYPE_MISMATCH
 * - 否则报告 ARGUMENT_TYPE_MISMATCH
 */
private fun argumentTypeMismatch(
    source: CjSourceElement?,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    isMismatchDueToNullability: Boolean,
    anonymousFunction: CfirFunction?,
    session: CfirSession,
): CjDiagnostic? {
    if (source == null) return null

    if (anonymousFunction != null) {
        return CfirErrors.RETURN_TYPE_MISMATCH.on(
            source, expectedType, actualType, isMismatchDueToNullability, session,
        )
    }

    return CfirErrors.ARGUMENT_TYPE_MISMATCH.on(
        source, expectedType, actualType, isMismatchDueToNullability, session,
    )
}

// endregion

// region substituteTypeVariableTypes — 对齐 K2 第 957-965 行

/**
 * 通过约束系统替换类型变量为已固定的类型，残留未固定的类型变量替换为 ConeErrorType。
 */
private fun ConeCangJieType.substituteTypeVariableTypes(
    candidate: AbstractCallCandidate<*>,
    typeContext: ConeTypeContext,
): ConeCangJieType {
    val nonErrorSubstitutionMap = candidate.system.asReadOnlyStorage().fixedTypeVariables.filterValues { it !is ConeErrorType }
    val substitutor = typeContext.typeSubstitutorByTypeConstructor(nonErrorSubstitutionMap).asCone()

    return substitutor.substituteOrSelf(this).removeTypeVariableTypes(typeContext, TypeVariableReplacement.ErrorType)
}


// endregion

// region mapConeAmbiguityError

private fun ConeAmbiguityError.mapConeAmbiguityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    val referenceName = name.asString()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, referenceName, null, session))
}

// endregion

// region mapOtherDiagnostic

private fun ConeDiagnostic.mapOtherDiagnostic(
    source: CjSourceElement?,
    valueParameter: CfirValueParameter?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return null
    return when (this) {
        is ConeCannotInferTypeParameterType ->
            CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, typeParameter, session)

        is ConeSimpleDiagnostic -> when (kind) {
            DiagnosticKind.CannotInferParameterType -> {
                valueParameter
                    ?.typeParameters
                    ?.firstOrNull()
                    ?.symbol
                    ?.let { it as? CfirTypeParameterSymbol }
                    ?.let { CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, it, session) }
            }

            else -> null
        }
        is ConeUnresolvedNameError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, name.asString(), operator, session)
        is ConeUnresolvedReferenceError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, name.asString(), null, session)
        is ConeUnresolvedSymbolError -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, classId.asString(), null, session)
        else -> null
    }
}

// endregion

// region 诊断工厂扩展

private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        override val languageVersionSettings = session.languageVersionSettings
        override val containingFilePath: String? = null
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> CjDiagnosticFactory1<A>.on(
    source: AbstractCjSourceElement,
    a: A,
    session: CfirSession,
): CjDiagnostic? = on(source, a, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.on(
    source: AbstractCjSourceElement,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))

@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C> CjDiagnosticFactory3<A, B, C>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    c: C,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, c, null, diagnosticContext(session))

// endregion
