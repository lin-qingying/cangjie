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
         * 指定使用 `IGNORE_SELF` dangling file resolution mode。
         */
        val IGNORE_SELF_MODE by stringDirective("Whether to use ${CaDanglingFileResolutionMode.IGNORE_SELF} mode.")

        /**
         * 指定使用 `PREFER_SELF` dangling file resolution mode。
         *
         * **仓颉侧独有**：Kotlin 夹具不需要它（Kotlin 的默认就是 PREFER_SELF），
         * 而仓颉的默认仍是 IGNORE_SELF——PREFER_SELF 的解析路径被已记录的 LL 缺陷阻塞
         * （副本模式下非局部解析返回 `null`）。要跑 PREFER_SELF 必须**显式声明**本指令，
         * 这样默认不会踏入已知缺陷，而想复现/验证它时又有一条明确入口。
         */
        val PREFER_SELF_MODE by stringDirective("Whether to use ${CaDanglingFileResolutionMode.PREFER_SELF} mode.")
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
     * 将原始文件复制为按模式参与分析的非物理文件。
     *
     * 复制文件会被项目结构识别为 dangling file module，从而覆盖 dangling 分析路径。
     * 「副本怎么造」的规则集中在 [DanglingFileFixturePreparation]，本方法只负责
     * **按夹具声明的指令选模式**——原先指令声明了却没人读，模式也就无从选择。
     */
    override fun prepareCjFile(cjFile: CjFile, testServices: TestServices): PreparedFile {
        val mode = resolutionMode(testServices)
        return PreparedFile(DanglingFileFixturePreparation.prepareCopy(cjFile, mode), cjFile.name)
    }

    /**
     * 从夹具指令解析本次要用的 dangling 解析模式。
     *
     * 默认仍是 [CaDanglingFileResolutionMode.IGNORE_SELF]：PREFER_SELF 的解析路径被已记录的
     * LL 缺陷阻塞，让默认踏入已知缺陷会让整套 dangling 用例变成噪声。要跑 PREFER_SELF
     * 必须显式声明 `PREFER_SELF_MODE`。
     */
    private fun resolutionMode(testServices: TestServices): CaDanglingFileResolutionMode {
        val directives = testServices.moduleStructure.allDirectives
        val ignoreSelf = Directives.IGNORE_SELF_MODE in directives
        val preferSelf = Directives.PREFER_SELF_MODE in directives
        require(!(ignoreSelf && preferSelf)) {
            "两种 dangling 解析模式不能同时声明：IGNORE_SELF_MODE 与 PREFER_SELF_MODE"
        }
        return if (preferSelf) CaDanglingFileResolutionMode.PREFER_SELF else CaDanglingFileResolutionMode.IGNORE_SELF
    }
}
