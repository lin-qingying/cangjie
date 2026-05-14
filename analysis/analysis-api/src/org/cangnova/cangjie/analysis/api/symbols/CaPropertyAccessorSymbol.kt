package org.cangnova.cangjie.analysis.api.symbols

/**
 * 属性访问器（getter / setter）公共父类型。
 *
 * 用 `sealed` 收敛 [CaPropertyGetterSymbol] 与 [CaPropertySetterSymbol] 两个叶子；
 * 访问器本身仍是函数（继承 [CaFunctionSymbol]），但额外承担"属性子声明"角色，
 * 因此暴露所属 property 的回链与是否默认实现等 property 视角才关心的语义。
 */
sealed class CaPropertyAccessorSymbol : CaFunctionSymbol() {
    /**
     * 当前访问器所属的属性符号。
     */
    abstract    val owningProperty: CaPropertySymbol

    /**
     * 是否为编译器自动合成的默认访问器（源码未显式书写访问器体）。
     */
    abstract val isDefault: Boolean

    /**
     * 是否为 getter。
     *
     * 与 [CaPropertyGetterSymbol] / [CaPropertySetterSymbol] 的具体子类型互为冗余，
     * 但提供一个无需类型判断即可读取的便捷标志。
     */
    abstract  val isGetter: Boolean
}
