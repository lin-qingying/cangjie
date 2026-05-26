package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfirFile
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractGetOrBuildFirTest` 的 low-level 结构测试。
 *
 * 测试会读取 `<expr>` / `<caret>` 标记的 PSI，调用 `getOrBuildCfir()`，
 * 并校验返回节点属于同一个 `CfirFile`。
 */
abstract class AbstractGetOrBuildCfirTest : AbstractAnalysisApiBasedTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        fun findElement(qualifier: String): CjElement? {
            val selected = testServices.expressionMarkerProvider.getTopmostSelectedElementOfTypeByDirectiveOrNull(
                file = mainFile,
                module = mainModule,
                defaultType = CjElement::class,
                qualifier = qualifier,
            ) as? CjElement
            if (selected != null) return selected
            return testServices.expressionMarkerProvider.getBottommostElementOfTypeAtCaretOrNull<CjElement>(
                file = mainFile,
                qualifier = qualifier,
            )
        }

        val elementsToAnalyze = buildList {
            add(findElement("") ?: error("No <expr> / <caret> marker found in ${mainFile.name}"))

            var index = 1
            while (true) {
                val candidate = findElement(index.toString()) ?: break
                add(candidate)
                index += 1
            }
        }

        val resolutionFacade = mainFile.getResolutionFacadeForTest()
        val cfirFile by lazy { mainFile.getOrBuildCfirFile(resolutionFacade) }
        val skipContainmentCheck = Directives.SKIP_CONTAINMENT_CHECK in mainModule.testModule.directives

        val results = elementsToAnalyze.map { element ->
            val cfirElement = element.getOrBuildCfir(resolutionFacade)
            if (!skipContainmentCheck && cfirElement != null) {
                check(isInside(cfirElement, cfirFile)) {
                    "CFIR element `${cfirElement::class.simpleName}` is not reachable from `${mainFile.name}`."
                }
            }

            renderActualCfir(
                cfir = cfirElement,
                cjElement = element,
                cfirFile = cfirFile,
            )
        }

        val actual = if (results.size == 1) {
            results.single()
        } else {
            results.withIndex().joinToString(separator = "\n\n=====\n\n") { (index, result) ->
                "Analysis attempt #$index\n$result"
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private fun isInside(target: CfirElement, file: CfirFile): Boolean {
        var found = false
        file.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
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

    private object Directives : SimpleDirectivesContainer() {
        val SKIP_CONTAINMENT_CHECK by directive("跳过“返回的 CFIR 节点必须属于同一 CfirFile”校验。")
    }
}

private fun renderActualCfir(
    cfir: CfirElement?,
    cjElement: CjElement,
    cfirFile: CfirFile,
): String = buildString {
    val renderer = CfirRenderer.withReadability()
    appendLine("CJ element: ${cjElement::class.simpleName}")
    appendLine("CJ element text:")
    appendLine(cjElement.text)
    appendLine("CFIR element: ${cfir?.let { it::class.simpleName }}")
    appendLine("CFIR source kind: ${cfir?.source?.kind?.let { it::class.simpleName }}")
    appendLine()
    appendLine("CFIR element rendered:")
    appendLine(cfir?.let(renderer::renderElementAsString) ?: "null")
    appendLine()
    appendLine("CFIR FILE:")
    append(renderer.renderElementAsString(cfirFile))
}

abstract class AbstractSourceGetOrBuildCfirTest : AbstractGetOrBuildCfirTest()
