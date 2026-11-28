package org.cangnova.cangjie.parsing

/** 官方 CheckVariableParams 的声明级参数投影，诊断坐标仍由真实 parser owner 提供。 */
object CjVariadicParameterRules {
    enum class ErrorKind { NON_FOREIGN, FIRST_PARAMETER, NOT_LAST_PARAMETER }
    data class Error(val kind: ErrorKind, val parameterIndex: Int)
    data class Result(
        val hasVariableLengthArgument: Boolean,
        val variableArgumentIndex: Int?,
        val erasedParameterIndex: Int?,
        val errors: List<Error>,
    ) {
        fun retains(index: Int): Boolean = index != erasedParameterIndex
    }

    /**
     * 对位 v1.1.3 CheckVariableParams 的完整声明入口：先判断所有参数列表的数量，
     * primary 始终按非 FOREIGN 检查，secondary 与普通函数读取本次返回的 FOREIGN。
     * 宏定义、main、finalizer 与访问器不经过这个官方检查；错误参数仍保留实际 owner。
     */
    fun select(
        kind: CjmpModifierDeclarationKind,
        parameterListCount: Int,
        effectiveForeign: Boolean,
        ellipsisParameters: List<Boolean>,
    ): Result {
        require(parameterListCount >= 0)
        require(parameterListCount != 0 || ellipsisParameters.isEmpty())
        if (parameterListCount != 1 || kind !in setOf(
                CjmpModifierDeclarationKind.FUNCTION,
                CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR,
                CjmpModifierDeclarationKind.SECONDARY_CONSTRUCTOR,
            )) return Result(false, null, null, emptyList())
        val isForeign = kind != CjmpModifierDeclarationKind.PRIMARY_CONSTRUCTOR && effectiveForeign
        var variableIndex: Int? = null
        var erased: Int? = null
        val errors = mutableListOf<Error>()
        for ((index, variable) in ellipsisParameters.withIndex()) {
            if (!variable) continue
            variableIndex = index
            if (!isForeign) {
                errors += Error(ErrorKind.NON_FOREIGN, index)
                break
            }
            when {
                index == 0 -> errors += Error(ErrorKind.FIRST_PARAMETER, index)
                index != ellipsisParameters.lastIndex -> errors += Error(ErrorKind.NOT_LAST_PARAMETER, index)
                else -> erased = index
            }
        }
        return Result(variableIndex != null, variableIndex, erased, errors.toList())
    }
}
