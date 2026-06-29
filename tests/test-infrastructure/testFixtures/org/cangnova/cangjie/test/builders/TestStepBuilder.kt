package org.cangnova.cangjie.test.builders
import org.cangnova.cangjie.test.Constructor2
import org.cangnova.cangjie.test.Constructor
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.TestStep
import org.cangnova.cangjie.test.model.AbstractGroupingPhaseTestFacade
import org.cangnova.cangjie.test.model.AbstractTestFacade
import org.cangnova.cangjie.test.model.AbstractTestFacadeBase
import org.cangnova.cangjie.test.model.AnalysisHandler
import org.cangnova.cangjie.test.model.AnalysisHandlerBase
import org.cangnova.cangjie.test.model.GroupingPhaseHandler
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestArtifactKind
import org.cangnova.cangjie.test.services.CompilationStage
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.utils.bind

/**
 * 表示 `TestStepBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
sealed class TestStepBuilder<InputArtifact, OutputArtifact, out FacadeStep>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>,
              FacadeStep : TestStep<InputArtifact, OutputArtifact> {
    /**
     * 提供 `createTestStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    @TestInfrastructureInternals
    abstract fun createTestStep(testServices: TestServices): FacadeStep

    /**
     * 表示 `FacadeStepBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
     */
    sealed class FacadeStepBuilder<InputArtifact, OutputArtifact, Facade, FacadeStep>(
        /**
         * 保存 `facade`，供测试配置构建在测试执行期间读取或传递。
         */
        val facade: Constructor<Facade>,
    ) : TestStepBuilder<InputArtifact, OutputArtifact, FacadeStep>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact>,
                  Facade : AbstractTestFacadeBase<InputArtifact, OutputArtifact>,
                  FacadeStep : TestStep<InputArtifact, OutputArtifact> {
        /**
         * 执行 `createTestStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        @TestInfrastructureInternals
        abstract override fun createTestStep(testServices: TestServices): FacadeStep

        /**
         * 表示 `NonGroupingPhase`，承载测试配置构建中的配置数据、测试产物或处理步骤。
         */
        class NonGroupingPhase<InputArtifact, OutputArtifact>(
            facade: Constructor<AbstractTestFacade<InputArtifact, OutputArtifact>>,
        ) : FacadeStepBuilder<
                InputArtifact,
                OutputArtifact,
                AbstractTestFacade<InputArtifact, OutputArtifact>,
                TestStep.NonGroupingStep.FacadeStep<InputArtifact, OutputArtifact>
                >(facade) where InputArtifact : ResultingArtifact<InputArtifact>,
                                OutputArtifact : ResultingArtifact<OutputArtifact> {
            /**
             * 执行 `createTestStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
             */
            @TestInfrastructureInternals
            override fun createTestStep(testServices: TestServices): TestStep.NonGroupingStep.FacadeStep<InputArtifact, OutputArtifact> {
                return TestStep.NonGroupingStep.FacadeStep(facade.invoke(testServices))
            }
        }

        /**
         * 表示 `GroupingPhase`，承载测试配置构建中的配置数据、测试产物或处理步骤。
         */
        class GroupingPhase<InputArtifact, OutputArtifact>(
            facade: Constructor<AbstractGroupingPhaseTestFacade<InputArtifact, OutputArtifact>>,
        ) : FacadeStepBuilder<
                InputArtifact,
                OutputArtifact,
                AbstractGroupingPhaseTestFacade<InputArtifact, OutputArtifact>,
                TestStep.GroupingPhaseStep.FacadeStep<InputArtifact, OutputArtifact>
                >(facade) where InputArtifact : ResultingArtifact<InputArtifact>,
                                OutputArtifact : ResultingArtifact<OutputArtifact> {
            /**
             * 执行 `createTestStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
             */
            @TestInfrastructureInternals
            override fun createTestStep(testServices: TestServices): TestStep.GroupingPhaseStep.FacadeStep<InputArtifact, OutputArtifact> {
                return TestStep.GroupingPhaseStep.FacadeStep(facade.invoke(testServices))
            }
        }
    }

    /**
     * 表示 `HandlersStepBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
     */
    sealed class HandlersStepBuilder<InputArtifact, InputArtifactKind, Handler, HandlersStep>(
        /**
         * 保存 `artifactKind`，供测试配置构建在测试执行期间读取或传递。
         */
        val artifactKind: InputArtifactKind,
        /**
         * 保存 `compilationStage`，供测试配置构建在测试执行期间读取或传递。
         */
        val compilationStage: CompilationStage,
    ) : TestStepBuilder<InputArtifact, Nothing, HandlersStep>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  InputArtifactKind : TestArtifactKind<InputArtifact>,
                  Handler : AnalysisHandlerBase<InputArtifact>,
                  HandlersStep : TestStep<InputArtifact, Nothing> {
        /**
         * 保存 `handlers`，供测试配置构建在测试执行期间读取或传递。
         */
        private val handlers: MutableList<Constructor<Handler>> = mutableListOf()

        /**
         * 执行 `useHandlers` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        fun useHandlers(vararg constructor: Constructor<Handler>) {
            handlers += constructor
        }

        /**
         * 执行 `useHandlers` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        fun useHandlers(vararg constructor: Constructor2<InputArtifactKind, Handler>) {
            constructor.mapTo(handlers) { it.bind(artifactKind) }
        }

        /**
         * 执行 `useHandlersAtFirst` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        fun useHandlersAtFirst(vararg constructor: Constructor<Handler>) {
            handlers.addAll(0, constructor.toList())
        }

        /**
         * 执行 `useHandlers` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        fun useHandlers(constructors: List<Constructor<Handler>>) {
            handlers += constructors
        }

        /**
         * 执行 `createTestStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        @TestInfrastructureInternals
        override fun createTestStep(testServices: TestServices): HandlersStep {
            val handlers = handlers.map { constructor ->
                constructor
                    .invoke(testServices)
                    .also { it.setCompilationStage(compilationStage) }
            }
            return createStep(handlers)
        }

        /**
         * 提供 `createStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
         */
        protected abstract fun createStep(handlers: List<Handler>): HandlersStep

        /**
         * 表示 `NonGroupingPhase`，承载测试配置构建中的配置数据、测试产物或处理步骤。
         */
        class NonGroupingPhase<InputArtifact, InputArtifactKind>(
            artifactKind: InputArtifactKind,
            compilationStage: CompilationStage,
        ) : HandlersStepBuilder<
                InputArtifact,
                InputArtifactKind,
                AnalysisHandler<InputArtifact>,
                TestStep.NonGroupingStep.HandlersStep<InputArtifact>>
            (artifactKind, compilationStage)
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      InputArtifactKind : TestArtifactKind<InputArtifact> {
            /**
             * 执行 `createStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
             */
            override fun createStep(handlers: List<AnalysisHandler<InputArtifact>>): TestStep.NonGroupingStep.HandlersStep<InputArtifact> {
                return TestStep.NonGroupingStep.HandlersStep(artifactKind, handlers)
            }
        }

        /**
         * 表示 `GroupingPhase`，承载测试配置构建中的配置数据、测试产物或处理步骤。
         */
        class GroupingPhase<InputArtifact, InputArtifactKind>(
            artifactKind: InputArtifactKind,
            compilationStage: CompilationStage,
        ) : HandlersStepBuilder<
                InputArtifact,
                InputArtifactKind,
                GroupingPhaseHandler<InputArtifact>,
                TestStep.GroupingPhaseStep.HandlersStep<InputArtifact>>
            (artifactKind, compilationStage)
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      InputArtifactKind : TestArtifactKind<InputArtifact> {
            /**
             * 执行 `createStep` 对应的测试配置构建流程，维持测试框架的阶段契约。
             */
            override fun createStep(handlers: List<GroupingPhaseHandler<InputArtifact>>): TestStep.GroupingPhaseStep.HandlersStep<InputArtifact> {
                return TestStep.GroupingPhaseStep.HandlersStep(artifactKind, handlers)
            }
        }
    }
}
