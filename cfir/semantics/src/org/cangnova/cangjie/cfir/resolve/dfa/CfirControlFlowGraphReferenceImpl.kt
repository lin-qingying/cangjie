package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 对位 Kotlin `FirControlFlowGraphReferenceImpl`。
 *
 * CFG 引用属于基础 DFA/CFG 模型，因此放在 `cfir:semantics`，
 * 由各解析阶段共享，而不是留在具体 resolve 实现层。
 */
class CfirControlFlowGraphReferenceImpl(
    val controlFlowGraph: ControlFlowGraph,
) : CfirControlFlowGraphReference() {
    override val source: CjSourceElement? get() = null

    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirControlFlowGraphReference {
        return this
    }
}

val CfirControlFlowGraphReference.controlFlowGraph: ControlFlowGraph?
    get() = (this as? CfirControlFlowGraphReferenceImpl)?.controlFlowGraph
