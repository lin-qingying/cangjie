package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.AbstractSymbolByFqNameTest
import org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols.SymbolsData
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.scopes.CaScopeLike
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `scopeProvider` 抽象测试公共基座。
 *
 * 对齐 Kotlin `AbstractScopeTestBase` 的职责边界：
 * - 子类只负责选出目标 scope；
 * - 基座统一做 scope 内声明渲染与 `.names.txt` 校验。
 */
abstract class AbstractScopeTestBase : AbstractSymbolByFqNameTest() {
    context(_: CaSession)
    protected abstract fun getScope(mainFile: CjFile, testServices: TestServices): CaScope

    context(_: CaSession)
    protected open fun getSymbolsFromScope(scope: CaScope): Sequence<CaDeclarationSymbol> = scope.declarations

    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyzeForTest(mainFile) {
            val scope = getScope(mainFile, testServices)
            val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
            testServices.assertions.assertEqualsToTestOutputFile(actualNames, extension = ".names.txt")
        }
    }

    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData =
        SymbolsData(getSymbolsFromScope(getScope(cjFile, testServices)).toList())
}

private fun org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter.renderNamesContainedInScope(scope: CaScopeLike) {
    appendLine("Classifier names:")
    withIndent {
        renderSortedNames(scope.getPossibleClassifierNames())
    }
    appendLine()
    appendLine("Callable names:")
    withIndent {
        renderSortedNames(scope.getPossibleCallableNames())
    }
}

private fun org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter.renderSortedNames(names: Set<Name>) {
    names.sorted().forEach { appendLine(it.asString()) }
}
