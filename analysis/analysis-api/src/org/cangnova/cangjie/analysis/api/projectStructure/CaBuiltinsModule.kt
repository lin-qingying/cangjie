package org.cangnova.cangjie.analysis.api.projectStructure

interface CaBuiltinsModule : CaModule {
    val builtinsName: String
        get() = "<builtins>"

    override val moduleDescription: String
        get() = "Builtins module $builtinsName"

    override val stableModuleName: String?
        get() = builtinsName
}
