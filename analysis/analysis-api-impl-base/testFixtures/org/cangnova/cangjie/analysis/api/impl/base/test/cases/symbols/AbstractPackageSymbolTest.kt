package org.cangnova.cangjie.analysis.api.impl.base.test.cases.symbols

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * package symbol 查询的抽象测试。
 *
 * 测试遍历所有文件指令中声明的包名，分别断言指定包存在或不存在。
 */
abstract class AbstractPackageSymbolTest : org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest() {
    /**
     * 当前 package symbol 测试额外注册的包存在性指令。
     *
     * 这些指令在所有测试文件中收集后统一参与断言。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + listOf(Directives)

    /**
     * 执行 package symbol 存在性断言。
     *
     * 方法在一次分析会话中读取所有 `HAS_PACKAGE` 与 `NO_PACKAGE` 指令，并调用公开 `getPackageSymbol` 查询。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val allDirectives = testServices.cjTestModuleStructure.testModuleStructure.allDirectives

        analyze(mainFile) {
            for (packageName in allDirectives[Directives.HAS_PACKAGE]) {
                val packageSymbol = getPackageSymbol(packageName)
                testServices.assertions.assertNotNull(packageSymbol) {
                    "Package '$packageName' should exist"
                }
            }

            for (packageName in allDirectives[Directives.NO_PACKAGE]) {
                val packageSymbol = getPackageSymbol(packageName)
                testServices.assertions.assertEquals(null, packageSymbol) {
                    "Package '$packageName' should not exist"
                }
            }
        }
    }

    /**
     * package symbol 测试的专用指令集合。
     *
     * 指令值会被解析成 `FqName`，其中 `<root>` 特别表示根包。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 期望公开 package provider 能查到的包名。
         *
         * 每个值都会调用 `getPackageSymbol` 并断言结果非空。
         */
        val HAS_PACKAGE by valueDirective(
            description = "Check whether the specified package exists",
            applicability = DirectiveApplicability.File,
            parser = ::parsePackageName,
        )

        /**
         * 期望公开 package provider 查不到的包名。
         *
         * 每个值都会调用 `getPackageSymbol` 并断言结果为空。
         */
        val NO_PACKAGE by valueDirective(
            description = "Check whether the specified package does not exist",
            applicability = DirectiveApplicability.File,
            parser = ::parsePackageName,
        )

        /**
         * 将指令文本解析为包全名。
         *
         * `<root>` 被映射为 `FqName.ROOT`，其它值按普通 FqName 文本处理。
         */
        private fun parsePackageName(value: String): FqName {
            return if (value == "<root>") FqName.ROOT else FqName(value)
        }
    }
}
