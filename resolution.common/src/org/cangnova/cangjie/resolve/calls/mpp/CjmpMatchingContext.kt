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
    NOMINAL_TYPE_PARAMETER_COUNT,
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

enum class CjmpTypeCompatibility {
    COMPATIBLE,
    INCOMPATIBLE,
    UNRESOLVED,
}

data class CjmpMatchingValueParameter<T>(
    val type: T?,
    val isNamed: Boolean,
    val name: String,
    val hasDefaultValue: Boolean,
)

/**
 * 由具体前端提供符号属性、类型运算与配对存储；共享 matcher 不依赖 CFIR 或 session。
 */
interface CjmpMatchingContext<D : CjmpDeclarationSymbolMarker, T : CjmpTypeParameterSymbolMarker, Type> {
    fun classLikeKind(declaration: D): String?

    fun isExtend(declaration: D): Boolean

    fun haveSameExtendKey(first: D, second: D): CjmpTypeCompatibility

    fun callableKind(declaration: D): String

    fun isFunction(declaration: D): Boolean

    fun isConstructor(declaration: D): Boolean

    fun typeParameters(declaration: D): List<T>

    fun valueParameters(declaration: D): List<CjmpMatchingValueParameter<Type>>

    fun returnType(declaration: D): Type?

    fun areTypesEquivalent(specific: Type?, common: Type?, typeParameterMapping: Map<T, T>): CjmpTypeCompatibility

    fun isSubtypeOf(specific: Type?, common: Type?, typeParameterMapping: Map<T, T>): CjmpTypeCompatibility

    fun needToReportMissingBody(common: D, specific: D): Boolean

    fun trySetSpecificImplementation(
        specific: D,
        common: D,
        typeParameterMapping: Map<T, T>,
    ): Boolean

    fun recordMismatch(specific: D, kind: CjmpMismatchKind, common: D)

    fun recordParameterMismatch(specific: D, mismatch: CjmpParameterMismatch, common: D)
}
