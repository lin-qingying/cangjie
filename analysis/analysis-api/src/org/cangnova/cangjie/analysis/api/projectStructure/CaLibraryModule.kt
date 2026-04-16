package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem

interface CaLibraryModule : CaModule {
    val libraryName: String

    val binaryRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Library binaries of $libraryName"

    override val stableModuleName: String?
        get() = libraryName
}
