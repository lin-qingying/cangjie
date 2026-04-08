package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider

/**
 * 宏展开解析处理器。
 *
 * 位于 [CfirResolvePhase.MACRO_EXPAND] 阶段，在 IMPORTS 之后、SUPER_TYPES 之前执行。
 * 委托 [MacroExpandAction] 执行实际宏展开，展开后将新文件重新注册到 [CfirProviderImpl]。
 *
 * 若未提供 [MacroExpandAction]（如 IDE 场景），则 pass-through 返回原文件列表。
 */
class CfirMacroExpandResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
    private val macroExpandAction: MacroExpandAction?,
) : CfirFileReplacingResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.MACRO_EXPAND,
) {
    override fun processAndReplace(files: List<CfirFile>): List<CfirFile> {
        if (macroExpandAction == null) return files

        val expandedFiles = macroExpandAction.expand(session, files)
        if (expandedFiles === files) return files

        // 展开后的新文件需要重新注册到 CfirProvider，供后续阶段的符号查找使用
        val provider = session.cfirProvider as CfirProviderImpl
        expandedFiles.forEach(provider::recordFile)
        return expandedFiles
    }
}
