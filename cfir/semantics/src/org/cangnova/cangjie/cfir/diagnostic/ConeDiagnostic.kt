package org.cangnova.cangjie.cfir.diagnostic

import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassifierAmbiguityDiagnostic
import org.cangnova.cangjie.cfir.types.ConeRecoverableNominalDiagnostic
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
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 未解析类 Cone 诊断的公共基类。
 *
 * 该分支用于描述名称、符号、类型限定名等 lookup 阶段无法解析成功的失败。
 */
sealed interface ConeUnresolvedError : ConeDiagnostic

/**
 * 类型限定名逐段解析失败。
 *
 * 对齐 Kotlin FIR `ConeUnresolvedTypeQualifierError`。
 *
 * @property qualifiers 已成功拆分出的限定名片段序列。
 */
class ConeUnresolvedTypeQualifierError(
    /**
     * 已成功拆分出的限定名片段序列。
     */
    val qualifiers: List<CfirQualifierPart>,
) : ConeUnresolvedError {
    /** 用于诊断消息展示的点分限定名。 */
    val qualifier: String get() = qualifiers.joinToString(separator = ".") { it.name.asString() }

    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Symbol not found for $qualifier"

    /** 面向类型构造器错误渲染的可读描述。 */
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unresolved qualified name: $qualifier"
}

/**
 * 类型位置的名称已经解析到普通 callable，而不是类型声明。
 *
 * 该诊断与 [ConeUnresolvedTypeQualifierError] 分离，使所有类型使用位置都能区分
 * “名称不存在”和“名称存在但不是类型”，无需由具体声明 checker 反查源码。
 */
data class ConeNotATypeError(
    /** 已解析到非类型声明的名称。 */
    val name: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "${name.asString()} is not a type"
}

/**
 * 普通名称引用解析失败。
 *
 * @property name 未能解析到符号的名称。
 */
data class ConeUnresolvedReferenceError(
    /**
     * 未能解析到符号的名称。
     */
    val name: Name,
) : ConeUnresolvedError {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "unresolved reference: ${name.asString()}"
}

/**
 * 调用候选的约束系统存在矛盾。
 *
 * @property candidate 触发矛盾的调用候选。
 */
class ConeConstraintSystemHasContradiction(
    /**
     * 触发约束系统矛盾的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String
        get() = "CS errors: ${
             describeSymbol(
                candidateSymbol
            )
        }"

    /** 与该矛盾直接关联的候选符号。 */
    override val candidateSymbol: CfirBasedSymbol<*> get() = candidate.symbol
}

/**
 * classId 级符号解析失败。
 *
 * @property classId 未能解析到声明的 classId。
 */
data class ConeUnresolvedSymbolError(
    /**
     * 未能解析到声明的 classId。
     */
    val classId: ClassId,
) : ConeUnresolvedError {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "unresolved symbol: ${classId.asString()}"
}

/**
 * 类型构造器已解析成功，但类型实参数量不匹配。
 *
 * @property symbol 已解析到的类型声明符号。
 * @property expectedCount 声明侧要求的类型实参数量。
 * @property actualCount 调用侧实际提供的类型实参数量。
 * @property providedTypeArguments 调用侧提供的类型实参引用。
 */
data class ConeUnmatchedTypeArgumentsError(
    /**
     * 已解析到的类型声明符号。
     */
    val symbol: CfirClassLikeSymbol<*>,
    /**
     * 声明侧要求的类型实参数量。
     */
    val expectedCount: Int,
    /**
     * 调用侧实际提供的类型实参数量。
     */
    val actualCount: Int,
    /**
     * 调用侧提供的类型实参引用。
     */
    val providedTypeArguments: List<CfirTypeRef>,
) : ConeRecoverableNominalDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "type argument count mismatch for ${describeSymbol(symbol)}: expected $expectedCount but got $actualCount"
}

/**
 * 携带多个候选的 Cone 诊断。
 */
interface ConeDiagnosticWithCandidates : ConeDiagnostic {
    /** 参与该诊断的候选集合。 */
    val candidates: Collection<AbstractCandidate>

    /** 候选集合对应的符号集合。 */
    val candidateSymbols: Collection<CfirBasedSymbol<*>> get() = candidates.map { it.symbol }
}

