package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext

/**
 * code fragment 解析时挂载的上下文快照。
 *
 * Kotlin FIR 这里还会携带 smart-cast 快照。
 * 当前仓颉主干没有接入那套 CFG / DFA 主结构，因此主接口只暴露已经稳定存在的
 * tower data context，避免在主干里伪造并不存在的数据流模型。
 */
interface CfirCodeFragmentContext {
    /** code fragment 恢复解析时使用的 tower data context 快照。 */
    val towerDataContext: CfirTowerDataContext
}

/** code fragment context 在声明数据注册表中的 key。 */
private object CodeFragmentContextDataKey : CfirDeclarationDataKey()

/** code fragment 声明上挂载的解析上下文快照。 */
var CfirCodeFragment.codeFragmentContext: CfirCodeFragmentContext? by CfirDeclarationDataRegistry.data(CodeFragmentContextDataKey)
