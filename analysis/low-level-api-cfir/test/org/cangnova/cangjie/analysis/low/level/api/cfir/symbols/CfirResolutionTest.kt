package org.cangnova.cangjie.analysis.low.level.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches.tolerateMissingCachedSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * [CfirResolution] 与缺失容错路径的契约测试。
 *
 * 覆盖三条框架级不变量：
 * 1. 缺失是**值**而不是异常，`map`/`flatMap` 在缺失时必须短路且不执行转换；
 * 2. 仍要求非空符号的旧调用点拿到的是**可分类**的异常，而不是无类型的 `IllegalArgumentException`；
 * 3. 「容错」只针对可预期缺失，来源/PSI 失效必须继续上抛——否则真问题会被静默吞掉。
 */
class CfirResolutionTest {

    /**
     * 一次可预期缺失：声明不在索引里。
     */
    private val missing = CfirResolution.Unresolved(
        reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
        diagnostics = ResolutionDiagnostics("declaration is not in the index"),
    )

    /**
     * 成功结果应能直接取值。
     */
    @Test
    fun `resolved exposes value and reports no reason`() {
        val resolved = CfirResolution.Resolved("symbol")

        assertEquals("symbol", resolved.valueOrNull)
        assertNull(resolved.unresolvedReason)
    }

    /**
     * 缺失结果应暴露原因且取值为 null。
     */
    @Test
    fun `unresolved exposes reason and has no value`() {
        assertNull(missing.valueOrNull)
        assertEquals(CaSymbolUnavailabilityReason.MISSING_DECLARATION, missing.unresolvedReason)
    }

    /**
     * 缺失必须短路：转换函数不得被执行，否则会把「没找到」变成「对着 null 继续算」。
     */
    @Test
    fun `map short-circuits on unresolved`() {
        var invoked = false

        val mapped = missing.map<Int, Int> {
            invoked = true
            it + 1
        }

        assertFalse(invoked, "map 不应在缺失结果上执行转换")
        assertNull(mapped.valueOrNull)
        assertSame(missing, mapped, "缺失结果应原样传递，不包装成新对象")
    }

    /**
     * 旧调用点通过 `getOrThrow` 拿到的异常必须携带分类，供上层按类型降级。
     */
    @Test
    fun `getOrThrow raises classified exception`() {
        val failure = assertThrows(CfirUnresolvedSymbolException::class.java) {
            missing.getOrThrow()
        }

        assertEquals(CaSymbolUnavailabilityReason.MISSING_DECLARATION, failure.reason)
        assertEquals("declaration is not in the index", failure.message)
    }

    /**
     * 可预期缺失在缓存边界上被降级为「未找到」。
     */
    @Test
    fun `expected absence is tolerated as missing symbol`() {
        val classId = ClassId(FqName("demo"), Name.identifier("Widget"))

        val result = tolerateMissingCachedSymbol<Int>(classId) {
            throw CaSymbolUnavailableException(
                reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
                message = "library type is not indexed yet",
            )
        }

        assertNull(result)
    }

    /**
     * 代码缺陷类缺失（恢复链没覆盖某种声明形态）不属于「可预期缺失」，必须继续上抛。
     */
    @Test
    fun `unexpected unavailability is rethrown`() {
        val classId = ClassId(FqName("demo"), Name.identifier("Widget"))

        val failure = assertThrows(CaSymbolUnavailableException::class.java) {
            tolerateMissingCachedSymbol<Int>(classId) {
                throw CaSymbolUnavailableException(
                    reason = CaSymbolUnavailabilityReason.UNHANDLED_DECLARATION_KIND,
                    message = "declaration kind is not handled by the searcher",
                )
            }
        }

        assertEquals(CaSymbolUnavailabilityReason.UNHANDLED_DECLARATION_KIND, failure.reason)
    }
}
