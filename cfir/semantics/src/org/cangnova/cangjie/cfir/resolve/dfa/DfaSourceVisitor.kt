package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CFGNode
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirSourceVisitor
import java.util.Collections
import java.util.IdentityHashMap

/**
 * DFA 保留状态的来源访问协议。Kotlin 的 snapshot copier 保留 node.fir；仓颉的独立
 * target 换代还必须访问图、缓存、flow 及变量中的 IR 和类型载荷，不能靠普通 IR visitor 推测。
 * 所有入口只读取已有事实，调用方负责源槽位的准备、验证与共同提交。
 */
interface DfaSourceVisitor : CfirSourceVisitor {
    fun visitGraph(graph: ControlFlowGraph)
    fun visitNode(node: CFGNode<*>)
    fun visitVariable(variable: DataFlowVariable)
    fun visitFlow(flow: Flow)
}

/** 按真实对象身份去重；实际丢弃图由调用方的失效计划排除，不复制或完成正在构造的图。 */
fun createDfaSourceVisitor(
    onElement: (CfirElement) -> Unit,
    onType: (ConeCangJieType) -> Unit,
    canVisitGraph: (ControlFlowGraph) -> Boolean,
): DfaSourceVisitor = object : DfaSourceVisitor {
    private val graphs = Collections.newSetFromMap(IdentityHashMap<ControlFlowGraph, Boolean>())
    private val nodes = Collections.newSetFromMap(IdentityHashMap<CFGNode<*>, Boolean>())
    private val variables = Collections.newSetFromMap(IdentityHashMap<DataFlowVariable, Boolean>())
    private val flows = Collections.newSetFromMap(IdentityHashMap<Flow, Boolean>())
    private val types = Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>())

    override fun visitElement(element: CfirElement) = onElement(element)

    override fun visitType(type: ConeCangJieType) {
        if (types.add(type)) onType(type)
    }

    override fun visitGraph(graph: ControlFlowGraph) {
        if (canVisitGraph(graph) && graphs.add(graph)) graph.acceptSourceChildren(this)
    }

    override fun visitNode(node: CFGNode<*>) {
        if (!canVisitGraph(node.owner) || !nodes.add(node)) return
        visitGraph(node.owner)
        node.acceptSourceChildren(this)
    }

    override fun visitVariable(variable: DataFlowVariable) {
        if (variables.add(variable)) variable.acceptSourceChildren(this)
    }

    override fun visitFlow(flow: Flow) {
        if (flows.add(flow)) flow.acceptSourceChildren(this)
    }
}
