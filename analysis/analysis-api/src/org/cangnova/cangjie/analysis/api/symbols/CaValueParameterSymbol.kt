package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.name.Name

/**
 * 值参数公共符号。
 *
 * 当前 analysis-api 在值参数层只公开“是否声明默认值”这一稳定语义，
 * 不把默认值源码文本本身塞进符号接口。
 */
abstract class CaValueParameterSymbol : CaParameterSymbol() {
    abstract  val isNamed: Boolean
    public abstract val isVararg: Boolean

    abstract val hasDefaultValue: Boolean
}
