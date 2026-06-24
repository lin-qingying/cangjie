package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 函数返回目标。
 *
 * 对齐 Kotlin FIR 的 `FirFunctionTarget`：
 * - `return` 并不是直接持有函数声明本体，而是先持有一个稳定的 target 对象；
 * - raw builder 在进入函数/匿名函数体之前先把 target 压栈；
 * - 构建完函数节点后再把 target 绑定回真实函数声明。
 *
 * 这样 `return` 的归属就不会依赖“最近语法块猜测”，而是依赖一条显式的框架级绑定链。
 */
class CfirFunctionTarget(
    labelName: String? = null,
    /**
     * 当前 target 是否属于 lambda/匿名函数。
     */
    val isLambda: Boolean,
) : CfirAbstractTarget<CfirFunction>(labelName) {
    /**
     * 延迟绑定的函数符号。
     */
    private lateinit var targetSymbol: CfirFunctionSymbol<*>

    /**
     * 当前 target 绑定的函数声明。
     */
    override var _labeledElement: CfirFunction
        get() = targetSymbol.cfir
        set(value) {
            targetSymbol = value.symbol
        }
}
