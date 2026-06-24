package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirQualifierPart
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirOptionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildUserTypeRef
import org.cangnova.cangjie.name.FqName

/**
 * 对齐 Kotlin `FirSpecificTypeResolverTransformer`：
 * 自身不做类型求解，只委托 `session.typeResolver.resolveType(...)`。
 */
class CfirSpecificTypeResolverTransformer(
    /**
     * 当前类型解析所属的 CFIR 会话。
     */
    override val session: CfirSession,
    /**
     * 错误类型是否仍写入 resolved type ref 的 coneType。
     */
    private val errorTypeAsResolved: Boolean = true,
    /**
     * 类型解析时是否解析废弃信息。
     */
    private val resolveDeprecations: Boolean = true,
    /**
     * supertype 解析时使用的超类型供应器。
     */
    private val supertypeSupplier: SupertypeSupplier = SupertypeSupplier.Default,
    /**
     * 是否展开类型别名。
     */
    private val expandTypeAliases: Boolean = true,
) : CfirAbstractTreeTransformer<TypeResolutionConfiguration>(phase = CfirResolvePhase.SUPER_TYPES) {

    /**
     * 当前类型解析是否允许 bare type。
     */
    @set:Suppress("MemberVisibilityCanBePrivate")
    var areBareTypesAllowed: Boolean = false

    /**
     * 在指定作用域内临时切换 bare type 允许状态。
     */
    inline fun <R> withBareTypes(allowed: Boolean = true, block: () -> R): R {
        val oldValue = areBareTypesAllowed
        areBareTypesAllowed = allowed
        return try {
            block()
        } finally {
            areBareTypesAllowed = oldValue
        }
    }

    /**
     * 当前类型引用是否位于 `is` 操作符操作数位置。
     */
    @set:Suppress("MemberVisibilityCanBePrivate")
    var isOperandOfIsOperator: Boolean = false

    /**
     * 在指定作用域内按 `is` 操作符操作数规则解析类型。
     */
    inline fun <R> withIsOperandOfIsOperator(block: () -> R): R {
        val oldValue = isOperandOfIsOperator
        isOperandOfIsOperator = true
        return try {
            block()
        } finally {
            isOperandOfIsOperator = oldValue
        }
    }

    /**
     * 解析普通类型引用并替换为 resolved/error 类型引用。
     */
    override fun transformTypeRef(typeRef: CfirTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        withBareTypes(allowed = false) {
            typeRef.transformChildren(this, data)
        }

        val resolution = session.typeResolver.resolveType(
            typeRef = typeRef,
            configuration = data,
            areBareTypesAllowed = areBareTypesAllowed,
            isOperandOfIsOperator = isOperandOfIsOperator,
            resolveDeprecations = resolveDeprecations,
            supertypeSupplier = supertypeSupplier,
            expandTypeAliases = expandTypeAliases,
        )
        return transformType(typeRef, resolution.type, resolution.diagnostic, data)
    }

    /**
     * 根据类型解析结果构造最终类型引用节点。
     */
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

    /**
     * 构造错误类型引用，并尽量保留部分可解析前缀。
     */
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
            val partiallyResolvedTypeRef = tryCalculatingPartiallyResolvedTypeRef(typeRef, data)
            this.partiallyResolvedTypeRef = partiallyResolvedTypeRef
            this.diagnostic = when {
                diagnostic is ConeUnresolvedTypeQualifierError -> {
                    ConeUnresolvedTypeQualifierError(smallestUnresolvablePrefix(diagnostic.qualifiers, partiallyResolvedTypeRef))
                }
                else -> diagnostic
            }
        }
    }

    /**
     * 返回给定限定名中最小的不可解析前缀。
     *
     * 对齐 Kotlin FIR `FirSpecificTypeResolverTransformer.smallestUnresolvablePrefix`。
     */
    private fun smallestUnresolvablePrefix(
        qualifiers: List<CfirQualifierPart>,
        partiallyResolvedTypeRef: CfirResolvedTypeRef?,
    ): List<CfirQualifierPart> {
        val totalQualifierCount = qualifiers.size
        val resolvedQualifierCount = (partiallyResolvedTypeRef?.delegatedTypeRef as? CfirUserTypeRef)?.qualifier?.size
            ?: calculatePartiallyResolvablePackageSegments(qualifiers)

        val unresolvedQualifierCount = totalQualifierCount - resolvedQualifierCount

        return if (unresolvedQualifierCount > 1) {
            qualifiers.dropLast(unresolvedQualifierCount - 1)
        } else {
            qualifiers
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
                source = qualifiersToTry.last().source
                coneType = resolvedType
                delegatedTypeRef = typeRefToTry
            }
        }

        return null
    }

    /**
     * 计算限定名左侧可解析为包名的段数。
     *
     * 对齐 Kotlin FIR `calculatePartiallyResolvablePackageSegments`。
     */
    private fun calculatePartiallyResolvablePackageSegments(qualifiers: List<CfirQualifierPart>): Int {
        if (qualifiers.size <= 1) {
            return 0
        }

        val packageSegmentsToTry = qualifiers.mapTo(mutableListOf()) { it.name.asString() }

        while (packageSegmentsToTry.size > 1) {
            packageSegmentsToTry.removeLast()
            if (session.symbolProvider.hasPackage(FqName.fromSegments(packageSegmentsToTry))) {
                return packageSegmentsToTry.size
            }
        }

        return 0
    }

    /**
     * 已解析类型引用不再重复解析。
     */
    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return resolvedTypeRef
    }

    /**
     * 错误类型引用只递归修正其中的部分解析类型引用。
     */
    override fun transformErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        errorTypeRef.transformPartiallyResolvedTypeRef(this, data)
        return errorTypeRef
    }

    /**
     * 隐式类型引用由 body resolve 或推断阶段处理，类型解析阶段直接保留。
     */
    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return implicitTypeRef
    }

    /**
     * Option 类型引用按普通类型引用路径解析。
     */
    override fun transformOptionTypeRef(optionTypeRef: CfirOptionTypeRef, data: TypeResolutionConfiguration): CfirTypeRef {
        return transformTypeRef(optionTypeRef, data)
    }
}
