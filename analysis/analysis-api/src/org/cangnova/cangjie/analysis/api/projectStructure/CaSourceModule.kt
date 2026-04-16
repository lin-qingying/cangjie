package org.cangnova.cangjie.analysis.api.projectStructure

import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.LanguageVersionSettings

interface CaSourceModule : CaModule {
    val name: String

    val languageVersionSettings: LanguageVersionSettings

    val psiRoots: List<PsiFileSystemItem>
        get() = emptyList()

    override val moduleDescription: String
        get() = "Sources of $name"

    override val stableModuleName: String?
        get() = name
}
