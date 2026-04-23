package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.EdgeLabel

sealed class FlowPath {
    data object Default : FlowPath()

    data class CfgEdge(val label: EdgeLabel, val fir: CfirElement) : FlowPath()
}
