package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
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
        addDeclarationsWithMembers(members.globalVariables)
        addDeclarationsWithMembers(members.globalFunctions)
        addDeclarationsWithMembers(members.importedVariables)
        addDeclarationsWithMembers(members.importedFunctions)
        addDeclarationsWithMembers(typeDefinitions)
        addDeclarationsWithMembers(importedTypeDefinitions)
        modules.forEach { module -> addDeclarationsWithMembers(module.declarations) }
    }

private fun MutableList<ChirDeclaration>.addDeclarationsWithMembers(declarations: List<ChirDeclaration>) {
    declarations.forEach { declaration ->
        add(declaration)
        if (declaration is ChirCustomTypeDeclaration) {
            addDeclarationsWithMembers(declaration.effectiveMemberDeclarations())
        }
    }
}

/**
 * 返回自定义类型的完整成员声明视图。
 *
 * `ChirStructDeclaration` 同时暴露 `fieldDeclarations` 与 `memberDeclarations`；这里将两者按
 * semanticId 合并，保证校验、后端扫描和代码生成不会因为构造方只填了其中一个集合而漏掉字段。
 */
fun ChirCustomTypeDeclaration.effectiveMemberDeclarations(): List<ChirDeclaration> {
    if (this !is ChirStructDeclaration) return memberDeclarations
    return (fieldDeclarations + memberDeclarations).distinctBy { it.semanticId }
}
