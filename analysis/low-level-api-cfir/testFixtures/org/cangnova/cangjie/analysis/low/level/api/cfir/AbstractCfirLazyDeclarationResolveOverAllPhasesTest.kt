package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.LLResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractFirLazyDeclarationResolveOverAllPhasesTest`：
 * 针对选定 declaration 依次推进所有 CFIR resolve phase，并在每一阶段输出目标声明与相关文件快照。
 */
abstract class AbstractCfirLazyDeclarationResolveOverAllPhasesTest : AbstractCfirLazyDeclarationResolveTestCase() {
    protected open val outputExtension: String
        get() = ".txt"

    protected enum class OutputRenderingMode {
        ALL_FILES_FROM_ALL_MODULES,
        ONLY_TARGET_DECLARATION,
    }

    protected open fun checkResolutionFacade(resolutionFacade: LLResolutionFacade) {}

    protected fun doLazyResolveTest(
        cjFile: CjFile,
        testServices: TestServices,
        outputRenderingMode: OutputRenderingMode,
        resolverProvider: (LLResolutionFacade) -> Pair<CfirElementWithResolveState, (CfirResolvePhase) -> Unit>,
    ) {
        val resultBuilder = StringBuilder()
        val resolutionFacade = cjFile.getResolutionFacadeForTest()
        checkResolutionFacade(resolutionFacade)

        val (targetDeclaration, resolver) = resolverProvider(resolutionFacade)
        val filesToRender = when (outputRenderingMode) {
            OutputRenderingMode.ALL_FILES_FROM_ALL_MODULES -> {
                val currentFile = cjFile.getOrBuildCfirFile(resolutionFacade)
                testServices.cjTestModuleStructure.allCjFiles
                    .map { file -> file.getOrBuildCfirFile(file.getResolutionFacadeForTest()) }
                    .plus(currentFile)
                    .distinct()
            }
            OutputRenderingMode.ONLY_TARGET_DECLARATION -> emptyList()
        }

        val basePhase = targetDeclaration.resolvePhase
        val renderTargetSeparately = filesToRender.none { file -> file.containsElement(targetDeclaration) }
        for (phase in CfirResolvePhase.entries) {
            if (phase < basePhase) continue
            resolver(phase)

            if (resultBuilder.isNotEmpty()) {
                resultBuilder.appendLine()
            }

            resultBuilder.appendLine("${phase.name}:")
            if (renderTargetSeparately) {
                resultBuilder.appendLine("TARGET:")
                resultBuilder.appendLine(renderCfirWithResolvePhases(targetDeclaration))
            }

            filesToRender.forEach { file ->
                resultBuilder.appendLine(renderCfirWithResolvePhases(file))
            }
        }

        val resolvedFile = cjFile.getOrBuildCfirFile(resolutionFacade)
        resolvedFile.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        if (resultBuilder.isNotEmpty()) {
            resultBuilder.appendLine()
        }
        resultBuilder.appendLine("FILE RAW TO BODY:")
        resultBuilder.append(renderCfirWithResolvePhases(resolvedFile))

        testServices.assertions.assertEqualsToTestOutputFile(
            resultBuilder.toString(),
            extension = outputExtension,
        )
    }

    private fun CfirFile.containsElement(target: CfirElementWithResolveState): Boolean {
        if (this === target) return true
        var found = false
        acceptChildren(object : org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid() {
            override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
                if (found) return
                if (element === target) {
                    found = true
                    return
                }
                element.acceptChildren(this)
            }
        })
        return found
    }
}
