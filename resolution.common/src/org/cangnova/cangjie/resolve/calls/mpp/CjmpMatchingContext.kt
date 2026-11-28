package org.cangnova.cangjie.resolve.calls.mpp

/** CJMP 匹配算法可见的声明符号标记；具体声明访问留给前端 context。 */
interface CjmpDeclarationSymbolMarker

interface CjmpClassLikeSymbolMarker : CjmpDeclarationSymbolMarker

interface CjmpCallableSymbolMarker : CjmpDeclarationSymbolMarker

interface CjmpFunctionSymbolMarker : CjmpCallableSymbolMarker

interface CjmpTypeParameterSymbolMarker : CjmpDeclarationSymbolMarker

interface CjmpValueParameterSymbolMarker : CjmpDeclarationSymbolMarker

enum class CjmpMismatchKind {
    CALLABLE_KIND,
    PARAMETER_SHAPE,
    PARAMETER_COUNT,
    FUNCTION_TYPE_PARAMETER_COUNT,
    PARAMETER_TYPES,
    PARAMETER_NAMES,
    FUNCTION_TYPE_PARAMETER_UPPER_BOUNDS,
    FUNCTION_TYPE,
    PARAMETER_DEFAULT_VALUE_BOTH_SIDES,
    CLASS_KIND,
    NOMINAL_GENERICNESS,
    TYPE_NOT_RESOLVED,
    MISSING_BODY,
    SECOND_BINDING,
}

data class CjmpParameterMismatch(val kind: CjmpMismatchKind, val parameterIndex: Int)

sealed class CjmpMatchResult {
    data object Matched : CjmpMatchResult()

    data class Mismatched(val kind: CjmpMismatchKind) : CjmpMatchResult()

    data class ParameterMismatched(val mismatch: CjmpParameterMismatch) : CjmpMatchResult()

    data object TypeNotResolved : CjmpMatchResult()
}

/**
 * 一个 common 候选的声明级匹配事实，不包含全组绑定占用状态。
 *
 * 候选顺序由调用方提供并完整保留：官方 first-fit 在已占用候选失败后仍可继续查找。
 * 实现体要求是独立事实，只有绑定策略确定候选未被占用后才能据此报告诊断。
 */
data class CjmpCandidateMatch<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker>(
    val common: D,
    val compatibility: CjmpMatchResult,
    val typeParameterMapping: Map<T, T>,
    val requiresImplementation: Boolean,
)

/** 资格拒绝时 signatureCompatibility 为 null，明确表示尚未计算类型签名。 */
data class CjmpPreparedCandidate<T : CjmpTypeParameterSymbolMarker>(
    val eligibility: CjmpMatchResult,
    val signatureCompatibility: CjmpMatchResult?,
    val typeParameterMapping: Map<T, T>,
    val requiresImplementation: Boolean,
)

enum class CjmpTypeCompatibility {
    COMPATIBLE,
    INCOMPATIBLE,
    UNRESOLVED,
}

/** CFIR nominal 声明在 CJMP 资格比较中使用的语言级种类。 */
enum class CjmpClassLikeKind {
    CLASS,
    STRUCT,
    INTERFACE,
    ENUM,
    TYPE_ALIAS,
    BUILT_IN,
    PRIMITIVE,
}

/** CJMP 可调用声明的配对种类；非候选 callable 不提供该种类。 */
enum class CjmpCallableKind {
    CONSTRUCTOR,
    FUNCTION,
    PROPERTY,
    VARIABLE,
}

data class CjmpMatchingValueParameter<T>(
    val type: T?,
    val isNamed: Boolean,
    val name: String,
    val hasDefaultValue: Boolean,
)

/**
 * 由具体前端提供只读的符号属性与类型运算；共享 matcher 不写入配对存储或报告诊断。
 */
interface CjmpMatchingContext<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> {
    fun classLikeKind(declaration: D): CjmpClassLikeKind?

    fun isExtend(declaration: D): Boolean

    fun haveSameExtendKey(first: D, second: D): CjmpTypeCompatibility

    fun callableKind(declaration: D): CjmpCallableKind?

    fun isFunction(declaration: D): Boolean

    fun isConstructor(declaration: D): Boolean

    /** 只有双方都是 enum 构造项时才使用完整函数类型等价；mixed 声明仍走普通函数/变量规则。 */
    fun areEnumConstructors(specific: D, common: D): Boolean

    /** 官方 MatchEnumFuncTypes：包含 payload 与 enum 返回类型，按 common→specific 泛型映射后严格判等。 */
    fun areEnumConstructorTypesEquivalent(specific: D, common: D, typeParameterMapping: Map<T, T>): CjmpTypeCompatibility

    fun typeParameters(declaration: D): List<T>

    fun valueParameters(declaration: D): List<CjmpMatchingValueParameter<Type>>

    /** alpha.66 起匹配阶段检查双方默认值冲突；更早版本由 parser 拒绝 specific 默认值。 */
    fun checkParameterDefaultValueConflict(): Boolean

    /** 根据当前声明头区分真实 Quest、具体类型及错误；不能按曾经省略类型注解豁免失配。 */
    fun areReturnTypesCompatible(specific: D, common: D, typeParameterMapping: Map<T, T>): CjmpTypeCompatibility

    fun areTypesEquivalent(specific: Type?, common: Type?, typeParameterMapping: Map<T, T>): CjmpTypeCompatibility

    fun needToReportMissingBody(common: D, specific: D): Boolean
}
