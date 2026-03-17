package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirFunctionTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTupleTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.CfirVArrayTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.typeResolver
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 鏄惧紡绫诲瀷寮曠敤瑙ｆ瀽鍣ㄣ€? *
 * 瀵归綈 Kotlin: `org.jetbrains.kotlin.fir.resolve.transformers.FirSpecificTypeResolverTransformer`
 * 鎵€濮旀墭鐨勬樉寮忕被鍨嬭В鏋愯亴璐ｃ€? */
class CfirExplicitTypeRefResolver(
    private val session: CfirSession,
) : CfirSessionComponent {
    /** 瑙ｆ瀽鏄惧紡绫诲瀷寮曠敤骞惰繑鍥炶В鏋愬悗/鍥為€€鍚庣殑绫诲瀷寮曠敤銆?*/
    fun resolveExplicitTypeRef(
        typeRef: CfirTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): CfirTypeRef = when (typeRef) {
        is CfirBasicTypeRef -> resolveBasicTypeRef(typeRef)
        is CfirUserTypeRef -> resolveUserTypeRef(typeRef, scopeTypeParameters)
        is CfirFunctionTypeRef -> {
            val parameterTypes = typeRef.parameterTypeRefs.map { resolveConeType(it, scopeTypeParameters) }
            val returnType = resolveConeType(typeRef.returnTypeRef, scopeTypeParameters)
            buildResolvedTypeRef {
                source = typeRef.source
                coneType = ConeFuncType(parameterTypes = parameterTypes, returnType = returnType)
                delegatedTypeRef = typeRef
            }
        }
        is CfirTupleTypeRef -> {
            val elementTypes = typeRef.elementTypeRefs.map { resolveConeType(it, scopeTypeParameters) }
            buildResolvedTypeRef {
                source = typeRef.source
                coneType = ConeTupleType(elementTypes = elementTypes)
                delegatedTypeRef = typeRef
            }
        }
        is CfirVArrayTypeRef -> {
            val elementType = resolveConeType(typeRef.elementTypeRef, scopeTypeParameters)
            val size = typeRef.sizeLiteral.toLongOrNull()
            buildResolvedTypeRef {
                source = typeRef.source
                coneType = if (size != null) {
                    ConeVArrayType(elementType = elementType, size = size)
                } else {
                    ConeErrorType("Invalid VArray size: ${typeRef.sizeLiteral}")
                }
                delegatedTypeRef = typeRef
            }
        }
        else -> typeRef
    }

    /** 瑙ｆ瀽鍩虹绫诲瀷寮曠敤銆?*/
    private fun resolveBasicTypeRef(basicTypeRef: CfirBasicTypeRef): CfirTypeRef {
        val name = basicTypeRef.name.asString()
        val primitiveType = session.builtinTypes.getPrimitiveTypeByName(name)
        return if (primitiveType != null) {
            buildResolvedTypeRef {
                source = basicTypeRef.source
                coneType = primitiveType
                delegatedTypeRef = basicTypeRef
            }
        } else {
            buildErrorTypeRef {
                source = basicTypeRef.source
                reason = "Unknown basic type: $name"
            }
        }
    }

    /** 瑙ｆ瀽鐢ㄦ埛绫诲瀷寮曠敤锛堝惈绫诲瀷鍙傛暟鍚嶄笌绫诲悕涓ょ被璺緞锛夈€?*/
    private fun resolveUserTypeRef(
        userTypeRef: CfirUserTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): CfirTypeRef {
        if (userTypeRef.qualifier.isEmpty()) return userTypeRef

        if (userTypeRef.qualifier.size == 1) {
            val typeParameterName = userTypeRef.qualifier.single().asString()
            if (scopeTypeParameters.containsKey(typeParameterName)) {
                return buildResolvedTypeRef {
                    source = userTypeRef.source
                    coneType = ConeTypeParameterType(
                        lookupTag = ConeTypeParameterLookupTag(typeParameterName),
                    )
                    delegatedTypeRef = userTypeRef
                }
            }
        }

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        val classId = ClassId(packageFqName, className)
        val resolvedClass = session.typeResolver.resolveClass(classId)

        return if (resolvedClass != null) {
            val resolvedArguments = userTypeRef.typeArguments.map { argument ->
                resolveConeType(argument, scopeTypeParameters)
            }
            buildResolvedTypeRef {
                source = userTypeRef.source
                coneType = ConeClassLikeType(
                    lookupTag = ConeClassLookupTagImpl(classId),
                    typeArguments = resolvedArguments,
                    isInterface = resolvedClass.classKind == CfirClassKind.INTERFACE,
                )
                delegatedTypeRef = userTypeRef
            }
        } else {
            buildResolvedTypeRef {
                source = userTypeRef.source
                coneType = ConeErrorType(ConeUnresolvedSymbolError(classId))
                delegatedTypeRef = userTypeRef
            }
        }
    }

    /** 灏嗕换鎰忕被鍨嬪紩鐢ㄨ绾︿负 `ConeCangjieType`銆?*/
    private fun resolveConeType(
        typeRef: CfirTypeRef,
        scopeTypeParameters: Map<String, CfirTypeParameter>,
    ): ConeCangjieType {
        val errorTypeRef = typeRef as? CfirErrorTypeRef
        if (errorTypeRef != null) return ConeErrorType(errorTypeRef.reason)
        val resolvedTypeRef = resolveExplicitTypeRef(typeRef, scopeTypeParameters) as? CfirResolvedTypeRef
        return resolvedTypeRef?.coneType ?: ConeErrorType("Unresolved type: $typeRef")
    }
}

