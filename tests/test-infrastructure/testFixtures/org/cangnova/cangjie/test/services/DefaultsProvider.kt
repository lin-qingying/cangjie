package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendKind



class DefaultsProvider(
    val frontendKind: FrontendKind<*>,
    val backendKind: BackendKind<*>,
    private val defaultLanguageSettingsBuilder: LanguageVersionSettingsBuilder,
    val artifactKind: ArtifactKind<*>,
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

    fun newLanguageSettingsBuilder(): LanguageVersionSettingsBuilder {
        return LanguageVersionSettingsBuilder.fromExistingSettings(defaultLanguageSettingsBuilder)
    }
}
val TestServices.defaultsProvider: DefaultsProvider by TestServices.testServiceAccessor()
