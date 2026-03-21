package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.CandidateApplicability
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic


/**
 * Base type for unresolved diagnostics.
 */
sealed interface ConeUnresolvedError : ConeDiagnostic

data class ConeUnresolvedReferenceError(
    val name: Name,
) : ConeUnresolvedError {
    override val reason: String = "unresolved reference: ${name.asString()}"
}

data class ConeUnresolvedSymbolError(
    val classId: ClassId,
) : ConeUnresolvedError {
    override val reason: String = "unresolved symbol: ${classId.asString()}"
}

interface ConeDiagnosticWithCandidates : ConeDiagnostic {
    val candidates: Collection<AbstractCandidate>
    val candidateSymbols: Collection<CfirSymbol<*>> get() = candidates.map { it.symbol }
}

class ConeAmbiguityError(
    val name: Name,
    val applicability: CandidateApplicability,
    val candidatesWithErrors: Map<out AbstractCandidate, ConeDiagnostic?>
) : ConeDiagnosticWithCandidates {
    override val reason: String get() = "Ambiguity: $name, ${candidateSymbols.map { describeSymbol(it) }}"
    override val candidates: Collection<AbstractCandidate> get() = candidatesWithErrors.keys
}

interface ConeDiagnosticWithSingleCandidate : ConeDiagnosticWithCandidates {
    val candidate: AbstractCallCandidate<*>
    val candidateSymbol: CfirSymbol<*> get() = candidate.symbol
    override val candidates: Collection<AbstractCallCandidate<*>> get() = listOf(candidate)
    override val candidateSymbols: Collection<CfirSymbol<*>> get() = listOf(candidateSymbol)
}

class ConeInapplicableCandidateError(
    val applicability: CandidateApplicability,
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "Inapplicable($applicability): ${describeSymbol(candidateSymbol)}"
}

data class ConeUnresolvedNameError(
    val name: Name,
    val operator: String? = null,
    val receiverType: ConeCangJieType? = null,
) : ConeUnresolvedError {
    override val reason: String = buildString {
        append("unresolved name: ")
        append(name.asString())
        if (operator != null) {
            append(", operator=")
            append(operator)
        }
        if (receiverType != null) {
            append(", receiver=")
            append(receiverType)
        }
    }
}

private fun describeSymbol(symbol: CfirSymbol<*>): String {
    return when (symbol) {
        is CfirClassLikeSymbol<*> -> symbol.classId.asString()
        is CfirCallableSymbol<*> -> symbol.callableIdAsString()
        else -> "$symbol"
    }
}

/**
 * 通用诊断，用于无法归类到具体诊断类型的错误。
 * 对齐 K2 `ConeSimpleDiagnostic`。
 */
/**
 * Mirrors Kotlin FIR `ConeCannotInferTypeParameterType` without symbol-layer dependency.
 */

class ConeCannotInferTypeParameterType(
    val typeParameter: CfirTypeParameterSymbol,
    override val reason: String = "Cannot infer type for parameter ${typeParameter.name}"
) : ConeCannotInferType() {
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for type parameter ${typeParameter.name}"
}

abstract class ConeCannotInferType : ConeDiagnostic

class ConeSimpleDiagnostic(override val reason: String, val kind: DiagnosticKind = DiagnosticKind.Other) :
    ConeDiagnostic

/**
 * 诊断分类，对齐 K2 `DiagnosticKind`。
 */
enum class DiagnosticKind {
    IllegalConstExpression,
    DeserializationError,
    InferenceError,
    RecursionInImplicitTypes,
    ReturnNotAllowed,
    UnresolvedSupertype,
    CannotInferParameterType,
    EnumInitializerError,
    Other,
}
