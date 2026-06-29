package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import com.intellij.openapi.components.service
import org.cangnova.cangjie.analysis.api.components.render
import org.cangnova.cangjie.analysis.api.platform.modification.CaSessionInvalidationService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForDebug
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.session.restoreSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 跨模块恢复 symbol pointer 的抽象测试。
 *
 * 测试在声明所在模块创建 pointer，失效相关模块 session 后，在另一个 caret 指定模块中恢复 pointer，
 * 并比较 debug/source 两种 renderer 输出。
 */
abstract class AbstractSymbolRestoreFromDifferentModuleTest : AbstractAnalysisApiBasedTest() {
    /**
     * 执行跨模块 symbol pointer 恢复测试。
     *
     * 方法分别定位声明位置与恢复位置，创建 pointer、触发 session 失效，再在恢复位置分析上下文中恢复 symbol。
     */
    override fun doTest(testServices: TestServices) {
        val declaration =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<CjDeclaration>(testServices).single().first

        val restoreAt =
            testServices.expressionMarkerProvider.getBottommostElementsOfTypeAtCarets<CjElement>(
                testServices,
                qualifier = "restoreAt",
            ).single().first

        val project = declaration.project
        val declarationModule = CangJieProjectStructureProvider.getModule(project, declaration, useSiteModule = null)
        val restoreAtModule = CangJieProjectStructureProvider.getModule(project, restoreAt, useSiteModule = null)

        val (debugRendered, prettyRendered, pointer) = analyzeForTest(declaration) {
            val symbol = declaration.symbol
            Triple(
                symbol.render(CaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES),
                symbol.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES),
                symbol.createPointer(),
            )
        }
        project.service<CaSessionInvalidationService>().invalidate(setOf(declarationModule, restoreAtModule))

        val (debugRenderedRestored, prettyRenderedRestored) = analyzeForTest(restoreAt) {
            val restoredSymbol = restoreSymbol(pointer) as? CaDeclarationSymbol
            Pair(
                restoredSymbol?.render(CaDeclarationRendererForDebug.WITH_QUALIFIED_NAMES),
                restoredSymbol?.render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES),
            )
        }

        val actualDebug = buildString {
            appendLine("Inital from ${declarationModule.moduleDescription}:")
            appendLine(debugRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(debugRenderedRestored ?: NOT_RESTORED)
        }.trimEnd()
        testServices.assertions.assertEqualsToTestOutputFile(actualDebug)

        val actualPretty = buildString {
            appendLine("Inital from ${declarationModule.moduleDescription}:")
            appendLine(prettyRendered)
            appendLine()
            appendLine("Restored in ${restoreAtModule.moduleDescription}:")
            appendLine(prettyRenderedRestored ?: NOT_RESTORED)
        }.trimEnd()
        testServices.assertions.assertEqualsToTestOutputFile(actualPretty, extension = ".pretty.txt")
    }

    /**
     * 跨模块恢复测试使用的常量集合。
     *
     * 当前只包含恢复失败时写入 golden 的占位文本。
     */
    private companion object {
        /**
         * symbol pointer 无法在目标模块上下文恢复时输出的固定占位符。
         */
        const val NOT_RESTORED = "<NOT RESTORED>"
    }
}
