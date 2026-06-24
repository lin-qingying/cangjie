package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.EdgeLabel

/**
 * DFA flow 的分支路径标识。
 *
 * 同一个 CFG 节点可以保存默认 flow 和按边标签区分的备用 flow；该模型用于索引这些路径。
 */
sealed class FlowPath {
    /** 默认控制流路径。 */
    data object Default : FlowPath()

    /**
     * 某条 CFG 边对应的备用 flow 路径。
     *
     * @property label 边标签。
     * @property fir 产生该边语义的 CFIR 元素。
     */
    data class CfgEdge(val label: EdgeLabel, val fir: CfirElement) : FlowPath()
}
