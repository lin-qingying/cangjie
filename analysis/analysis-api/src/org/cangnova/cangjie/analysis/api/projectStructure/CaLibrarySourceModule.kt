package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem

interface CaLibrarySourceModule : CaModule {
    val libraryName: String

    val binaryLibraryModule: CaLibraryModule

    val sourceRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Library sources of $libraryName"

    override val stableModuleName: String?
        get() = libraryName
}
