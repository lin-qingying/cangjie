package org.cangnova.cangjie.chir.core.declaration

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef

enum class ChirTypeDefinitionKind {
    CLASS,
    STRUCT,
    ENUM,
    EXTEND,
    GENERIC,
}

sealed interface ChirDeclaration : ChirNode {
    val name: String
    val attributes: Set<ChirAttribute>
}

interface ChirTypeDeclaration : ChirDeclaration {
    val typeParameters: List<String>
}

interface ChirCustomTypeDeclaration : ChirTypeDeclaration {
    val definitionKind: ChirTypeDefinitionKind
    val memberDeclarations: List<ChirDeclaration>
}

interface ChirVariableDeclaration : ChirDeclaration {
    val type: ChirTypeRef
    val mutable: Boolean
}

interface ChirPropertyDeclaration : ChirVariableDeclaration

interface ChirFunctionDeclaration : ChirDeclaration {
    val returnType: ChirTypeRef
    val parameters: List<ChirVariableDeclaration>
    val blocks: List<ChirBlock>
    val entryBlockId: ChirSemanticId
}

interface ChirClassDeclaration : ChirCustomTypeDeclaration {
    val superTypes: List<ChirTypeRef>
    val implementedTypes: List<ChirTypeRef>
}

interface ChirStructDeclaration : ChirCustomTypeDeclaration {
    val fieldDeclarations: List<ChirVariableDeclaration>
}

interface ChirEnumDeclaration : ChirCustomTypeDeclaration {
    val cases: List<String>
}

interface ChirExtendDeclaration : ChirCustomTypeDeclaration {
    val targetType: ChirTypeRef
    val extendedTypes: List<ChirTypeRef>
}

data class DefaultChirTypeDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val typeParameters: List<String> = emptyList(),
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirTypeDeclaration

data class DefaultChirClassDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val typeParameters: List<String> = emptyList(),
    override val superTypes: List<ChirTypeRef> = emptyList(),
    override val implementedTypes: List<ChirTypeRef> = emptyList(),
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirClassDeclaration {
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.CLASS
}

data class DefaultChirStructDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val typeParameters: List<String> = emptyList(),
    override val fieldDeclarations: List<ChirVariableDeclaration> = emptyList(),
    override val memberDeclarations: List<ChirDeclaration> = fieldDeclarations,
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirStructDeclaration {
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.STRUCT
}

data class DefaultChirEnumDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val typeParameters: List<String> = emptyList(),
    override val cases: List<String> = emptyList(),
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirEnumDeclaration {
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.ENUM
}

data class DefaultChirExtendDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val targetType: ChirTypeRef,
    override val extendedTypes: List<ChirTypeRef> = emptyList(),
    override val typeParameters: List<String> = emptyList(),
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirExtendDeclaration {
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.EXTEND
}

data class DefaultChirVariableDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val type: ChirTypeRef,
    override val mutable: Boolean,
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirVariableDeclaration

data class DefaultChirPropertyDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val type: ChirTypeRef,
    override val mutable: Boolean,
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirPropertyDeclaration

data class DefaultChirFunctionDeclaration(
    override val semanticId: ChirSemanticId,
    override val name: String,
    override val returnType: ChirTypeRef,
    override val parameters: List<ChirVariableDeclaration>,
    override val blocks: List<ChirBlock>,
    override val entryBlockId: ChirSemanticId,
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirFunctionDeclaration
