package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.services.TestService
import java.io.File

/**
 * This enum represents the relation between the module and its dependency (assume that B depends on A)
 * - [RegularDependency] means that B depend on A as a regular library dependency (A is passed to classpath of B);
 * - [FriendDependency] is the same as [RegularDependency], but in addition B can access internal declarations of A (like test-main relation);
 * - [DependsOnDependency] 表示同一层次模块结构中的 dependsOn 关系。
 */
enum class DependencyRelation {
    RegularDependency,
    FriendDependency,
    DependsOnDependency,
}

data class DependencyDescription(
    val dependencyModuleName: String,
    val relation: DependencyRelation = DependencyRelation.RegularDependency,
    val dependencyModule: TestModule? = null,
    val kind: DependencyKind = DependencyKind.Source,
) {
    constructor(
        dependencyModule: TestModule,
        kind: DependencyKind,
        relation: DependencyRelation,
    ) : this(
        dependencyModuleName = dependencyModule.name,
        relation = relation,
        dependencyModule = dependencyModule,
        kind = kind,
    )
}

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
    val allDependencies: List<DependencyDescription> = emptyList(),
    val directives: RegisteredDirectives = RegisteredDirectives.Empty,
    val languageVersionSettings: LanguageVersionSettings? = null,
) {
    // Backward-compatible alias with earlier local model naming.
    val dependencies: List<DependencyDescription>
        get() = allDependencies

    val regularDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.RegularDependency }

    val friendDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.FriendDependency }

    val dependsOnDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.DependsOnDependency }

    override fun equals(other: Any?): Boolean =
        other is TestModule && name == other.name

    override fun hashCode(): Int = name.hashCode()
}

abstract class TestModuleStructure : TestService {
    abstract val modules: List<TestModule>
    abstract val allDirectives: RegisteredDirectives
    abstract val originalTestDataFiles: List<File>
}