/**
 * 重载解析出现歧义。
 *
 * @property name 被解析的调用名。
 * @property applicability 歧义候选所在的适用性层级。
 * @property candidatesWithErrors 候选及其附带的结构化错误。
 * @property isCallLike 是否来自调用形式的引用。
 * @property typeUseSource classifier 类型使用歧义对应的完整源码范围；普通调用歧义为空。
 */
class ConeAmbiguityError(
    /**
     * 被解析的调用名。
     */
    val name: Name,
    /**
     * 歧义候选所在的适用性层级。
     */
    val applicability: CandidateApplicability,
    /**
     * 候选及其附带的结构化错误。
     */
    val candidatesWithErrors: Map<out AbstractCandidate, ConeDiagnostic?>,
    /**
     * 是否来自调用形式的引用。
     */
    val isCallLike: Boolean = false,
    /** 被外层调用结构化歧义支配、不得重复上报的内层诊断。 */
    val dominatedNestedDiagnostics: Set<ConeDiagnostic> = emptySet(),
    /** 所有候选是否都仅因结构化 error argument 失败。 */
    val isErrorArgumentCascade: Boolean = false,
    /**
     * classifier 类型使用歧义对应的完整源码范围。
     *
     * 类型解析必须保留整个 user type，而不是只保留最终 classifier token；诊断层据此绕过
     * 普通 qualified reference 的名称定位策略，完整标记 `A<X, Y>` 这类类型使用。
     */
    val typeUseSource: CjSourceElement? = null,
) : ConeDiagnosticWithCandidates, ConeClassifierAmbiguityDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Ambiguity: $name, ${candidateSymbols.map { describeSymbol(it) }}"

    /** 参与歧义判断的候选集合。 */
    override val candidates: Collection<AbstractCandidate> get() = candidatesWithErrors.keys
}

/**
 * 目标函数类型下存在多个可用函数引用。
 *
 * 与裸函数名的 [ConeAmbiguityError] 不同，该诊断表示引用已经进入官方 `ChkRefExpr`
 * 的目标类型检查，并且仍有多个函数类型满足目标函数类型。
 */
class ConeAmbiguousFunctionReferenceError(
    /** 被引用的函数名。 */
    val name: Name,
    /** 满足目标函数类型的候选及其结构化错误。 */
    val candidatesWithErrors: Map<out AbstractCandidate, ConeDiagnostic?>,
) : ConeDiagnosticWithCandidates {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String
        get() = "Ambiguous function reference: $name, ${candidateSymbols.map { describeSymbol(it) }}"

    /** 参与函数引用歧义判断的候选集合。 */
    override val candidates: Collection<AbstractCandidate>
        get() = candidatesWithErrors.keys
}

/**
 * 目标函数类型下没有可用函数引用。
 *
 * 该状态对应官方 `sema_no_match_function_declaration_for_ref`，必须与普通名称未解析、
 * 裸函数名歧义以及泛型函数缺失显式类型实参区分。
 */
class ConeNoMatchingFunctionReferenceError(
    /** 被引用的函数名。 */
    val name: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "No matching function declaration for reference: $name"
}

/**
 * 只关联单个调用候选的 Cone 诊断。
 */
interface ConeDiagnosticWithSingleCandidate : ConeDiagnosticWithCandidates {
    /** 触发诊断的调用候选。 */
    val candidate: AbstractCallCandidate<*>

    /** 触发诊断的候选符号。 */
    val candidateSymbol: CfirBasedSymbol<*> get() = candidate.symbol

    /** 单候选诊断的候选集合视图。 */
    override val candidates: Collection<AbstractCallCandidate<*>> get() = listOf(candidate)

    /** 单候选诊断的符号集合视图。 */
    override val candidateSymbols: Collection<CfirBasedSymbol<*>> get() = listOf(candidateSymbol)
}

/**
 * 调用候选不可适用。
 *
 * @property applicability 候选失败后的适用性分类。
 * @property candidate 不可适用的调用候选。
 */
