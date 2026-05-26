package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.low.level.api.cfir.collectCallableSymbols
import org.cangnova.cangjie.analysis.low.level.api.cfir.collectDirectOverriddenCallables
import org.cangnova.cangjie.analysis.low.level.api.cfir.renderCfirWithResolvePhases
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractLazyDeclarationResolveScopeBasedTest`：
 * 通过 type scope 抓取 class-like 可见 callable，并检查 override 链在 lazy resolve 前后的稳定性。
 */
abstract class AbstractLazyDeclarationResolveScopeBasedTest : AbstractAnalysisApiBasedTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val classLike = testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaret<CjTypeStatement>(mainFile)
        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val classLikeSymbol = classLike.resolveToCfirSymbolOfType<CfirClassLikeSymbol<*>>(resolutionFacade)
        val classLikeDeclaration = classLikeSymbol.cfir as? CfirClassLikeDeclaration
            ?: error("Caret 目标 `${classLike.name}` 不是 class-like declaration。")
        val scope = classLikeDeclaration.unsubstitutedScope(
            useSiteSession = resolutionFacade.useSiteCfirSession,
            scopeSession = resolutionFacade.getScopeSessionFor(resolutionFacade.useSiteCfirSession),
            withForcedTypeCalculator = false,
            memberRequiredPhase = CfirResolvePhase.STATUS,
        )
        val callables = collectCallableSymbols(scope).filterNot { it is CfirPropertyAccessorSymbol }
        testServices.assertions.assertEqualsToTestOutputFile(dumpSymbols(scope, callables), extension = "before.txt")

        callables.forEach { callable ->
            callable.cfir.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        }
        testServices.assertions.assertEqualsToTestOutputFile(dumpSymbols(scope, callables), extension = "after.txt")
    }

    private fun dumpSymbols(
        scope: org.cangnova.cangjie.cfir.scopes.CfirTypeScope,
        symbols: List<org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>>,
    ): String {
        return buildString {
            symbols.forEachIndexed { index, symbol ->
                if (index > 0) {
                    appendLine()
                }
                appendLine("${symbol::class.simpleName}: ${symbol.callableId}")
                appendLine(renderCfirWithResolvePhases(symbol.cfir))
                val overridden = collectDirectOverriddenCallables(symbol, scope)
                if (overridden.isNotEmpty()) {
                    appendLine("overridden:")
                    overridden.forEach { (candidate, _) ->
                        appendLine("  ${candidate::class.simpleName}: ${candidate.callableId}")
                    }
                }
            }
        }
    }
}

abstract class AbstractSourceLazyDeclarationResolveScopeBasedTest : AbstractLazyDeclarationResolveScopeBasedTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
