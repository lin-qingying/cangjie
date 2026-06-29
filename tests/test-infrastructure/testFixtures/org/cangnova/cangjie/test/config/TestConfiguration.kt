package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.directives.model.Directive
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.services.TestServices

/**
 * 执行 `interface` 对应的测试配置流程，维持测试框架的阶段契约。
 */
fun interface TestFacade {
    fun transform(module: TestModule, inputArtifact: Any?): Any?
}

/**
 * 定义 `AnalysisHandler` 接口，约束测试配置参与者需要暴露的协作能力。
 */
interface AnalysisHandler {
    /**
     * 执行 `processModule` 对应的测试配置流程，维持测试框架的阶段契约。
     */
    fun processModule(module: TestModule, artifact: Any?, testServices: TestServices)

    /**
     * 执行 `processAfterAllModules` 对应的测试配置流程，维持测试框架的阶段契约。
     */
    fun processAfterAllModules(testServices: TestServices) {}
}

/**
 * 表示 `TestConfiguration`，承载测试配置中的配置数据、测试产物或处理步骤。
 */
class TestConfiguration(
    /**
     * 保存 `facadeFactories`，供测试配置在测试执行期间读取或传递。
     */
    val facadeFactories: List<(TestServices) -> TestFacade>,
    /**
     * 保存 `handlerFactories`，供测试配置在测试执行期间读取或传递。
     */
    val handlerFactories: List<(TestServices) -> AnalysisHandler>,
    /**
     * 保存 `defaultDirectives`，供测试配置在测试执行期间读取或传递。
     */
    val defaultDirectives: List<Directive>,
    /**
     * 保存 `defaultsProviderBuilder`，供测试配置在测试执行期间读取或传递。
     */
    val defaultsProviderBuilder: DefaultsProviderBuilder = DefaultsProviderBuilder(),
)
