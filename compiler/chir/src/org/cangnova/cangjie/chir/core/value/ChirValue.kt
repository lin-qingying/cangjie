package org.cangnova.cangjie.chir.core.value

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef

enum class ChirValueKind {
    LITERAL,
    GLOBAL,
    PARAMETER,
    IMPORTED_FUNCTION,
    IMPORTED_VARIABLE,
    LOCAL,
    FUNCTION,
    BLOCK,
    BLOCK_GROUP,
}

sealed interface ChirValue : ChirNode {
    val kind: ChirValueKind
    val type: ChirTypeRef
    val displayName: String?
    val attributes: Set<ChirAttribute>
    val annotations: List<String>
    val userIds: Set<ChirSemanticId>
}

data class ChirLocalValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.LOCAL
    override val displayName: String = name
}

data class ChirConstantValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val literal: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.LITERAL
    override val displayName: String = literal
}

data class ChirParameterValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    val ownerFunctionId: ChirSemanticId,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.PARAMETER
    override val displayName: String = name
}

data class ChirGlobalValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.GLOBAL
    override val displayName: String = name
}

data class ChirImportedFunctionValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.IMPORTED_FUNCTION
    override val displayName: String = name
}

data class ChirImportedVariableValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.IMPORTED_VARIABLE
    override val displayName: String = name
}

data class ChirFunctionValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.FUNCTION
    override val displayName: String = name
}

data class ChirBlockValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    val ownerFunctionId: ChirSemanticId,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.BLOCK
    override val displayName: String = name
}

data class ChirBlockGroupValue(
    override val semanticId: ChirSemanticId,
    override val type: ChirTypeRef,
    val name: String,
    override val attributes: Set<ChirAttribute> = emptySet(),
    override val annotations: List<String> = emptyList(),
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    override val kind: ChirValueKind = ChirValueKind.BLOCK_GROUP
    override val displayName: String = name
}
