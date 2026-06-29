package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendKind



/**
 * 表示 `DefaultsProvider`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class DefaultsProvider(
    /**
     * 保存 `frontendKind`，供测试服务在测试执行期间读取或传递。
     */
    val frontendKind: FrontendKind<*>,
    /**
     * 保存 `backendKind`，供测试服务在测试执行期间读取或传递。
     */
    val backendKind: BackendKind<*>,
    /**
     * 保存 `defaultLanguageSettingsBuilder`，供测试服务在测试执行期间读取或传递。
     */
    private val defaultLanguageSettingsBuilder: LanguageVersionSettingsBuilder,
    /**
     * 保存 `artifactKind`，供测试服务在测试执行期间读取或传递。
     */
    val artifactKind: ArtifactKind<*>,
    /**
     * 保存 `defaultDependencyKind`，供测试服务在测试执行期间读取或传递。
     */
    val defaultDependencyKind: DependencyKind
) : TestService {
    constructor(
        frontendKind: FrontendKind<*>,
        backendKind: BackendKind<*>,
        defaultLanguageSettingsBuilder: LanguageVersionSettingsBuilder,
        @Suppress("UNUSED_PARAMETER") targetPlatform: Any?,
        artifactKind: ArtifactKind<*>,
        @Suppress("UNUSED_PARAMETER") targetBackend: Any?,
        defaultDependencyKind: DependencyKind,
    ) : this(
        frontendKind = frontendKind,
        backendKind = backendKind,
        defaultLanguageSettingsBuilder = defaultLanguageSettingsBuilder,
        artifactKind = artifactKind,
        defaultDependencyKind = defaultDependencyKind,
    )

    /**
     * 执行 `newLanguageSettingsBuilder` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun newLanguageSettingsBuilder(): LanguageVersionSettingsBuilder {
        return LanguageVersionSettingsBuilder.fromExistingSettings(defaultLanguageSettingsBuilder)
    }
}
/**
 * 保存 `TestServices.defaultsProvider`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.defaultsProvider: DefaultsProvider by TestServices.testServiceAccessor()
