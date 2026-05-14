package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives

/**
 * Analysis API 组件测试的抽象基类。
 *
 * 组件测试与平台/后端选择解耦后，需要一层公共基座来统一：
 * 1. 注册组件级文件指令；
 * 2. 从 `CjFile + CjTestModule` 反查测试基础设施中的 `TestFile`；
 * 3. 让抽象测试只围绕公开 Analysis API 断言，而不是再手工维护方法名到文件名的映射。
 */
abstract class AbstractAnalysisApiComponentTest : AbstractAnalysisApiBasedTest() {
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + AnalysisApiComponentTestDirectives

    /**
     * 反查主文件对应的测试文件指令。
     *
     * 这里优先按相对路径尾部匹配，再退回到文件名匹配，确保多模块场景下不会把同名文件
     * 直接错误绑定到别的模块。该查找失败时直接抛错，因为这意味着测试框架建模和 PSI 建模
     * 已经失去一致性，不能再做兜底。
     */
    protected fun directivesForMainFile(mainFile: CjFile, mainModule: CjTestModule): RegisteredDirectives {
        val matchingTestFiles = mainModule.testModule.files.filter { testFile ->
            testFile.relativePath.endsWith("/${mainFile.name}") || testFile.name == mainFile.name
        }

        val testFile = matchingTestFiles.singleOrNull()
            ?: error(
                "Cannot uniquely match PSI file `${mainFile.name}` to test file in module `${mainModule.name}`. " +
                    "Candidates: ${matchingTestFiles.map { it.relativePath }}",
            )

        return testFile.directives
    }

    /**
     * 统一规范公开类型渲染，用于跨后端/平台比较。
     *
     * 当前 Analysis API 的公开类型渲染仍处于演进阶段，不同实现可能在包与类之间使用
     * `.` 或 `/`。组件测试关注的是公开语义是否一致，而不是临时的分隔符选择，因此这里
     * 在断言前做统一规范化，避免测试数据被渲染细节绑死。
     */
    protected fun normalizeTypeRendering(rendered: String): String {
        return rendered.replace('/', '.')
    }
}
