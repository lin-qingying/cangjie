package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.TestModule

/**
 * Analysis API 测试模块。
 *
 * 它同时保留：
 * 1. 测试基础设施层的 [TestModule]
 * 2. Analysis API 层的主模块视图
 * 3. 供 binary dependency 使用的库二进制视图
 * 4. 当前模块对应的 PSI 文件集合
 */
class CjTestModule(
    val testModule: TestModule,
    val moduleKind: TestModuleKind,
    val caModule: CaModule,
    val binaryArtifactModule: CaLibraryModule?,
    val auxiliaryModules: List<CaModule>,
    val psiFiles: List<PsiFile>,
) {
    val name: String
        get() = testModule.name

    val cjFiles: List<CjFile>
        get() = psiFiles.filterIsInstance<CjFile>()

    val allCaModules: List<CaModule>
        get() = buildList {
            add(caModule)
            binaryArtifactModule
                ?.takeUnless { it === caModule }
                ?.let(::add)
            addAll(auxiliaryModules.filter { auxiliaryModule ->
                auxiliaryModule !== caModule && auxiliaryModule !== binaryArtifactModule
            })
        }

    /**
     * 根据依赖种类返回测试框架中应暴露的模块视图。
     */
    fun moduleForDependency(kind: DependencyKind): CaModule = when (kind) {
        DependencyKind.Source -> caModule
        DependencyKind.Binary -> binaryArtifactModule
            ?: error("Test module `$name` does not expose a binary artifact module.")
    }
}
