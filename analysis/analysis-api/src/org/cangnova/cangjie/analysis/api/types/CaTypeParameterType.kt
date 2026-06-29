package org.cangnova.cangjie.analysis.api.types

import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * 类型参数类型(type parameter type)。
 *
 * 表示一处对类型参数的引用,例如 `class Box<T>(val element: T)` 函数体内对 `T` 的引用。
 * 在未发生类型代换的 “原始位置” 出现的 type parameter 通过本类型暴露给 Analysis API 用户。
 *
 * 对齐 Kotlin Analysis API 的 `KaTypeParameterType`。
 */
abstract class CaTypeParameterType : CaType {
    /**
     * 类型参数的简单名,例如 `T`。
     */
    abstract val name: Name

    /**
     * 该 type parameter 对应的声明符号,持有约束、方差等额外信息。
     */
    abstract val symbol: CaTypeParameterSymbol

    /**
     * 创建可恢复该类型参数类型的类型指针。
     */
    abstract override fun createPointer(): CaTypePointer<CaTypeParameterType>
}
