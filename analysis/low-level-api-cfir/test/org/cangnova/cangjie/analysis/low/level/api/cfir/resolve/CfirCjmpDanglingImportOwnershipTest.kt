@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.resolve

import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.services.importBindingsOrNull
import org.cangnova.cangjie.cfir.session.importBindingStoreOrNull
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/** IGNORE_SELF 副本的文件导入事实归属 owner session，目标符号由 dangling consumer 解析。 */
class CfirCjmpDanglingImportOwnershipTest : AbstractAnalysisApiExecutionTest(
    "analysis/low-level-api-cfir/testData/cjmpDanglingImportOwnership",
) {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    @Test
    fun ignoreSelfCopyKeepsOwnerImportBindings(mainFile: CjFile, mainModule: CjTestModule) {
        val ownerFile = mainFile.getOrBuildCfirFile(mainFile.getResolutionFacadeForTest())
        val targetPsiFile = mainModule.cjFiles.single { it.name == "target.cj" }
        val targetFunction = targetPsiFile.getOrBuildCfirFile(targetPsiFile.getResolutionFacadeForTest())
            .declarations.filterIsInstance<CfirNamedFunction>().single()

        val copiedPsiFile = CjPsiFactory.contextual(mainFile, markGenerated = true)
            .createFile("copy.cj", mainFile.text)
            .apply { originalFile = mainFile }
        val danglingModule = CangJieProjectStructureProvider.getModule(
            copiedPsiFile.project,
            copiedPsiFile,
            useSiteModule = null,
        ) as CaDanglingFileModule
        assertEquals(CaDanglingFileResolutionMode.IGNORE_SELF, danglingModule.resolutionMode)

        val copiedCfirFile = copiedPsiFile.getOrBuildCfirFile(copiedPsiFile.getResolutionFacadeForTest())
        val copiedFunction = copiedCfirFile.declarations.filterIsInstance<CfirNamedFunction>().single()
        copiedFunction.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)

        assertNotNull(ownerFile.importBindingsOrNull(), "the original file owner must publish its explicit imports")
        assertNull(
            copiedCfirFile.moduleData.session.importBindingStoreOrNull?.getBindings(ownerFile),
            "the dangling consumer session must not store bindings for a foreign owner file",
        )

        val calls = mutableListOf<CfirFunctionCall>()
        copiedFunction.accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                element.acceptChildren(this)
            }

            override fun visitFunctionCall(functionCall: CfirFunctionCall) {
                calls += functionCall
                functionCall.acceptChildren(this)
            }
        })
        val resolvedReference = calls.single().calleeReference as CfirResolvedNamedReference
        assertSame(targetFunction.symbol, resolvedReference.resolvedSymbol)
    }
}
