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
    protected val testServices: TestServices,
    protected val additionalSourceProviders: List<AdditionalSourceProvider>,
    protected val moduleStructureTransformers: List<ModuleStructureTransformer>
) {
    abstract fun splitTestDataByModules(
        testDataFileName: String,
        directivesContainer: DirectivesContainer,
    ): TestModuleStructure

    companion object {
        const val DEFAULT_MODULE_NAME = "main"
        val CINTEROP_SOURCE_EXTENSIONS = setOf("c", "cpp", "m", "mm")
    }
}
