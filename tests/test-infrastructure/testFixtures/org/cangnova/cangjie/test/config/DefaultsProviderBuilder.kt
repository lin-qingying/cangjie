package org.cangnova.cangjie.test.config

import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.LanguageVersionSettingsBuilder
import org.cangnova.cangjie.test.services.DefaultsProvider
import org.cangnova.cangjie.test.model.ArtifactKind
import org.cangnova.cangjie.test.model.BackendKind
import org.cangnova.cangjie.test.model.DependencyKind
import org.cangnova.cangjie.test.model.FrontendKind

class DefaultsProviderBuilder {
    var targetBackend: TargetBackend? = null
    lateinit var frontend: FrontendKind<*>
    lateinit var dependencyKind: DependencyKind

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
