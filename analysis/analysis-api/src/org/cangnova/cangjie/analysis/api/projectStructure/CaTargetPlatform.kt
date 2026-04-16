package org.cangnova.cangjie.analysis.api.projectStructure

data class CaTargetPlatform(
    val platformId: String,
) {
    companion object {
        val DEFAULT = CaTargetPlatform("default")
        val IDE = CaTargetPlatform("ide")
        val STANDALONE = CaTargetPlatform("standalone")
        val LSP = CaTargetPlatform("lsp")
    }
}
