package org.cangnova.cangjie.analysis.api.projectStructure

interface CaNotUnderContentRootModule : CaModule {
    val name: String

    val originalModule: CaModule?
        get() = null

    override val moduleDescription: String
        get() = "Not-under-content-root module $name"
}
