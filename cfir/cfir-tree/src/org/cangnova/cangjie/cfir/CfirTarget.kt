package org.cangnova.cangjie.cfir

/**
 * 对齐 Kotlin FIR 的内部 target 抽象。
 *
 * 当前仓颉前端还没有公开显式 label/target 语法，但 jump 绑定、控制流和诊断仍需要稳定的内部目标模型。
 */
interface CfirTarget<E : CfirTargetElement> {
    val labelName: String?

    val labeledElement: E

    fun bind(element: E)
}
