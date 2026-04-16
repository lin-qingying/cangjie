package org.cangnova.cangjie.analysis.api.projectStructure

interface CaLibraryFallbackDependenciesModule : CaModule {
    val dependencyOwnerName: String

    override val moduleDescription: String
        get() = "Fallback dependencies of $dependencyOwnerName"
}
