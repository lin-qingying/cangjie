package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability


/**
 * Base type for unresolved diagnostics.
 */
sealed interface ConeUnresolvedError : ConeDiagnostic

/**
 * 类型限定名逐段解析失败。
 *
 * 对齐 Kotlin FIR `ConeUnresolvedTypeQualifierError`，保留每一段 qualifier 的名字与显式类型实参，
 * 供 analysis-api 和 low-level API 做高保真投影。
 */
data class ConeUnresolvedQualifierPart(
    val name: Name,
    val typeArguments: List<CfirTypeRef>,
)

data class ConeUnresolvedTypeQualifierError(
    val qualifiers: List<ConeUnresolvedQualifierPart>,
) : ConeUnresolvedError {
    override val reason: String = buildString {
        append("unresolved type qualifier: ")
        append(
            qualifiers.joinToString(".") { qualifier ->
                buildString {
                    append(qualifier.name.asString())
                    if (qualifier.typeArguments.isNotEmpty()) {
                        append('<')
                        append(qualifier.typeArguments.joinToString(",") { it.toString() })
                        append('>')
                    }
                }
            }
        )
    }
}

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
    override val candidateSymbol:CfirBasedSymbol<*> get() = candidate.symbol
}

data class ConeUnresolvedSymbolError(
    val classId: ClassId,
) : ConeUnresolvedError {
    override val reason: String = "unresolved symbol: ${classId.asString()}"
}

/**
 * 类型构造器已解析成功，但类型实参数量不匹配。
 */
data class ConeUnmatchedTypeArgumentsError(
    val symbol: CfirClassLikeSymbol<*>,
    val expectedCount: Int,
    val actualCount: Int,
    val providedTypeArguments: List<CfirTypeRef>,
) : ConeDiagnostic {
    override val reason: String =
        "type argument count mismatch for ${describeSymbol(symbol)}: expected $expectedCount but got $actualCount"
}

