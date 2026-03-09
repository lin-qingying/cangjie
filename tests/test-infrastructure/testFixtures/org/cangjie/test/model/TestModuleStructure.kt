package org.cangjie.test.model

import org.cangjie.test.directives.model.RegisteredDirectives
import org.cangjie.test.services.TestService
import java.io.File

enum class DependencyRelation {
    Regular,
}

data class DependencyDescription(
    val dependencyModuleName: String,
    val relation: DependencyRelation = DependencyRelation.Regular,
)

data class TestFile(
    val name: String,
    val content: String,
    val originalFile: File? = null,
)

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

