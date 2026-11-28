@file:OptIn(org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class)

package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.cjmpMatchFacts
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.calls.CallResolutionTestFixtures
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirNullSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchEvaluator
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchPlan
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 同名模块的声明仍由真实 moduleData 身份决定归属，不能进入其它模块的配对或匹配组。 */
class CfirCjmpModuleIdentityTest {
    @Test
    fun `common candidate from equally named ordinary dependency is excluded from refinement matching`() {
        val commonSession = CallResolutionTestFixtures.newTestSession()
        val ordinarySession = CallResolutionTestFixtures.newTestSession()
        val specificSession = specificSession(commonSession.moduleData, ordinarySession.moduleData)
        val common = function(commonSession, common = true)
        val ordinaryCommon = function(ordinarySession, common = true)
        val specific = function(specificSession, specific = true)
        install(commonSession, listOf(common))
        install(ordinarySession, listOf(ordinaryCommon))
        install(specificSession, listOf(specific))
        // 依赖 provider 有意先返回普通依赖；只有 refinement 身份过滤能排除这个同名候选。
        specificSession.register(
            DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY,
            DeclarationProvider(specificSession, listOf(ordinaryCommon, common)),
        )

        assertEquals(common.moduleData.name, ordinaryCommon.moduleData.name)
        assertNotSame(common.moduleData, ordinaryCommon.moduleData)
        assertEquals(listOf(common), CfirCjmpResolver.findCommonCandidates(specific, specificSession))

        val facts = CfirCjmpMatchRunner.prepareDeclaration(specific, specificSession)
        val plan = facts.plans.single() as CjmpMatchPlan.Declaration<*, *, *>
        assertEquals(listOf(common.symbol), plan.candidates.map { it.common })
        assertFalse(ordinaryCommon.symbol in plan.candidates.map { it.common })
        val aggregate = CjmpMatchEvaluator.evaluate(facts.plans)
        assertSame(specific.symbol, aggregate.implementations[common.symbol])
        assertTrue(aggregate.events.isEmpty())
        assertNull(specific.cjmpMatchFacts, "preparing immutable root facts must not publish session results")
    }

    @Test
    fun `specific declaration from equally named dependency is excluded from the source matching group`() {
        val commonSession = CallResolutionTestFixtures.newTestSession()
        val dependencySession = CallResolutionTestFixtures.newTestSession()
        val specificSession = specificSession(commonSession.moduleData, dependencySession.moduleData)
        val common = function(commonSession, common = true)
        val dependencySpecific = function(dependencySession, specific = true)
        val specific = function(specificSession, specific = true)
        install(commonSession, listOf(common))
        install(dependencySession, listOf(dependencySpecific))
        // use-site provider 的完整可见面包括 dependency，不能把可见性误当本模块声明归属。
        install(specificSession, listOf(dependencySpecific, specific))
        specificSession.register(
            DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY,
            DeclarationProvider(specificSession, listOf(common, dependencySpecific)),
        )

        assertEquals(specific.moduleData.name, dependencySpecific.moduleData.name)
        assertNotSame(specific.moduleData, dependencySpecific.moduleData)
        assertEquals(listOf(specific), CfirCjmpResolver.findSpecificDeclarationsInMatchOrder(specific, specificSession))

        val facts = CfirCjmpMatchRunner.prepareDeclaration(specific, specificSession)
        val plan = facts.plans.single() as CjmpMatchPlan.Declaration<*, *, *>
        assertEquals(listOf(common.symbol), plan.candidates.map { it.common })
        assertFalse(dependencySpecific.symbol in plan.candidates.map { it.common })
        val aggregate = CjmpMatchEvaluator.evaluate(facts.plans)
        assertSame(specific.symbol, aggregate.implementations[common.symbol])
        assertNull(dependencySpecific.cjmpMatchFacts, "an equally named ordinary dependency must remain outside this session")
    }

    /** 测试 session 在创建时一次性绑定真实依赖图，不替换已绑定的模块组件。 */
    private fun specificSession(
        refinement: CfirModuleData,
        ordinary: CfirModuleData,
    ): CallResolutionTestFixtures.TestSession = CallResolutionTestFixtures.newTestSession(
        dependencies = listOf(refinement, ordinary),
        refinementDependencies = listOf(refinement),
    )

    private fun function(
        session: CallResolutionTestFixtures.TestSession,
        common: Boolean = false,
        specific: Boolean = false,
    ): CfirNamedFunction = CallResolutionTestFixtures.buildFunctionSymbol(session, "operation").cfir.apply {
        (status as CfirDeclarationStatusImpl).apply {
            isCommon = common
            isSpecific = specific
        }
    }

    private fun install(session: CfirSession, declarations: List<CfirNamedFunction>) {
        val provider = DeclarationProvider(session, declarations)
        session.register(CfirSymbolProvider::class, provider)
        session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
    }

    /** 只按已绑定库声明的真实 CallableId 查询；模块资格完全由生产 resolver 判断。 */
    private class DeclarationProvider(
        session: CfirSession,
        private val declarations: List<CfirNamedFunction>,
    ) : CfirSymbolProvider(session) {
        override val symbolNamesProvider = CfirNullSymbolNamesProvider
        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null
        override fun hasPackage(fqName: FqName): Boolean = declarations.any { it.symbol.callableId.packageName == fqName }
        override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
            destination += functions(packageFqName, name)
        }
        override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
            destination += functions(packageFqName, name)
        }
        override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {}

        private fun functions(packageFqName: FqName, name: Name): List<CfirNamedFunctionSymbol> = declarations
            .filter { it.symbol.callableId.packageName == packageFqName && it.name == name }
            .map { it.symbol }
    }
}
