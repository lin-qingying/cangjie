package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.getFileScope
import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.symbol
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions
import org.junit.jupiter.api.Assertions.assertFalse

/**
 * `scopeProvider.fileScope` 的抽象测试。
 *
 * 对齐 Kotlin `AbstractFileScopeTest` 的职责边界：
 * 基座负责 file scope 内容渲染；这里额外校验 file symbol 输出。
 */
abstract class AbstractFileScopeTest : AbstractScopeTestBase() {
    context(_: CaSession)
    /**
     * 获取当前文件的公开 file scope。
     */
    override fun getScope(mainFile: CjFile, testServices: TestServices): CaScope = mainFile.getFileScope()

    /**
     * 执行 file scope 测试。
     *
     * 除基座的 scope 内容输出外，额外校验 file symbol 渲染和不应出现的名称。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        super.doTestByMainFile(mainFile, mainModule, testServices)

        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedAbsentNames = directives[AnalysisApiComponentTestDirectives.FILE_SCOPE_ABSENT_NAME]
        analyzeForTest(mainFile) {
            val fileScope = mainFile.getFileScope()
            val renderedFileSymbol = renderSymbolForComparison(mainFile.symbol)
            testServices.assertions.assertEqualsToTestOutputFile(renderedFileSymbol, extension = ".file_symbol.txt")

            expectedAbsentNames.forEach { absentName ->
                assertFalse(
                    fileScope.mayContainName(Name.identifier(absentName)),
                    "文件作用域不应暴露名字 `$absentName`。",
                )
            }
        }
    }
}