class ConeInapplicableCandidateError(
    /**
     * 候选失败后的适用性分类。
     */
    val applicability: CandidateApplicability,
    /**
     * 不可适用的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Inapplicable($applicability): ${describeSymbol(candidateSymbol)}"
}

/**
 * 对象接收者访问 static 成员。
 *
 * @property memberName 被对象接收者错误访问的 static 成员名。
 * @property candidate 触发错误的调用候选。
 */
class ConeObjectCannotAccessStaticMemberError(
    /**
     * 被对象接收者错误访问的 static 成员名。
     */
    val memberName: Name,
    /**
     * 触发错误的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "object cannot access static member '${memberName.asString()}'"
}

/**
 * 类型名访问实例成员。
 *
 * @property memberName 被类型名错误访问的实例成员名。
 * @property candidate 触发错误的调用候选。
 */
class ConeIllegalAccessNonStaticMemberError(
    /**
     * 被类型名错误访问的实例成员名。
     */
    val memberName: Name,
    /**
     * 触发错误的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "'${memberName.asString()}' is non-static member, cannot access by type name"
}

/**
 * 调用候选因可见性或隐藏状态不能被选择。
 *
 * @property candidate 被隐藏的调用候选。
 */
class ConeHiddenCandidateError(
    /**
     * 被隐藏的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Hidden candidate: ${describeSymbol(candidateSymbol)}"
}

/**
 * 已解析符号不可访问。
 *
 * @property symbol 触发可见性错误的符号。
 */
class ConeVisibilityError(
    /**
     * 触发可见性错误的符号。
     */
    val symbol: CfirBasedSymbol<*>,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Cannot access: ${describeSymbol(symbol)}"
}


/**
 * 带上下文的名称解析失败。
 *
 * @property name 未能解析的名称。
 * @property operator 参与解析的运算符名称；为空表示普通名称解析。
 * @property receiverType 接收者类型；为空表示无显式接收者或接收者未知。
 * @property argumentTypes 调用实参类型列表。
 */
data class ConeUnresolvedNameError(
    /**
     * 未能解析的名称。
     */
    val name: Name,
    /**
     * 参与解析的运算符名称；为空表示普通名称解析。
     */
    val operator: String? = null,
    /**
     * 接收者类型；为空表示无显式接收者或接收者未知。
     */
    val receiverType: ConeCangJieType? = null,
    /**
     * 调用实参类型列表。
     */
    val argumentTypes: List<ConeCangJieType> = emptyList(),
) : ConeUnresolvedError {
    /** 面向普通诊断渲染的失败原因。 */
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

/**
 * 函数调用期望错误。
 *
 * 当一个变量或属性被写成调用形式，但候选集合中没有可调用函数时使用。
 *
 * @property name 被调用的名称。
 * @property hasValueParameters 调用语法是否携带值参数。
 * @property candidates 已解析到但不能作为函数调用的候选。
 */
data class ConeFunctionCallExpectedError(
    /**
     * 被调用的名称。
     */
    val name: Name,
    /**
     * 调用语法是否携带值参数。
     */
    val hasValueParameters: Boolean,
    /**
     * 已解析到但不能作为函数调用的候选集合。
     */
    override val candidates: Collection<AbstractCallCandidate<*>>,
) : ConeDiagnosticWithCandidates {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String
        get() = "Function call expected: $name(${if (hasValueParameters) "..." else ""})"
}

/**
 * 非函数表达式被当作函数调用。
 *
 * @property expressionName 表达式在诊断中展示的名称。
 * @property type 表达式实际类型。
 */
data class ConeFunctionExpectedError(
    /**
     * 表达式在诊断中展示的名称。
     */
    val expressionName: String,
    /**
     * 表达式实际类型。
     */
    val type: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "Expression '$expressionName' of type '$type' cannot be invoked as a function"
}

/**
 * 非函数表达式使用 `()` 调用且没有匹配的 operator 函数。
 *
 * 对齐官方 `sema_no_match_operator_function_call`。
 */
object ConeNoMatchOperatorFunctionCallError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "No matching function for operator '()' function call"
}

/**
 * 调用解析到了分类器而不是可调用声明。
 *
 * @property candidate 触发错误的候选。
 * @property classifier 被解析到的分类器符号。
 */
data class ConeResolutionToClassifierError(
    /**
     * 触发错误的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
    /**
     * 被解析到的分类器符号。
     */
    val classifier: CfirClassLikeSymbol<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "Resolution to classifier: ${describeSymbol(classifier)}"
}

/**
 * 构造器查询失败。
 */
object ConeNoConstructorError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "No constructor found"
}

/**
 * enum 类型名不能像 class / struct 那样直接作为类型构造器调用。
 *
 * 官方 C++ Sema 在 call-kind 划分里也把 enum constructor 与普通 type constructor
 * 明确区分开来；这里单独建模，避免把 `A(1)` 这类错误继续混成普通无构造器调用。
 *
 * @property enumName 被错误当作构造器调用的 enum 类型名。
 */
data class ConeEnumTypeCannotBeUsedAsConstructorError(
    /**
     * 被错误当作构造器调用的 enum 类型名。
     */
    val enumName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "enum type '${enumName.asString()}' cannot be used as a type constructor; use an enum constructor instead"
}

/**
 * effects 特性在 PSI 层始终建树，但是否允许进入语义阶段由 CFIR 控制。
 * 因此这里单独建模 feature gate 诊断，避免把 effect 语法再次退回 parser 层。
 *
 * @property constructName 被 feature gate 拦截的构造名称。
 */
data class ConeEffectsFeatureDisabledError(
    /**
     * 被 feature gate 拦截的构造名称。
     */
    val constructName: String,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "effects feature is disabled for '$constructName'"
}

/**
 * `perform` 表达式的命令类型不兼容。
 *
 * @property actualType 实际解析到的命令表达式类型；为空表示类型未知。
 */
data class ConeCommandIncompatibleTypeError(
    /**
     * 实际解析到的命令表达式类型；为空表示类型未知。
     */
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "performed expression must implement 'stdx.effect.Command<T>', actual type is '${actualType ?: "<unknown>"}'"
}

/**
 * handler 接收的命令句柄类型不合法。
 *
 * @property actualType 实际句柄类型；为空表示类型未知。
 */
data class ConeCommandHandleTypeError(
    /**
     * 实际句柄类型；为空表示类型未知。
     */
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "the command handle type must implement 'stdx.effect.Command<T>', actual type is '${actualType ?: "<unknown>"}'"
}

/**
 * `resume` 位于 immediate handler 外且没有显式恢复参数。
 */
object ConeImplicitResumeOutsideHandlerError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "'resume' outside of an immediate handler must have a resumption argument"
}

/**
 * 非 Unit resumption 缺少 `with` 或 `throwing` 子句。
 *
 * @property resumptionType 需要恢复的非 Unit 类型。
 */
data class ConeResumeNoWithError(
    /**
     * 需要恢复的非 Unit 类型。
     */
    val resumptionType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "a resumption of non-Unit type '$resumptionType' must have a 'with' or 'throwing' clause"
}

/**
 * `resume throwing` 的异常类型不满足约束。
 *
 * @property actualType 实际 throwing 表达式类型；为空表示类型未知。
 */
data class ConeResumeThrowingMismatchTypeError(
    /**
     * 实际 throwing 表达式类型；为空表示类型未知。
     */
    val actualType: ConeCangJieType?,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "the type of 'resume throwing' must be a subtype of std.core.Exception or std.core.Error, actual type is '${actualType ?: "<unknown>"}'"
}

/**
 * handler 分支块类型与已计算的公共父类型不一致。
 *
 * @property actualType 当前 handle block 的实际类型。
 * @property expectedType 之前分支计算出的最小公共父类型。
 */
data class ConeMismatchingHandleBlockError(
    /**
     * 当前 handle block 的实际类型。
     */
    val actualType: ConeCangJieType,
    /**
     * 之前分支计算出的最小公共父类型。
     */
    val expectedType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String =
        "the type of this handle block is '$actualType', which mismatches the smallest common supertype '$expectedType' of previous branches"
}

/**
 * expect-like 声明不能合成隐式默认构造器。
 */
object ConeNoImplicitDefaultConstructorOnExpectClass : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String = "No implicit default constructor on expect-like declaration"
}

/**
 * 将符号转换为诊断消息中稳定可读的名称。
 */
private fun describeSymbol(symbol: CfirBasedSymbol<*>): String {
    return when (symbol) {
        is CfirClassLikeSymbol<*> -> symbol.classId.asString()
        is CfirCallableSymbol<*> -> symbol.callableIdAsString()
        else -> "$symbol"
    }
}

/**
 * 无法推断类型参数类型。
 *
 * 对齐 Kotlin FIR `ConeCannotInferTypeParameterType`，但只依赖 CFIR 符号层。
 *
 * @property typeParameter 无法推断类型的类型参数符号。
 * @property reason 面向普通诊断渲染的失败原因。
 */
class ConeCannotInferTypeParameterType(
    /**
     * 无法推断类型的类型参数符号。
     */
    val typeParameter: CfirTypeParameterSymbol,
    /**
     * 面向普通诊断渲染的失败原因。
     */
    override val reason: String = "Cannot infer type for parameter ${typeParameter.name}"
) : ConeCannotInferType() {
    /** 面向类型构造器错误渲染的可读描述。 */
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for type parameter ${typeParameter.name}"
}

/**
 * 无法推断泛型函数的类型参数。
 *
 * @property typeParameter 无法推断类型的泛型函数类型参数。
 * @property reason 面向普通诊断渲染的失败原因。
 */
class ConeCannotInferGenericFunctionTypeParameterType(
    /**
     * 无法推断类型的泛型函数类型参数。
     */
    val typeParameter: CfirTypeParameterSymbol,
    /**
     * 面向普通诊断渲染的失败原因。
     */
    override val reason: String = "Cannot infer type arguments for generic function"
) : ConeCannotInferType() {
    /** 面向类型构造器错误渲染的可读描述。 */
    override val readableDescriptionAsTypeConstructor: String
        get() = "Unknown type for generic function type parameter ${typeParameter.name}"
}

/**
 * 类型推断失败类 Cone 诊断的公共基类。
 */
abstract class ConeCannotInferType : ConeDiagnostic

/**
 * 无法推断值参数类型。
 *
 * @property valueParameter 无法推断类型的值参数符号；为空时表示隐式 `it`。
 * @property isTopLevelLambda 是否来自顶层 lambda 参数。
 */
class ConeCannotInferValueParameterType(
    /**
     * 无法推断类型的值参数符号；为空时表示隐式 `it`。
     */
    val valueParameter: CfirValueParameterSymbol?,
    reason: String? = null,
    /**
     * 是否来自顶层 lambda 参数。
     */
    val isTopLevelLambda: Boolean = false,
) : ConeCannotInferType() {
    /** 外部传入的原因文本；为空时按参数名生成默认消息。 */
    private val _reason: String? = reason

    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String
        get() = _reason
            ?: ("Cannot infer type for parameter " + (valueParameter?.let { "${it.name}" } ?: "it"))
}


/**
 * 类型参数被用于限定访问。
 *
 * @property symbol 被错误用于限定访问的类型参数符号。
 */
class ConeTypeParameterInQualifiedAccess(val symbol: CfirTypeParameterSymbol) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "Type parameter ${symbol.cfir.name} in qualified access"
}

