package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.diagnosticProvider

import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileResolutionMode
import org.cangnova.cangjie.analysis.api.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.contextModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjPsiFactory

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
