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
    /**
     * 获取当前测试要观察的公开 scope。
     *
     * 子类可以返回 file、package、member、declared member 或 type scope。
     */
    protected abstract fun getScope(mainFile: CjFile, testServices: TestServices): CaScope

    context(_: CaSession)
    /**
     * 从 scope 中提取要参与 symbol 渲染和 pointer 恢复的声明 symbol。
     *
     * 默认返回 scope.declarations，特殊 scope 可以覆盖以筛选声明集合。
     */
    protected open fun getSymbolsFromScope(scope: CaScope): Sequence<CaDeclarationSymbol> = scope.declarations

    /**
     * 执行 scope 内容与可用名称快照测试。
     *
     * 该方法先复用 symbol 基类验证 scope declarations，再额外输出 classifier/callable 名称集合。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        analyzeForTest(mainFile) {
            val scope = getScope(mainFile, testServices)
            val actualNames = prettyPrint { renderNamesContainedInScope(scope) }
            testServices.assertions.assertEqualsToTestOutputFile(actualNames, extension = ".names.txt")
        }
    }

    /**
     * 将 scope 中的声明 symbol 转换为公共 symbol 测试基座所需的数据模型。
     *
     * 这样每个 scope 测试同时覆盖 scope declarations、symbol 渲染和 pointer 恢复。
     */
    override fun CaSession.collectSymbols(cjFile: CjFile, testServices: TestServices): SymbolsData =
        SymbolsData(getSymbolsFromScope(getScope(cjFile, testServices)).toList())
}

/**
 * 渲染 scope 中可能包含的 classifier 与 callable 名称集合。
 *
 * 输出按类别分组，并交给 `renderSortedNames` 保持稳定排序。
 */
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

/**
 * 按名称字典序渲染名称集合。
 *
 * scope 名称输出必须排序，以避免底层集合遍历顺序影响 golden。
 */
private fun org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter.renderSortedNames(names: Set<Name>) {
    names.sorted().forEach { appendLine(it.asString()) }
}
