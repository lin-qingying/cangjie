package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendKind

/**
 * 表示 `DefaultsProviderBuilder`，承载测试配置中的配置数据、测试产物或处理步骤。
 */
class DefaultsProviderBuilder {
    /**
     * 维护 `targetBackend`，供测试配置在测试执行期间读取或传递。
     */
    var targetBackend: TargetBackend? = null
    /**
     * 保存 `frontend`，供测试配置在测试执行期间读取或传递。
     */
    lateinit var frontend: FrontendKind<*>
    /**
     * 保存 `dependencyKind`，供测试配置在测试执行期间读取或传递。
     */
    lateinit var dependencyKind: DependencyKind

    /**
     * 执行 `build` 对应的测试配置流程，维持测试框架的阶段契约。
     */
    fun build(): DefaultsProvider {
        return DefaultsProvider(

            frontendKind = frontend,
            backendKind = BackendKind.NoBackend,
            defaultLanguageSettingsBuilder = LanguageVersionSettingsBuilder(),
            artifactKind = ArtifactKind.NoArtifact,
            defaultDependencyKind = dependencyKind,


            )
    }
}
