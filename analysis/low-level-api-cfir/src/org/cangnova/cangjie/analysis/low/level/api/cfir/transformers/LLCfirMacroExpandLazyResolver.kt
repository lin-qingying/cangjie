package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/**
 * 仓颉 low-level 不在 IDE 侧自行执行宏展开。
 * 这里对齐主干 `CfirMacroExpandResolveProcessor` 在 `macroExpandAction == null` 时的 pass-through 语义。
 */
internal object LLCfirMacroExpandLazyResolver : LLCfirLazyResolver(CfirResolvePhase.MACRO_EXPAND) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirMacroExpandTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) = Unit
}

private class LLCfirMacroExpandTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.MACRO_EXPAND) {
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) = Unit
}
