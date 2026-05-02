package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `scopeProvider.fileScope` 的抽象测试。
 *
 * 文件作用域通过统一的 `CaScope` 协议暴露当前文件上下文中按名字可查询到的公开声明。
 */
abstract class AbstractFileScopeTest : AbstractScopeProviderTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedAvailableNames = directives[AnalysisApiComponentTestDirectives.FILE_SCOPE_AVAILABLE_NAME]
        val expectedFileClassifiers = directives[AnalysisApiComponentTestDirectives.FILE_SCOPE_CLASSIFIER]
        val expectedFileCallables = directives[AnalysisApiComponentTestDirectives.FILE_SCOPE_CALLABLE]
        val expectedPackageClassifiers = directives[AnalysisApiComponentTestDirectives.PACKAGE_SCOPE_CLASSIFIER]
        val expectedPackageCallables = directives[AnalysisApiComponentTestDirectives.PACKAGE_SCOPE_CALLABLE]

        analyzeForTest(mainFile) {
            val fileScope = mainFile.getFileScope()
            val packageScope = getPackageScope(mainFile.packageFqName)

            assertScopeContents(
                scope = fileScope,
                expectedAvailableNames = expectedAvailableNames,
                expectedClassifiers = expectedFileClassifiers,
                expectedCallables = expectedFileCallables,
                scopeLabel = "文件作用域",
            )

            assertNotNull(packageScope, "包作用域应可以直接从 Analysis API 获取。")
            assertScopeContents(
                scope = packageScope!!,
                expectedAvailableNames = emptyList(),
                expectedClassifiers = expectedPackageClassifiers,
                expectedCallables = expectedPackageCallables,
                scopeLabel = "包作用域",
            )
        }
    }
}
