package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 已完成名字查找、等待期望函数类型的命名函数引用。
 *
 * Kotlin 的 `FirCallableReferenceAccess` 可直接凭 `::` 语法进入 postponed 阶段；仓颉的
 * `foo` / `receiver.foo` 必须先排除变量、属性和类型限定符，才能确定相同的解析阶段。
 * 此时保留同一 tower group 的函数候选，不选择代表候选，也不产生尚未成立的歧义诊断。
 */
internal class CfirContextDependentNamedReference(
    override val source: CjSourceElement?,
    override val name: Name,
    val candidates: List<Candidate>,
) : CfirNamedReference {
    init {
        require(candidates.isNotEmpty()) { "A context-dependent function reference must retain its candidates" }
    }

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirElement = this
}
