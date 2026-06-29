package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.*

/**
 * 测试步骤
 *
 * 对应 Kotlin K2 的 TestStep
 */
sealed class TestStep<InputArtifact, OutputArtifact>
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact> {
    /**
     * 保存 `inputArtifactKind`，供测试基础设施在测试执行期间读取或传递。
     */
    abstract val inputArtifactKind: TestArtifactKind<InputArtifact>

    /**
     * 定义 `FacadeStep` 接口，约束测试基础设施参与者需要暴露的协作能力。
     */
    sealed interface FacadeStep<InputArtifact, OutputArtifact>
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {
        /**
         * 保存 `facade`，供测试基础设施在测试执行期间读取或传递。
         */
        val facade: AbstractTestFacadeBase<InputArtifact, OutputArtifact>
    }

    /**
     * 定义 `HandlersStep` 接口，约束测试基础设施参与者需要暴露的协作能力。
     */
    sealed interface HandlersStep<InputArtifact>
            where InputArtifact : ResultingArtifact<InputArtifact>{
        /**
         * 保存 `handlers`，供测试基础设施在测试执行期间读取或传递。
         */
        val handlers: List<AnalysisHandlerBase<InputArtifact>>
    }

    /**
     * 表示 `NonGroupingStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    sealed class NonGroupingStep<InputArtifact, OutputArtifact> : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {

        /**
         * 提供 `shouldProcessModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        open fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
            return inputArtifact.kind == inputArtifactKind
        }

        /**
         * 提供 `processModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        abstract fun processModule(
            module: TestModule,
            inputArtifact: InputArtifact,
            thereWereExceptionsOnPreviousSteps: Boolean,
        ): StepResult<out OutputArtifact>

        /**
         * 表示 `FacadeStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class FacadeStep<InputArtifact, OutputArtifact>(
            /**
             * 保存 `facade`，供测试基础设施在测试执行期间读取或传递。
             */
            override val facade: AbstractTestFacade<InputArtifact, OutputArtifact>,
        ) : NonGroupingStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            /**
             * 保存 `inputArtifactKind`，供测试基础设施在测试执行期间读取或传递。
             */
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind

            /**
             * 执行 `shouldProcessModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun shouldProcessModule(module: TestModule, inputArtifact: ResultingArtifact<*>): Boolean {
                return super.shouldProcessModule(module, inputArtifact) && facade.shouldTransform(module)
            }

            /**
             * 执行 `processModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun processModule(
                module: TestModule,
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean,
            ): StepResult<out OutputArtifact> {
                val outputArtifact = try {
                    facade.transform(module, inputArtifact) ?: return StepResult.NoArtifactFromFacade
                } catch (e: Throwable) {
                    // TODO: remove inheritors of WrappedException.FromFacade
                    return StepResult.ErrorFromFacade(WrappedException.FromFacade(e, module, facade))
                }
                return StepResult.Artifact(outputArtifact)
            }

            /**
             * 执行 `toString` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun toString(): String {
                return "Facade: ${facade::class.simpleName}"
            }
        }

        /**
         * 表示 `HandlersStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class HandlersStep<InputArtifact : ResultingArtifact<InputArtifact>>(
            /**
             * 保存 `inputArtifactKind`，供测试基础设施在测试执行期间读取或传递。
             */
            override val inputArtifactKind: TestArtifactKind<InputArtifact>,
            /**
             * 保存 `handlers`，供测试基础设施在测试执行期间读取或传递。
             */
            override val handlers: List<AnalysisHandler<InputArtifact>>
        ) : NonGroupingStep<InputArtifact, Nothing>(), TestStep.HandlersStep<InputArtifact> {
            init {
                for (handler in handlers) {
                    require(handler.artifactKind == inputArtifactKind) {
                        "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                                "In handler $handler artifact kind is ${handler.artifactKind}"
                    }
                }
            }

            /**
             * 执行 `processModule` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun processModule(
                module: TestModule,
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean
            ): StepResult.HandlersResult {
                val exceptions = mutableListOf<WrappedException>()
                var shouldRunNextSteps = true
                for (outputHandler in handlers) {
                    if (outputHandler.shouldRun(thereWasAnException = thereWereExceptionsOnPreviousSteps || exceptions.isNotEmpty())) {
                        try {
                            outputHandler.processModule(module, inputArtifact)
                        } catch (e: Throwable) {
                            exceptions += WrappedException.FromHandler(e, module, outputHandler)
                            if (outputHandler.failureDisablesNextSteps) {
                                shouldRunNextSteps = false
                            }
                        }
                    }
                }
                return StepResult.HandlersResult(exceptions, shouldRunNextSteps)
            }

            /**
             * 执行 `toString` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun toString(): String {
                return "Handlers for $inputArtifactKind"
            }
        }
    }

    /**
     * 表示 `GroupingPhaseStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    sealed class GroupingPhaseStep<InputArtifact, OutputArtifact> : TestStep<InputArtifact, OutputArtifact>()
            where InputArtifact : ResultingArtifact<InputArtifact>,
                  OutputArtifact : ResultingArtifact<OutputArtifact> {

        /**
         * 提供 `process` 对应的测试基础设施流程，维持测试框架的阶段契约。
         */
        abstract fun process(inputArtifact: InputArtifact, thereWereExceptionsOnPreviousSteps: Boolean): StepResult<out OutputArtifact>

        /**
         * 表示 `FacadeStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class FacadeStep<InputArtifact, OutputArtifact>(
            /**
             * 保存 `facade`，供测试基础设施在测试执行期间读取或传递。
             */
            override val facade: AbstractGroupingPhaseTestFacade<InputArtifact, OutputArtifact>,
        ) : GroupingPhaseStep<InputArtifact, OutputArtifact>(), TestStep.FacadeStep<InputArtifact, OutputArtifact>
                where InputArtifact : ResultingArtifact<InputArtifact>,
                      OutputArtifact : ResultingArtifact<OutputArtifact> {
            /**
             * 保存 `inputArtifactKind`，供测试基础设施在测试执行期间读取或传递。
             */
            override val inputArtifactKind: TestArtifactKind<InputArtifact>
                get() = facade.inputKind


            /**
             * 执行 `process` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun process(
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean,
            ): StepResult<out OutputArtifact> {
                val outputArtifact = try {
                    facade.transform(inputArtifact) ?: return StepResult.NoArtifactFromFacade
                } catch (e: Throwable) {
                    return StepResult.ErrorFromFacade(WrappedException.FromGroupingFacade(e, facade))
                }
                return StepResult.Artifact(outputArtifact)
            }

            /**
             * 执行 `toString` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun toString(): String {
                return "Facade: ${facade::class.simpleName}"
            }
        }

        /**
         * 表示 `HandlersStep`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class HandlersStep<InputArtifact : ResultingArtifact<InputArtifact>>(
            /**
             * 保存 `inputArtifactKind`，供测试基础设施在测试执行期间读取或传递。
             */
            override val inputArtifactKind: TestArtifactKind<InputArtifact>,
            /**
             * 保存 `handlers`，供测试基础设施在测试执行期间读取或传递。
             */
            override val handlers: List<GroupingPhaseHandler<InputArtifact>>
        ) : GroupingPhaseStep<InputArtifact, Nothing>(), TestStep.HandlersStep<InputArtifact> {
            init {
                for (handler in handlers) {
                    require(handler.artifactKind == inputArtifactKind) {
                        "Artifact kind mismatch. Artifact kind of each handler must match input artifact kind ($inputArtifactKind). " +
                                "In handler $handler artifact kind is ${handler.artifactKind}"
                    }
                }
            }

            /**
             * 执行 `process` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun process(
                inputArtifact: InputArtifact,
                thereWereExceptionsOnPreviousSteps: Boolean,
            ): StepResult.HandlersResult {
                val exceptions = mutableListOf<WrappedException>()
                var shouldRunNextSteps = true
                for (outputHandler in handlers) {
                    try {
                        outputHandler.processArtifact(inputArtifact)
                    } catch (e: Throwable) {
                        exceptions += WrappedException.FromGroupingHandler(e, outputHandler)
                        if (outputHandler.failureDisablesNextSteps) {
                            shouldRunNextSteps = false
                        }
                    }
                }
                return StepResult.HandlersResult(exceptions, shouldRunNextSteps)
            }

            /**
             * 执行 `toString` 对应的测试基础设施流程，维持测试框架的阶段契约。
             */
            override fun toString(): String {
                return "Handlers for $inputArtifactKind"
            }
        }
    }

    /**
     * 表示 `StepResult`，承载测试基础设施中的配置数据、测试产物或处理步骤。
     */
    sealed class StepResult<OutputArtifact : ResultingArtifact<OutputArtifact>> {

        /**
         * 表示 `Artifact`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class Artifact<OutputArtifact : ResultingArtifact<OutputArtifact>>(val outputArtifact: OutputArtifact) :
            StepResult<OutputArtifact>()

        /**
         * 表示 `ErrorFromFacade`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        class ErrorFromFacade<OutputArtifact : ResultingArtifact<OutputArtifact>>(val exception: WrappedException) :
            StepResult<OutputArtifact>()

        /**
         * 表示 `HandlersResult`，承载测试基础设施中的配置数据、测试产物或处理步骤。
         */
        data class HandlersResult(
            /**
             * 保存 `exceptionsFromHandlers`，供测试基础设施在测试执行期间读取或传递。
             */
            val exceptionsFromHandlers: Collection<WrappedException>,
            /**
             * 保存 `shouldRunNextSteps`，供测试基础设施在测试执行期间读取或传递。
             */
            val shouldRunNextSteps: Boolean
        ) : StepResult<Nothing>()

        /**
         * 提供 `NoArtifactFromFacade` 单例，集中承载测试基础设施的共享状态、常量或默认行为。
         */
        data object NoArtifactFromFacade : StepResult<Nothing>()
    }
}

