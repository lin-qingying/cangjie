package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirNominalTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException

/**
 * LLVM aggregate field resolved from a CHIR member name and its declared layout.
 */
internal data class LlvmAggregateMember(
    val index: Int,
    val type: ChirTypeRef,
)

/**
 * Returns the declaration order used by LLVM aggregate layout and member-path lowering.
 */
internal fun llvmAggregateFieldDeclarations(
    declaration: ChirCustomTypeDeclaration,
): List<ChirVariableDeclaration> = when (declaration) {
    is ChirStructDeclaration -> declaration.fieldDeclarations
    is ChirClassDeclaration -> declaration.memberDeclarations.filterIsInstance<ChirVariableDeclaration>()
    else -> throw IllegalArgumentException(
        "${declaration.definitionKind} declarations do not have an LLVM aggregate field layout",
    )
}

/**
 * Resolves one named member against the concrete CHIR aggregate type and its declaration.
 */
internal fun resolveLlvmAggregateMember(
    aggregateType: ChirTypeRef,
    memberName: String,
    declarationsById: Map<ChirTypeDeclarationId, ChirNominalTypeDeclaration>,
    semanticId: ChirSemanticId,
): LlvmAggregateMember {
    val resolvedType = (aggregateType as? ChirResolvedTypeRef)?.type
        ?: throw CodegenLoweringException(
            "cannot resolve member '$memberName' on unresolved type ${aggregateType.renderName}",
            semanticId,
        )
    val (declarationId, fieldTypes) = when (resolvedType) {
        is ChirStructType -> resolvedType.declarationId to resolvedType.fieldTypes
        is ChirClassType -> resolvedType.declarationId to resolvedType.fieldTypes
        else -> throw CodegenLoweringException(
            "member '$memberName' requires a class or struct aggregate, got ${aggregateType.renderName}",
            semanticId,
        )
    }
    val declaration = declarationsById[declarationId]
        ?: throw CodegenLoweringException(
            "member '$memberName' has no CHIR declaration for ${declarationId.qualifiedName}",
            semanticId,
        )
    val fields = llvmAggregateFieldDeclarations(declaration)
    if (fields.size != fieldTypes.size) {
        throw CodegenLoweringException(
            "CHIR type ${declarationId.qualifiedName} has ${fieldTypes.size} instantiated field types " +
                "but its declaration has ${fields.size} LLVM fields",
            semanticId,
        )
    }
    val matchingIndices = fields.indices.filter { fields[it].name == memberName }
    if (matchingIndices.size != 1) {
        val reason = if (matchingIndices.isEmpty()) "does not declare" else "declares more than one"
        throw CodegenLoweringException(
            "CHIR type ${declarationId.qualifiedName} $reason member '$memberName'",
            semanticId,
        )
    }
    val index = matchingIndices.single()
    return LlvmAggregateMember(index, fieldTypes[index])
}

/**
 * Extracts a directly addressable aggregate type behind a CHIR reference value.
 */
internal fun referencedAggregateType(
    referenceType: ChirTypeRef,
    semanticId: ChirSemanticId,
): ChirTypeRef = ((referenceType as? ChirResolvedTypeRef)?.type as? ChirRefType)?.referencedType
    ?: throw CodegenLoweringException(
        "named member access requires a resolved reference address, got ${referenceType.renderName}",
        semanticId,
    )
