package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer
import org.cangnova.cangjie.cfir.resolve.dfa.DfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.Flow
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphCopier
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * CFIR 数据流上下文，对位 Kotlin FIR `DataFlowAnalyzerContext`。
 *
 * 状态由 CFG、mini 赋值分析器和赋值编号持有；变量规范表属于每条 Flow。
 * - [graphBuilder]：CFG 构造器
 * - [variableAssignmentAnalyzer]：局部变量赋值分析器
 * - [assignmentCounter]：赋值编号计数器
 *
 * 不再保留旧的 frame-only 兼容层。
 */
class CfirDataFlowAnalyzerContext(
    /** CFG 构造器。 */
    var graphBuilder: ControlFlowGraphBuilder = ControlFlowGraphBuilder(),
    /** 局部变量赋值分析器。 */
    var variableAssignmentAnalyzer: CfirLocalVariableAssignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer(),
    /** 赋值编号计数器。 */
    var assignmentCounter: Int = 0,
) {
    /** 访问全部保留状态，由各实际 owner 枚举自身的完整表与栈，不在 LL 层复制私有状态知识。 */
    fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        graphBuilder.acceptSourceChildren(visitor)
        variableAssignmentAnalyzer.acceptSourceChildren(visitor)
    }

    /**
     * 创建可恢复的 DFA 快照。
     *
     * 语义对位 Kotlin FIR：
     * - 使用真实 [ControlFlowGraphCopier] 深拷贝 CFG 状态
     * - 深拷贝赋值分析器；CFG 节点共享其不可变 Flow，分叉独立修改全部变量表
     * - 原样复制 [assignmentCounter]
     * - 返回真实 [CfirDataFlowAnalyzerContextSnapshot.graphMapping]
     */
    @OptIn(CfgInternals::class)
    fun createSnapshot(firMapper: SnapshotCfirMapper, smartCastPosition: Flow?): CfirDataFlowAnalyzerContextSnapshot {
        val graphCopier = ControlFlowGraphCopier()
        val graphBuilderSnapshot = graphBuilder.createSnapshot(graphCopier)
        graphCopier.finish()

        return CfirDataFlowAnalyzerContextSnapshot(
            context = CfirDataFlowAnalyzerContext(
                graphBuilder = graphBuilderSnapshot,
                variableAssignmentAnalyzer = variableAssignmentAnalyzer.createSnapshot(firMapper),
                assignmentCounter = assignmentCounter,
            ),
            graphMapping = graphCopier.graphMapping,
            smartCastPosition = smartCastPosition,
        )
    }

    /**
     * 先 reset，再直接接管 source 的状态引用。
     */
    fun resetFrom(source: CfirDataFlowAnalyzerContext) {
        reset()
        graphBuilder = source.graphBuilder
        variableAssignmentAnalyzer = source.variableAssignmentAnalyzer
        assignmentCounter = source.assignmentCounter
    }

    /**
     * 仅重置 builder / assignment analyzer；Flow 随实际 CFG 生命周期持有。
     *
     * 与 Kotlin FIR 对位：`assignmentCounter` 不在这里回零。
     */
    fun reset() {
        graphBuilder.reset()
        variableAssignmentAnalyzer.reset()
    }

    /**
     * 在独立 DFA/CFG 上下文中执行 speculative body resolve。
     *
     * overload-by-lambda 候选试跑需要真实 CFG 来收集 lambda return 表达式，但试跑结果不应写入
     * 外层正在构造的 CFG / 变量赋值状态。这里保存并恢复原状态对象引用，避免对大型嵌套 CFG 做深拷贝。
     */
    fun <T> withIsolatedContext(block: () -> T): T {
        val originalGraphBuilder = graphBuilder
        val originalVariableAssignmentAnalyzer = variableAssignmentAnalyzer
        val originalAssignmentCounter = assignmentCounter

        graphBuilder = ControlFlowGraphBuilder()
        variableAssignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer()
        assignmentCounter = originalAssignmentCounter

        return try {
            block()
        } finally {
            graphBuilder = originalGraphBuilder
            variableAssignmentAnalyzer = originalVariableAssignmentAnalyzer
            assignmentCounter = originalAssignmentCounter
        }
    }

    /** 分配新的变量赋值序号，并推进计数器。 */
    fun newAssignmentIndex(): Int {
        return assignmentCounter++
    }
}

/** DFA 上下文快照，包含上下文副本和 CFG 复制映射。 */
class CfirDataFlowAnalyzerContextSnapshot(
    /** 快照中的独立 DFA 上下文。 */
    val context: CfirDataFlowAnalyzerContext,
    /** 原 CFG 到复制后 CFG 的映射。 */
    val graphMapping: Map<ControlFlowGraph, ControlFlowGraph>,
    /** 保存实际解析位置；调用实参后可以与 builder.lastNode 不同，不能在恢复时重新猜测。 */
    val smartCastPosition: Flow?,
)

/**
 * 对位 Kotlin `SnapshotFirMapper`。
 */
interface SnapshotCfirMapper {
    /** 把原 symbol 映射为快照中的对应 symbol。 */
    fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T
    /** 把原 CFIR 元素映射为快照中的对应元素。 */
    fun <T : CfirElement> mapElement(element: T): T
}