/**
 * 变量已解析但其类型上没有匹配的 invoke 操作符。
 * 例如 `a()` 中 a 是类型 C 的变量，但 C 未定义 `operator func ()()`。
 *
 * @property name 被调用的名称。
 * @property receiverType 接收者实际类型。
 */
data class ConeNoMatchingInvokeOperatorError(
    /**
     * 被调用的名称。
     */
    val name: Name,
    /**
     * 接收者实际类型。
     */
    val receiverType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
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
 *
 * @property packageFqName 被独立引用的包全名。
 */
data class ConeCannotRefToPackageNameError(
    /**
     * 被独立引用的包全名。
     */
    val packageFqName: org.cangnova.cangjie.name.FqName,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "package name '$packageFqName' cannot be referred independently"
}

/**
 * 已导入包短名或别名指向多个包。
 *
 * 对齐 C++ `GetImportedPackageDecl` 返回 conflict 后报告的包名歧义。
 *
 * @property packageName 出现歧义的包短名或导入别名。
 */
data class ConePackageNameConflictError(
    /**
     * 出现歧义的包短名或导入别名。
     */
    val packageName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "package name '$packageName' is ambiguous"
}

/**
 * 泛型类型替换不一致。
 *
 * 对齐 C++ sema_generic_type_inconsistent。
 *
 * @property typeParameterName 替换不一致的泛型参数名。
 */
data class ConeGenericTypeInconsistentError(
    /**
     * 替换不一致的泛型参数名。
     */
    val typeParameterName: Name,
    /**
     * 触发不一致替换的调用候选。
     */
    override val candidate: AbstractCallCandidate<*>,
) : ConeDiagnosticWithSingleCandidate {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "generic type '$typeParameterName' is inconsistent in substitution"
}

/**
 * 泛型参数个数不匹配。
 *
 * 对齐 C++ sema_generic_argument_no_match。
 *
 * @property expectedCount 声明侧期望的泛型实参数量。
 * @property actualCount 调用侧实际提供的泛型实参数量。
 */
data class ConeGenericArgumentNoMatchError(
    /**
     * 声明侧期望的泛型实参数量。
     */
    val expectedCount: Int,
    /**
     * 调用侧实际提供的泛型实参数量。
     */
    val actualCount: Int,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "expected $expectedCount type argument(s) but got $actualCount"
}

/**
 * 泛型类型实参不满足声明侧约束。
 *
 * 对齐 C++ sema_generic_type_argument_not_match_constraint。
 *
 * @property genericType 声明侧泛型类型。
 * @property actualType 实际传入的类型实参。
 * @property upperBound 声明侧要求的上界。
 */
data class ConeGenericTypeArgumentNotMatchConstraintError(
    /**
     * 声明侧泛型类型。
     */
    val genericType: ConeCangJieType,
    /**
     * 实际传入的类型实参。
     */
    val actualType: ConeCangJieType,
    /**
     * 声明侧要求的上界。
     */
    val upperBound: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String
        get() = "generic type argument '$actualType' does not match upper bound '$upperBound' of '$genericType'"
}

/**
 * 子类型的泛型约束不能比父类型更宽松。
 *
 * 对齐 C++ sema_generic_constraint_not_looser。
 */
class ConeGenericConstraintNotLooserError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "generic constraint cannot be looser than the parent"
}

