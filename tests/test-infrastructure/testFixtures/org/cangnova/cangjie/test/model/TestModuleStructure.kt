package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.services.TestService
import java.io.File

enum class DependencyRelation {
    Regular,
}

data class DependencyDescription(
    val dependencyModuleName: String,
    val relation: DependencyRelation = DependencyRelation.Regular,
)

class TestFile(
    val relativePath: String,
    val originalContent: String,
    val originalFile: File,
    val startLineNumberInOriginalFile: Int, // line count starts with 0
    /*
     * isAdditional means that this file provided as addition to sources of testdata
     *   and there is no need to apply any handlers or preprocessors over it
     */
    val isAdditional: Boolean,
    val directives: RegisteredDirectives
) {
    val name: String = relativePath.split("/").last()

    override fun toString(): String = relativePath

    fun copy(): TestFile = TestFile(
        relativePath,
        originalContent,
        originalFile,
        startLineNumberInOriginalFile,
        isAdditional,
        directives
    )
}
data class TestModule(
    val name: String,
    val files: List<TestFile>,
    val dependencies: List<DependencyDescription> = emptyList(),
    val directives: RegisteredDirectives = RegisteredDirectives.Empty,
)

abstract class TestModuleStructure : TestService {
    abstract val modules: List<TestModule>
    abstract val allDirectives: RegisteredDirectives
    abstract val originalTestDataFiles: List<File>
}

