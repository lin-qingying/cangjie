package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirResolution
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirUnresolvedSymbolException
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.ResolutionDiagnostics
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CaCfirSymbol.cfirSymbolOrNull] 的契约测试。
 *
 * 回归背景一：该入口最初只声明在子接口 `CaCfirPsiSymbol` 上，而 `CaSymbol` 上另有一个同名扩展
 * 做适配；扩展体 `(this as? CaCfirSymbol<*>)?.cfirSymbolOrNull` 在 `CaCfirSymbol` 上解析不到成员，
 * 于是解析回扩展自身——一次真实调用就是 `StackOverflowError`，而编译器不会报警
 * （递归调用本身是合法的）。
 *
 * 回归背景二：`compiled` 声明在索引里缺失时，属性层必须能拿到 `null` 并按类型捕获异常，
 * 而不是让 `IllegalArgumentException` 沿补全渲染链上抛。这里用一个「恢复结果恒为
 * Unresolved」的符号替身钉住这条语义——不需要构造真实库或损坏的 `.cjo`。
 */
class CaCfirSymbolOrNullContractTest {

    /**
     * 降级入口必须由 `CaCfirSymbol` 自身声明。
     */
    @Test
    fun `degraded accessor is declared on the base symbol protocol`() {
        val declaredOnBase = CaCfirSymbol::class.java.methods.any { it.name == "getCfirSymbolOrNull" }

        assertTrue(
            declaredOnBase,
            "cfirSymbolOrNull 必须声明在 CaCfirSymbol 上；只声明在子接口时，" +
                "CaSymbol 上的同名扩展会解析回自身并无限递归",
        )
    }

    /**
     * 缺失必须表达成 `null`；强制取符号时抛出的是**可分类**的异常。
     */
    @Test
    fun `missing declaration degrades to null and keeps a classified exception`() {
        val symbol = PendingLibrarySymbol()

        assertNull(symbol.cfirSymbolOrNull, "缺失必须表达成 null，而不是把异常抛给属性层")

        val failure = assertThrows(CfirUnresolvedSymbolException::class.java) { symbol.cfirSymbol }
        assertEquals(CaSymbolUnavailabilityReason.MISSING_DECLARATION, failure.reason)
    }

    /**
     * 「恢复结果恒为 Unresolved」的 PSI 承载符号替身。
     *
     * 只实现 `cfirSymbolOrNull` 需要的最小面：`analysisSession` / `containingModule` 一旦被读取就
     * 说明实现走错了路径，因此直接 `error(...)`，让误用立刻暴露。
     */
    private class PendingLibrarySymbol : CaCfirPsiSymbol<PsiElement, CfirBasedSymbol<*>> {
        override val backingPsi: PsiElement? = null

        override val lazyCfirSymbol: Lazy<CfirBasedSymbol<*>> = memoizedResolutionLazy {
            CfirResolution.Unresolved(
                reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
                diagnostics = ResolutionDiagnostics("library declaration is not in the index"),
            )
        }

        override val analysisSession: CaCfirSession
            get() = error("符号缺失的判定不应读取分析 session")

        override val origin: CaSymbolOrigin get() = CaSymbolOrigin.LIBRARY

        override val containingModule: CaModule
            get() = error("符号缺失的判定不应读取模块")

        override val psi: PsiElement? get() = null

        override fun createPointer(): CaSymbolPointer<CaSymbol> = error("缺失符号没有稳定指针")

        override val location: CaSymbolLocation get() = CaSymbolLocation.TOP_LEVEL
    }
}
