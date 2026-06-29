package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.services.ServiceRegistrationData
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.defaultsProvider

/**
 * 定义 `ServicesAndDirectivesContainer` 接口，约束测试模型参与者需要暴露的协作能力。
 */
interface ServicesAndDirectivesContainer {
    /**
     * 保存 `additionalServices`，供测试模型在测试执行期间读取或传递。
     */
    val additionalServices: List<ServiceRegistrationData>
        get() = emptyList()

    /**
     * 保存 `directiveContainers`，供测试模型在测试执行期间读取或传递。
     */
    val directiveContainers: List<DirectivesContainer>
        get() = emptyList()
}

/**
 * 表示 `AbstractTestFacadeBase`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
sealed class AbstractTestFacadeBase<InputArtifact, OutputArtifact> : ServicesAndDirectivesContainer
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>
{
    /**
     * 保存 `inputKind`，供测试模型在测试执行期间读取或传递。
     */
    abstract val inputKind: TestArtifactKind<InputArtifact>
    /**
     * 保存 `outputKind`，供测试模型在测试执行期间读取或传递。
     */
    abstract val outputKind: TestArtifactKind<OutputArtifact>
}

// ----------------------------- non-grouping phase -----------------------------

/**
 * 表示 `AbstractTestFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractTestFacade<InputArtifact, OutputArtifact> : AbstractTestFacadeBase<InputArtifact, OutputArtifact>()
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>
{
    /**
     * 提供 `transform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun transform(module: TestModule, inputArtifact: InputArtifact): OutputArtifact?
    /**
     * 提供 `shouldTransform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun shouldTransform(module: TestModule): Boolean
}

/**
 * 表示 `FrontendFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class FrontendFacade<FrontendOutputArtifact>(
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    final override val outputKind: FrontendKind<FrontendOutputArtifact>
) : AbstractTestFacade<ResultingArtifact.Source, FrontendOutputArtifact>()
        where FrontendOutputArtifact : ResultingArtifact.FrontendOutput<FrontendOutputArtifact> {
    final override val inputKind: TestArtifactKind<ResultingArtifact.Source>
        get() = SourcesKind

    /**
     * 执行 `shouldTransform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.frontendKind == outputKind
    }

    /**
     * 提供 `analyze` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun analyze(module: TestModule): FrontendOutputArtifact?

    /**
     * 执行 `transform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    final override fun transform(module: TestModule, inputArtifact: ResultingArtifact.Source): FrontendOutputArtifact? {
        // TODO: pass sources
        return analyze(module)
    }
}

/**
 * 表示 `Frontend2BackendConverter`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class Frontend2BackendConverter<FrontendOutputArtifact, BackendInputArtifact>(
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    final override val inputKind: FrontendKind<FrontendOutputArtifact>,
    final override val outputKind: BackendKind<BackendInputArtifact>,
) : AbstractTestFacade<FrontendOutputArtifact, BackendInputArtifact>()
        where FrontendOutputArtifact : ResultingArtifact.FrontendOutput<FrontendOutputArtifact>,
              BackendInputArtifact : ResultingArtifact.BackendInput<BackendInputArtifact> {
    /**
     * 执行 `shouldTransform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == outputKind
    }
}

/**
 * 表示 `IrPreSerializationLoweringFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class IrPreSerializationLoweringFacade<BackendInputArtifact>(
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    final override val inputKind: BackendKind<BackendInputArtifact>,
    final override val outputKind: BackendKind<BackendInputArtifact>,
) : AbstractTestFacade<BackendInputArtifact, BackendInputArtifact>()
        where BackendInputArtifact : ResultingArtifact.BackendInput<BackendInputArtifact>

/**
 * 表示 `BackendFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class BackendFacade<BackendInputArtifact, BinaryOutputArtifact>(
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    final override val inputKind: BackendKind<BackendInputArtifact>,
    final override val outputKind: ArtifactKind<BinaryOutputArtifact>,
) : AbstractTestFacade<BackendInputArtifact, BinaryOutputArtifact>()
        where BackendInputArtifact : ResultingArtifact.BackendInput<BackendInputArtifact>,
              BinaryOutputArtifact : ResultingArtifact.Binary<BinaryOutputArtifact> {
    /**
     * 执行 `shouldTransform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun shouldTransform(module: TestModule): Boolean {
        return with(testServices.defaultsProvider) {
            backendKind == inputKind && artifactKind == outputKind
        }
    }
}

/**
 * 表示 `DeserializerFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class DeserializerFacade<BinaryArtifact, BackendInputArtifact>(
    /**
     * 保存 `testServices`，供测试模型在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    final override val inputKind: ArtifactKind<BinaryArtifact>,
    final override val outputKind: BackendKind<BackendInputArtifact>,
) : AbstractTestFacade<BinaryArtifact, BackendInputArtifact>()
        where BinaryArtifact : ResultingArtifact.Binary<BinaryArtifact>,
              BackendInputArtifact : ResultingArtifact.BackendInput<BackendInputArtifact> {

    /**
     * 执行 `shouldTransform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun shouldTransform(module: TestModule): Boolean {
        return testServices.defaultsProvider.backendKind == outputKind
    }
}

// ----------------------------- grouping phase -----------------------------

/**
 * 表示 `AbstractGroupingPhaseTestFacade`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractGroupingPhaseTestFacade<InputArtifact, OutputArtifact> : AbstractTestFacadeBase<InputArtifact, OutputArtifact>()
        where InputArtifact : ResultingArtifact<InputArtifact>,
              OutputArtifact : ResultingArtifact<OutputArtifact>
{
    /**
     * 提供 `transform` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    abstract fun transform(inputArtifact: InputArtifact): OutputArtifact?
}
