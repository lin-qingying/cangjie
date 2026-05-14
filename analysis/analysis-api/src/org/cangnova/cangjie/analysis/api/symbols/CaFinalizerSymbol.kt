package org.cangnova.cangjie.analysis.api.symbols

import org.cangnova.cangjie.name.ClassId

/**
 * finalizer（析构器）符号。
 *
 * 在仓颉中 finalizer 是声明在 class 体内、对象销毁时由运行时触发的特殊函数：
 * - 没有显式调用方，不属于普通 callable 调用图；
 * - 至多有一个 finalizer，且无返回值。
 *
 * 这里复用 [CaFunctionSymbol] 的能力，仅额外暴露所属类型身份。
 */
abstract class CaFinalizerSymbol : CaFunctionSymbol() {
    /**
     * 所属类型的稳定身份。
     *
     * 对匿名/局部类的 finalizer 可能为 `null`。
     */
    abstract val containingClassId: ClassId?
}
