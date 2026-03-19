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

class SpecHelpersSourceFilesProvider(
    testServices: TestServices,
) : AdditionalSourceProvider(testServices) {
    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(AdditionalFilesDirectives)

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

    private fun resolveSpecHelpersRoot(): File? {
        val candidates = listOf(
            File("cfir/analysis-tests/testData/helpers/spec"),
            File("cfir/analysis-tests/testData/spec/helpers"),
            File("tests/spec/helpers"),
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
    }
}

