package org.cangnova.cangjie.analysis.api.lightDeclarations

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

interface CaLightDeclarationProvider {
    fun getLightDeclaration(symbol: CaSymbol): CaLightDeclaration?

    fun getLightDeclarations(file: CjFile, useSiteModule: CaModule? = null): List<CaLightDeclaration>

    fun getLightDeclarations(module: CaModule): List<CaLightDeclaration>

    fun getPackageLightDeclaration(packageFqName: FqName, useSiteModule: CaModule): CaLightDeclaration?

    fun findLightDeclarations(packageFqName: FqName, name: Name, useSiteModule: CaModule): List<CaLightDeclaration>

    companion object {
        fun getInstance(project: Project): CaLightDeclarationProvider = project.service()
    }
}
