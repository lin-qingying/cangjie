package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.scopeProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.AnalysisApiComponentTestDirectives
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertNotNull

/**
 * `scopeProvider.packageScope` 的抽象测试。
 *
 * 同时验证 `getPackageScope(fqName)` 和 `packageSymbol.packageScope`
 * 两个入口，保证它们观察到的是同一份公开语义模型。
 */
abstract class AbstractPackageScopeTest : AbstractScopeProviderTest() {
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val directives = directivesForMainFile(mainFile, mainModule)
        val expectedAvailableNames = directives[AnalysisApiComponentTestDirectives.PACKAGE_SCOPE_AVAILABLE_NAME]
        val expectedClassifiers = directives[AnalysisApiComponentTestDirectives.PACKAGE_SCOPE_CLASSIFIER]
        val expectedCallables = directives[AnalysisApiComponentTestDirectives.PACKAGE_SCOPE_CALLABLE]

        analyzeForTest(mainFile) {
            val packageSymbol = getPackageSymbol(mainFile.packageFqName)
            val packageScopeByFqName = getPackageScope(mainFile.packageFqName)
            val packageScopeBySymbol = packageSymbol?.packageScope

            assertNotNull(packageSymbol, "包符号应可直接从 Analysis API 获取。")
            assertNotNull(packageScopeByFqName, "按包名查询的包作用域不应为空。")
            assertNotNull(packageScopeBySymbol, "按包符号查询的包作用域不应为空。")

            assertScopeContents(
                scope = packageScopeByFqName!!,
                expectedAvailableNames = expectedAvailableNames,
                expectedClassifiers = expectedClassifiers,
                expectedCallables = expectedCallables,
                scopeLabel = "按包名查询的包作用域",
            )
            assertScopeContents(
                scope = packageScopeBySymbol!!,
                expectedAvailableNames = expectedAvailableNames,
                expectedClassifiers = expectedClassifiers,
                expectedCallables = expectedCallables,
                scopeLabel = "按包符号查询的包作用域",
            )
        }
    }
}
