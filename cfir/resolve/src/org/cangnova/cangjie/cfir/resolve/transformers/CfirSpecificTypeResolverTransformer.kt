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
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef

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

        val (resolvedType, diagnostic) = session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = data,
            areBareTypesAllowed = areBareTypesAllowed,
            isOperandOfIsOperator = isOperandOfIsOperator,
            resolveDeprecations = resolveDeprecations,
            supertypeSupplier = supertypeSupplier,
            expandTypeAliases = expandTypeAliases,
        )
        return transformType(typeRef, resolvedType, diagnostic, data)
    }

    private fun transformType(
        typeRef: CfirTypeRef,
        resolvedType: ConeCangJieType,
        diagnostic: ConeDiagnostic?,
        data: TypeResolutionConfiguration,
    ): CfirResolvedTypeRef {
        return when {
            resolvedType is ConeErrorType -> {
                buildErrorType(typeRef, resolvedType, resolvedType.diagnostic, data)
            }

            diagnostic != null -> {
                buildErrorType(typeRef, resolvedType, diagnostic, data)
            }

            else -> {
                // 正常路径：成功解析
                buildResolvedTypeRef {
                    source = typeRef.source
                    coneType = resolvedType
                    annotations += typeRef.annotations
                    delegatedTypeRef = typeRef
                }
            }
        }
    }

    private fun buildErrorType(
        typeRef: CfirTypeRef,
        resolvedType: ConeCangJieType,
        diagnostic: ConeDiagnostic,
        data: TypeResolutionConfiguration,
    ): CfirErrorTypeRef {
        return buildErrorTypeRef {
            source = typeRef.source
            if (errorTypeAsResolved || resolvedType !is ConeErrorType) {
                coneType = resolvedType
            }
            annotations += typeRef.annotations
            delegatedTypeRef = typeRef
            partiallyResolvedTypeRef = tryCalculatingPartiallyResolvedTypeRef(typeRef, data)
            this.diagnostic = diagnostic
        }
    }

    /**
     * 对齐 Kotlin FIR：当用户类型部分可解析时，保留前缀的已解析结果，
     * 供错误类型引用承载 IDE/诊断所需的部分语义信息。
     */
    private fun tryCalculatingPartiallyResolvedTypeRef(
        typeRef: CfirTypeRef,
        data: TypeResolutionConfiguration,
    ): CfirResolvedTypeRef? {
        if (typeRef !is CfirUserTypeRef) return null
        if (typeRef.qualifier.size <= 1) return null

        val qualifiersToTry = typeRef.qualifier.toMutableList()
        while (qualifiersToTry.size > 1) {
            qualifiersToTry.removeLast()

            val typeRefToTry = buildUserTypeRef {
                source = typeRef.source
                annotations += typeRef.annotations
                qualifier += qualifiersToTry
            }

            val (resolvedType, diagnostic) = withBareTypes {
                session.typeResolver.resolveType(
                    typeRef = typeRefToTry,
                    configuration = data,
                    areBareTypesAllowed = areBareTypesAllowed,
                    isOperandOfIsOperator = isOperandOfIsOperator,
                    resolveDeprecations = resolveDeprecations,
                    supertypeSupplier = supertypeSupplier,
                    expandTypeAliases = expandTypeAliases,
                )
            }

            if (resolvedType is ConeErrorType || diagnostic != null) continue

            return buildResolvedTypeRef {
                source = typeRefToTry.source
                coneType = resolvedType
                delegatedTypeRef = typeRefToTry
            }
        }

        return null
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return resolvedTypeRef
    }

    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        errorTypeRef.transformPartiallyResolvedTypeRef(this, data)
        return errorTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return implicitTypeRef
    }
}
