package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices

/**
 * 对齐 Kotlin `AbstractFirLazyDeclarationResolveByReferenceTest`：
 * 通过 caret 位置的 reference 先恢复 declaration，再验证该 declaration 的 lazy resolve 行为。
 */
abstract class AbstractCfirLazyDeclarationResolveByReferenceTest : AbstractCfirLazyDeclarationResolveOverAllPhasesTest() {
    /**
     * 通过 caret 引用恢复目标声明，并对该声明执行全阶段 lazy resolve golden 测试。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        doLazyResolveTest(
            cjFile = mainFile,
            testServices = testServices,
            outputRenderingMode = OutputRenderingMode.ONLY_TARGET_DECLARATION,
        ) {
            val position = testServices.expressionMarkerProvider.getCaret(mainFile)
            val reference = mainFile.findReferenceAt(position) ?: error("Caret 位置没有 reference。")
            val declaration = reference.resolve() as? CjDeclaration
                ?: error("Caret 位置引用的不是 declaration，而是 `${reference.resolve()?.javaClass?.simpleName}`。")
            val resolutionFacade = declaration.containingCjFile.getResolutionFacadeForTest()
            val symbol = declaration.resolveToCfirSymbol(resolutionFacade, phase = CfirResolvePhase.RAW_CFIR)
            val cfirDeclaration = symbol.cfir as? org.cangnova.cangjie.cfir.CfirElementWithResolveState
                ?: error("引用恢复出的 symbol `${symbol::class.simpleName}` 没有 resolve-state declaration。")
            cfirDeclaration to { phase ->
                cfirDeclaration.lazyResolveToPhaseByDirective(phase, testServices)
            }
        }
    }
}

/**
 * source 配置下的 by-reference lazy declaration resolve 测试基类。
 */
abstract class AbstractCfirSourceLazyDeclarationResolveByReferenceTest : AbstractCfirLazyDeclarationResolveByReferenceTest() {
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
}