interface ConeDiagnosticWithCandidates : ConeDiagnostic {
    val candidates: Collection<AbstractCandidate>
    val candidateSymbols: Collection<CfirBasedSymbol<*>> get() = candidates.map { it.symbol }
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
    val candidateSymbol: CfirBasedSymbol<*> get() = candidate.symbol
    override val candidates: Collection<AbstractCallCandidate<*>> get() = listOf(candidate)
    override val candidateSymbols: Collection<CfirBasedSymbol<*>> get() = listOf(candidateSymbol)
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
    val symbol: CfirBasedSymbol<*>,
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

private fun describeSymbol(symbol: CfirBasedSymbol<*>): String {
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

// ════════════════════════════════════════════════════════════════
// resolve 管线补齐：以下 Cone 诊断类对应 CfirDiagnosticsList 中
// 尚未被任何 resolve 路径报告的诊断。
// 对齐 Kotlin K2 FIR 的 ConeDiagnostic 分层方式。
// ════════════════════════════════════════════════════════════════

/**
 * 包名不能独立作为引用使用。
 *
 * 对齐 C++ sema_cannot_ref_to_pkg_name:引用解析到一个包路径(不是包内声明),
 * 则视作裸包引用,不允许作为值/类型使用。
 */
data class ConeCannotRefToPackageNameError(
    val packageFqName: org.cangnova.cangjie.name.FqName,
) : ConeDiagnostic {
    override val reason: String get() = "package name '$packageFqName' cannot be referred independently"
}

/**
 * 泛型类型替换不一致。
 *
 * 对齐 C++ sema_generic_type_inconsistent。
 */
data class ConeGenericTypeInconsistentError(
    val typeParameterName: Name,
) : ConeDiagnostic {
    override val reason: String get() = "generic type '$typeParameterName' is inconsistent in substitution"
}

/**
 * 泛型参数个数不匹配。
 *
 * 对齐 C++ sema_generic_argument_no_match。
 */
data class ConeGenericArgumentNoMatchError(
    val expectedCount: Int,
    val actualCount: Int,
) : ConeDiagnostic {
    override val reason: String get() = "expected $expectedCount type argument(s) but got $actualCount"
}

/**
 * 子类型的泛型约束不能比父类型更宽松。
 *
 * 对齐 C++ sema_generic_constraint_not_looser。
 */
class ConeGenericConstraintNotLooserError : ConeDiagnostic {
    override val reason: String get() = "generic constraint cannot be looser than the parent"
}

/**
 * 泛型实例化后导致函数签名歧义。
 *
 * 对齐 C++ sema_generic_instantiation_causes_ambiguous_functions。
 */
data class ConeGenericInstantiationCausesAmbiguousFunctionsError(
    val instantiation: Name,
    val functionName: Name,
) : ConeDiagnostic {
    override val reason: String get() = "generic instantiation of '$instantiation' causes ambiguous functions for '$functionName'"
}

/**
 * 通过 extend 间接满足约束不被允许。
 *
 * 对齐 C++ sema_meet_constraint_indirectly。
 */
class ConeMeetConstraintIndirectlyError : ConeDiagnostic {
    override val reason: String get() = "constraint is met indirectly through extend, which is not allowed"
}

/**
 * 不是某类型的成员。
 *
 * 对齐 C++ sema_not_member_of。
 */
data class ConeNotMemberOfError(
    val memberName: Name,
    val kind: String,
    val typeName: Name,
) : ConeDiagnostic {
    override val reason: String get() = "'$memberName' is not a $kind of '$typeName'"
}

/**
 * 成员未导入。
 *
 * 对齐 C++ sema_member_not_imported。
 */
data class ConeMemberNotImportedError(
    val memberName: Name,
) : ConeDiagnostic {
    override val reason: String get() = "'$memberName' is not imported"
}

/**
 * 无效的一元运算符。
 *
 * 对齐 C++ sema_invalid_unary_expr。
 */
data class ConeInvalidUnaryExprError(
    val operator: String,
    val type: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String get() = "invalid unary operator '$operator' for type $type"
}

/**
 * 无效的一元运算符（含目标返回类型）。
 *
 * 对齐 C++ sema_invalid_unary_expr_with_target。
 */
data class ConeInvalidUnaryExprWithTargetError(
    val operator: String,
    val type: ConeCangJieType,
    val returnType: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String get() = "invalid unary operator '$operator' for type $type (expected return type: $returnType)"
}

/**
 * 对非 optional 类型使用 optional chaining。
 *
 * 对齐 C++ sema_optional_chain_non_optional。
 */
data class ConeOptionalChainNonOptionalError(
    val type: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String get() = "cannot use optional chaining on non-optional type $type"
}

/**
 * 无法推断泛型函数的类型参数。
 *
 * 对齐 C++ sema_unable_to_infer_generic_func。
 */
class ConeUnableToInferGenericFuncError : ConeDiagnostic {
    override val reason: String get() = "unable to infer type arguments for generic function"
}

/**
 * resolve 完成后节点仍无效。
 *
 * 对齐 C++ sema_invalid_node_after_check。
 */
class ConeInvalidNodeAfterCheckError : ConeDiagnostic {
    override val reason: String get() = "node is invalid after semantic check"
}

/**
 * 类型不匹配（带原因说明）。
 *
 * 对齐 C++ sema_mismatched_types_because。
 */
data class ConeMismatchedTypesBecauseError(
    val expectedType: ConeCangJieType,
    val actualType: ConeCangJieType,
    val because: String,
) : ConeDiagnostic {
    override val reason: String get() = "type mismatch: expected $expectedType but got $actualType because $because"
}

/**
 * 多重赋值类型不匹配。
 *
 * 对齐 C++ sema_mismatched_types_multiple_assign。
 */
data class ConeMismatchedTypesMultipleAssignError(
    val actualType: ConeCangJieType,
) : ConeDiagnostic {
    override val reason: String get() = "type mismatch in multiple assignment: $actualType"
}

/**
 * 参数个数不匹配（通用场景）。
 *
 * 对齐 C++ sema_param_count_mismatch。
 */
data class ConeParamCountMismatchError(
    val expected: Int,
    val actual: Int,
) : ConeDiagnostic {
    override val reason: String get() = "expected $expected parameter(s) but got $actual"
}

/**
 * 变量在初始化之前被闭包捕获。
 *
 * 对齐 C++ sema_capture_before_initialization。
 */
data class ConeCaptureBeforeInitializationError(
    val variableName: Name,
) : ConeDiagnostic {
    override val reason: String get() = "cannot capture variable '$variableName' before initialization"
}
