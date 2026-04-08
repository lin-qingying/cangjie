package org.cangnova.cangjie.cfir

/**
 * `CfirTarget` 的通用实现骨架。
 *
 * 当前阶段 `labelName` 只是保留位，raw builder 会统一填 `null`；
 * 后续若补显式 label 语义，可直接在现有结构上扩展。
 */
abstract class CfirAbstractTarget<E : CfirTargetElement>(
    override val labelName: String?,
) : CfirTarget<E> {
    protected abstract var _labeledElement: E

    final override val labeledElement: E
        get() = _labeledElement

    override fun bind(element: E) {
        _labeledElement = element
    }
}
