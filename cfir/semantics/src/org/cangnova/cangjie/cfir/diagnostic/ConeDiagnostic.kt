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

class ConeConstraintSystemHasContradiction(
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    override val reason: String
        get() = "CS errors: ${
             describeSymbol(
                candidateSymbol
            )
        }"
    override val candidateSymbol:CfirSymbol<*> get() = candidate.symbol
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
    val argumentTypes: List<ConeCangJieType> = emptyList(),
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
        if (argumentTypes.isNotEmpty()) {
            append(", arguments=")
            append(argumentTypes.joinToString(prefix = "[", postfix = "]"))
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

/**
 * enum 类型名不能像 class / struct 那样直接作为类型构造器调用。
 *
 * 官方 C++ Sema 在 call-kind 划分里也把 enum constructor 与普通 type constructor
 * 明确区分开来；这里单独建模，避免把 `A(1)` 这类错误继续混成普通无构造器调用。
 */
data class ConeEnumTypeCannotBeUsedAsConstructorError(
    val enumName: Name,
) : ConeDiagnostic {
    override val reason: String =
        "enum type '${enumName.asString()}' cannot be used as a type constructor; use an enum constructor instead"
}

/**
 * effects 特性在 PSI 层始终建树，但是否允许进入语义阶段由 CFIR 控制。
 * 因此这里单独建模 feature gate 诊断，避免把 effect 语法再次退回 parser 层。
 */
data class ConeEffectsFeatureDisabledError(
    val constructName: String,
) : ConeDiagnostic {
    override val reason: String = "effects feature is disabled for '$constructName'"
}

data class ConeCommandIncompatibleTypeError(
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    override val reason: String =
        "performed expression must implement 'stdx.effect.Command<T>', actual type is '${actualType ?: "<unknown>"}'"
}

data class ConeCommandHandleTypeError(
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    override val reason: String =
        "the command handle type must implement 'stdx.effect.Command<T>', actual type is '${actualType ?: "<unknown>"}'"
}

object ConeImplicitResumeOutsideHandlerError : ConeDiagnostic {
    override val reason: String = "'resume' outside of an immediate handler must have a resumption argument"
}

data class ConeResumeNoWithError(
    val resumptionType: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String =
        "a resumption of non-Unit type '$resumptionType' must have a 'with' or 'throwing' clause"
}

data class ConeResumeThrowingMismatchTypeError(
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    override val reason: String =
        "the type of 'resume throwing' must be a subtype of std.core.Exception or std.core.Error, actual type is '${actualType ?: "<unknown>"}'"
}

data class ConeMismatchingHandleBlockError(
    val actualType: ConeCangJieType,
    val expectedType: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String =
        "the type of this handle block is '$actualType', which mismatches the smallest common supertype '$expectedType' of previous branches"
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



class ConeTypeParameterInQualifiedAccess(val symbol: CfirTypeParameterSymbol) : ConeDiagnostic {
    override val reason: String get() = "Type parameter ${symbol.cfir.name} in qualified access"
}

/**
 * 变量已解析但其类型上没有匹配的 invoke 操作符。
 * 例如 `a()` 中 a 是类型 C 的变量，但 C 未定义 `operator func ()()`。
 */
data class ConeNoMatchingInvokeOperatorError(
    val name: Name,
    val receiverType: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String get() = "no matching operator '()' for type $receiverType"
}
