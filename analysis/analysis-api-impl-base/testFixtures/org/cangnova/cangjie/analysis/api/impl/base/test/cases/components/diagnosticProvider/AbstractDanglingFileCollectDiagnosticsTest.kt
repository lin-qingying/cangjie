package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
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
 *
 * 注意：Kotlin 参考实现只在 `IGNORE_SELF_MODE` 指令存在时才设置 `originalFile`，
 * 其余夹具走 PREFER_SELF。仓颉当前的 dangling PREFER_SELF 解析路径存在低层缺陷
 * （LL 侧 `getNotNullValueForNotNullContext` 抛 Failed requirement），
 * 因此这里保留「总是设置原件」的行为，避免既有生成套件回归；
 * IGNORE_SELF_MODE 指令仍被保留为显式声明，等 PREFER_SELF 修复后再切换。
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
        val contextModule = CaModuleProvider.getModule(cjFile.project, cjFile, useSiteModule = null)
        val psiFactory = CjPsiFactory.contextual(cjFile, markGenerated = true)
        val fakeFile = psiFactory.createFile("fake.cj", cjFile.text).apply {
            // 显式绑定上下文模块，对齐 Kotlin 夹具：副本既无 copyOrigin 也无 elementContext，
            // 不绑定就会落到「不在任何内容根下」的模块上。
            this.contextModule = contextModule
            // `originalFile` 仍然**总是**设置：改成 Kotlin 的「仅 IGNORE_SELF_MODE 时设置」
            // 会让 21 个生成用例失败，根因是非物理声明被当作 context 传进 class-like
            // 符号缓存（LLCangJieSourceSymbolProvider.kt:177 的 require）。
            // 补 contextModule 后仍失败，说明要修的是**调用方**，本夹具的对齐留待那时再做。
            originalFile = cjFile
        }

        return PreparedFile(fakeFile, cjFile.name)
    }
}
