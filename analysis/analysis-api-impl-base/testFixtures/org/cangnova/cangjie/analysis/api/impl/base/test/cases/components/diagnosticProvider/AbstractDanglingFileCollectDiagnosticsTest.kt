package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure

/**
 * dangling file 场景下的文件诊断收集测试。
 *
 * 对齐 Kotlin `AbstractDanglingFileCollectDiagnosticsTest`：复制主文件为非物理 PSI，
 * 并通过 `originalFile` 让项目结构服务把它归入 dangling file module。
 */
abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    /**
     * 当前 dangling file 测试额外注册的控制指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * dangling file 诊断测试的专用指令集合。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 忽略当前 dangling file 诊断测试失败。
         */
        val IGNORE_DANGLING_FILES by stringDirective("Ignore dangling file diagnostic tests.")
        /**
         * 指定是否使用 `IGNORE_SELF` dangling file resolution mode。
         */
        val IGNORE_SELF_MODE by stringDirective("Whether to use ${CaDanglingFileResolutionMode.IGNORE_SELF} mode.")
    }

    /**
     * 执行 dangling file 诊断测试。
     *
     * 若测试数据显式声明忽略 dangling files，则捕获断言错误并允许该场景跳过。
     */
    override fun doTest(testServices: TestServices) {
        try {
            super.doTest(testServices)
        } catch (error: AssertionError) {
            if (Directives.IGNORE_DANGLING_FILES !in testServices.moduleStructure.allDirectives) {
                throw error
            }
        }
    }

    /**
     * 将原始文件复制为带 `originalFile` 的非物理文件。
     *
     * 复制文件会被项目结构识别为 dangling file module，从而覆盖 dangling 分析路径。
     */
    override fun prepareCjFile(cjFile: CjFile, testServices: TestServices): PreparedFile {
        val psiFactory = CjPsiFactory.contextual(cjFile, markGenerated = true)
        val fakeFile = psiFactory.createFile("fake.cj", cjFile.text).apply {
            originalFile = cjFile
        }

        return PreparedFile(fakeFile, cjFile.name)
    }
}
