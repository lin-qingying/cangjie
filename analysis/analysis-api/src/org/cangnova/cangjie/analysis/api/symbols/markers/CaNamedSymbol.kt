package org.cangnova.cangjie.analysis.api.symbols.markers

import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.Name

/**
 * 标记型 trait：声明该符号具备稳定的非空名字。
 *
 * 只有实现该接口的符号才能在源码中按名字查找/引用；
 * 匿名声明（如函数字面量、匿名类等）不实现该接口。
 *
 * 顶层扩展属性 [org.cangnova.cangjie.analysis.api.symbols.name] 据此判定是否返回 `null`。
 */
interface CaNamedSymbol : CaSymbol {
    /**
     * 符号的稳定名字。
     */
    val name: Name
}
