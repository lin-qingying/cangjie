package org.cangnova.cangjie.cfir.types

/**
 * 函数类型，对应仓颉编译器中的 FuncTy。
 * 仓颉中函数是一等公民，函数类型表示 (P1, P2, ...) -> R。
 */
class ConeFunctionType(
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeFunctionType) return false
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
