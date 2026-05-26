package org.cangnova.cangjie.analysis.low.level.api.cfir.api

import org.cangnova.cangjie.analysis.low.level.api.cfir.test.configurators.analysisApiCfirSourceTestConfigurator
import org.cangnova.cangjie.analysis.low.level.api.cfir.test.getResolutionFacadeForTest
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.render
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 对齐 Kotlin `AbstractResolveToFirSymbolTest` 的 low-level 入口测试。
 *
 * 该测试验证 `resolveToCfirSymbol()` 会为带 `<caret>` 标记的声明恢复出
 * 对应的 CFIR symbol，并输出它最终绑定的声明渲染结果。
 */
abstract class AbstractResolveToCfirSymbolTest : AbstractAnalysisApiBasedTest() {
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    private data class TargetDeclaration(
        val declaration: CjDeclaration,
        val locationDescription: String,
    )

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val targets = testServices.cjTestModuleStructure.mainModules
            .flatMap { module -> module.cjFiles }
            .flatMap { file ->
                testServices.expressionMarkerProvider.getAllCarets(file).map { marker ->
                    val declaration = testServices.expressionMarkerProvider
                        .getBottommostElementOfTypeAtCaret<CjDeclaration>(file, qualifier = marker.qualifier)
                    TargetDeclaration(
                        declaration = declaration,
                        locationDescription = "${file.name}:${declaration.textRange.startOffset}",
                    )
                }
            }
            .sortedWith(
                compareBy<TargetDeclaration> { it.locationDescription }
                    .thenBy { it.declaration.textRange.startOffset },
            )

        val actual = buildString {
            for ((declaration, locationDescription) in targets) {
                val resolvedDeclaration = declaration
                    .resolveToCfirSymbol(
                        resolutionFacade = declaration.containingCjFile.getResolutionFacadeForTest(),
                        phase = CfirResolvePhase.BODY_RESOLVE,
                    )
                    .cfir
                    .render()

                appendLine("${declaration::class.simpleName} '${declaration.name ?: "<anonymous>"}' in $locationDescription:")
                appendLine(indent(resolvedDeclaration, 2))
                appendLine()
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    private fun indent(text: String, size: Int): String {
        val prefix = " ".repeat(size)
        return text.lineSequence().joinToString(separator = "\n") { line -> prefix + line }
    }
}