/**
 * 泛型实例化后导致函数签名歧义。
 *
 * 对齐 C++ sema_generic_instantiation_causes_ambiguous_functions。
 *
 * @property instantiation 触发歧义的泛型实例化名称。
 * @property functionName 因实例化产生歧义的函数名。
 */
data class ConeGenericInstantiationCausesAmbiguousFunctionsError(
    /**
     * 触发歧义的泛型实例化名称。
     */
    val instantiation: Name,
    /**
     * 因实例化产生歧义的函数名。
     */
    val functionName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "generic instantiation of '$instantiation' causes ambiguous functions for '$functionName'"
}

/**
 * 通过 extend 间接满足约束不被允许。
 *
 * 对齐 C++ sema_meet_constraint_indirectly。
 */
class ConeMeetConstraintIndirectlyError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "constraint is met indirectly through extend, which is not allowed"
}

/**
 * 不是某类型的成员。
 *
 * 对齐 C++ sema_not_member_of。
 *
 * @property memberName 被查询的成员名。
 * @property kind 成员种类描述。
 * @property typeName 目标类型名。
 */
data class ConeNotMemberOfError(
    /**
     * 被查询的成员名。
     */
    val memberName: Name,
    /**
     * 成员种类描述。
     */
    val kind: String,
    /**
     * 目标类型名。
     */
    val typeName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "'$memberName' is not a $kind of '$typeName'"
}

