package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.resolve.calls.CommonSuperTypeCalculator
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.supertypes

fun CfirResolvedTypeRef.withReplacedSourceAndType(newSource: CjSourceElement?, newType: ConeCangJieType): CfirResolvedTypeRef {
    val originalPartiallyResolvedTypeRef = (this as? CfirErrorTypeRef)
        ?.partiallyResolvedTypeRef
        ?.let { typeRef ->
            if (newSource != null) {
                typeRef.copyWithNewSource(newSource)
            } else {
                typeRef
            }
        }

    return when {
        newType is ConeErrorType -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = newType.diagnostic
                partiallyResolvedTypeRef = originalPartiallyResolvedTypeRef
            }
        }
        this is CfirErrorTypeRef -> {
            buildErrorTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                diagnostic = this@withReplacedSourceAndType.diagnostic
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
                partiallyResolvedTypeRef = originalPartiallyResolvedTypeRef
            }
        }
        else -> {
            buildResolvedTypeRef {
                source = newSource
                coneType = newType
                annotations += this@withReplacedSourceAndType.annotations
                delegatedTypeRef = this@withReplacedSourceAndType.delegatedTypeRef
            }
        }
    }
}

fun ConeTypeParameterType.collectUpperBounds(typeContext: ConeTypeContext): Set<ConeCangJieType> {
    val upperBounds = linkedSetOf<ConeCangJieType>()
    val seen = linkedSetOf<ConeCangJieType>()

    fun collect(type: ConeCangJieType) {
        if (!seen.add(type)) return

        when (type) {
            is ConeErrorType -> return
            is ConeTypeParameterType -> {
                type.lookupTag.collectUpperBoundsTo(::collect)
            }
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return
                originalTypeParameter.collectUpperBoundsTo(::collect)
            }
            is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
            else -> upperBounds += type
        }
    }

    collect(this)
    return upperBounds
}

fun ConeCangJieType.hasSupertypeWithGivenClassId(classId: org.cangnova.cangjie.name.ClassId, typeContext: ConeTypeContext): Boolean {
    val seen = linkedSetOf<ConeCangJieType>()

    fun visit(type: ConeCangJieType): Boolean {
        if (!seen.add(type)) return false
        if (type.classIdOrPrimitiveClassId == classId) return true

        return when (type) {
            is ConeErrorType -> false
            is ConeTypeParameterType -> type.collectUpperBounds(typeContext).any(::visit)
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return false
                originalTypeParameter.collectUpperBounds().any(::visit)
            }
            is ConeIntersectionType -> type.intersectedTypes.any(::visit)
            else -> {
                val constructor = (type as? ConeRigidType)?.getConstructor() ?: return false
                with(typeContext) {
                    constructor.supertypes().filterIsInstance<ConeCangJieType>().any(::visit)
                }
            }
        }
    }

    return visit(this)
}

private fun ConeTypeParameterLookupTag.collectUpperBounds(): List<ConeCangJieType> {
    typeParameterSymbol.lazyResolveToPhase(CfirResolvePhase.TYPES)
    return typeParameterSymbol.resolvedBounds.map { it.coneType }
}

private inline fun ConeTypeParameterLookupTag.collectUpperBoundsTo(collect: (ConeCangJieType) -> Unit) {
    collectUpperBounds().forEach(collect)
}

fun ConeInferenceContext.commonSuperTypeOrNull(types: List<ConeCangJieType>): ConeCangJieType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> with(CommonSuperTypeCalculator) {
            commonSuperType(types).asCone()
        }
    }
}