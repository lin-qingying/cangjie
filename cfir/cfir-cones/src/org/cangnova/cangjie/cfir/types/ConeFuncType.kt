package org.cangnova.cangjie.cfir.types

/**
 * 函数类型，对应仓颉编译器中的 FuncTy。
 * 仓颉中函数是一等公民，函数类型表示 (P1, P2, ...) -> R。
 */
class ConeFuncType(
    val parameterTypes: List<ConeCangJieType>,
    val returnType: ConeCangJieType,
    /** 是否为 CFunc（C 互操作函数类型），对应 FuncTy::isC */
    val isCFunc: Boolean = false,
    /** 是否为闭包类型，对应 FuncTy::isClosureTy */
    val isClosureType: Boolean = false,
    /** 是否含有变长参数（C 函数），对应 FuncTy::hasVariableLenArg */
    val hasVariableLenArg: Boolean = false,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * 函数类型的“结构参数”由参数类型和返回类型共同组成。
     *
     * 这样做可以让通用类型遍历逻辑直接深入函数签名内部，
     * 不需要在每个算法里单独 hardcode `ConeFuncType`。
     */
    override val typeArguments: List<ConeTypeProjection>
        get() = (parameterTypes + returnType).map(::ConeTypeProjection)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeFuncType) return false
        return parameterTypes == other.parameterTypes &&
            returnType == other.returnType &&
            isCFunc == other.isCFunc &&
            isClosureType == other.isClosureType &&
            hasVariableLenArg == other.hasVariableLenArg
    }

    override fun hashCode(): Int {
        var result = parameterTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + isCFunc.hashCode()
        result = 31 * result + isClosureType.hashCode()
        result = 31 * result + hasVariableLenArg.hashCode()
        return result
    }


}
