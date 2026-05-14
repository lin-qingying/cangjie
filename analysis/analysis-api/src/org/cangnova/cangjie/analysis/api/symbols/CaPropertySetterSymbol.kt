package org.cangnova.cangjie.analysis.api.symbols

/**
 * 属性 setter 符号。
 *
 * 表示属性的写路径，包含一个固定的"新值"参数。
 * 暴露该参数符号便于上层做参数级注解、重命名等动作。
 */
abstract class CaPropertySetterSymbol : CaPropertyAccessorSymbol() {
    /**
     * setter 的"新值"参数符号。
     */
    abstract val parameter: CaValueParameterSymbol
}
