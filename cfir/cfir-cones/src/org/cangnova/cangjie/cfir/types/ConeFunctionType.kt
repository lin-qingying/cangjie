package org.cangnova.cangjie.cfir.types

/**
 * 函数类型，对应仓颉编译器中的 FuncTy。
 * 仓颉中函数是一等公民，函数类型表示 (P1, P2, ...) -> R。
 *
 * @property parameterTypes 参数类型列表。
 * @property returnType 返回类型。
 * @property isCFunc 是否为 CFunc（C 互操作函数类型）。
 * @property isClosureType 是否为闭包类型。
 * @property hasVariableLenArg 是否含有变长参数。
 * @property attributes 函数类型附带的属性。
 */
class ConeFunctionType(
    val parameterTypes: List<ConeCangJieType>,
    val returnType: ConeCangJieType,
    val isCFunc: Boolean = false,
    val isClosureType: Boolean = false,
    val hasVariableLenArg: Boolean = false,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * 函数类型按参数、返回值和函数语义标记判等。
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeFunctionType) return false
        return parameterTypes == other.parameterTypes &&
            returnType == other.returnType &&
            isCFunc == other.isCFunc &&
            isClosureType == other.isClosureType &&
            hasVariableLenArg == other.hasVariableLenArg
    }

    /**
     * 函数类型的结构哈希。
     */
    override fun hashCode(): Int {
        var result = parameterTypes.hashCode()
        result = 31 * result + returnType.hashCode()
        result = 31 * result + isCFunc.hashCode()
        result = 31 * result + isClosureType.hashCode()
        result = 31 * result + hasVariableLenArg.hashCode()
        return result
    }


}
