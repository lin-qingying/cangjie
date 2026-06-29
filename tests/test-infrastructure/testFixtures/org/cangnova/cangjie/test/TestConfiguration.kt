package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.ModuleStructureExtractor
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.TestServices

/**
 * 定义 `Constructor` 类型别名，统一测试基础设施中的回调或构造签名。
 */
typealias Constructor<R> = (TestServices) -> R

/**
 * 定义 `Constructor2` 类型别名，统一测试基础设施中的回调或构造签名。
 */
typealias Constructor2<T, R> = (TestServices, T) -> R

/**
 * 测试配置
 *
 * 对应 Kotlin K2 的 TestConfiguration
 */
interface TestConfiguration<Step : TestStep<*, *>> {
    /**
     * 保存 `rootDisposable`，供测试基础设施在测试执行期间读取或传递。
     */
    val rootDisposable: Disposable
    /**
     * 保存 `testServices`，供测试基础设施在测试执行期间读取或传递。
     */
    val testServices: TestServices
    /**
     * 保存 `directives`，供测试基础设施在测试执行期间读取或传递。
     */
    val directives: DirectivesContainer
    /**
     * 保存 `defaultRegisteredDirectives`，供测试基础设施在测试执行期间读取或传递。
     */
    val defaultRegisteredDirectives: RegisteredDirectives
    /**
     * 保存 `moduleStructureExtractor`，供测试基础设施在测试执行期间读取或传递。
     */
    val moduleStructureExtractor: ModuleStructureExtractor
    /**
     * 保存 `preAnalysisHandlers`，供测试基础设施在测试执行期间读取或传递。
     */
    val preAnalysisHandlers: List<PreAnalysisHandler>
    /**
     * 保存 `metaTestConfigurators`，供测试基础设施在测试执行期间读取或传递。
     */
    val metaTestConfigurators: List<MetaTestConfigurator>
    /**
     * 保存 `afterAnalysisCheckers`，供测试基础设施在测试执行期间读取或传递。
     */
    val afterAnalysisCheckers: List<AfterAnalysisChecker>
    /**
     * 保存 `metaInfoHandlerEnabled`，供测试基础设施在测试执行期间读取或传递。
     */
    val metaInfoHandlerEnabled: Boolean

    /**
     * 保存 `steps`，供测试基础设施在测试执行期间读取或传递。
     */
    val steps: List<Step>
}

/**
 * 执行 `declaration` 对应的测试基础设施流程，维持测试框架的阶段契约。
 */
fun <R> (() -> R).coerce(): Constructor<R> {
    return { this.invoke() }
}
