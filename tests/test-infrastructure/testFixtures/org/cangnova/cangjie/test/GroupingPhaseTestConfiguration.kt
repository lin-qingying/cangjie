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
 * 定义 `GroupingPhaseTestConfiguration` 接口，约束测试基础设施参与者需要暴露的协作能力。
 */
interface GroupingPhaseTestConfiguration : TestConfiguration<TestStep.GroupingPhaseStep<*, *>> {
    /**
     * 保存 `mergerWorkers`，供测试基础设施在测试执行期间读取或传递。
     */
    val mergerWorkers: List<GroupingPhaseInputsMerger.Worker>
}

/**
 * 表示 `GroupingPhaseTestConfigurationImpl`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class GroupingPhaseTestConfigurationImpl(
    /**
     * 保存 `rootDisposable`，供测试基础设施在测试执行期间读取或传递。
     */
    override val rootDisposable: Disposable,
    /**
     * 保存 `testServices`，供测试基础设施在测试执行期间读取或传递。
     */
    override val testServices: TestServices,
    /**
     * 保存 `directives`，供测试基础设施在测试执行期间读取或传递。
     */
    override val directives: DirectivesContainer,
    /**
     * 保存 `defaultRegisteredDirectives`，供测试基础设施在测试执行期间读取或传递。
     */
    override val defaultRegisteredDirectives: RegisteredDirectives,
    /**
     * 保存 `moduleStructureExtractor`，供测试基础设施在测试执行期间读取或传递。
     */
    override val moduleStructureExtractor: ModuleStructureExtractor,
    /**
     * 保存 `preAnalysisHandlers`，供测试基础设施在测试执行期间读取或传递。
     */
    override val preAnalysisHandlers: List<PreAnalysisHandler>,
    /**
     * 保存 `metaTestConfigurators`，供测试基础设施在测试执行期间读取或传递。
     */
    override val metaTestConfigurators: List<MetaTestConfigurator>,
    /**
     * 保存 `afterAnalysisCheckers`，供测试基础设施在测试执行期间读取或传递。
     */
    override val afterAnalysisCheckers: List<AfterAnalysisChecker>,
    /**
     * 保存 `metaInfoHandlerEnabled`，供测试基础设施在测试执行期间读取或传递。
     */
    override val metaInfoHandlerEnabled: Boolean,
    /**
     * 保存 `steps`，供测试基础设施在测试执行期间读取或传递。
     */
    override val steps: List<TestStep.GroupingPhaseStep<*, *>>,
    /**
     * 保存 `mergerWorkers`，供测试基础设施在测试执行期间读取或传递。
     */
    override val mergerWorkers: List<GroupingPhaseInputsMerger.Worker>,
) : GroupingPhaseTestConfiguration
