package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider.DanglingFileTestDirectives.IGNORE_DANGLING_FILES
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.moduleStructure

/**
 * dangling file 场景下的文件诊断收集测试。
 *
 * 对齐 Kotlin `AbstractDanglingFileCollectDiagnosticsTest`：复制主文件为非物理 PSI，
 * 并通过 `originalFile` 让项目结构服务把它归入 dangling file module。
 *
 * 模式选择：`originalFile` 只在 `IGNORE_SELF_MODE` 下设置（与 Kotlin 夹具一致），
 * 模式本身由 [DanglingFileFixturePreparation.resolutionMode] 从指令解析，
 * 默认 IGNORE_SELF；要跑 PREFER_SELF 的数据文件须显式声明 `PREFER_SELF_MODE`
 * （仓颉侧默认与 Kotlin 不同，见该指令的 KDoc）。
 */
abstract class AbstractDanglingFileCollectDiagnosticsTest : AbstractCollectDiagnosticsTest() {
    /**
     * 当前 dangling file 测试额外注册的控制指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + DanglingFileTestDirectives

    /**
     * 执行 dangling file 诊断测试。
     *
     * 若测试数据显式声明忽略 dangling files，则捕获断言错误并允许该场景跳过。
     */
    override fun doTest(testServices: TestServices) {
        try {
            super.doTest(testServices)
        } catch (error: AssertionError) {
            if (IGNORE_DANGLING_FILES !in testServices.moduleStructure.allDirectives) {
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
        val mode: CaDanglingFileResolutionMode =
            DanglingFileFixturePreparation.resolutionMode(testServices.moduleStructure.allDirectives)
        return PreparedFile(DanglingFileFixturePreparation.prepareCopy(cjFile, mode), cjFile.name)
    }
}
