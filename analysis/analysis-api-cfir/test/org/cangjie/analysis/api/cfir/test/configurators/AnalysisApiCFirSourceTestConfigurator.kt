package org.cangjie.analysis.api.cfir.test.configurators

import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.CaModule
import org.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * CFIR 源码模式测试配置器（对齐 Kotlin 的 AnalysisApiFirSourceTestConfigurator）。
 *
 * 使用 CFIR 引擎分析源码形式的 .cj 测试文件。
 */
open class AnalysisApiCFirSourceTestConfigurator(
    override val analyseInDependentSession: Boolean,
) : AnalysisApiTestConfigurator() {

    override val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> = listOf(
        AnalysisApiCFirServiceRegistrar,
    )

    override fun createModules(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure {
        // TODO: 实现完整的测试模块创建逻辑
        //  1. 从 testDataPath 查找 .cj 文件
        //  2. 创建 VirtualFile 和 PsiFile
        //  3. 构建 CaModule
        //  4. 组装 CjTestModule
        val testModule = CjTestModule(
            name = "main",
            moduleKind = TestModuleKind.Source,
            caModule = createTestModule(project),
            psiFiles = emptyList(), // TODO: 加载测试 .cj 文件
        )
        return CjTestModuleStructure(listOf(testModule))
    }

    private fun createTestModule(project: Project): CaModule {
        return object : CaModule {
            override val name: String = "testModule"
            override val project: Project = project
        }
    }
}