/**
 * 成员未导入。
 *
 * 对齐 C++ sema_member_not_imported。
 *
 * @property memberName 未被导入的成员名。
 */
data class ConeMemberNotImportedError(
    /**
     * 未被导入的成员名。
     */
    val memberName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "'$memberName' is not imported"
}

/**
 * 无效的一元运算符。
 *
 * 对齐 C++ sema_invalid_unary_expr。
 *
 * @property operator 一元运算符文本。
 * @property type 操作数类型。
 */
data class ConeInvalidUnaryExprError(
    /**
     * 一元运算符文本。
     */
    val operator: String,
    /**
     * 操作数类型。
     */
    val type: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "invalid unary operator '$operator' for type $type"
}

/**
 * 无效的一元运算符（含目标返回类型）。
 *
 * 对齐 C++ sema_invalid_unary_expr_with_target。
 *
 * @property operator 一元运算符文本。
 * @property type 操作数类型。
 * @property returnType 运算符期望返回类型。
 */
data class ConeInvalidUnaryExprWithTargetError(
    /**
     * 一元运算符文本。
     */
    val operator: String,
    /**
     * 操作数类型。
     */
    val type: ConeCangJieType,
    /**
     * 运算符期望返回类型。
     */
    val returnType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "invalid unary operator '$operator' for type $type (expected return type: $returnType)"
}

/**
 * 对非 optional 类型使用 optional chaining。
 *
 * 对齐 C++ sema_optional_chain_non_optional。
 *
 * @property type optional chaining 接收者的实际类型。
 */
data class ConeOptionalChainNonOptionalError(
    /**
     * optional chaining 接收者的实际类型。
     */
    val type: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "cannot use optional chaining on non-optional type $type"
}

