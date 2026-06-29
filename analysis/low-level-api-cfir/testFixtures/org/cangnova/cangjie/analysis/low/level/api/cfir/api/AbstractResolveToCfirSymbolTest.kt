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
    /**
     * 使用源码 low-level CFIR 测试配置。
     */
    override val configurator = analysisApiCfirSourceTestConfigurator(analyseInDependentSession = false)

    /**
     * 待解析声明及其测试输出位置描述。
     */
    private data class TargetDeclaration(
        /**
         * 带 caret 标记的 PSI 声明。
         */
        val declaration: CjDeclaration,
        /**
         * 输出中用于稳定排序和定位的文本描述。
         */
        val locationDescription: String,
    )

    /**
     * 收集所有 caret 声明，解析为 CFIR symbol 并渲染绑定声明。
     */
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

    /**
     * 给多行 [text] 添加 [size] 个空格缩进。
     */
    private fun indent(text: String, size: Int): String {
        val prefix = " ".repeat(size)
        return text.lineSequence().joinToString(separator = "\n") { line -> prefix + line }
    }
}
