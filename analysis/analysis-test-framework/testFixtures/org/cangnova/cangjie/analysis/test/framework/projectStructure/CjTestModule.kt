package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.model.TestModule

/**
 * Analysis API 测试模块。
 *
 * 它同时保留：
 * 1. 测试基础设施层的 [TestModule]；
 * 2. Analysis API 层的 [CaModule]；
 * 3. 当前模块对应的 PSI 文件集合。
 */
class CjTestModule(
    val testModule: TestModule,
    val moduleKind: TestModuleKind,
    val caModule: CaModule,
    val psiFiles: List<PsiFile>,
) {
    val name: String
        get() = testModule.name

    val cjFiles: List<CjFile>
        get() = psiFiles.filterIsInstance<CjFile>()
}
