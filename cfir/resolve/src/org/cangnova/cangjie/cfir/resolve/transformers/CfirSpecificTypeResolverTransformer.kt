package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef

/**
 * 对齐 Kotlin `FirSpecificTypeResolverTransformer`：
 * 自身不做类型求解，只委托 `session.typeResolver.resolveType(...)`。
 */
class CfirSpecificTypeResolverTransformer(
    override val session: CfirSession,
    private val errorTypeAsResolved: Boolean = true,
    private val resolveDeprecations: Boolean = true,
    private val supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    private val expandTypeAliases: Boolean = true,
) : CfirAbstractTreeTransformer<TypeResolutionConfiguration>(phase = CfirResolvePhase.SUPER_TYPES) {

    @set:Suppress("MemberVisibilityCanBePrivate")
    var areBareTypesAllowed: Boolean = false

    inline fun <R> withBareTypes(allowed: Boolean = true, block: () -> R): R {
        val oldValue = areBareTypesAllowed
        areBareTypesAllowed = allowed
        return try {
            block()
        } finally {
            areBareTypesAllowed = oldValue
        }
    }

    @set:Suppress("MemberVisibilityCanBePrivate")
    var isOperandOfIsOperator: Boolean = false

    inline fun <R> withIsOperandOfIsOperator(block: () -> R): R {
        val oldValue = isOperandOfIsOperator
        isOperandOfIsOperator = true
        return try {
            block()
        } finally {
            isOperandOfIsOperator = oldValue
        }
    }

    override fun transformTypeRef(typeRef: CfirTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        withBareTypes(allowed = false) {
            typeRef.transformChildren(this, data)
        }

        val result = session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = data,
            areBareTypesAllowed = areBareTypesAllowed,
            isOperandOfIsOperator = isOperandOfIsOperator,
            resolveDeprecations = resolveDeprecations,
            supertypeSupplier = supertypeSupplier,
            expandTypeAliases = expandTypeAliases,
        )
        val resolvedType = result.type
        val errorType = resolvedType as? ConeErrorType

        if (errorType != null && !errorTypeAsResolved) {
            return buildErrorTypeRef {
                source = typeRef.source
                annotations += typeRef.annotations
                coneType = errorType
                delegatedTypeRef = typeRef
                diagnostic = errorType.diagnostic
            }
        }

        if (errorType == null && result.diagnostic == null) {
            // 正常路径：成功解析
            return buildResolvedTypeRef {
                source = typeRef.source
                coneType = resolvedType
                annotations += typeRef.annotations
                delegatedTypeRef = typeRef
            }
        }

        // 错误路径：对齐 K2 — 构建 CfirResolvedTypeRef 并将 ConeErrorType 作为 coneType，
        // 这样下游 CfirErrorNodeDiagnosticCollectorComponent.visitResolvedTypeRef 能通过
        // coneType.diagnostic 提取结构化诊断（如 ConeUnresolvedSymbolError），正确报告 UNRESOLVED_REFERENCE。
        val diagnosticConeType = errorType ?: ConeErrorType(
            result.diagnostic ?: ConeSimpleDiagnostic("Unresolved type")
        )
        return buildResolvedTypeRef {
            source = typeRef.source
            coneType = diagnosticConeType
            annotations += typeRef.annotations
            delegatedTypeRef = typeRef
        }
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return resolvedTypeRef
    }

    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return errorTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return implicitTypeRef
    }
}
