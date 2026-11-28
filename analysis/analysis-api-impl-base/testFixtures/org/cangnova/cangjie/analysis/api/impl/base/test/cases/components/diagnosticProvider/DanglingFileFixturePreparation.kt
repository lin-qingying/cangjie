package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer

/**
 * dangling 诊断夹具的**指令集合**。
 *
 * 指令必须与消费点放在一起看：这三个指令此前声明在抽象测试类里，而模式指令
 * **从没人读**——「两种模式」在测试里既跑不到也验不了（任务 2.4 的原始缺陷）。
 * 消费点收敛到 [DanglingFileFixturePreparation.resolutionMode]，本容器与它同文件，
 * 谁加指令谁就看得见旁边必须有的消费分支。
 */
object DanglingFileTestDirectives : SimpleDirectivesContainer() {
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
     * 而仓颉的默认仍是 IGNORE_SELF——两个模式的解析行为由 `analysis-api-cfir` 的
     * `AnalysisApiDanglingFileExecutionTest` 按解析目标验证（PREFER_SELF→副本、
     * IGNORE_SELF→原件），本夹具只需把模式传到准备逻辑。
     */
    val PREFER_SELF_MODE by stringDirective("Whether to use ${CaDanglingFileResolutionMode.PREFER_SELF} mode.")
}

/**
 * dangling 诊断夹具的**副本准备**逻辑。
 *
 * 抽成独立对象是为了让它**可被直接验证**：原先它长在抽象测试类里，
 * 只有走完整诊断管线才能碰到，于是「副本怎么造」这件事一直没有针对性用例
 * （而它决定了整条 dangling 路径走哪个模式）。
 *
 * 两种模式的差别只有一处：**是否把平台 `PsiFile.originalFile` 指向原件**。
 * - [CaDanglingFileResolutionMode.IGNORE_SELF]：指向原件，副本里的非局部引用解析回原件；
 * - [CaDanglingFileResolutionMode.PREFER_SELF]：不指向原件，副本只靠 `contextModule` 绑定上下文
 *   （对齐 Kotlin 夹具；Kotlin 那边副本也没有 `copyOrigin`）。
 */
object DanglingFileFixturePreparation {
    /**
     * 从夹具指令解析本次要用的 dangling 解析模式。
     *
     * 默认 [CaDanglingFileResolutionMode.IGNORE_SELF]；要跑 PREFER_SELF 必须显式声明
     * [DanglingFileTestDirectives.PREFER_SELF_MODE]。两种模式同时声明是夹具写错，
     * 直接失败而不是悄悄挑一个——挑错了只会让用例以另一种模式跑绿。
     */
    fun resolutionMode(directives: RegisteredDirectives): CaDanglingFileResolutionMode {
        val ignoreSelf = DanglingFileTestDirectives.IGNORE_SELF_MODE in directives
        val preferSelf = DanglingFileTestDirectives.PREFER_SELF_MODE in directives
        require(!(ignoreSelf && preferSelf)) {
            "两种 dangling 解析模式不能同时声明：IGNORE_SELF_MODE 与 PREFER_SELF_MODE"
        }
        return if (preferSelf) CaDanglingFileResolutionMode.PREFER_SELF else CaDanglingFileResolutionMode.IGNORE_SELF
    }

    /**
     * 把 [cjFile] 复制成按 [mode] 参与分析的 dangling 副本。
     *
     * 副本是**非物理**文件：既没有 `copyOrigin` 也没有 elementContext，
     * 因此必须显式绑定上下文模块，否则会落到「不在任何内容根下」的模块上。
     */
    fun prepareCopy(cjFile: CjFile, mode: CaDanglingFileResolutionMode): CjFile {
        val contextModule = CaModuleProvider.getModule(cjFile.project, cjFile, useSiteModule = null)
        return CjPsiFactory.contextual(cjFile, markGenerated = true).createFile("fake.cj", cjFile.text).apply {
            this.contextModule = contextModule
            when (mode) {
                CaDanglingFileResolutionMode.IGNORE_SELF -> originalFile = cjFile
                CaDanglingFileResolutionMode.PREFER_SELF -> Unit
            }
        }
    }
}
