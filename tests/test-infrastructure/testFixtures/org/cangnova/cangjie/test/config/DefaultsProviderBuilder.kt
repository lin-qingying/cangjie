package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.services.DependencyKind
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.FrontendKind

class DefaultsProviderBuilder {
    var targetBackend: TargetBackend? = null

    fun build(): DefaultsProvider {
        return DefaultsProvider(
            frontendKind = FrontendKind.NoFrontend,
            backendKind = BackendKind.NoBackend,
            defaultLanguageSettingsBuilder = LanguageVersionSettingsBuilder(),
            artifactKind = ArtifactKind.NoArtifact,
            defaultDependencyKind = DependencyKind.Regular,
        )
    }
}
