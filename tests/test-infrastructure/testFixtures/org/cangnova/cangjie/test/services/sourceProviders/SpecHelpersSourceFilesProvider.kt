package org.cangnova.cangjie.test.services.sourceProviders

import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives
import org.cangnova.cangjie.test.directives.AdditionalFilesDirectives.SPEC_HELPERS
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.AdditionalSourceProvider
import org.cangnova.cangjie.test.services.TestServices
import java.io.File

/**
 * 表示 `SpecHelpersSourceFilesProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class SpecHelpersSourceFilesProvider(
    testServices: TestServices,
) : AdditionalSourceProvider(testServices) {
    /**
     * 保存 `directiveContainers`，供测试服务在测试执行期间读取或传递。
     */
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(AdditionalFilesDirectives)

    /**
     * 执行 `produceAdditionalFiles` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    override fun produceAdditionalFiles(
        globalDirectives: RegisteredDirectives,
        module: TestModule,
        testModuleStructure: TestModuleStructure,
    ): List<TestFile> {
        if (!containsDirective(globalDirectives, module, SPEC_HELPERS)) return emptyList()

        val helperRoot = resolveSpecHelpersRoot() ?: return emptyList()
        val prefix = "specHelpers"
        return helperRoot.walkTopDown()
            .filter { it.isFile && it.extension == "cj" }
            .sortedBy { it.path }
            .map { file ->
                val relativeParent = helperRoot.toPath()
                    .relativize(file.parentFile.toPath())
                    .toString()
                    .replace('\\', '/')
                    .takeIf { it.isNotEmpty() }
                val relativePath = if (relativeParent == null) prefix else "$prefix/$relativeParent"
                file.toTestFile(relativePath)
            }
            .toList()
    }

    /**
     * 提供 `resolveSpecHelpersRoot` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun resolveSpecHelpersRoot(): File? {
        val candidates = listOf(
            File("cfir/analysis-tests/testData/helpers/spec"),
            File("cfir/analysis-tests/testData/spec/helpers"),
            File("tests/spec/helpers"),
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
    }
}
