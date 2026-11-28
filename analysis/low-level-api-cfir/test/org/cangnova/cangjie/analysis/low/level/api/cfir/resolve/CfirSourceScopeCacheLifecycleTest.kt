@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.ownExtendProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 官方 SDK 接受的真实 SOURCE diamond；不注入供应器，不以副本阶段代替原声明阶段。 */
class CfirSourceScopeCacheLifecycleTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/sourceScopeCacheLifecycle",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun ordinarySourceNullStatusBodyMaintainsOwnedRaw(mainFile: CjFile) {
        val facade = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facade)
        val session = facade.useSiteCfirSession
        val storage = facade.getScopeSessionFor(session)
        val leaf = file.declarations.filterIsInstance<CfirClass>().single()
        val scoped = sourceScopeCallables(file)
        assertEquals(5, scoped.size)
        assertTrue(scoped.any { it.resolvePhase < CfirResolvePhase.STATUS }, "SOURCE 必须保留真正的 callable 冷输入")
        assertTrue(scoped.all { it.resolvePhase < CfirResolvePhase.BODY_RESOLVE })

        // 只完成真实父图/索引输入，尚未请求 callable STATUS 或 BODY。
        checkNotNull(session.typeAwareSupertypeProviderOrNull).getDirectSupertypeDescriptors(leaf.symbol.constructType())
        assertTrue(scoped.any { it.resolvePhase < CfirResolvePhase.STATUS })
        assertTrue(scoped.all { it.resolvePhase < CfirResolvePhase.BODY_RESOLVE })
        fun raw(phase: CfirResolvePhase?) = session.cangjieScopeProvider.getUseSiteMemberScope(
            leaf.symbol, session, storage, scopeKind = CfirClassMemberScopeKind.USE_SITE, memberRequiredPhase = phase,
        )
        fun wrapped(phase: CfirResolvePhase?) = leaf.unsubstitutedScope(session, storage, false, phase)

        val initialRaw = raw(null)
        val initialWrapper = wrapped(null)
        val initialVersions = versions(session)
        assertSame(initialRaw, raw(null))
        assertTrue(scoped.all { it.resolvePhase < CfirResolvePhase.BODY_RESOLVE })

        val statusRaw = raw(CfirResolvePhase.STATUS)
        assertPhase(scoped, CfirResolvePhase.STATUS)
        assertEquals(initialVersions, versions(session), "固定 SOURCE 输入的阶段升级不应发布另一份结构产品")
        assertSame(initialRaw, statusRaw)
        assertSame(initialWrapper, wrapped(CfirResolvePhase.STATUS))

        val bodyRaw = raw(CfirResolvePhase.BODY_RESOLVE)
        assertPhase(scoped, CfirResolvePhase.BODY_RESOLVE)
        assertEquals(initialVersions, versions(session))
        assertSame(initialRaw, bodyRaw)
        assertSame(initialWrapper, wrapped(CfirResolvePhase.BODY_RESOLVE))
        assertNames(bodyRaw)

        val topLevel = file.declarations.filterIsInstance<CfirNamedFunction>()
        assertEquals(2, topLevel.size)
        topLevel.forEach { it.symbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE) }
        assertPhase(scoped + topLevel, CfirResolvePhase.BODY_RESOLVE)
        assertNoErrors(mainFile)
        assertSame(initialRaw, raw(null))
        assertSame(initialWrapper, wrapped(CfirResolvePhase.BODY_RESOLVE))
    }

    @Test
    fun ordinarySourceExtendRepublicationRetiresRawStaticAlias(mainFile: CjFile) {
        val facade = mainFile.getResolutionFacadeForTest()
        val file = mainFile.getOrBuildCfirFile(facade)
        val session = facade.useSiteCfirSession
        val storage = facade.getScopeSessionFor(session)
        val leaf = file.declarations.filterIsInstance<CfirClass>().single()
        val extend = file.declarations.filterIsInstance<CfirExtend>().single()
        val alias = file.declarations.filterIsInstance<CfirTypeAlias>().single()
        val sourceProvider = session.ownExtendProvider
        assertSame(extend, sourceProvider.getAllExtends().single())
        assertNoErrors(mainFile)
        alias.symbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val type = checkNotNull(alias.expandedTypeRef.coneTypeOrNull).fullyExpandedType(session)
        fun raw() = session.cangjieScopeProvider.getUseSiteMemberScope(
            leaf.symbol, session, storage, ownerType = type, dispatchReceiverType = type,
            scopeKind = CfirClassMemberScopeKind.USE_SITE, memberRequiredPhase = CfirResolvePhase.BODY_RESOLVE,
        )
        fun wrapper() = checkNotNull(CfirClassUseSiteMemberScope.createForUseSiteType(
            session, type, storage, memberRequiredPhase = CfirResolvePhase.BODY_RESOLVE,
        ))
        fun static() = leaf.symbol.staticScopeForQualifierType(
            session, storage, qualifierType = type, memberRequiredPhase = CfirResolvePhase.BODY_RESOLVE,
        )
        fun aliasScope() = session.cangjieScopeProvider.getTypealiasConstructorScope(alias, session, storage)

        val oldRaw = raw()
        val oldWrapper = wrapper()
        val oldStatic = static()
        val oldAlias = aliasScope()
        assertNames(oldRaw)
        assertSame(oldWrapper, wrapper())
        assertSame(oldStatic, static())
        assertSame(oldAlias, aliasScope())
        val before = sourceProvider.modificationCount
        assertSame(extend, sourceProvider.getAllExtends().single())
        assertTrue(sourceProvider.modificationCount > before, "必须实际触发第二次 SOURCE 索引发布")

        val currentRaw = raw()
        val currentWrapper = wrapper()
        val currentStatic = static()
        val currentAlias = aliasScope()
        assertNotSame(oldRaw, currentRaw)
        assertNotSame(oldWrapper, currentWrapper)
        assertNotSame(oldStatic, currentStatic)
        assertNotSame(oldAlias, currentAlias)
        assertNames(currentRaw)
        assertSame(currentRaw, raw())
        assertSame(currentWrapper, wrapper())
        assertSame(currentStatic, static())
        assertSame(currentAlias, aliasScope())
        assertSame(file, mainFile.getOrBuildCfirFile(facade))
        assertNoErrors(mainFile)
    }

    private fun sourceScopeCallables(file: CfirFile): List<CfirCallableDeclaration> =
        file.declarations.filterIsInstance<CfirClassLikeDeclaration>().flatMap { it.declarations }
            .plus(file.declarations.filterIsInstance<CfirExtend>().flatMap { it.declarations })
            .filterIsInstance<CfirCallableDeclaration>().filter { it.source?.kind == CjRealSourceElementKind }

    private fun assertPhase(declarations: List<CfirCallableDeclaration>, phase: CfirResolvePhase) {
        declarations.forEach { assertTrue(it.resolvePhase >= phase, "原 SOURCE ${it.symbol} 未满足 $phase：${it.resolvePhase}") }
    }

    private fun versions(session: CfirSession): Pair<Long, Long> =
        session.extendProvider.modificationCount to checkNotNull(session.directSupertypeProviderOrNull).modificationCount

    private fun assertNames(scope: CfirTypeScope) {
        assertTrue(scope.getCallableNames().containsAll(listOf("value", "extensionValue", "staticMarker").map(Name::identifier)))
    }

    private fun assertNoErrors(file: CjFile) {
        val errors = file.collectDiagnosticsForFile(file.getResolutionFacadeForTest(), DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
            .filter { it.severity == Severity.ERROR }
        assertTrue(errors.isEmpty(), errors.joinToString())
    }
}
