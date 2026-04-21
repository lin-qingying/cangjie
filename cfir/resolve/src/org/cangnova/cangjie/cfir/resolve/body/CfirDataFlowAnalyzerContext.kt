package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer
import org.cangnova.cangjie.cfir.resolve.dfa.VariableStorage
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphBuilder
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraphCopier
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * CFIR 数据流上下文，对位 Kotlin FIR `DataFlowAnalyzerContext`。
 *
 * 正式状态面只保留四项：
 * - [graphBuilder]：CFG 构造器
 * - [variableAssignmentAnalyzer]：局部变量赋值分析器
 * - [variableStorage]：数据流变量承载
 * - [assignmentCounter]：赋值编号计数器
 *
 * 不再保留旧的 frame-only 兼容层。
 */
class CfirDataFlowAnalyzerContext(
    var graphBuilder: ControlFlowGraphBuilder = ControlFlowGraphBuilder(),
    var variableAssignmentAnalyzer: CfirLocalVariableAssignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer(),
    var variableStorage: VariableStorage = VariableStorage(),
    var assignmentCounter: Int = 0,
) {
    /**
     * 创建可恢复的 DFA 快照。
     *
     * 语义对位 Kotlin FIR：
     * - 使用真实 [ControlFlowGraphCopier] 深拷贝 CFG 状态
     * - 深拷贝赋值分析器与变量存储
     * - 原样复制 [assignmentCounter]
     * - 返回真实 [CfirDataFlowAnalyzerContextSnapshot.graphMapping]
     */
    @OptIn(CfgInternals::class)
    fun createSnapshot(firMapper: SnapshotCfirMapper): CfirDataFlowAnalyzerContextSnapshot {
        val graphCopier = ControlFlowGraphCopier()
        val graphBuilderSnapshot = graphBuilder.createSnapshot(graphCopier)
        graphCopier.finish()

        return CfirDataFlowAnalyzerContextSnapshot(
            context = CfirDataFlowAnalyzerContext(
                graphBuilder = graphBuilderSnapshot,
                variableAssignmentAnalyzer = variableAssignmentAnalyzer.createSnapshot(firMapper),
                variableStorage = variableStorage.createSnapshot(),
                assignmentCounter = assignmentCounter,
            ),
            graphMapping = graphCopier.graphMapping,
        )
    }

    /**
     * 先 reset，再直接接管 source 的状态引用。
     */
    fun resetFrom(source: CfirDataFlowAnalyzerContext) {
        reset()
        graphBuilder = source.graphBuilder
        variableAssignmentAnalyzer = source.variableAssignmentAnalyzer
        variableStorage = source.variableStorage
        assignmentCounter = source.assignmentCounter
    }

    /**
     * 仅重置 builder / assignment analyzer / variable storage。
     *
     * 与 Kotlin FIR 对位：`assignmentCounter` 不在这里回零。
     */
    fun reset() {
        graphBuilder.reset()
        variableAssignmentAnalyzer.reset()
        variableStorage.reset()
    }

    fun newAssignmentIndex(): Int {
        return assignmentCounter++
    }
}

class CfirDataFlowAnalyzerContextSnapshot(
    val context: CfirDataFlowAnalyzerContext,
    val graphMapping: Map<ControlFlowGraph, ControlFlowGraph>,
)

/**
 * 对位 Kotlin `SnapshotFirMapper`。
 */
interface SnapshotCfirMapper {
    fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T
    fun <T : CfirElement> mapElement(element: T): T
}
