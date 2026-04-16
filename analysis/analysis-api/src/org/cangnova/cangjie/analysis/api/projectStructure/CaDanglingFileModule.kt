package org.cangnova.cangjie.analysis.api.projectStructure

interface CaDanglingFileModule : CaSourceModule {
    val contextModule: CaModule?

    override val moduleDescription: String
        get() = "Dangling file module $name"
}
