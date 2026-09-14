package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.containingModuleProvider

import org.cangnova.cangjie.analysis.api.getModule
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * 元素归属模块查询（`session.getModule`）的抽象测试。
 *
 * 对齐 Kotlin `AbstractGetModuleTest`：以主文件为查询元素，渲染 `CaModule` 的
 * 运行时类别、`moduleDescription`、`stableModuleName` 与 `isResolvable`，
 * 覆盖单模块与跨模块依赖场景。
 */
abstract class AbstractGetModuleTest : AbstractAnalysisApiComponentTest() {
    /**
     * 执行归属模块查询测试。
     *
     * 方法把主文件交给 `getModule`，输出模块公开视图；`stableModuleName` 为 `null`
     * 时显式输出占位文本，保证多模块归属错误在 golden 中可见。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val actual = analyzeForTest(mainFile) {
            val module = getModule(mainFile)
            buildString {
                appendLine("moduleClass: ${module::class.simpleName}")
                appendLine("moduleDescription: ${module.moduleDescription}")
                appendLine("stableModuleName: ${module.stableModuleName ?: "null"}")
                appendLine("isResolvable: ${module.isResolvable}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual, extension = "module.txt")
    }
}
