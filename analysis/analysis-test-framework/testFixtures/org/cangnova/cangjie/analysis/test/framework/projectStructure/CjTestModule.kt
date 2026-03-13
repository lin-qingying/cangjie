package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile

/**
 * 测试模块（对齐 Kotlin 的 KtTestModule）。
 *
 * 封装测试框架的模块元数据和 Analysis API 的语义模块，以及模块中的 PSI 文件。
 */
class CjTestModule(
    val name: String,
    val moduleKind: TestModuleKind,
    val caModule: CaModule,
    val psiFiles: List<PsiFile>,
) {
    val cjFiles: List<CjFile>
        get() = psiFiles.filterIsInstance<CjFile>()
}
