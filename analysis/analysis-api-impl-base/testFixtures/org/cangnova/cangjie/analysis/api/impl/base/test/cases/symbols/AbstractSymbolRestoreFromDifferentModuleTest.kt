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

abstract class AbstractSymbolRestoreFromDifferentModuleTest : AbstractAnalysisApiBasedTest() {
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

    private companion object {
        const val NOT_RESTORED = "<NOT RESTORED>"
    }
}
