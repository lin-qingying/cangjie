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

/**
 * 表示 `DependencyDescription`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
data class DependencyDescription(
    /**
     * 保存 `dependencyModuleName`，供测试模型在测试执行期间读取或传递。
     */
    val dependencyModuleName: String,
    /**
     * 保存 `relation`，供测试模型在测试执行期间读取或传递。
     */
    val relation: DependencyRelation = DependencyRelation.RegularDependency,
    /**
     * 保存 `dependencyModule`，供测试模型在测试执行期间读取或传递。
     */
    val dependencyModule: TestModule? = null,
    /**
     * 保存 `kind`，供测试模型在测试执行期间读取或传递。
     */
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

/**
 * 表示 `TestFile`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
class TestFile(
    /**
     * 保存 `relativePath`，供测试模型在测试执行期间读取或传递。
     */
    val relativePath: String,
    /**
     * 保存 `originalContent`，供测试模型在测试执行期间读取或传递。
     */
    val originalContent: String,
    /**
     * 保存 `originalFile`，供测试模型在测试执行期间读取或传递。
     */
    val originalFile: File,
    /**
     * 保存 `startLineNumberInOriginalFile`，供测试模型在测试执行期间读取或传递。
     */
    val startLineNumberInOriginalFile: Int, // line count starts with 0
    /*
     * isAdditional means that this file provided as addition to sources of testdata
     *   and there is no need to apply any handlers or preprocessors over it
     */
    val isAdditional: Boolean,
    /**
     * 保存 `directives`，供测试模型在测试执行期间读取或传递。
     */
    val directives: RegisteredDirectives
) {
    /**
     * 保存 `name`，供测试模型在测试执行期间读取或传递。
     */
    val name: String = relativePath.split("/").last()

    /**
     * 执行 `toString` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun toString(): String = relativePath

    /**
     * 执行 `copy` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    fun copy(): TestFile = TestFile(
        relativePath,
        originalContent,
        originalFile,
        startLineNumberInOriginalFile,
        isAdditional,
        directives
    )
}

/**
 * 表示 `TestModule`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
data class TestModule(
    /**
     * 保存 `name`，供测试模型在测试执行期间读取或传递。
     */
    val name: String,
    /**
     * 保存 `files`，供测试模型在测试执行期间读取或传递。
     */
    val files: List<TestFile>,
    /**
     * 保存 `allDependencies`，供测试模型在测试执行期间读取或传递。
     */
    val allDependencies: List<DependencyDescription> = emptyList(),
    /**
     * 保存 `directives`，供测试模型在测试执行期间读取或传递。
     */
    val directives: RegisteredDirectives = RegisteredDirectives.Empty,
    /**
     * 保存 `languageVersionSettings`，供测试模型在测试执行期间读取或传递。
     */
    val languageVersionSettings: LanguageVersionSettings? = null,
) {
    // Backward-compatible alias with earlier local model naming.
    /**
     * 保存 `dependencies`，供测试模型在测试执行期间读取或传递。
     */
    val dependencies: List<DependencyDescription>
        get() = allDependencies

    /**
     * 保存 `regularDependencies`，供测试模型在测试执行期间读取或传递。
     */
    val regularDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.RegularDependency }

    /**
     * 保存 `friendDependencies`，供测试模型在测试执行期间读取或传递。
     */
    val friendDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.FriendDependency }

    /**
     * 保存 `dependsOnDependencies`，供测试模型在测试执行期间读取或传递。
     */
    val dependsOnDependencies: List<DependencyDescription>
        get() = allDependencies.filter { it.relation == DependencyRelation.DependsOnDependency }

    /**
     * 执行 `equals` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun equals(other: Any?): Boolean =
        other is TestModule && name == other.name

    /**
     * 执行 `hashCode` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun hashCode(): Int = name.hashCode()
}

/**
 * 表示 `TestModuleStructure`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
abstract class TestModuleStructure : TestService {
    /**
     * 保存 `modules`，供测试模型在测试执行期间读取或传递。
     */
    abstract val modules: List<TestModule>
    /**
     * 保存 `allDirectives`，供测试模型在测试执行期间读取或传递。
     */
    abstract val allDirectives: RegisteredDirectives
    /**
     * 保存 `originalTestDataFiles`，供测试模型在测试执行期间读取或传递。
     */
    abstract val originalTestDataFiles: List<File>
}
