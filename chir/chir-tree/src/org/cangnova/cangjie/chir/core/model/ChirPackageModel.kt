package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * CHIR 包访问级别。
 */
enum class ChirPackageAccessLevel {
    PRIVATE,
    INTERNAL,
    PUBLIC,
}

/**
 * CHIR 包级成员集合。
 */
data class ChirPackageMembers(
    /**
     * 包级全局变量声明列表。
     */
    val globalVariables: List<ChirVariableDeclaration> = emptyList(),

    /**
     * 包级全局函数声明列表。
     */
    val globalFunctions: List<ChirFunctionDeclaration> = emptyList(),

    /**
     * 包导入变量声明列表。
     */
    val importedVariables: List<ChirVariableDeclaration> = emptyList(),

    /**
     * 包导入函数声明列表。
     */
    val importedFunctions: List<ChirFunctionDeclaration> = emptyList(),
)

/**
 * CHIR 模块节点。
 */
data class ChirModule(
    /**
     * 模块语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 模块名称。
     */
    val name: String,

    /**
     * 模块内声明列表。
     */
    val declarations: List<ChirDeclaration>,
) : ChirNode

/**
 * CHIR 包节点。
 */
data class ChirPackage(
    /**
     * 包语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 包名称。
     */
    val name: String,

    /**
     * 包内模块列表。
     */
    val modules: List<ChirModule>,

    /**
     * 包级成员集合。
     */
    val members: ChirPackageMembers = ChirPackageMembers(),

    /**
     * 包内类型定义列表。
     */
    val typeDefinitions: List<ChirTypeDeclaration> = emptyList(),

    /**
     * 包导入类型定义列表。
     */
    val importedTypeDefinitions: List<ChirTypeDeclaration> = emptyList(),

    /**
     * 包初始化函数标识。
     */
    val packageInitFunctionId: ChirSemanticId? = null,

    /**
     * 包字面量初始化函数标识。
     */
    val packageLiteralInitFunctionId: ChirSemanticId? = null,

    /**
     * 包访问级别。
     */
    val accessLevel: ChirPackageAccessLevel = ChirPackageAccessLevel.INTERNAL,
) : ChirNode

/**
 * 包内全部声明的扁平化视图。
 */
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

/**
 * 将声明及其自定义类型成员递归加入当前列表。
 */
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
