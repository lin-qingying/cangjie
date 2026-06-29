package org.cangnova.cangjie.analysis.test.framework.projectStructure

import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
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
    /**
     * 测试基础设施中的原始模块描述。
     */
    val testModule: TestModule,
    /**
     * 当前测试模块映射到的 Analysis API 模块种类。
     */
    val moduleKind: TestModuleKind,
    /**
     * 当前测试模块在 Analysis API 中使用的主模块视图。
     */
    val caModule: CaModule,
    /**
     * 若源码模块同时产出 binary artifact，则保存对应库模块视图。
     */
    val binaryArtifactModule: CaLibraryModule?,
    /**
     * 当前测试模块关联的 PSI 文件集合。
     */
    val psiFiles: List<PsiFile>,
) {
    /**
     * 测试模块名称。
     */
    val name: String
        get() = testModule.name

    /**
     * 当前模块中的仓颉 PSI 文件集合。
     */
    val cjFiles: List<CjFile>
        get() = psiFiles.filterIsInstance<CjFile>()

    /**
     * 当前测试模块暴露给 Analysis API 的全部模块视图。
     */
    val allCaModules: List<CaModule>
        get() = buildList {
            add(caModule)
            binaryArtifactModule
                ?.takeUnless { it === caModule }
                ?.let(::add)
        }

    /**
     * 根据依赖种类返回测试框架中应暴露的模块视图。
     */
    fun moduleForDependency(kind: DependencyKind): CaModule = when (kind) {
        DependencyKind.Source -> caModule
        DependencyKind.Binary -> caModule as? CaLibraryModule
            ?: binaryArtifactModule
            ?: error("Test module `$name` does not expose a binary dependency module.")
    }
}
