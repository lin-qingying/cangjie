package org.cangnova.cangjie.analysis.test.framework.test.configurators

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModuleStructure
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import java.nio.file.Path

/**
 * Analysis API 测试 configurator。
 *
 * 每个具体前端或平台模式都通过 configurator 声明：
 * 1. 需要注册哪些应用/项目服务；
 * 2. 测试数据如何转换成 Analysis API 模块结构；
 * 3. 测试文件在进入断言前需要做哪些预处理。
 */
abstract class AnalysisApiTestConfigurator {
    /**
     * 是否在 dependent session 模式下运行当前测试。
     */
    abstract val analyseInDependentSession: Boolean

    /**
     * 当前 configurator 负责注册的服务装配器。
     */
    abstract val serviceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>>

    /**
     * 从测试数据路径构建 Analysis API 测试模块结构。
     */
    abstract fun createModules(
        testDataPath: Path,
        testServices: TestServices,
        project: Project,
        additionalDirectives: List<DirectivesContainer>,
    ): CjTestModuleStructure

    /**
     * 模块创建完成后的文件预处理钩子。
     */
    open fun prepareFilesInModule(cjTestModule: CjTestModule, testServices: TestServices) {}

    /**
     * 允许 configurator 重写测试数据路径映射规则。
     *
     * 默认保持原路径不变。
     */
    open fun computeTestDataPath(path: Path): Path = path
}
