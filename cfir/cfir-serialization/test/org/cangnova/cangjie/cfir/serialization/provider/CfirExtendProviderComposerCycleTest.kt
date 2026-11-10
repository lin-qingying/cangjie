package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * 循环模块依赖下的 extend provider 组合必须终止，且不能过度抑制 own 查询。
 *
 * 对应 `CfirExtendProviderComposer.lazyProvider` 的重入防护。
 */
class CfirExtendProviderComposerCycleTest {
    @Test
    fun twoCycleTerminatesAndQueriesBothOwnProvidersOnce() {
        val firstQueries = AtomicInteger()
        val secondQueries = AtomicInteger()
        val firstOwn = countingProvider(firstQueries)
        val secondOwn = countingProvider(secondQueries)

        var first: CfirExtendProvider? = null
        var second: CfirExtendProvider? = null
        first = CfirExtendProviderComposer.lazyCombine(firstOwn) { listOf(second!!) }
        second = CfirExtendProviderComposer.lazyCombine(secondOwn) { listOf(first!!) }

        assertTrue(first!!.getAllExtends().isEmpty())
        assertEquals(1, firstQueries.get(), "own provider 应被查询一次（不能被重入防护整体抑制）")
        assertEquals(1, secondQueries.get(), "own provider 应被查询一次")

        assertTrue(second!!.getAllExtends().isEmpty())
        assertEquals(2, firstQueries.get())
        assertEquals(2, secondQueries.get())
    }

    @Test
    fun threeCycleTerminates() {
        var first: CfirExtendProvider? = null
        var second: CfirExtendProvider? = null
        var third: CfirExtendProvider? = null
        first = CfirExtendProviderComposer.lazyCombine(emptyProvider()) { listOf(second!!) }
        second = CfirExtendProviderComposer.lazyCombine(emptyProvider()) { listOf(third!!) }
        third = CfirExtendProviderComposer.lazyCombine(emptyProvider()) { listOf(first!!) }

        assertTrue(first!!.getAllExtends().isEmpty())
        assertTrue(
            first!!.getExtendsForTarget(
                CfirExtendTargetKey.ClassLike(ClassId(FqName("test"), FqName("Target"))),
            ).isEmpty(),
        )
    }

    private fun emptyProvider(): CfirExtendProvider = object : CfirExtendProvider {
        override fun getAllExtends(): List<CfirExtend> = emptyList()
        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = emptyList()
        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = emptyList()
        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = emptyList()
        override fun getExtendsForBuiltinType(kind: org.cangnova.cangjie.cfir.types.PrimitiveTypeKind): List<CfirExtend> = emptyList()
        override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = null
        override fun getPackageFqName(extend: CfirExtend): FqName? = null
        override fun getContainingFile(extend: CfirExtend): CfirFile? = null
        override fun getExtendByStructuralKey(key: String): CfirExtend? = null
    }

    private fun countingProvider(counter: AtomicInteger): CfirExtendProvider = object : CfirExtendProvider {
        override fun getAllExtends(): List<CfirExtend> {
            counter.incrementAndGet()
            return emptyList()
        }

        override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = emptyList()
        override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = emptyList()
        override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = emptyList()
        override fun getExtendsForBuiltinType(kind: org.cangnova.cangjie.cfir.types.PrimitiveTypeKind): List<CfirExtend> = emptyList()
        override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = null
        override fun getPackageFqName(extend: CfirExtend): FqName? = null
        override fun getContainingFile(extend: CfirExtend): CfirFile? = null
        override fun getExtendByStructuralKey(key: String): CfirExtend? = null
    }
}
