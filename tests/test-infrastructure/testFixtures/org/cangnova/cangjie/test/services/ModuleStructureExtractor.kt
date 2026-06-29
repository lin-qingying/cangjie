package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.model.TestModuleStructure

/**
 * 模块结构提取器
 *
 * 对应 Kotlin K2 的 ModuleStructureExtractor
 */
abstract class ModuleStructureExtractor @OptIn(TestInfrastructureInternals::class) constructor(
    /**
     * 保存 `testServices`，供测试服务在测试执行期间读取或传递。
     */
    protected val testServices: TestServices,
    /**
     * 保存 `additionalSourceProviders`，供测试服务在测试执行期间读取或传递。
     */
    protected val additionalSourceProviders: List<AdditionalSourceProvider>,
    /**
     * 保存 `moduleStructureTransformers`，供测试服务在测试执行期间读取或传递。
     */
    protected val moduleStructureTransformers: List<ModuleStructureTransformer>
) {
    /**
     * 提供 `splitTestDataByModules` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
        val CINTEROP_SOURCE_EXTENSIONS = setOf("c", "cpp", "m", "mm")
    }
}
