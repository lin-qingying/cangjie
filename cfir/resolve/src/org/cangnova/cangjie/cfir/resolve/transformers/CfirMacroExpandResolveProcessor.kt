package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * `MACRO_EXPAND` 阶段处理器 —— 当前 batch 起改为 **no-op**。
 *
 * Baseline 第 1 节将宏展开从 ordinary [CfirResolvePhase] 移出，改为
 * source provider 注册前的 construction step
 * （由 [org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService] 完成）。
 * 当 pipeline 进入到 ordinary resolve 时：
 * - 源文件已经经过 macro construction step；
 * - 它们已经通过
 *   [org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce]
 *   注册到 source provider；
 * - 当前 phase **不允许**再 mutate provider，也不应再调用任何宏展开实现。
 *
 * 因此这里只作为占位符存在。Batch 3 会删除整个 phase + 此 processor。
 *
 * [macroExpandAction] 仍接受为构造参数，仅为 Batch 1-2 过渡期保持调用方签名兼容，
 * 在内部完全忽略。
 */
@Suppress("UNUSED_PARAMETER")
class CfirMacroExpandResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
    macroExpandAction: MacroExpandAction?,
) : CfirFileReplacingResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.MACRO_EXPAND,
) {
    override fun processAndReplace(files: List<CfirFile>): List<CfirFile> {
        // No-op：macro construction step 已在 provider 注册前完成展开。
        return files
    }
}
