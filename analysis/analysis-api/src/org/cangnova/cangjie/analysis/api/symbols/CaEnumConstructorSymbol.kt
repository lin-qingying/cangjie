package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol

/**
 * 仓颉枚举成员在语义上是“枚举构造器”。
 *
 * 它既不是变量，也不完全等同于普通函数：
 * - 它参与 callable 名字查找与调用；
 * - 它隶属于某个枚举类型；
 * - 它的 payload 是按“类型列表”建模，而不是按 let/var 或具名参数建模。
 *
 * 因此公开 API 直接把它定义为专用 callable 抽象，
 * 明确暴露“所属枚举类型 + payload 类型列表”这两个核心语义。
 */
abstract class CaEnumConstructorSymbol : CaCallableSymbol(), CaNamedSymbol {
    /**
     * 所属枚举类型的稳定身份。
     */
    abstract val containingEnumClassId: ClassId?

    /**
     * 枚举构造器 payload 的类型列表。
     *
     * 无参构造器时为空列表。
     */
    abstract    val payloadTypes: List<CaType>

    /**
     * 是否显式携带 payload。
     */
    val hasPayload: Boolean
        get() = payloadTypes.isNotEmpty()
}
