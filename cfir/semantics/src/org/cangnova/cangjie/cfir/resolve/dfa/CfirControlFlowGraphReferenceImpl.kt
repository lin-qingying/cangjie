package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.visitors.CfirTransformer
import org.cangnova.cangjie.cfir.visitors.CfirVisitor
import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 对位 Kotlin `FirControlFlowGraphReferenceImpl`。
 *
 * CFG 引用属于基础 DFA/CFG 模型，因此放在 `cfir:semantics`，
 * 由各解析阶段共享，而不是留在具体 resolve 实现层。
 *
 * @property controlFlowGraph 被引用的控制流图。
 */
class CfirControlFlowGraphReferenceImpl(
    /**
     * 被引用的控制流图。
     */
    val controlFlowGraph: ControlFlowGraph,
) : CfirControlFlowGraphReference() {
    /** CFG 引用是合成节点，不直接绑定源码位置。 */
    override val source: CjSourceElement? get() = null

    /** CFG引用没有独立源锚点，不能迁移到某个源文件节点。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun replaceSource(newSource: CjSourceElement?) {
        require(newSource == null)
    }

    /**
     * CFG 引用没有 CFIR 子节点。
     */
    override fun <R, D> acceptChildren(visitor: CfirVisitor<R, D>, data: D) {}

    /** CFG 仍保留转化前的求值节点；来源迁移访问全部图节点，而普通语法 visitor 不重复消费它们。 */
    @org.cangnova.cangjie.cfir.CfirImplementationDetail
    override fun <R, D> acceptSourceChildren(visitor: CfirVisitor<R, D>, data: D) {
        val visited = Collections.newSetFromMap(IdentityHashMap<ControlFlowGraph, Boolean>())
        fun visit(graph: ControlFlowGraph) {
            if (!visited.add(graph)) return
            graph.nodes.forEach { it.fir.accept(visitor, data) }
            graph.subGraphs.forEach(::visit)
        }
        visit(controlFlowGraph)
    }

    /**
     * CFG 引用没有可变换子节点，直接返回自身。
     */
    override fun <D> transformChildren(transformer: CfirTransformer<D>, data: D): CfirControlFlowGraphReference {
        return this
    }
}

/**
 * 从抽象 CFG 引用中取出具体控制流图。
 */
val CfirControlFlowGraphReference.controlFlowGraph: ControlFlowGraph?
    get() = (this as? CfirControlFlowGraphReferenceImpl)?.controlFlowGraph
