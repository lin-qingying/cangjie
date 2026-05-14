package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.name.Name

/**
 * 值参数公共符号。
 *
 * 当前 analysis-api 在值参数层只公开“是否声明默认值”这一稳定语义，
 * 不把默认值源码文本本身塞进符号接口。
 */
abstract class CaValueParameterSymbol : CaParameterSymbol() {
    /**
     * 是否为命名参数（在调用侧可用 `name: value` 方式传递）。
     */
    abstract  val isNamed: Boolean

    /**
     * 是否为可变长（vararg）参数。
     */
    abstract val isVararg: Boolean

    /**
     * 是否声明了默认值。
     *
     * 此处只暴露"是否存在"，不暴露默认值的源码文本或求值结果，避免把表达式语义渗入参数符号。
     */
    abstract val hasDefaultValue: Boolean
}
