package org.cangjie.analysis.test.framework.test.configurators

import com.intellij.openapi.project.Project
import org.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * 测试配置器（对齐 Kotlin 的 AnalysisApiTestConfigurator）。
 *
 * 每个具体配置器（如 CFIR）实现此抽象类，
 * 定义服务注册器列表和模块创建逻辑。
 */
abstract class AnalysisApiTestConfigurator {
    abstract val analyseInDependentSession: Boolean

    abstract val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>

    /**
     * 从测试数据路径创建模块结构。
     *
     * 实现应加载 .cj 测试文件、创建 PSI、构建 [CjTestModule] 并组装为 [CjTestModuleStructure]。
     */
    abstract fun createModules(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
    ): CjTestModuleStructure

    /** 模块创建后的文件预处理钩子 */
    open fun prepareFilesInModule(cjTestModule: CjTestModule, testServices: TestServices) {}

    /** 测试数据路径变换（默认原样返回） */
    open fun computeTestDataPath(path: Path): Path = path
}
