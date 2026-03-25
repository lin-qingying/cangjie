package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability


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

class ConeHiddenCandidateError(
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String get() = "Hidden candidate: ${describeSymbol(candidateSymbol)}"
}

class ConeVisibilityError(
    val symbol: CfirSymbol<*>,
) : ConeDiagnostic {
    override val reason: String get() = "Cannot access: ${describeSymbol(symbol)}"
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

// 函数调用期望错误：一个变量被当作函数调用，但实际是变量访问
data class ConeFunctionCallExpectedError(
    val name: Name,
    val hasValueParameters: Boolean,
    override val candidates: Collection<AbstractCallCandidate<*>>,
) : ConeDiagnosticWithCandidates {
    override val reason: String
        get() = "Function call expected: $name(${if (hasValueParameters) "..." else ""})"
}

// 函数期望错误：某个表达式不是函数类型，但被当作函数调用
data class ConeFunctionExpectedError(
    val expressionName: String,
    val type: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String = "Expression '$expressionName' of type '$type' cannot be invoked as a function"
}

// 解析到分类器（类/接口等）的错误
data class ConeResolutionToClassifierError(
    override val candidate: AbstractCallCandidate<*>,
    val classifier: CfirClassLikeSymbol<*>,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String = "Resolution to classifier: ${describeSymbol(classifier)}"
}

object ConeNoConstructorError : ConeDiagnostic {
    override val reason: String = "No constructor found"
}

object ConeNoImplicitDefaultConstructorOnExpectClass : ConeDiagnostic {
    override val reason: String = "No implicit default constructor on expect-like declaration"
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

class ConeCannotInferValueParameterType(
    val valueParameter: CfirValueParameterSymbol?,
    reason: String? = null,
    val isTopLevelLambda: Boolean = false,
) : ConeCannotInferType() {
    private val _reason: String? = reason
    override val reason: String
        get() = _reason
            ?: ("Cannot infer type for parameter " + (valueParameter?.let { "${it.name}" } ?: "it"))
}



