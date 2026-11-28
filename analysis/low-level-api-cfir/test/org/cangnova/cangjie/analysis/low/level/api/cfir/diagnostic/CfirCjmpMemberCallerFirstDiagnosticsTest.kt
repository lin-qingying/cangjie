package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostic

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDiagnosticsForFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.CfirDiagnosticsDirectives
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 成员 tower 首次查询时完成正向实际化；普通宿主类型使本测试不依赖 nominal 合并。 */
class CfirCjmpMemberCallerFirstDiagnosticsTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/diagnostics/cjmpMemberCallerFirst",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)
    override val additionalDirectives = super.additionalDirectives + CfirDiagnosticsDirectives

    @Test
    fun extendMemberDefaultCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertMemberCallerFirst(mainFile, mainModule, testServices, emptyList())
    }

    @Test
    fun extendMemberShadowedCommonCallerFirst(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        assertMemberCallerFirst(mainFile, mainModule, testServices, listOf("CFIR_DEPRECATED_WARNING"))
    }

    private fun assertMemberCallerFirst(
        mainFile: CjFile,
        mainModule: CjTestModule,
        testServices: TestServices,
        expectedDiagnostics: List<String>,
    ) {
        // 模式由模块配置进入实际 session；只验证输入，不提前请求任何解析相位。
        for (module in testServices.cjTestModuleStructure.mainModules) {
            val file = module.cjFiles.first()
            val rawFile = file.getOrBuildCfirFile(file.getResolutionFacadeForTest())
            assertEquals(module.testModule.directives[CfirDiagnosticsDirectives.CJMP_MODE].single(), rawFile.moduleData.session.cjmpSettings.mode)
        }
        val facade = mainFile.getResolutionFacadeForTest()
        val callerFile = mainFile.getOrBuildCfirFile(facade)
        val calleePsi = mainModule.cjFiles.single { it !== mainFile }
        val calleeFile = calleePsi.getOrBuildCfirFile(calleePsi.getResolutionFacadeForTest())
        val owner = calleeFile.declarations.filterIsInstance<CfirExtend>().single()
        val callee = owner.declarations.filterIsInstance<CfirNamedFunction>().single()
        val caller = callerFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(owner.resolvePhase < CfirResolvePhase.CJMP_MATCHING)
        assertTrue(callee.resolvePhase < CfirResolvePhase.CJMP_MATCHING)

        val diagnostics = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(expectedDiagnostics, diagnostics.map { it.factoryName }.sorted())
        assertTrue(
            callee.body is CfirLazyBlock,
            "collecting caller diagnostics resolved the specific body: phase=${callee.resolvePhase}",
        )

        val calls = mutableListOf<CfirFunctionCall>()
        requireNotNull(caller.body).accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) = element.acceptChildren(this)

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val call = calls.single()
        val resolved = (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir as CfirCallableDeclaration
        assertSame(callee.symbol, resolved.unwrapSubstitutionOverrides().symbol, "member lookup must bind the specific declaration")
        assertEquals(ConePrimitiveType.INT64, call.coneTypeOrNull)
        assertTrue(
            callee.body is CfirLazyBlock,
            "member lookup must not resolve the specific body: body=${callee.body?.javaClass?.name}, phase=${callee.resolvePhase}",
        )

        val repeated = mainFile.collectDiagnosticsForFile(facade, DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS)
        assertEquals(expectedDiagnostics, repeated.map { it.factoryName }.sorted())
        assertSame(resolved, (call.calleeReference as CfirResolvedNamedReference).resolvedSymbol.cfir)
    }
}
