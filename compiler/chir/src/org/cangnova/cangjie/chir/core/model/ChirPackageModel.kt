package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

enum class ChirPackageAccessLevel {
    PRIVATE,
    INTERNAL,
    PUBLIC,
}

data class ChirPackageMembers(
    val globalVariables: List<ChirVariableDeclaration> = emptyList(),
    val globalFunctions: List<ChirFunctionDeclaration> = emptyList(),
    val importedVariables: List<ChirVariableDeclaration> = emptyList(),
    val importedFunctions: List<ChirFunctionDeclaration> = emptyList(),
)

data class ChirModule(
    override val semanticId: ChirSemanticId,
    val name: String,
    val declarations: List<ChirDeclaration>,
) : ChirNode

data class ChirPackage(
    override val semanticId: ChirSemanticId,
    val name: String,
    val modules: List<ChirModule>,
    val members: ChirPackageMembers = ChirPackageMembers(),
    val typeDefinitions: List<ChirTypeDeclaration> = emptyList(),
    val importedTypeDefinitions: List<ChirTypeDeclaration> = emptyList(),
    val packageInitFunctionId: ChirSemanticId? = null,
    val packageLiteralInitFunctionId: ChirSemanticId? = null,
    val accessLevel: ChirPackageAccessLevel = ChirPackageAccessLevel.INTERNAL,
) : ChirNode

val ChirPackage.allDeclarations: List<ChirDeclaration>
    get() = buildList {
        addAll(members.globalVariables)
        addAll(members.globalFunctions)
        addAll(members.importedVariables)
        addAll(members.importedFunctions)
        addAll(typeDefinitions)
        addAll(importedTypeDefinitions)
        modules.forEach { addAll(it.declarations) }
    }
