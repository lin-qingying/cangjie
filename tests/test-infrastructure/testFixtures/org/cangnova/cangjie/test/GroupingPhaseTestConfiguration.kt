package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.AfterAnalysisChecker
import org.cangnova.cangjie.test.services.MetaTestConfigurator
import org.cangnova.cangjie.test.services.ModuleStructureExtractor
import org.cangnova.cangjie.test.services.PreAnalysisHandler
import org.cangnova.cangjie.test.services.TestServices

interface GroupingPhaseTestConfiguration : TestConfiguration<TestStep.GroupingPhaseStep<*, *>> {
    val mergerWorkers: List<GroupingPhaseInputsMerger.Worker>
}

class GroupingPhaseTestConfigurationImpl(
    override val rootDisposable: Disposable,
    override val testServices: TestServices,
    override val directives: DirectivesContainer,
    override val defaultRegisteredDirectives: RegisteredDirectives,
    override val moduleStructureExtractor: ModuleStructureExtractor,
    override val preAnalysisHandlers: List<PreAnalysisHandler>,
    override val metaTestConfigurators: List<MetaTestConfigurator>,
    override val afterAnalysisCheckers: List<AfterAnalysisChecker>,
    override val metaInfoHandlerEnabled: Boolean,
    override val steps: List<TestStep.GroupingPhaseStep<*, *>>,
    override val mergerWorkers: List<GroupingPhaseInputsMerger.Worker>,
) : GroupingPhaseTestConfiguration
