package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirResolution
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.CfirUnresolvedSymbolException
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbols.ResolutionDiagnostics
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInvalidDeclarationSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [memoizedResolutionLazy] 的记忆化契约测试。
 *
 * 这条契约是「缺失是一等状态」能够成立的前提：Kotlin 的 `lazy` 在初始化抛异常时**不写缓存**，
 * 若直接用它包装恢复结果，每次属性访问都会重跑整条恢复链（补全里一次渲染可能访问十几次）。
 * 反过来，如果对可恢复缺失也做永久记忆，一次「索引还没就绪」就会被冻结成整个会话的缺失——
 * session 只在模块修改事件时重建，不会因为 dumb 模式结束而重建。
 *
 * 因此这里钉住三件事：
 * 1. 不可恢复缺失 → 永久记忆；
 * 2. 可恢复缺失 → TTL 内记忆，TTL 到期后重算；
 * 3. 成功结果 → 永久记忆。
 */
class MemoizedResolutionLazyTest {

    /**
     * 构造一个指定原因的缺失结果。
     */
    private fun unresolved(reason: CaSymbolUnavailabilityReason): CfirResolution<CfirBasedSymbol<*>> =
        CfirResolution.Unresolved(reason, ResolutionDiagnostics("declaration is not in the index"))

    /**
     * 代码缺陷类缺失只需计算一次：原因不会因为再查一次而改变，重复计算只会重复告警。
     */
    @Test
    fun `unrecoverable miss is remembered`() {
        var computeCount = 0
        val lazySymbol = memoizedResolutionLazy<CfirBasedSymbol<*>> {
            computeCount++
            unresolved(CaSymbolUnavailabilityReason.UNHANDLED_DECLARATION_KIND)
        }

        repeat(3) {
            assertThrows(CfirUnresolvedSymbolException::class.java) { lazySymbol.value }
        }

        assertEquals(1, computeCount, "不可恢复缺失不应重复执行恢复链")
        assertTrue(lazySymbol.isInitialized(), "缺失结论本身应被记忆化")
    }

    /**
     * 可恢复缺失在 TTL 内只计算一次：补全期间同一符号的多次属性访问不应层层重算。
     */
    @Test
    fun `recoverable miss is remembered within ttl`() {
        var computeCount = 0
        val lazySymbol = memoizedResolutionLazy<CfirBasedSymbol<*>> {
            computeCount++
            unresolved(CaSymbolUnavailabilityReason.MISSING_DECLARATION)
        }

        repeat(3) {
            assertThrows(CfirUnresolvedSymbolException::class.java) { lazySymbol.value }
        }

        assertEquals(1, computeCount, "TTL 内的可恢复缺失应被记忆化")
    }

    /**
     * 可恢复缺失在 TTL 到期后必须重算：索引就绪后的查询要能看到符号，而不是一直拿到旧结论。
     */
    @Test
    fun `recoverable miss is re-evaluated after ttl`() {
        var computeCount = 0
        val lazySymbol = memoizedResolutionLazy<CfirBasedSymbol<*>> {
            computeCount++
            unresolved(CaSymbolUnavailabilityReason.MISSING_DECLARATION)
        }

        assertThrows(CfirUnresolvedSymbolException::class.java) { lazySymbol.value }
        assertEquals(1, computeCount)

        // 记忆化窗口是 500ms（见 MemoizedResolutionLazy.RECOVERABLE_MISS_TTL_NANOS），留出余量。
        Thread.sleep(700)

        assertThrows(CfirUnresolvedSymbolException::class.java) { lazySymbol.value }
        assertEquals(2, computeCount, "TTL 到期后应重新计算，否则缺失会被冻结到会话结束")
    }

    /**
     * 成功结果永久记忆：符号一旦恢复出来就是稳定结论，不需要按 TTL 重算。
     */
    @Test
    fun `success is remembered`() {
        var computeCount = 0
        val lazySymbol = memoizedResolutionLazy<CfirBasedSymbol<*>> {
            computeCount++
            CfirResolution.Resolved(CfirInvalidDeclarationSymbol())
        }

        repeat(3) { lazySymbol.value }

        assertEquals(1, computeCount)
    }
}
