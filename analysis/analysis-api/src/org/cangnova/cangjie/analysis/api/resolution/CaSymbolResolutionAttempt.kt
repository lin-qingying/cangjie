package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * [CaSymbolResolutionAttempt] 表示一次元素级符号解析尝试的结果。
 *
 * 与 [CaCallResolutionAttempt] 分开建模：调用解析给出 [CaCall]，
 * 元素级解析只给出 [CaSymbol] 集合或带候选符号的诊断。
 * 文件划分对齐 Kotlin `KaSymbolResolutionAttempt.kt`。
 */
sealed interface CaSymbolResolutionAttempt : CaLifetimeOwner

/**
 * 元素级解析成功，携带解析出的符号集合（多目标时多于一个）。
 *
 * 与 [CaCallResolutionSuccess] 一致，用 `@SubclassOptInRequired` 把实现细节挡在实现模块内。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaSymbolResolutionSuccess : CaSymbolResolutionAttempt {
    /**
     * 解析成功得到的符号集合。
     */
    val symbols: List<CaSymbol>
}

/**
 * 元素级解析失败，携带诊断与真实参与决策的候选符号。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaSymbolResolutionError : CaSymbolResolutionAttempt {
    /**
     * 描述本次符号解析失败的诊断。
     */
    val diagnostic: CaDiagnostic

    /**
     * 解析过程中真实参与决策的候选符号；无法恢复时为空。
     */
    val candidateSymbols: List<CaSymbol>
}

/**
 * 取本次解析尝试暴露的符号集合：成功时为目标符号，失败时退化为候选符号。
 */
val CaSymbolResolutionAttempt.symbols: List<CaSymbol>
    get() = when (this) {
        is CaSymbolResolutionSuccess -> symbols
        is CaSymbolResolutionError -> candidateSymbols
    }