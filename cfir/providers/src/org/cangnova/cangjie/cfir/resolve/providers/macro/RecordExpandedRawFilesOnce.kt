package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirMacroExpression
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * Source [CfirProviderImpl] 的唯一注册入口。
 *
 * 约束（baseline 第 5 节"Provider 状态机"）：
 * - 在 construction 前 provider 必须处于 `EMPTY` 状态；
 * - 本函数将 provider 从 `EMPTY` 单调推进至 `FINALIZED`；
 * - finalized 后再次调用将抛出 `IllegalStateException`。
 *
 * `recordExpandedRawFilesOnce` 是 source CFIR 文件进入 ordinary resolve 的唯一桥梁。
 * 任何旁路写入（例如绕过 [MacroConstructionService] 直接拿到 `List<CfirFile>` 灌 provider）
 * 都被视为对 baseline 硬性边界的违反。
 *
 * Baseline 第 5 节"`recordExpandedRawFilesOnce` 检查"列表：
 * - source provider state（已由 [CfirProviderImpl] 状态机强制）
 * - no `CfirMacroExpression` macro call（架构 guard，本函数内置）
 *
 * 当前冻结基线默认强制拒绝旧 [CfirMacroExpression] 进入 final provider：
 * - `true`：发现残留即抛出（Batch 10 默认值）；
 * - `false`：仅供显式架构审计/诊断调用方选择 warn 后继续注册，生产默认路径不得依赖该分支。
 *
 * @param provider 当前 session 的 source provider
 * @param files 经 [MacroConstructionService] 产出的可注册文件
 * @param registry 构造期 registry；当前 batch 用于将来与 provider 关联，
 *                 Batch 9 起将持久化到 session 上
 * @param enforceLegacyMacroExpressionAbsent
 *        若为 `true`，残留 [CfirMacroExpression] 视为非法，直接抛 [IllegalStateException]。
 */
fun recordExpandedRawFilesOnce(
    provider: CfirProviderImpl,
    files: RecordableRawCfirFiles,
    @Suppress("UNUSED_PARAMETER") registry: MacroExpansionRegistry,
    enforceLegacyMacroExpressionAbsent: Boolean = true,
) {
    val residualMacroExpressions = countLegacyMacroExpressions(files.files)
    if (residualMacroExpressions > 0) {
        val msg = "recordExpandedRawFilesOnce: $residualMacroExpressions residual " +
            "CfirMacroExpression node(s) detected in final CFIR; macro construction step did not " +
            "fully resolve all macro calls (baseline 第 2 节硬性边界 #5 / 第 9 节)."
        if (enforceLegacyMacroExpressionAbsent) {
            error(msg)
        }
        // 显式关闭守卫时仅 warn 到 stderr；默认生产路径不应进入该分支。
        System.err.println("[macro-construction] WARN $msg")
    }

    provider.recordExpandedFilesOnce(files)
}

private fun countLegacyMacroExpressions(files: List<CfirFile>): Int {
    var count = 0
    val visitor = object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this, null)
        }

        override fun visitMacroExpression(macroExpression: CfirMacroExpression) {
            count++
            super.visitMacroExpression(macroExpression)
        }
    }
    for (file in files) {
        file.accept(visitor, null)
    }
    return count
}