/**
 * 无法推断泛型函数的类型参数。
 *
 * 对齐 C++ sema_unable_to_infer_generic_func。
 */
class ConeUnableToInferGenericFuncError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "unable to infer type arguments for generic function"
}

/**
 * 独立泛型函数值引用缺少显式类型实参且没有目标函数类型。
 *
 * 该诊断与普通泛型函数调用的推断失败共享用户可见诊断名，但语义锚点固定为函数 selector，
 * 使诊断映射无需从 PSI/LightTree 宿主形状反推当前错误属于函数值引用还是调用。
 */
class ConeGenericFunctionReferenceWithoutTypeArgumentsError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "generic function reference requires explicit type arguments"
}

/**
 * 无法从当前表达式上下文反推出完整类型。
 *
 * 对齐 C++ `sema_unable_to_infer_expr`；典型场景是 expected interface 能匹配 generic enum
 * 的父类型，但父类型没有携带足够的 owner 类型参数信息。
 */
class ConeUnableToInferExpressionTypeError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "unable to infer the type of this expression"
}

/**
 * resolve 完成后节点仍无效。
 *
 * 对齐 C++ sema_invalid_node_after_check。
 */
class ConeInvalidNodeAfterCheckError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "node is invalid after semantic check"
}

/**
 * 通用类型不匹配。
 *
 * 用于 resolve 阶段已经确定期望类型与实际类型的语义节点，后续统一映射到
 * CFIR 的 `TYPE_MISMATCH` 诊断，避免用字符串 reason 表示结构化类型信息。
 *
 * @property expectedType 语义上下文期望类型。
 * @property actualType 实际解析到的类型。
 */
data class ConeTypeMismatchError(
    /**
     * 语义上下文期望类型。
     */
    val expectedType: ConeCangJieType,
    /**
     * 实际解析到的类型。
     */
    val actualType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "type mismatch: expected $expectedType but got $actualType"
}

/**
 * 数组字面量元素类型不一致。
 *
 * 对齐 C++ `SynArrayLit` 中的 `sema_inconsistency_elemType`。
 */
class ConeInconsistentArrayLiteralElementTypeError : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "inconsistent element type for array literal"
}

/**
 * 类型不匹配（带原因说明）。
 *
 * 对齐 C++ sema_mismatched_types_because。
 *
 * @property expectedType 语义上下文期望类型。
 * @property actualType 实际解析到的类型。
 * @property because 附加原因说明。
 */
data class ConeMismatchedTypesBecauseError(
    /**
     * 语义上下文期望类型。
     */
    val expectedType: ConeCangJieType,
    /**
     * 实际解析到的类型。
     */
    val actualType: ConeCangJieType,
    /**
     * 附加原因说明。
     */
    val because: String,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "type mismatch: expected $expectedType but got $actualType because $because"
}

/**
 * 多重赋值类型不匹配。
 *
 * 对齐 C++ sema_mismatched_types_multiple_assign。
 *
 * @property expectedType 首个不兼容目标所要求的类型。
 * @property actualType 多重赋值右侧或分量的实际类型。
 */
data class ConeMismatchedTypesMultipleAssignError(
    /**
     * 首个不兼容目标所要求的类型。
     */
    val expectedType: ConeCangJieType,
    /**
     * 多重赋值右侧或分量的实际类型。
     */
    val actualType: ConeCangJieType,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() =
        "type mismatch in multiple assignment: expected $expectedType but got $actualType"
}

/**
 * 参数个数不匹配（通用场景）。
 *
 * 对齐 C++ sema_param_count_mismatch。
 *
 * @property expected 期望参数数量。
 * @property actual 实际参数数量。
 */
data class ConeParamCountMismatchError(
    /**
     * 期望参数数量。
     */
    val expected: Int,
    /**
     * 实际参数数量。
     */
    val actual: Int,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "expected $expected parameter(s) but got $actual"
}

/**
 * 变量在初始化之前被闭包捕获。
 *
 * 对齐 C++ sema_capture_before_initialization。
 *
 * @property variableName 被过早捕获的变量名。
 */
data class ConeCaptureBeforeInitializationError(
    /**
     * 被过早捕获的变量名。
     */
    val variableName: Name,
) : ConeDiagnostic {
    /** 面向普通诊断渲染的失败原因。 */
    override val reason: String get() = "cannot capture variable '$variableName' before initialization"
}
